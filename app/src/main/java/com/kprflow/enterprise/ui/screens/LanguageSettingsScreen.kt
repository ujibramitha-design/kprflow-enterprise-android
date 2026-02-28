package com.kprflow.enterprise.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kprflow.enterprise.ui.components.LanguageCard
import com.kprflow.enterprise.ui.viewmodel.LanguageSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSettingsScreen(
    onBackClick: () -> Unit,
    viewModel: LanguageSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val languagesState by viewModel.languagesState.collectAsState()
    val currentLanguageState by viewModel.currentLanguageState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadLanguages()
        viewModel.loadCurrentLanguage()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Language Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(
                onClick = { viewModel.refreshData() }
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh"
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Current Language
        when (currentLanguageState) {
            is LanguageState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            is LanguageState.Success -> {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Current Language",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentLanguageState.currentLanguage.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            if (currentLanguageState.currentLanguage.isDefault) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = "Default",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            is LanguageState.Error -> {
                ErrorCard(
                    message = currentLanguageState.message,
                    onRetry = { viewModel.loadCurrentLanguage() }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Available Languages
        when (languagesState) {
            is LanguageState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            is LanguageState.Success -> {
                Text(
                    text = "Available Languages",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(languagesState.languages) { language ->
                        LanguageCard(
                            language = language,
                            isSelected = language.code == currentLanguageState.currentLanguage?.code,
                            onSelect = { viewModel.selectLanguage(language) }
                        )
                    }
                }
            }
            
            is LanguageState.Error -> {
                ErrorCard(
                    message = languagesState.message,
                    onRetry = { viewModel.loadLanguages() }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Translation Management (Admin only)
        if (viewModel.isAdmin()) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Translation Management",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.showTranslationManager() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Manage")
                        }
                        
                        OutlinedButton(
                            onClick = { viewModel.exportTranslations() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export")
                        }
                    }
                }
            }
        }
    }
    
    // Translation Manager Dialog
    if (uiState.showTranslationManager) {
        TranslationManagerDialog(
            onDismiss = { viewModel.hideTranslationManager() },
            onAddTranslation = { key, language, value, category ->
                viewModel.addTranslation(key, language, value, category)
            },
            onUpdateTranslation = { key, language, value ->
                viewModel.updateTranslation(key, language, value)
            },
            onDeleteTranslation = { key, language ->
                viewModel.deleteTranslation(key, language)
            }
        )
    }
    
    // Export Dialog
    if (uiState.showExportDialog) {
        ExportDialog(
            onDismiss = { viewModel.hideExportDialog() },
            onExport = { format ->
                viewModel.exportTranslations(format)
            }
        )
    }
}

@Composable
private fun ErrorCard(
    message: String,
    onRetry: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "❌ Error",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = onRetry
            ) {
                Text("Retry")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TranslationManagerDialog(
    onDismiss: () -> Unit,
    onAddTranslation: (String, String, String, String?) -> Unit,
    onUpdateTranslation: (String, String, String) -> Unit,
    onDeleteTranslation: (String, String) -> Unit
) {
    var key by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var showAddForm by remember { mutableStateOf(false) }
    
    val categories = listOf("common", "dashboard", "dossier", "document", "payment", "notification", "error", "validation")
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Translation Manager")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!showAddForm) {
                    // Search and filter options
                    OutlinedTextField(
                        value = key,
                        onValueChange = { key = it },
                        label = { Text("Search Key") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAddForm = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Add Translation")
                        }
                        
                        Button(
                            onClick = {
                                if (key.isNotBlank()) {
                                    onDeleteTranslation(key, language)
                                }
                            },
                            enabled = key.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Delete")
                        }
                    }
                } else {
                    // Add/Edit form
                    Text(
                        text = if (key.isBlank()) "Add Translation" else "Edit Translation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = key,
                        onValueChange = { key = it },
                        label = { Text("Key") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = key.isBlank()
                    )
                    
                    OutlinedTextField(
                        value = language,
                        onValueChange = { language = it },
                        label = { Text("Language") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        label = { Text("Value") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                    
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAddForm = false }
                        ) {
                            Text("Cancel")
                        }
                        
                        Button(
                            onClick = {
                                if (key.isNotBlank() && language.isNotBlank() && value.isNotBlank()) {
                                    if (key.isBlank()) {
                                        onAddTranslation(key, language, value, category.ifBlank { null })
                                    } else {
                                        onUpdateTranslation(key, language, value)
                                    }
                                    showAddForm = false
                                }
                            },
                            enabled = key.isNotBlank() && language.isNotBlank() && value.isNotBlank()
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportDialog(
    onDismiss: () -> Unit,
    onExport: (String) -> Unit
) {
    val formats = listOf("CSV", "JSON")
    var selectedFormat by remember { mutableStateOf(formats.first()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Export Translations")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Select export format:",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                formats.forEach { format ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedFormat == format,
                            onClick = { selectedFormat = format }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(format)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onExport(selectedFormat)
                    onDismiss()
                }
            ) {
                Text("Export")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// UI States
sealed class LanguageState<T> {
    object Loading : LanguageState<Nothing>()
    data class Success<T>(val data: T) : LanguageState<T>()
    data class Error(val message: String) : LanguageState<Nothing>()
}

data class LanguageSettingsUiState(
    val showTranslationManager: Boolean = false,
    val showExportDialog: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)
