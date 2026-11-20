package com.example.ui.components

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.data.WorkoutSet
import com.example.data.calculateSummary
import java.util.Locale

enum class SessionImageScope(val label: String) {
    TODAY("Hoy"),
    LATEST("Última sesión"),
    ALL("Todo"),
}

fun filterSetsForImageScope(
    allSets: List<WorkoutSet>,
    scope: SessionImageScope,
    todayIso: String,
): Pair<String, List<WorkoutSet>> {
    if (allSets.isEmpty()) return todayIso to emptyList()
    return when (scope) {
        SessionImageScope.TODAY -> {
            "Sesión del $todayIso" to allSets.filter { it.fecha == todayIso }
        }
        SessionImageScope.LATEST -> {
            val latestDate = allSets.maxOf { it.fecha }
            "Sesión del $latestDate" to allSets.filter { it.fecha == latestDate }
        }
        SessionImageScope.ALL -> {
            val minDate = allSets.minOf { it.fecha }
            val maxDate = allSets.maxOf { it.fecha }
            val label = if (minDate == maxDate) "Sesión del $maxDate" else "$minDate al $maxDate"
            label to allSets
        }
    }
}

fun renderSessionSummaryBitmap(
    context: Context,
    subtitleDate: String,
    sets: List<WorkoutSet>,
): Bitmap {
    val width = 1080
    val pad = 56f
    val cardInnerPad = 32f
    val maxDisplayedSets = 18

    val orderedSets = sets.sortedWith(
        compareBy<WorkoutSet> { it.fecha }
            .thenBy { it.grupo }
            .thenBy { it.ejercicio }
            .thenBy { it.serie }
    )
    val displayedSets = orderedSets.take(maxDisplayedSets)
    val extraCount = (orderedSets.size - displayedSets.size).coerceAtLeast(0)

    val summary = calculateSummary(orderedSets)
    val latestBodyWeight = orderedSets.lastOrNull { it.pesoCorporal != null }?.pesoCorporal

    val rowHeight = 76f
    val headerHeight = 220f
    val metricsBlockHeight = 256f
    val bodyWeightBannerHeight = if (latestBodyWeight != null) 70f else 0f
    val tableHeaderHeight = 64f
    val rowsHeight = (displayedSets.size.coerceAtLeast(1)) * rowHeight
    val extraNoteHeight = if (extraCount > 0) 48f else 0f
    val footerHeight = 96f

    val totalHeight = (
        pad + headerHeight +
            28f + metricsBlockHeight +
            (if (latestBodyWeight != null) 20f + bodyWeightBannerHeight else 0f) +
            28f + tableHeaderHeight + rowsHeight + extraNoteHeight +
            footerHeight + pad
        ).toInt().coerceAtLeast(780)

    val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val displayFont = runCatching {
        ResourcesCompat.getFont(context, R.font.space_grotesk)
    }.getOrNull() ?: Typeface.DEFAULT_BOLD
    val bodyFont = runCatching {
        ResourcesCompat.getFont(context, R.font.plus_jakarta_sans)
    }.getOrNull() ?: Typeface.DEFAULT
    val monoFont = runCatching {
        ResourcesCompat.getFont(context, R.font.jetbrains_mono)
    }.getOrNull() ?: Typeface.MONOSPACE

    // Background gradient
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f,
            0f,
            0f,
            totalHeight.toFloat(),
            Color.parseColor("#0B1422"),
            Color.parseColor("#111C2D"),
            Shader.TileMode.CLAMP,
        )
    }
    canvas.drawRect(0f, 0f, width.toFloat(), totalHeight.toFloat(), bgPaint)

    // Subtle top accent glow
    val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(38, 56, 189, 248)
    }
    canvas.drawCircle(width - 140f, 110f, 260f, glowPaint)

    // Header Card
    var y = pad
    val headerRect = RectF(pad, y, width - pad, y + headerHeight)
    val headerCardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            headerRect.left,
            headerRect.top,
            headerRect.right,
            headerRect.bottom,
            Color.parseColor("#1565C0"),
            Color.parseColor("#0D3B73"),
            Shader.TileMode.CLAMP,
        )
    }
    canvas.drawRoundRect(headerRect, 32f, 32f, headerCardPaint)

    val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(46, 255, 255, 255)
    }
    val badgeRect = RectF(pad + cardInnerPad, y + 30f, pad + cardInnerPad + 300f, y + 74f)
    canvas.drawRoundRect(badgeRect, 22f, 22f, badgePaint)

    val badgeTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E0F2FE")
        textSize = 24f
        typeface = Typeface.create(displayFont, Typeface.BOLD)
        letterSpacing = 0.08f
    }
    canvas.drawText("REGISTRO GYM", badgeRect.left + 22f, badgeRect.top + 31f, badgeTextPaint)

    val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 52f
        typeface = Typeface.create(displayFont, Typeface.BOLD)
    }
    canvas.drawText("Resumen de entrenamiento", pad + cardInnerPad, y + 140f, titlePaint)

    val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#BAE6FD")
        textSize = 30f
        typeface = Typeface.create(bodyFont, Typeface.NORMAL)
    }
    canvas.drawText(subtitleDate, pad + cardInnerPad, y + 188f, subtitlePaint)

    y += headerHeight + 28f

    // 4 Metric Tiles (2x2 grid)
    val tileGap = 20f
    val tileW = (width - pad * 2 - tileGap) / 2f
    val tileH = (metricsBlockHeight - tileGap) / 2f

    data class TileSpec(val label: String, val value: String, val accentHex: String)
    val tiles = listOf(
        TileSpec("SERIES", summary.totalSets.toString(), "#60A5FA"),
        TileSpec("REPETICIONES", summary.totalReps.toString(), "#FB923C"),
        TileSpec("VOLUMEN TOTAL", String.format(Locale.US, "%.1f kg", summary.totalVolumeKg), "#38BDF8"),
        TileSpec("MEJOR 1RM EST.", String.format(Locale.US, "%.1f kg", summary.bestEstimated1RmKg), "#4ADE80"),
    )

    val tileBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#182538")
    }
    val tileBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2A3B55")
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    val tileLabelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#94A3B8")
        textSize = 23f
        typeface = Typeface.create(bodyFont, Typeface.BOLD)
        letterSpacing = 0.06f
    }
    val tileValuePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 42f
        typeface = Typeface.create(displayFont, Typeface.BOLD)
    }

    tiles.forEachIndexed { idx, tile ->
        val col = idx % 2
        val row = idx / 2
        val left = pad + col * (tileW + tileGap)
        val top = y + row * (tileH + tileGap)
        val rect = RectF(left, top, left + tileW, top + tileH)
        canvas.drawRoundRect(rect, 24f, 24f, tileBgPaint)
        canvas.drawRoundRect(rect, 24f, 24f, tileBorderPaint)
        canvas.drawText(tile.label, left + 26f, top + 44f, tileLabelPaint)
        tileValuePaint.color = Color.parseColor(tile.accentHex)
        canvas.drawText(tile.value, left + 26f, top + 96f, tileValuePaint)
    }

    y += metricsBlockHeight

    if (latestBodyWeight != null) {
        y += 20f
        val bwRect = RectF(pad, y, width - pad, y + bodyWeightBannerHeight)
        canvas.drawRoundRect(bwRect, 20f, 20f, tileBgPaint)
        canvas.drawRoundRect(bwRect, 20f, 20f, tileBorderPaint)
        val bwPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0")
            textSize = 28f
            typeface = Typeface.create(bodyFont, Typeface.BOLD)
        }
        val bwNum = formatCleanKg(latestBodyWeight)
        canvas.drawText("Peso corporal registrado: $bwNum kg", pad + 26f, y + 45f, bwPaint)
        y += bodyWeightBannerHeight
    }

    y += 28f

    // Sets breakdown card
    val listCardHeight = tableHeaderHeight + rowsHeight + extraNoteHeight + 16f
    val listCardRect = RectF(pad, y, width - pad, y + listCardHeight)
    val listBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#141F30")
    }
    canvas.drawRoundRect(listCardRect, 26f, 26f, listBgPaint)
    canvas.drawRoundRect(listCardRect, 26f, 26f, tileBorderPaint)

    val sectionPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 30f
        typeface = Typeface.create(displayFont, Typeface.BOLD)
    }
    canvas.drawText("Detalle de series (${orderedSets.size})", pad + 28f, y + 44f, sectionPaint)

    val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#253449")
        strokeWidth = 2f
    }
    canvas.drawLine(pad + 24f, y + tableHeaderHeight, width - pad - 24f, y + tableHeaderHeight, dividerPaint)

    val exNamePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 26f
        typeface = Typeface.create(bodyFont, Typeface.BOLD)
    }
    val exSubPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#94A3B8")
        textSize = 22f
        typeface = Typeface.create(bodyFont, Typeface.NORMAL)
    }
    val exMetricPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#60A5FA")
        textSize = 26f
        typeface = Typeface.create(monoFont, Typeface.BOLD)
        textAlign = Paint.Align.RIGHT
    }
    val exRmPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4ADE80")
        textSize = 21f
        typeface = Typeface.create(monoFont, Typeface.NORMAL)
        textAlign = Paint.Align.RIGHT
    }

    var rowY = y + tableHeaderHeight
    if (displayedSets.isEmpty()) {
        canvas.drawText("Sin series registradas", pad + 28f, rowY + 46f, exSubPaint)
    } else {
        displayedSets.forEachIndexed { index, item ->
            if (index > 0) {
                canvas.drawLine(pad + 24f, rowY, width - pad - 24f, rowY, dividerPaint)
            }
            val leftText = "${item.ejercicio} · S${item.serie}"
            val ellipsizedLeft = TextUtils.ellipsize(
                leftText,
                exNamePaint,
                490f,
                TextUtils.TruncateAt.END,
            ).toString()
            canvas.drawText(ellipsizedLeft, pad + 28f, rowY + 33f, exNamePaint)

            val subLeft = "${item.grupo} · ${item.fecha} · RIR ${item.rir}"
            canvas.drawText(subLeft, pad + 28f, rowY + 61f, exSubPaint)

            val rightTop = "${formatCleanKg(item.peso)} kg × ${item.reps}"
            canvas.drawText(rightTop, width - pad - 28f, rowY + 33f, exMetricPaint)

            val rightBottom = "1RM ${String.format(Locale.US, "%.1f", item.estimated1RmKg)} kg"
            canvas.drawText(rightBottom, width - pad - 28f, rowY + 61f, exRmPaint)

            rowY += rowHeight
        }
    }

    if (extraCount > 0) {
        canvas.drawText(
            "+ $extraCount series adicionales (exporta CSV para ver todas)",
            pad + 28f,
            rowY + 32f,
            exSubPaint,
        )
    }

    // Footer
    val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#64748B")
        textSize = 22f
        typeface = Typeface.create(bodyFont, Typeface.NORMAL)
    }
    canvas.drawText(
        "Volumen = peso × reps · 1RM Epley = peso × (1 + reps / 30)",
        pad,
        totalHeight - pad + 10f,
        footerPaint,
    )

    return bitmap
}

fun saveSessionBitmapToGallery(
    context: Context,
    bitmap: Bitmap,
    fileName: String,
): Result<Unit> {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        return Result.failure(UnsupportedOperationException("Use SAF launcher on Android < 10"))
    }
    return runCatching {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/RegistroGym")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("No se pudo crear el archivo en la galería.")
        try {
            resolver.openOutputStream(uri)?.use { out ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                    "No se pudo codificar la imagen PNG."
                }
            } ?: error("No se pudo abrir el flujo de salida.")
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
    }
}

fun saveSessionBitmapToUri(
    context: Context,
    bitmap: Bitmap,
    uri: Uri,
): Result<Unit> {
    return runCatching {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                "No se pudo guardar la imagen PNG."
            }
        } ?: error("No se pudo abrir el destino seleccionado.")
    }
}

private fun formatCleanKg(value: Double): String {
    return if (value % 1.0 == 0.0) value.toInt().toString()
    else String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
}
