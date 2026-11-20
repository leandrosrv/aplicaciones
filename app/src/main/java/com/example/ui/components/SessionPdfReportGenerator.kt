package com.example.ui.components

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.TextPaint
import android.text.TextUtils
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.data.ProgressMetric
import com.example.data.TimeGranularity
import com.example.data.WorkoutSet
import com.example.data.calculateSummary
import com.example.ui.ALL_GROUPS_CHART
import com.example.ui.ChartModel
import com.example.ui.buildChartModel
import com.example.ui.buildExercisePrs
import com.example.ui.theme.ChartSeriesColors
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor

data class PdfReportSpec(
    val scope: SessionImageScope,
    val scopeSubtitle: String,
    val scopedSets: List<WorkoutSet>,
    val allSetsForCharts: List<WorkoutSet>,
    val chartGroup: String,
    val granularity: TimeGranularity,
    val chartMetrics: List<ProgressMetric>,
)

fun buildShareReportSummaryText(spec: PdfReportSpec): String {
    val summary = calculateSummary(spec.scopedSets)
    val groups = spec.scopedSets.map { it.grupo }.distinct().joinToString(", ")
    return buildString {
        appendLine("Informe de entrenamiento - Registro Gym")
        appendLine("${spec.scopeSubtitle}")
        if (groups.isNotEmpty()) {
            appendLine("Grupos: $groups")
        }
        appendLine(
            "Series: ${summary.totalSets} | Reps: ${summary.totalReps} | " +
                "Volumen: ${String.format(Locale.US, "%.1f", summary.totalVolumeKg)} kg | " +
                "Mejor 1RM est.: ${String.format(Locale.US, "%.1f", summary.bestEstimated1RmKg)} kg"
        )
        append("Adjunto informe completo en PDF con detalle de series y gráficos.")
    }
}

fun saveReportPdfToDownloads(
    context: Context,
    spec: PdfReportSpec,
    fileName: String,
): Result<Unit> {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        return Result.failure(UnsupportedOperationException("Use SAF launcher on Android < 10"))
    }
    return runCatching {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/RegistroGym")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("No se pudo crear el archivo PDF en Descargas.")
        try {
            resolver.openOutputStream(uri)?.use { out ->
                renderAndWritePdf(context, spec, out)
            } ?: error("No se pudo abrir el flujo de salida.")
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
    }
}

fun saveReportPdfToUri(
    context: Context,
    spec: PdfReportSpec,
    uri: Uri,
): Result<Unit> {
    return runCatching {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            renderAndWritePdf(context, spec, out)
        } ?: error("No se pudo abrir el destino seleccionado.")
    }
}

fun createShareablePdfUri(
    context: Context,
    spec: PdfReportSpec,
    fileName: String,
): Result<Uri> {
    return runCatching {
        val dir = File(context.cacheDir, "reports").apply {
            mkdirs()
            listFiles()?.forEach { old ->
                if (old.isFile && old.name.endsWith(".pdf")) {
                    old.delete()
                }
            }
        }
        val target = File(dir, fileName)
        FileOutputStream(target).use { out ->
            renderAndWritePdf(context, spec, out)
        }
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            target,
        )
    }
}

fun launchSharePdfIntent(
    context: Context,
    pdfUri: Uri,
    subject: String,
    bodyText: String,
) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, pdfUri)
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, bodyText)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(
        sendIntent,
        context.getString(R.string.share_pdf_chooser),
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
}

fun renderAndWritePdf(
    context: Context,
    spec: PdfReportSpec,
    outputStream: OutputStream,
) {
    val renderer = SessionPdfRenderer(context, spec)
    renderer.writeTo(outputStream)
}

private class SessionPdfRenderer(
    private val context: Context,
    private val spec: PdfReportSpec,
) {
    private val pageWidth = 595
    private val pageHeight = 842
    private val margin = 34f
    private val contentWidth = pageWidth - margin * 2f
    private val footerReserve = 34f
    private val maxY = pageHeight - margin - footerReserve

    private val displayFont = runCatching {
        ResourcesCompat.getFont(context, R.font.space_grotesk)
    }.getOrNull() ?: Typeface.DEFAULT_BOLD
    private val bodyFont = runCatching {
        ResourcesCompat.getFont(context, R.font.plus_jakarta_sans)
    }.getOrNull() ?: Typeface.DEFAULT
    private val monoFont = runCatching {
        ResourcesCompat.getFont(context, R.font.jetbrains_mono)
    }.getOrNull() ?: Typeface.MONOSPACE

    private val pdfDocument = PdfDocument()
    private var currentPage: PdfDocument.Page? = null
    private var pageNum = 0
    private var y = margin

    private val colSlate900 = Color.parseColor("#0F172A")
    private val colSlate600 = Color.parseColor("#475569")
    private val colSlate400 = Color.parseColor("#94A3B8")
    private val colBorder = Color.parseColor("#CBD5E1")
    private val colDivider = Color.parseColor("#E2E8F0")
    private val colTintBg = Color.parseColor("#F8FAFC")
    private val colCobalt = Color.parseColor("#1565C0")
    private val colCobaltDark = Color.parseColor("#0D3B73")
    private val colCobaltSoft = Color.parseColor("#EFF6FF")
    private val colEmber = Color.parseColor("#D84315")
    private val colEmerald = Color.parseColor("#14833B")

    fun writeTo(outputStream: OutputStream) {
        try {
            startNewPage()
            drawHeaderBanner()
            drawSummarySection()
            drawGroupBreakdownSection()
            drawExercisePrSection()
            drawChartsSection()
            drawSetsTableSection()
            finishCurrentPage()
            pdfDocument.writeTo(outputStream)
        } finally {
            pdfDocument.close()
        }
    }

    private fun startNewPage() {
        finishCurrentPage()
        pageNum += 1
        val info = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
        currentPage = pdfDocument.startPage(info)
        y = margin
    }

    private fun finishCurrentPage() {
        val page = currentPage ?: return
        drawFooter(page.canvas)
        pdfDocument.finishPage(page)
        currentPage = null
    }

    private fun canvas(): Canvas = checkNotNull(currentPage?.canvas)

    private fun ensureSpace(needed: Float, onPageBreak: (() -> Unit)? = null) {
        if (y + needed > maxY) {
            startNewPage()
            onPageBreak?.invoke()
        }
    }

    private fun drawFooter(c: Canvas) {
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colDivider
            strokeWidth = 0.8f
        }
        val footerTop = pageHeight - margin - 18f
        c.drawLine(margin, footerTop, pageWidth - margin, footerTop, linePaint)

        val leftPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colSlate600
            textSize = 8f
            typeface = bodyFont
        }
        val rightPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colSlate600
            textSize = 8f
            typeface = Typeface.create(monoFont, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        c.drawText(
            "Registro Gym · Volumen = peso × reps · 1RM Epley = peso × (1 + reps / 30)",
            margin,
            footerTop + 12f,
            leftPaint,
        )
        c.drawText(
            "Página $pageNum",
            pageWidth - margin,
            footerTop + 12f,
            rightPaint,
        )
    }

    private fun drawHeaderBanner() {
        val c = canvas()
        val bannerH = 92f
        val rect = RectF(margin, y, pageWidth - margin, y + bannerH)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                rect.left,
                rect.top,
                rect.right,
                rect.bottom,
                colCobalt,
                colCobaltDark,
                Shader.TileMode.CLAMP,
            )
        }
        c.drawRoundRect(rect, 12f, 12f, bgPaint)

        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(46, 255, 255, 255)
        }
        val badgeRect = RectF(margin + 16f, y + 12f, margin + 178f, y + 29f)
        c.drawRoundRect(badgeRect, 8f, 8f, badgePaint)

        val badgeText = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E0F2FE")
            textSize = 8.5f
            typeface = Typeface.create(displayFont, Typeface.BOLD)
            letterSpacing = 0.06f
        }
        c.drawText("REGISTRO GYM · INFORME", badgeRect.left + 10f, badgeRect.top + 12f, badgeText)

        val generatedAt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        val genPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#DBEAFE")
            textSize = 8.5f
            typeface = bodyFont
            textAlign = Paint.Align.RIGHT
        }
        c.drawText("Generado: $generatedAt", rect.right - 16f, y + 23f, genPaint)

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 20f
            typeface = Typeface.create(displayFont, Typeface.BOLD)
        }
        c.drawText("Informe de Entrenamiento", margin + 16f, y + 54f, titlePaint)

        val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E0F2FE")
            textSize = 10.5f
            typeface = bodyFont
        }
        val subLine = "${spec.scopeSubtitle} · Gráficos: ${spec.chartGroup} (${spec.granularity.label})"
        val subEllip = TextUtils.ellipsize(subLine, subPaint, contentWidth - 32f, TextUtils.TruncateAt.END).toString()
        c.drawText(subEllip, margin + 16f, y + 74f, subPaint)

        y += bannerH + 14f
    }

    private fun drawSummarySection() {
        val orderedSets = spec.scopedSets.sortedWith(
            compareBy<WorkoutSet> { it.fecha }
                .thenBy { it.createdAtMillis }
                .thenBy { it.serie }
        )
        val summary = calculateSummary(orderedSets)
        val latestBw = orderedSets.lastOrNull { it.pesoCorporal != null }?.pesoCorporal

        val tileH = 52f
        val extraBwH = if (latestBw != null) 26f else 0f
        ensureSpace(tileH + extraBwH + 14f)

        val c = canvas()
        val gap = 8f
        val tileW = (contentWidth - gap * 3f) / 4f

        data class Kpi(val label: String, val value: String, val accent: Int)
        val kpis = listOf(
            Kpi("SERIES", summary.totalSets.toString(), colCobalt),
            Kpi("REPETICIONES", summary.totalReps.toString(), colEmber),
            Kpi("VOLUMEN TOTAL", "${formatNum(summary.totalVolumeKg)} kg", colCobalt),
            Kpi("MEJOR 1RM EST.", String.format(Locale.US, "%.1f kg", summary.bestEstimated1RmKg), colEmerald),
        )

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colTintBg }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colBorder
            style = Paint.Style.STROKE
            strokeWidth = 0.9f
        }
        val lblPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colSlate600
            textSize = 7.8f
            typeface = Typeface.create(bodyFont, Typeface.BOLD)
            letterSpacing = 0.05f
        }
        val valPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 14.5f
            typeface = Typeface.create(displayFont, Typeface.BOLD)
        }

        kpis.forEachIndexed { idx, kpi ->
            val left = margin + idx * (tileW + gap)
            val rect = RectF(left, y, left + tileW, y + tileH)
            c.drawRoundRect(rect, 8f, 8f, bgPaint)
            c.drawRoundRect(rect, 8f, 8f, borderPaint)
            c.drawText(kpi.label, left + 10f, y + 18f, lblPaint)
            valPaint.color = kpi.accent
            c.drawText(kpi.value, left + 10f, y + 40f, valPaint)
        }
        y += tileH

        if (latestBw != null) {
            y += 6f
            val bwRect = RectF(margin, y, pageWidth - margin, y + 20f)
            val softPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colCobaltSoft }
            c.drawRoundRect(bwRect, 6f, 6f, softPaint)
            val bwPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colCobaltDark
                textSize = 9.5f
                typeface = Typeface.create(bodyFont, Typeface.BOLD)
            }
            c.drawText(
                "Peso corporal registrado en el período: ${formatNum(latestBw)} kg",
                margin + 10f,
                y + 13.5f,
                bwPaint,
            )
            y += 20f
        }
        y += 14f
    }

    private fun drawGroupBreakdownSection() {
        val grouped = spec.scopedSets.groupBy { it.grupo }
        if (grouped.isEmpty()) return

        val entries = grouped.entries.map { (group, sets) ->
            val s = calculateSummary(sets)
            GroupSummaryRow(
                group = group,
                sets = s.totalSets,
                reps = s.totalReps,
                volume = s.totalVolumeKg,
                best1Rm = s.bestEstimated1RmKg,
            )
        }.sortedByDescending { it.volume }

        val rowH = 18f
        val headerH = 22f
        val sectionTitleH = 20f
        val needed = sectionTitleH + headerH + entries.size * rowH + 12f
        ensureSpace(needed.coerceAtMost(140f))

        drawSectionHeading("Resumen por grupo muscular")

        val cols = listOf(165f, 70f, 85f, 105f, 102f)
        val headers = listOf("Grupo", "Series", "Reps", "Volumen (kg)", "Mejor 1RM (kg)")
        val aligns = listOf(Paint.Align.LEFT, Paint.Align.RIGHT, Paint.Align.RIGHT, Paint.Align.RIGHT, Paint.Align.RIGHT)

        fun drawHeaderBar() {
            val c = canvas()
            val rect = RectF(margin, y, pageWidth - margin, y + headerH)
            val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colCobaltSoft }
            c.drawRoundRect(rect, 5f, 5f, bg)
            val hp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colCobaltDark
                textSize = 8.5f
                typeface = Typeface.create(bodyFont, Typeface.BOLD)
            }
            var x = margin
            headers.forEachIndexed { idx, title ->
                hp.textAlign = aligns[idx]
                val drawX = if (aligns[idx] == Paint.Align.LEFT) x + 8f else x + cols[idx] - 8f
                c.drawText(title, drawX, y + 14f, hp)
                x += cols[idx]
            }
            y += headerH
        }

        drawHeaderBar()

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colSlate900
            textSize = 9f
            typeface = bodyFont
        }
        val numPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colSlate900
            textSize = 8.8f
            typeface = monoFont
            textAlign = Paint.Align.RIGHT
        }
        val zebraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colTintBg }
        val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colDivider
            strokeWidth = 0.6f
        }

        entries.forEachIndexed { idx, row ->
            ensureSpace(rowH + 6f) { drawHeaderBar() }
            val c = canvas()
            if (idx % 2 == 1) {
                c.drawRect(margin, y, pageWidth - margin, y + rowH, zebraPaint)
            }
            var x = margin
            c.drawText(
                TextUtils.ellipsize(row.group, textPaint, cols[0] - 14f, TextUtils.TruncateAt.END).toString(),
                x + 8f,
                y + 12.5f,
                textPaint,
            )
            x += cols[0]
            c.drawText(row.sets.toString(), x + cols[1] - 8f, y + 12.5f, numPaint)
            x += cols[1]
            c.drawText(row.reps.toString(), x + cols[2] - 8f, y + 12.5f, numPaint)
            x += cols[2]
            c.drawText(String.format(Locale.US, "%.1f", row.volume), x + cols[3] - 8f, y + 12.5f, numPaint)
            x += cols[3]
            c.drawText(String.format(Locale.US, "%.1f", row.best1Rm), x + cols[4] - 8f, y + 12.5f, numPaint)
            c.drawLine(margin, y + rowH, pageWidth - margin, y + rowH, divPaint)
            y += rowH
        }
        y += 14f
    }

    private fun drawExercisePrSection() {
        val prs = buildExercisePrs(spec.scopedSets, ALL_GROUPS_CHART).take(10)
        if (prs.isEmpty()) return

        val rowH = 18f
        val headerH = 22f
        ensureSpace(20f + headerH + minOf(prs.size, 4) * rowH)

        drawSectionHeading("Marcas destacadas de la sesión")

        val cols = listOf(225f, 75f, 110f, 117f)
        val headers = listOf("Ejercicio", "Series", "Peso máx. (kg)", "Mejor 1RM (kg)")
        val aligns = listOf(Paint.Align.LEFT, Paint.Align.RIGHT, Paint.Align.RIGHT, Paint.Align.RIGHT)

        fun drawHeaderBar() {
            val c = canvas()
            val rect = RectF(margin, y, pageWidth - margin, y + headerH)
            val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colCobaltSoft }
            c.drawRoundRect(rect, 5f, 5f, bg)
            val hp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colCobaltDark
                textSize = 8.5f
                typeface = Typeface.create(bodyFont, Typeface.BOLD)
            }
            var x = margin
            headers.forEachIndexed { idx, title ->
                hp.textAlign = aligns[idx]
                val drawX = if (aligns[idx] == Paint.Align.LEFT) x + 8f else x + cols[idx] - 8f
                c.drawText(title, drawX, y + 14f, hp)
                x += cols[idx]
            }
            y += headerH
        }

        drawHeaderBar()

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colSlate900
            textSize = 9f
            typeface = bodyFont
        }
        val numPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colSlate900
            textSize = 8.8f
            typeface = monoFont
            textAlign = Paint.Align.RIGHT
        }
        val rmPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colEmerald
            textSize = 8.8f
            typeface = Typeface.create(monoFont, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val zebraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colTintBg }
        val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colDivider
            strokeWidth = 0.6f
        }

        prs.forEachIndexed { idx, pr ->
            ensureSpace(rowH + 6f) { drawHeaderBar() }
            val c = canvas()
            if (idx % 2 == 1) {
                c.drawRect(margin, y, pageWidth - margin, y + rowH, zebraPaint)
            }
            var x = margin
            c.drawText(
                TextUtils.ellipsize(pr.exerciseName, textPaint, cols[0] - 14f, TextUtils.TruncateAt.END).toString(),
                x + 8f,
                y + 12.5f,
                textPaint,
            )
            x += cols[0]
            c.drawText(pr.totalSets.toString(), x + cols[1] - 8f, y + 12.5f, numPaint)
            x += cols[1]
            c.drawText(formatNum(pr.maxWeightKg), x + cols[2] - 8f, y + 12.5f, numPaint)
            x += cols[2]
            c.drawText(String.format(Locale.US, "%.1f", pr.best1RmKg), x + cols[3] - 8f, y + 12.5f, rmPaint)
            c.drawLine(margin, y + rowH, pageWidth - margin, y + rowH, divPaint)
            y += rowH
        }
        y += 14f
    }

    private fun drawChartsSection() {
        if (spec.chartMetrics.isEmpty()) return

        ensureSpace(220f)
        drawSectionHeading(
            "Gráficos de evolución · ${spec.chartGroup} (${spec.granularity.label})"
        )

        spec.chartMetrics.forEach { metric ->
            val model = buildChartModel(
                allSets = spec.allSetsForCharts,
                group = spec.chartGroup,
                metric = metric,
                granularity = spec.granularity,
            )
            if (model.buckets.isEmpty() || model.series.isEmpty()) {
                ensureSpace(40f)
                drawEmptyChartBox(metric)
            } else {
                val legendRows = estimateLegendRows(model)
                val cardH = 182f + legendRows * 13f
                ensureSpace(cardH + 10f)
                drawVectorChartCard(model, cardH)
            }
        }
    }

    private fun estimateLegendRows(model: ChartModel): Int {
        if (model.series.isEmpty()) return 0
        val maxItemsPerLine = 3
        return ((model.series.size + maxItemsPerLine - 1) / maxItemsPerLine).coerceAtLeast(1)
    }

    private fun drawEmptyChartBox(metric: ProgressMetric) {
        val c = canvas()
        val rect = RectF(margin, y, pageWidth - margin, y + 32f)
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colTintBg }
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colBorder
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }
        c.drawRoundRect(rect, 8f, 8f, bg)
        c.drawRoundRect(rect, 8f, 8f, border)
        val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colSlate600
            textSize = 9f
            typeface = bodyFont
        }
        c.drawText(
            "${metric.label}: sin datos suficientes para graficar en esta selección.",
            margin + 12f,
            y + 19f,
            tp,
        )
        y += 40f
    }

    private fun drawVectorChartCard(model: ChartModel, cardH: Float) {
        val c = canvas()
        val rect = RectF(margin, y, pageWidth - margin, y + cardH)
        val cardBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val cardBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colBorder
            style = Paint.Style.STROKE
            strokeWidth = 0.9f
        }
        c.drawRoundRect(rect, 9f, 9f, cardBg)
        c.drawRoundRect(rect, 9f, 9f, cardBorder)

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colSlate900
            textSize = 11f
            typeface = Typeface.create(displayFont, Typeface.BOLD)
        }
        val unitPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colSlate600
            textSize = 8.5f
            typeface = bodyFont
            textAlign = Paint.Align.RIGHT
        }
        c.drawText(model.metric.label, margin + 12f, y + 18f, titlePaint)
        c.drawText(model.metric.yAxisLabel, pageWidth - margin - 12f, y + 18f, unitPaint)

        // Legend
        var legendY = y + 32f
        val legendPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colSlate900
            textSize = 8f
            typeface = bodyFont
        }
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val colWidth = (contentWidth - 24f) / 3f
        model.series.forEachIndexed { idx, series ->
            val col = idx % 3
            val row = idx / 3
            val cx = margin + 12f + col * colWidth
            val cy = legendY + row * 13f
            dotPaint.color = ChartSeriesColors[idx % ChartSeriesColors.size].toArgb()
            c.drawCircle(cx + 3.5f, cy - 3f, 3.5f, dotPaint)
            val ell = TextUtils.ellipsize(series.name, legendPaint, colWidth - 16f, TextUtils.TruncateAt.END).toString()
            c.drawText(ell, cx + 10f, cy, legendPaint)
        }

        val legendRows = estimateLegendRows(model)
        val plotTop = y + 34f + legendRows * 13f + 4f
        val plotBottom = y + cardH - 22f
        val plotLeft = margin + 44f
        val plotRight = pageWidth - margin - 16f
        val plotW = (plotRight - plotLeft).coerceAtLeast(10f)
        val plotH = (plotBottom - plotTop).coerceAtLeast(20f)

        val allValues = model.series.flatMap { it.values.filterNotNull() }
        val rawMin = allValues.minOrNull() ?: 0.0
        val rawMax = allValues.maxOrNull() ?: 10.0
        val range = (rawMax - rawMin).let { if (it < 1e-6) maxOf(1.0, rawMax * 0.2) else it }
        val minY = floor((rawMin - range * 0.12).coerceAtLeast(0.0))
        val maxY = ceil(rawMax + range * 0.12)
        val ySpan = (maxY - minY).coerceAtLeast(1.0)

        // Y grid + tick labels
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colDivider
            strokeWidth = 0.7f
            pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f)
        }
        val baseLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colSlate400
            strokeWidth = 0.9f
        }
        val yTickPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colSlate600
            textSize = 7.8f
            typeface = monoFont
            textAlign = Paint.Align.RIGHT
        }

        val ticks = 4
        for (i in 0..ticks) {
            val gy = plotTop + plotH * (1f - i.toFloat() / ticks)
            val valTick = minY + ySpan * i / ticks
            c.drawText(formatAxisTick(valTick), plotLeft - 6f, gy + 3f, yTickPaint)
            c.drawLine(plotLeft, gy, plotRight, gy, if (i == 0) baseLinePaint else gridPaint)
        }

        fun xFor(idx: Int): Float {
            return if (model.buckets.size <= 1) {
                plotLeft + plotW / 2f
            } else {
                plotLeft + plotW * (idx.toFloat() / (model.buckets.size - 1))
            }
        }

        fun yFor(v: Double): Float {
            val norm = ((v - minY) / ySpan).toFloat().coerceIn(0f, 1f)
            return plotTop + plotH * (1f - norm)
        }

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val pointRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val pointFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        model.series.forEachIndexed { sIdx, series ->
            val argb = ChartSeriesColors[sIdx % ChartSeriesColors.size].toArgb()
            linePaint.color = argb
            pointFillPaint.color = argb

            val path = Path()
            var started = false
            series.values.forEachIndexed { bIdx, value ->
                if (value == null) {
                    started = false
                } else {
                    val px = xFor(bIdx)
                    val py = yFor(value)
                    if (!started) {
                        path.moveTo(px, py)
                        started = true
                    } else {
                        path.lineTo(px, py)
                    }
                }
            }
            c.drawPath(path, linePaint)

            series.values.forEachIndexed { bIdx, value ->
                if (value != null) {
                    val px = xFor(bIdx)
                    val py = yFor(value)
                    c.drawCircle(px, py, 3.6f, pointRingPaint)
                    c.drawCircle(px, py, 2.5f, pointFillPaint)
                }
            }
        }

        // X axis labels (up to 5 evenly spaced)
        val xLabelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colSlate600
            textSize = 7.6f
            typeface = monoFont
            textAlign = Paint.Align.CENTER
        }
        val indicesToShow = when {
            model.buckets.size <= 5 -> model.buckets.indices.toList()
            else -> listOf(
                0,
                model.buckets.size / 4,
                model.buckets.size / 2,
                (model.buckets.size * 3) / 4,
                model.buckets.lastIndex,
            ).distinct()
        }
        indicesToShow.forEach { bIdx ->
            val label = model.buckets[bIdx].axisLabel
            val xPos = xFor(bIdx).coerceIn(plotLeft + 16f, plotRight - 16f)
            c.drawText(label, xPos, plotBottom + 13f, xLabelPaint)
        }

        y += cardH + 10f
    }

    private fun drawSetsTableSection() {
        val orderedSets = spec.scopedSets.sortedWith(
            compareBy<WorkoutSet> { it.fecha }
                .thenBy { it.grupo }
                .thenBy { it.createdAtMillis }
                .thenBy { it.serie }
        )
        if (orderedSets.isEmpty()) return

        ensureSpace(64f)
        drawSectionHeading("Detalle de series registradas (${orderedSets.size})")

        // Total = 527f
        val cols = listOf(58f, 62f, 138f, 26f, 48f, 34f, 26f, 65f, 70f)
        val headers = listOf("Fecha", "Grupo", "Ejercicio", "Ser", "Peso", "Reps", "RIR", "Volumen", "1RM Est.")
        val aligns = listOf(
            Paint.Align.LEFT,
            Paint.Align.LEFT,
            Paint.Align.LEFT,
            Paint.Align.RIGHT,
            Paint.Align.RIGHT,
            Paint.Align.RIGHT,
            Paint.Align.RIGHT,
            Paint.Align.RIGHT,
            Paint.Align.RIGHT,
        )
        val headerH = 21f

        fun drawTableHeader() {
            val c = canvas()
            val rect = RectF(margin, y, pageWidth - margin, y + headerH)
            val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colCobaltDark }
            c.drawRoundRect(rect, 5f, 5f, bg)
            val hp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 8f
                typeface = Typeface.create(bodyFont, Typeface.BOLD)
            }
            var x = margin
            headers.forEachIndexed { idx, title ->
                hp.textAlign = aligns[idx]
                val drawX = if (aligns[idx] == Paint.Align.LEFT) x + 5f else x + cols[idx] - 5f
                c.drawText(title, drawX, y + 13.5f, hp)
                x += cols[idx]
            }
            y += headerH
        }

        drawTableHeader()

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colSlate900
            textSize = 8.4f
            typeface = bodyFont
        }
        val notePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colSlate600
            textSize = 7.6f
            typeface = bodyFont
        }
        val monoLeftPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colSlate600
            textSize = 8f
            typeface = monoFont
        }
        val monoRightPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colSlate900
            textSize = 8.2f
            typeface = monoFont
            textAlign = Paint.Align.RIGHT
        }
        val rmRightPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colEmerald
            textSize = 8.2f
            typeface = Typeface.create(monoFont, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val zebraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colTintBg }
        val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colDivider
            strokeWidth = 0.5f
        }

        orderedSets.forEachIndexed { idx, item ->
            val hasNote = !item.obs.isNullOrBlank()
            val rowH = if (hasNote) 29f else 18f
            ensureSpace(rowH + 4f) { drawTableHeader() }

            val c = canvas()
            if (idx % 2 == 1) {
                c.drawRect(margin, y, pageWidth - margin, y + rowH, zebraPaint)
            }
            var x = margin
            c.drawText(item.fecha, x + 4f, y + 12.5f, monoLeftPaint)
            x += cols[0]

            val grpEllip = TextUtils.ellipsize(item.grupo, textPaint, cols[1] - 8f, TextUtils.TruncateAt.END).toString()
            c.drawText(grpEllip, x + 4f, y + 12.5f, textPaint)
            x += cols[1]

            val exEllip = TextUtils.ellipsize(item.ejercicio, textPaint, cols[2] - 8f, TextUtils.TruncateAt.END).toString()
            c.drawText(exEllip, x + 4f, y + 12.5f, textPaint)
            x += cols[2]

            c.drawText(item.serie.toString(), x + cols[3] - 4f, y + 12.5f, monoRightPaint)
            x += cols[3]

            c.drawText("${formatNum(item.peso)} kg", x + cols[4] - 4f, y + 12.5f, monoRightPaint)
            x += cols[4]

            c.drawText(item.reps.toString(), x + cols[5] - 4f, y + 12.5f, monoRightPaint)
            x += cols[5]

            c.drawText(item.rir.toString(), x + cols[6] - 4f, y + 12.5f, monoRightPaint)
            x += cols[6]

            c.drawText("${formatNum(item.volumeKg)} kg", x + cols[7] - 4f, y + 12.5f, monoRightPaint)
            x += cols[7]

            c.drawText(String.format(Locale.US, "%.1f kg", item.estimated1RmKg), x + cols[8] - 4f, y + 12.5f, rmRightPaint)

            if (hasNote) {
                val noteEllip = TextUtils.ellipsize(
                    "Nota: ${item.obs}",
                    notePaint,
                    contentWidth - 16f,
                    TextUtils.TruncateAt.END,
                ).toString()
                c.drawText(noteEllip, margin + 8f, y + 24f, notePaint)
            }

            c.drawLine(margin, y + rowH, pageWidth - margin, y + rowH, divPaint)
            y += rowH
        }
    }

    private fun drawSectionHeading(title: String) {
        val c = canvas()
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colSlate900
            textSize = 12.5f
            typeface = Typeface.create(displayFont, Typeface.BOLD)
        }
        c.drawText(title, margin, y + 12f, paint)
        y += 19f
    }

    private data class GroupSummaryRow(
        val group: String,
        val sets: Int,
        val reps: Long,
        val volume: Double,
        val best1Rm: Double,
    )

    private fun formatNum(value: Double): String {
        return if (value % 1.0 == 0.0) value.toInt().toString()
        else String.format(Locale.US, "%.1f", value)
    }

    private fun formatAxisTick(v: Double): String {
        return if (v >= 1000.0) {
            String.format(Locale.US, "%.1fk", v / 1000.0)
        } else if (v % 1.0 == 0.0) {
            v.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", v)
        }
    }
}
