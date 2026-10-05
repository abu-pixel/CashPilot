package com.Abubeker.cashpilot.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Abubeker.cashpilot.ui.BusinessViewModel
import com.Abubeker.cashpilot.data.Product
import java.text.NumberFormat
import java.util.*

@Composable
fun InventoryScreen(viewModel: BusinessViewModel) {
    val products by viewModel.allProducts.collectAsState()
    val profile by viewModel.businessProfile.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val currencyFormatter = remember(profile?.currency) {
        NumberFormat.getCurrencyInstance().apply {
            try {
                currency = Currency.getInstance(profile?.currency ?: "ETB")
            } catch (e: Exception) {
                currency = Currency.getInstance("ETB")
            }
        }
    }

    val filteredProducts = products.filter {
        it.name.contains(searchQuery, ignoreCase = true)
    }
    
    val totalStockValue = products.sumOf { it.stock * it.price }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Product") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(horizontal = 16.dp)) {
            Text(
                text = "Stock Management",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            
            // Stock Value Summary
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Total Estimated Stock Value", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = currencyFormatter.format(totalStockValue),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by product name...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }
                    }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
            
            if (filteredProducts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SearchOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        Text(
                            if (searchQuery.isEmpty()) "Your inventory is currently empty." else "No matches for \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        ProductCard(
                            product = product,
                            formatter = currencyFormatter,
                            onEdit = { productToEdit = product },
                            onDelete = { viewModel.deleteProduct(product) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddEditProductDialogImproved(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, stock, threshold, price ->
                viewModel.addProduct(name, stock, threshold, price)
                showAddDialog = false
            }
        )
    }

    if (productToEdit != null) {
        AddEditProductDialogImproved(
            product = productToEdit,
            onDismiss = { productToEdit = null },
            onConfirm = { name, stock, threshold, price ->
                viewModel.updateProduct(productToEdit!!.copy(name = name, stock = stock, lowStockThreshold = threshold, price = price))
                productToEdit = null
            }
        )
    }
}

@Composable
fun ProductCard(product: Product, formatter: NumberFormat, onEdit: () -> Unit, onDelete: () -> Unit) {
    val isLowStock = product.stock <= product.lowStockThreshold

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLowStock) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (isLowStock) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Warning, contentDescription = "Low Stock", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Price: ${formatter.format(product.price)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${product.stock} in stock", 
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isLowStock) Color(0xFFC62828) else Color(0xFF2E7D32),
                        fontWeight = FontWeight.ExtraBold
                    )
                    if (isLowStock) {
                        Text(" (Order More)", style = MaterialTheme.typography.labelSmall, color = Color(0xFFC62828))
                    }
                }
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun AddEditProductDialogImproved(
    product: Product? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, Int, Int, Double) -> Unit
) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var stock by remember { mutableStateOf(product?.stock?.toString() ?: "") }
    var threshold by remember { mutableStateOf(product?.lowStockThreshold?.toString() ?: "10") }
    var price by remember { mutableStateOf(product?.price?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (product == null) "New Inventory Item" else "Update Item", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Provide product details for automated sales tracking and restock alerts.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                OutlinedTextField(
                    value = name, 
                    onValueChange = { name = it }, 
                    label = { Text("Product Name") }, 
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stock, 
                        onValueChange = { if (it.all { c -> c.isDigit() }) stock = it }, 
                        label = { Text("Current Stock") }, 
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = threshold, 
                        onValueChange = { if (it.all { c -> c.isDigit() }) threshold = it }, 
                        label = { Text("Alert Level") }, 
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = price, 
                    onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) price = it }, 
                    label = { Text("Standard Unit Price") }, 
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    prefix = { Text("Amt ") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(
                            name,
                            stock.toIntOrNull() ?: 0,
                            threshold.toIntOrNull() ?: 10,
                            price.toDoubleOrNull() ?: 0.0
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp),
                enabled = name.isNotBlank()
            ) {
                Text("Confirm Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
