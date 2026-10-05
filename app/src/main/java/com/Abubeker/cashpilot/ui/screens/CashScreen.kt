package com.Abubeker.cashpilot.ui.screens

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Abubeker.cashpilot.data.*
import com.Abubeker.cashpilot.ui.BusinessViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CashScreen(viewModel: BusinessViewModel) {
    val transactions by viewModel.allTransactions.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalExpense by viewModel.totalExpense.collectAsState()
    val customers by viewModel.allCustomers.collectAsState()
    val suppliers by viewModel.allSuppliers.collectAsState()
    val products by viewModel.allProducts.collectAsState()
    val profile by viewModel.businessProfile.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }

    val currencyFormatter = remember(profile?.currency) {
        NumberFormat.getCurrencyInstance().apply {
            try {
                currency = Currency.getInstance(profile?.currency ?: "ETB")
            } catch (e: Exception) {
                currency = Currency.getInstance("ETB")
            }
        }
    }

    val filteredTransactions = transactions.filter {
        val matchesSearch = it.note.contains(searchQuery, ignoreCase = true) || 
                          it.type.name.replace("_", " ").contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (selectedFilter) {
            "In" -> it.type in listOf(TransactionType.CASH_SALE, TransactionType.DIGITAL_PAYMENT, TransactionType.CUSTOMER_DEBT_PAYMENT, TransactionType.OTHER_INCOME, TransactionType.OPENING_CASH)
            "Out" -> it.type in listOf(TransactionType.EXPENSE, TransactionType.STOCK_PURCHASE, TransactionType.TRANSPORT, TransactionType.RENT, TransactionType.SALARY, TransactionType.UTILITIES, TransactionType.SUPPLIER_DEBT_PAYMENT)
            "Credit" -> it.isCredit
            else -> true
        }
        matchesSearch && matchesFilter
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Entry") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(horizontal = 16.dp)) {
            Text(
                text = "Business Ledger",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            
            // Stats Section
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Current Balance", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                    Text(
                        currencyFormatter.format(totalIncome - totalExpense),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        FinanceStat("Total In", currencyFormatter.format(totalIncome), Color(0xFF2E7D32))
                        FinanceStat("Total Out", currencyFormatter.format(totalExpense), Color(0xFFC62828))
                    }
                }
            }

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search description or type...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Professional Filter Bar
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(listOf("All", "In", "Out", "Credit")) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) },
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = if (selectedFilter == filter) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredTransactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        Text("No records found", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    // Group transactions by date
                    val grouped = filteredTransactions.groupBy { 
                        SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date(it.date)) 
                    }
                    
                    grouped.forEach { (date, dailyTransactions) ->
                        item {
                            Text(
                                text = date,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                            )
                        }
                        items(dailyTransactions, key = { it.id }) { transaction ->
                            val customer = customers.find { it.id == transaction.customerId }
                            val supplier = suppliers.find { it.id == transaction.supplierId }
                            val product = products.find { it.id == transaction.productId }
                            TransactionRow(
                                transaction = transaction, 
                                formatter = currencyFormatter, 
                                customer = customer, 
                                supplier = supplier,
                                product = product,
                                onLongClick = { transactionToDelete = transaction }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddTransactionDialogFull(
            viewModel = viewModel,
            customers = customers,
            suppliers = suppliers,
            products = products,
            onDismiss = { showAddDialog = false }
        )
    }

    if (transactionToDelete != null) {
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("Delete This Entry?", fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently remove the record and reverse its impact on your balances and stock levels.") },
            confirmButton = {
                Button(
                    onClick = {
                        transactionToDelete?.let { viewModel.deleteTransaction(it) }
                        transactionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun FinanceStat(label: String, amount: String, color: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.7f))
        Text(amount, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = color)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionRow(
    transaction: Transaction, 
    formatter: NumberFormat, 
    customer: Customer?, 
    supplier: Supplier?,
    product: Product?,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    val isIncome = transaction.type in listOf(
        TransactionType.CASH_SALE, TransactionType.DIGITAL_PAYMENT, 
        TransactionType.CUSTOMER_DEBT_PAYMENT, TransactionType.OTHER_INCOME, TransactionType.OPENING_CASH
    )
    val color = if (isIncome) Color(0xFF2E7D32) else Color(0xFFC62828)
    val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(transaction.date))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { },
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = color.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = getIconForType(transaction.type),
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(transaction.type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(timeStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        if (customer != null || supplier != null) {
                            Text(" • ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text(customer?.name ?: supplier?.name ?: "", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (transaction.note.isNotBlank() || product != null) {
                        Text(
                            text = if (product != null) "${product.name} ${if(transaction.note.isNotBlank()) "- ${transaction.note}" else ""}" else transaction.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    (if (isIncome) "+" else "-") + formatter.format(transaction.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = if (transaction.isCredit) MaterialTheme.colorScheme.outline else color
                )
                if (transaction.isCredit) {
                    Surface(color = Color(0xFFFFEBEE), shape = RoundedCornerShape(4.dp)) {
                        Text("CREDIT", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                    }
                } else {
                    IconButton(onClick = {
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            val receiptText = """
                                --- CASH PILOT RECEIPT ---
                                Date: ${SimpleDateFormat("MMM dd, yyyy HH:mm").format(Date(transaction.date))}
                                Activity: ${transaction.type.name.replace("_", " ")}
                                Amount: ${formatter.format(transaction.amount)}
                                Status: ${if(transaction.isCredit) "Pending Credit" else "Paid Full"}
                                ${customer?.let { "Customer: ${it.name}" } ?: ""}
                                ${supplier?.let { "Supplier: ${it.name}" } ?: ""}
                                --------------------------
                                Professionally managed via CashPilot
                            """.trimIndent()
                            putExtra(Intent.EXTRA_TEXT, receiptText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Receipt"))
                    }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

fun getIconForType(type: TransactionType): ImageVector {
    return when (type) {
        TransactionType.CASH_SALE, TransactionType.DIGITAL_PAYMENT -> Icons.Default.PointOfSale
        TransactionType.CUSTOMER_DEBT_PAYMENT -> Icons.Default.Handshake
        TransactionType.EXPENSE -> Icons.Default.Payments
        TransactionType.STOCK_PURCHASE -> Icons.Default.Inventory
        TransactionType.RENT -> Icons.Default.HomeWork
        TransactionType.SALARY -> Icons.Default.Badge
        TransactionType.UTILITIES -> Icons.Default.Bolt
        TransactionType.TRANSPORT -> Icons.Default.LocalShipping
        TransactionType.OPENING_CASH -> Icons.Default.LockOpen
        else -> Icons.Default.ReceiptLong
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialogFull(
    viewModel: BusinessViewModel, 
    customers: List<Customer>, 
    suppliers: List<Supplier>, 
    products: List<Product>,
    onDismiss: () -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.CASH_SALE) }
    var isCredit by remember { mutableStateOf(false) }
    var selectedCustomerId by remember { mutableStateOf<Long?>(null) }
    var selectedSupplierId by remember { mutableStateOf<Long?>(null) }
    var selectedProductId by remember { mutableStateOf<Long?>(null) }
    
    var typeExpanded by remember { mutableStateOf(false) }
    var entityExpanded by remember { mutableStateOf(false) }
    var productExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Ledger Entry", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Type Picker
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = !typeExpanded }
                ) {
                    TextField(
                        value = selectedType.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("What happened?") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        TransactionType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                    selectedType = type
                                    typeExpanded = false
                                    if (type == TransactionType.CUSTOMER_DEBT_PAYMENT || type == TransactionType.SUPPLIER_DEBT_PAYMENT || type == TransactionType.OPENING_CASH) {
                                        isCredit = false
                                    }
                                }
                            )
                        }
                    }
                }

                // Product Link (for Sales/Purchases)
                if (selectedType == TransactionType.CASH_SALE || selectedType == TransactionType.DIGITAL_PAYMENT || selectedType == TransactionType.STOCK_PURCHASE) {
                    ExposedDropdownMenuBox(
                        expanded = productExpanded,
                        onExpandedChange = { productExpanded = !productExpanded }
                    ) {
                        val currentProd = products.find { it.id == selectedProductId }?.name ?: "Link to Product (Optional)"
                        TextField(
                            value = currentProd,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Inventory Item") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = productExpanded,
                            onDismissRequest = { productExpanded = false }
                        ) {
                            DropdownMenuItem(text = { Text("None") }, onClick = { selectedProductId = null; productExpanded = false })
                            products.forEach { prod ->
                                DropdownMenuItem(
                                    text = { Text("${prod.name} (${prod.stock} in stock)") },
                                    onClick = {
                                        selectedProductId = prod.id
                                        if (amount.isBlank()) amount = prod.price.toString()
                                        productExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Credit Option
                if (selectedType != TransactionType.CUSTOMER_DEBT_PAYMENT && selectedType != TransactionType.SUPPLIER_DEBT_PAYMENT && selectedType != TransactionType.OPENING_CASH) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { isCredit = !isCredit }
                    ) {
                        Checkbox(checked = isCredit, onCheckedChange = { isCredit = it })
                        Text("Record as Credit (Debt account)", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // Person Picker (Customer/Supplier)
                if (isCredit || selectedType == TransactionType.CUSTOMER_DEBT_PAYMENT || selectedType == TransactionType.SUPPLIER_DEBT_PAYMENT) {
                    val isCustomer = selectedType.name.contains("SALE") || 
                                     selectedType == TransactionType.CUSTOMER_DEBT_PAYMENT || 
                                     (isCredit && !selectedType.name.contains("PURCHASE") && selectedType !in listOf(TransactionType.EXPENSE, TransactionType.STOCK_PURCHASE))
                    
                    val entities = if (isCustomer) customers.map { it.id to it.name } else suppliers.map { it.id to it.name }
                    
                    ExposedDropdownMenuBox(
                        expanded = entityExpanded,
                        onExpandedChange = { entityExpanded = !entityExpanded }
                    ) {
                        val currentName = if (selectedCustomerId != null) customers.find { it.id == selectedCustomerId }?.name 
                                         else if (selectedSupplierId != null) suppliers.find { it.id == selectedSupplierId }?.name
                                         else "Select ${if(isCustomer) "Customer" else "Supplier"}"
                        
                        TextField(
                            value = currentName ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(if (isCustomer) "Customer" else "Supplier") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = entityExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = entityExpanded,
                            onDismissRequest = { entityExpanded = false }
                        ) {
                            if (entities.isEmpty()) {
                                DropdownMenuItem(text = { Text("No profiles found. Add one first.") }, onClick = { })
                            }
                            entities.forEach { (id, name) ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        if (isCustomer) { selectedCustomerId = id; selectedSupplierId = null } 
                                        else { selectedSupplierId = id; selectedCustomerId = null }
                                        entityExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = amount, 
                    onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) amount = it }, 
                    label = { Text("Amount (ETB)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = note, 
                    onValueChange = { note = it }, 
                    label = { Text("Additional Note") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull()
                    if (amt != null && amt >= 0) {
                        viewModel.addTransaction(amt, selectedType, note, selectedCustomerId, selectedSupplierId, isCredit, selectedProductId)
                        onDismiss()
                    }
                },
                enabled = amount.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Complete Entry")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
