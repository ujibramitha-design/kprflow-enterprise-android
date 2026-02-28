package com.kprflow.enterprise.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.clearSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

// Accessible Button with proper content description
@Composable
fun AccessibleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier.semantics {
            this.contentDescription = contentDescription ?: text
        },
        enabled = enabled
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
        }
        Text(text)
    }
}

// Accessible Text Field with proper labels and descriptions
@Composable
fun AccessibleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = placeholder?.let { { Text(it) } },
            isError = isError,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "$label field, current value: $value"
                },
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(
                onAny = {
                    onImeAction?.invoke()
                    keyboardController?.hide()
                }
            )
        )
        
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(start = 16.dp, top = 4.dp)
                    .semantics {
                        contentDescription = "Error: $errorMessage"
                    }
            )
        }
    }
}

// Accessible Card with proper content description
@Composable
fun AccessibleCard(
    title: String,
    subtitle: String? = null,
    contentDescription: String? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    Card(
        onClick = onClick ?: {},
        modifier = modifier.semantics {
            this.contentDescription = contentDescription ?: "$title${subtitle?.let { ", $it" } ?: ""}"
        },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.clearSemantics() // Clear to avoid duplicate reading
            )
            
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clearSemantics()
                )
            }
            
            content()
        }
    }
}

// Accessible Status Badge
@Composable
fun AccessibleStatusBadge(
    status: String,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {
    Badge(
        modifier = modifier.semantics {
            contentDescription = "Status: $status"
        }
    ) {
        Text(
            text = status,
            modifier = Modifier.clearSemantics()
        )
    }
}

// Accessible Progress Indicator
@Composable
fun AccessibleProgressIndicator(
    progress: Float,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.semantics {
            contentDescription = "$label: ${(progress * 100).toInt()} percent complete"
        }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.clearSemantics()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .clearSemantics()
        )
        
        Text(
            text = "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clearSemantics()
        )
    }
}

// Accessible Navigation Item
@Composable
fun AccessibleNavigationItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBarItem(
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.clearSemantics()
            )
        },
        label = {
            Text(
                text = label,
                modifier = Modifier.clearSemantics()
            )
        },
        selected = selected,
        onClick = onClick,
        modifier = modifier.semantics {
            contentDescription = if (selected) {
                "$label tab, currently selected"
            } else {
                "$label tab"
            }
        }
    )
}

// Accessible Alert Dialog
@Composable
fun AccessibleAlertDialog(
    title: String,
    text: String,
    onDismiss: () -> Unit,
    onConfirmation: () -> Unit,
    dismissText: String = "Cancel",
    confirmText: String = "OK",
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.semantics {
            contentDescription = "Dialog: $title. $text"
        },
        title = {
            Text(
                text = title,
                modifier = Modifier.clearSemantics()
            )
        },
        text = {
            Text(
                text = text,
                modifier = Modifier.clearSemantics()
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirmation,
                modifier = Modifier.semantics {
                    contentDescription = confirmText
                }
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.semantics {
                    contentDescription = dismissText
                }
            ) {
                Text(dismissText)
            }
        }
    )
}

// Accessible Loading State
@Composable
fun AccessibleLoadingState(
    message: String = "Loading",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = message
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.clearSemantics()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clearSemantics()
            )
        }
    }
}

// Accessible Error State
@Composable
fun AccessibleErrorState(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Error: $message"
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = androidx.compose.material.icons.Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.clearSemantics(),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.clearSemantics()
        )
        
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onRetry,
                modifier = Modifier.semantics {
                    contentDescription = "Retry"
                }
            ) {
                Text("Retry")
            }
        }
    }
}
