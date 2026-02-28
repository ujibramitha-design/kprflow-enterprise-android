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
import androidx.navigation.NavController
import com.kprflow.enterprise.ui.components.*
import com.kprflow.enterprise.ui.theme.KPRFlowEnterpriseTheme
import com.kprflow.enterprise.viewmodel.ProductTypeSelectionViewModel

/**
 * Product Type Selection Screen
 * Customer & Marketing access for selecting product types
 * Phase 16: Mobile App Optimization - Product Types System
 */

@Composable
fun ProductTypeSelectionScreen(
    navController: NavController,
    viewModel: ProductTypeSelectionViewModel,
    unitId: String
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedProduct by remember { mutableStateOf<ProductTypeData?>(null) }
    var showProductDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(unitId) {
        viewModel.loadProductTypes(unitId)
    }
    
    KPRFlowEnterpriseTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Pilih Jenis Pengajuan") },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.refreshData() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Unit Information
                when (uiState) {
                    is ProductTypeSelectionUiState.Success -> {
                        UnitInfoCard(unit = uiState.unitInfo)
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Product Types List
                        Text(
                            text = "Jenis Pengajuan Tersedia:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.productTypes) { product ->
                                ProductTypeCard(
                                    product = product,
                                    unitPrice = uiState.unitInfo.price,
                                    onSelectProduct = {
                                        selectedProduct = product
                                        showProductDialog = true
                                    },
                                    onProceed = { productId ->
                                        navController.navigate("dossier_creation/$unitId/$productId")
                                    }
                                )
                            }
                        }
                    }
                    is ProductTypeSelectionUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is ProductTypeSelectionUiState.Error -> {
                        ErrorMessage(
                            message = uiState.message,
                            onRetry = { viewModel.refreshData() }
                        )
                    }
                }
            }
        }
        
        // Product Details Dialog
        if (showProductDialog && selectedProduct != null) {
            ProductDetailsDialog(
                product = selectedProduct!!,
                unitPrice = (uiState as? ProductTypeSelectionUiState.Success)?.unitInfo?.price ?: 0.0,
                onDismiss = { 
                    showProductDialog = false
                    selectedProduct = null
                },
                onProceed = { productId ->
                    showProductDialog = false
                    selectedProduct = null
                    navController.navigate("dossier_creation/$unitId/$productId")
                }
            )
        }
    }
}

@Composable
private fun UnitInfoCard(unit: UnitInfoData) {
    BentoBox {
        Column {
            Text(
                text = "Informasi Unit",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Unit: ${unit.blockNumber} - ${unit.unitNumber}")
                    Text("Tipe: ${unit.unitType}")
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = formatCurrency(unit.price),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "Recommended: ${unit.recommendedProductType}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductTypeCard(
    product: ProductTypeData,
    unitPrice: Double,
    onSelectProduct: (ProductTypeData) -> Unit,
    onProceed: (String) -> Unit
) {
    val isValid = validateProductForPrice(product, unitPrice)
    val isRecommended = product.productType == getRecommendedProductType(unitPrice)
    
    BentoBox {
        Column {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.productName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = product.productCategory,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row {
                        if (isRecommended) {
                            StatusChip(
                                status = "RECOMMENDED",
                                color = Success
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        
                        StatusChip(
                            status = if (isValid) "AVAILABLE" else "NOT SUITABLE",
                            color = if (isValid) Success else Warning
                        )
                    }
                }
                
                Icon(
                    imageVector = when (product.productCategory) {
                        "KPR" -> Icons.Default.Home
                        "CASH" -> Icons.Default.AccountBalance
                        else -> Icons.Default.Description
                    },
                    contentDescription = product.productCategory,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Description
            Text(
                text = product.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Requirements Summary
            Column {
                Text(
                    text = "Persyaratan Utama:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                
                product.requirements.take(3).forEach { requirement ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Required",
                            tint = Success,
                            modifier = Modifier.size(12.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = requirement,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                if (product.requirements.size > 3) {
                    Text(
                        text = "... dan ${product.requirements.size - 3} persyaratan lainnya",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Bank Integration Info
            if (product.bankIntegrations.isNotEmpty()) {
                Column {
                    Text(
                        text = "Bank Tersedia (${product.bankIntegrations.size}):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        product.bankIntegrations.take(5).forEach { bank ->
                            Card(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .height(32.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = bank.bankName,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                        
                        if (product.bankIntegrations.size > 5) {
                            Card(
                                modifier = Modifier
                                    .height(32.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "+${product.bankIntegrations.size - 5}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Validation Message
            if (!isValid) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Text(
                                text = "Tidak Sesuai dengan Harga Unit",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Text(
                            text = getValidationMessage(product, unitPrice),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 24.dp, top = 4.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onSelectProduct(product) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Lihat Detail")
                }
                
                Button(
                    onClick = { onProceed(product.id) },
                    enabled = isValid,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isValid) "Pilih" else "Tidak Tersedia")
                }
            }
        }
    }
}

@Composable
private fun ProductDetailsDialog(
    product: ProductTypeData,
    unitPrice: Double,
    onDismiss: () -> Unit,
    onProceed: (String) -> Unit
) {
    val isValid = validateProductForPrice(product, unitPrice)
    val isRecommended = product.productType == getRecommendedProductType(unitPrice)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(product.productName) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = "Informasi Produk:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Column {
                        Text("Kategori: ${product.productCategory}")
                        Text("Subkategori: ${product.productSubcategory}")
                        Text("Deskripsi: ${product.description}")
                        
                        if (isRecommended) {
                            Text(
                                text = "✅ RECOMMENDED untuk unit ini",
                                style = MaterialTheme.typography.bodySmall,
                                color = Success
                            )
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Persyaratan:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    product.requirements.forEach { requirement ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Required",
                                tint = Success,
                                modifier = Modifier.size(16.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Text(
                                text = requirement,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Bank Tersedia:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    product.bankIntegrations.forEach { bank ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = "${bank.bankName} - ${bank.displayName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Text(
                                    text = "Display di Bank: ${bank.displayCategory}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                Text(
                                    text = "Processing Time: ${bank.processingDays} hari",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                if (bank.originalCategory != null) {
                                    Text(
                                        text = "Kategori Asli: ${bank.originalCategory}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Info
                                    )
                                }
                            }
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Validasi Unit:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Column {
                        Text("Harga Unit: ${formatCurrency(unitPrice)}")
                        Text("Status: ${if (isValid) "✅ Valid" else "❌ Tidak Valid"}")
                        
                        if (!isValid) {
                            Text(
                                text = getValidationMessage(product, unitPrice),
                                style = MaterialTheme.typography.bodySmall,
                                color = Error
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onProceed(product.id) },
                enabled = isValid
            ) {
                Text(if (isValid) "Pilih Produk Ini" else "Tidak Tersedia")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

@Composable
private fun ErrorMessage(
    message: String,
    onRetry: () -> Unit
) {
    BentoBox {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

// =====================================================
// HELPER FUNCTIONS
// =====================================================

private fun validateProductForPrice(product: ProductTypeData, unitPrice: Double): Boolean {
    return when (product.productType) {
        "KPR_SUBSIDI" -> unitPrice <= 200000000
        "KPR_NON_SUBSIDI_SEMI_KOMERSIL" -> unitPrice > 200000000 && unitPrice <= 500000000
        "KPR_NON_SUBSIDI_KOMERSIL" -> unitPrice > 500000000
        "CASH_KERAS" -> true
        else -> false
    }
}

private fun getRecommendedProductType(unitPrice: Double): String {
    return when {
        unitPrice <= 200000000 -> "KPR_SUBSIDI"
        unitPrice <= 500000000 -> "KPR_NON_SUBSIDI_SEMI_KOMERSIL"
        else -> "KPR_NON_SUBSIDI_KOMERSIL"
    }
}

private fun getValidationMessage(product: ProductTypeData, unitPrice: Double): String {
    return when (product.productType) {
        "KPR_SUBSIDI" -> "Harga unit > 200 juta. Maksimal untuk KPR Subsidi adalah 200 juta."
        "KPR_NON_SUBSIDI_SEMI_KOMERSIL" -> {
            when {
                unitPrice <= 200000000 -> "Harga unit <= 200 juta. Seharusnya menggunakan KPR Subsidi."
                unitPrice > 500000000 -> "Harga unit > 500 juta. Seharusnya menggunakan KPR Komersil."
                else -> "Harga unit tidak sesuai dengan range KPR Semi Komersil."
            }
        }
        "KPR_NON_SUBSIDI_KOMERSIL" -> "Harga unit <= 500 juta. Minimal untuk KPR Komersil adalah 500 juta."
        else -> "Produk tidak valid untuk unit ini."
    }
}

private fun formatCurrency(amount: Double): String {
    return "Rp ${String.format("%,.0f", amount)}"
}

// =====================================================
// DATA CLASSES
// =====================================================

data class ProductTypeData(
    val id: String,
    val productType: String,
    val productCode: String,
    val productName: String,
    val productCategory: String,
    val productSubcategory: String,
    val description: String,
    val requirements: List<String>,
    val bankIntegrations: List<BankIntegrationData>,
    val isActive: Boolean
)

data class BankIntegrationData(
    val bankId: String,
    val bankName: String,
    val displayCategory: String,
    val displayName: String,
    val processingDays: Int,
    val originalCategory: String?
)

data class UnitInfoData(
    val id: String,
    val blockNumber: String,
    val unitNumber: String,
    val unitType: String,
    val price: Double,
    val recommendedProductType: String
)

// =====================================================
// UI STATES
// =====================================================

sealed class ProductTypeSelectionUiState {
    object Loading : ProductTypeSelectionUiState()
    data class Success(
        val productTypes: List<ProductTypeData>,
        val unitInfo: UnitInfoData
    ) : ProductTypeSelectionUiState()
    data class Error(val message: String) : ProductTypeSelectionUiState()
}
