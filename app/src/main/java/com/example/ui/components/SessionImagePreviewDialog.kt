package com.example.ui.components

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.WorkoutSet
import com.example.ui.currentTodayIsoDate

@Composable
fun SessionImagePreviewDialog(
    allSets: List<WorkoutSet>,
    initialScope: SessionImageScope = SessionImageScope.LATEST,
    onDismiss: () -> Unit,
    onSaveToGallery: (Bitmap, String) -> Unit,
    onSaveWithPicker: (Bitmap, String) -> Unit,
) {
    val context = LocalContext.current
    val today = remember { currentTodayIsoDate() }
    var selectedScope by remember { mutableStateOf(initialScope) }

    val (subtitleDate, scopedSets) = remember(allSets, selectedScope, today) {
        filterSetsForImageScope(allSets, selectedScope, today)
    }

    val previewBitmap = remember(scopedSets, subtitleDate) {
        renderSessionSummaryBitmap(
            context = context,
            subtitleDate = subtitleDate,
            sets = scopedSets,
        )
    }

    val suggestedFileName = remember(selectedScope, today) {
        when (selectedScope) {
            SessionImageScope.TODAY -> "registro_gym_$today.png"
            SessionImageScope.LATEST -> "registro_gym_ultima_sesion.png"
            SessionImageScope.ALL -> "registro_gym_resumen_total.png"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_image_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SessionImageScope.entries.forEach { scope ->
                        FilterChip(
                            selected = selectedScope == scope,
                            onClick = { selectedScope = scope },
                            label = { Text(scope.label) },
                            modifier = Modifier.testTag("image_scope_${scope.name.lowercase()}"),
                        )
                    }
                }

                if (scopedSets.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            text = stringResource(R.string.msg_image_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Image(
                            bitmap = previewBitmap.asImageBitmap(),
                            contentDescription = stringResource(R.string.cd_session_image_preview),
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium),
                        )
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && scopedSets.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { onSaveWithPicker(previewBitmap, suggestedFileName) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_image_picker_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_save_as))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        onSaveToGallery(previewBitmap, suggestedFileName)
                    } else {
                        onSaveWithPicker(previewBitmap, suggestedFileName)
                    }
                },
                enabled = scopedSets.isNotEmpty(),
                modifier = Modifier.testTag("save_image_confirm_button"),
            ) {
                Icon(
                    imageVector = Icons.Filled.PhotoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.btn_save_gallery))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        },
    )
}
