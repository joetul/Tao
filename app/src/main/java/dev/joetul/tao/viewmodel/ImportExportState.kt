package dev.joetul.tao.viewmodel

// Define the ImportExportState sealed class
sealed class ImportExportState {
    data object Idle : ImportExportState()
    data object Loading : ImportExportState()
    data class Success(val message: String) : ImportExportState()
    data class Error(val message: String) : ImportExportState()
}