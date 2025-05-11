package dev.joetul.tao.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.joetul.tao.viewmodel.JournalViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import dev.joetul.tao.R
import androidx.compose.ui.res.stringResource

@Composable
fun ImportExportSection(
    journalViewModel: JournalViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isLoading by journalViewModel.isLoading.collectAsState()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            journalViewModel.exportData(it)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            journalViewModel.importData(it)
        }
    }

    Column(modifier = modifier) {
        Text(
            text = stringResource(id = R.string.section_data_management),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
        )

        // Export Data
        Surface(
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clickable {
                        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                        val timestamp = LocalDateTime.now().format(formatter)
                        exportLauncher.launch("meditation_journal_$timestamp.json")
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(id = R.string.option_export_data),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(id = R.string.option_export_data_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = {
                    val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                    val timestamp = LocalDateTime.now().format(formatter)
                    exportLauncher.launch("meditation_journal_$timestamp.json")
                }) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = stringResource(id = R.string.cd_export_data)
                    )
                }
            }
        }

        // Import Data
        Surface(
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clickable { importLauncher.launch(arrayOf("application/json")) },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(id = R.string.option_import_data),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(id = R.string.option_import_data_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = { importLauncher.launch(arrayOf("application/json")) }) {
                    Icon(
                        imageVector = Icons.Default.Upload,
                        contentDescription = stringResource(id = R.string.cd_import_data)
                    )
                }
            }
        }

        // Loading indicator
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}