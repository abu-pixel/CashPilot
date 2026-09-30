package com.Abubeker.cashpilot.ui.screens

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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
    
    var showAddDialog by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val currencyFormatter = NumberFormat.getCurrencyInstance().apply {
        currency = Currency.getInstance("ETB")
    }

    val filteredTransactions = transactions.filter {
        it.note.contains(searchQuery, ignoreCase = true) || 
        it.type.name.replace("_", " ").contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("Cash Management", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Total Cash Position", style = MaterialTheme.typography.labelLarge)
                    Text(
                        currencyFormatter.format(totalIncome - totalExpense),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search ledger...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredTransactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (searchQuery.isEmpty()) "No activities recorded." else "No records found.", 
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTransactions) { transaction ->
                        val customer = customers.find { it.id == transaction.customerId }
                        val supplier = suppliers.find { it.id == transaction.supplierId }
                        TransactionItem(
                            transaction = transaction, 
                            formatter = currencyFormatter, 
                            customer = customer, 
                            supplier = supplier,
                            onLongClick = { transactionToDelete = transaction }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddTransactionDialog(
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
            title = { Text("Delete Entry") },
            text = { Text("Are you sure you want to delete this record? This will also update your cash and debt totals.") },
            confirmButton = {
                Button(
                    onClick = {
                        transactionToDelete?.let { viewModel.deleteTransaction(it) }
                        transactionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionItem(
    transaction: Transaction, 
    formatter: NumberFormat, 
    customer: Customer?, 
    supplier: Supplier?,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    val isIncome = transaction.type in listOf(
        TransactionType.CASH_SALE, TransactionType.DIGITAL_PAYMENT, 
        TransactionType.CUSTOMER_DEBT_PAYMENT, TransactionType.OTHER_INCOME, TransactionType.OPENING_CASH
    )
    val color = if (isIncome) Color(0xFF2E7D32) else Color(0xFFC62828)
    val dateStr = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(transaction.date))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { },
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.type.name.replace("_", " "), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(dateStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                customer?.let { Text("Cust: ${it.name}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
                supplier?.let { Text("Supp: ${it.name}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary) }
                if (transaction.note.isNotBlank()) {
                    Text(transaction.note, style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        (if (isIncome) "+" else "-") + formatter.format(transaction.amount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (transaction.isCredit) MaterialTheme.colorScheme.outline else color
                    )
                    if (transaction.isCredit) {
                        Text("CREDIT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                }
                IconButton(onClick = {
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        val receiptText = """
                            --- CASH PILOT RECEIPT ---
                            Date: $dateStr
                            Activity: ${transaction.type.name.replace("_", " ")}
                            Amount: ${formatter.format(transaction.amount)}
                            Type: ${if(transaction.isCredit) "Credit Account" else "Cash Payment"}
                            ${customer?.let { "Customer: ${it.name}" } ?: ""}
                            ${supplier?.let { "Supplier: ${it.name}" } ?: ""}
                            Notes: ${transaction.note}
                            --------------------------
                            CashPilot - Your Business Ledger
                        """.trimIndent()
                        putExtra(Intent.EXTRA_TEXT, receiptText)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Receipt"))
                }) {
                    Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
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
        title = { Text("Record New Activity") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Activity Type
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = !typeExpanded }
                ) {
                    TextField(
                        value = selectedType.name.replace("_", " "),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Activity Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        TransactionType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name.replace("_", " ")) },
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

                // 2. Product selection (optional for Sales)
                if (selectedType == TransactionType.CASH_SALE || (selectedType == TransactionType.DIGITAL_PAYMENT)) {
                    ExposedDropdownMenuBox(
                        expanded = productExpanded,
                        onExpandedChange = { productExpanded = !productExpanded }
                    ) {
                        val currentProductName = products.find { it.id == selectedProductId }?.name ?: "Select Product (Optional)"
                        TextField(
                            value = currentProductName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Product Sold") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = productExpanded,
                            onDismissRequest = { productExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("None") },
                                onClick = { selectedProductId = null; productExpanded = false }
                            )
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

                // 3. Credit Checkbox
                if (selectedType != TransactionType.CUSTOMER_DEBT_PAYMENT && selectedType != TransactionType.SUPPLIER_DEBT_PAYMENT && selectedType != TransactionType.OPENING_CASH) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isCredit, onCheckedChange = { isCredit = it })
                        Text("Record as Credit? (Increases debt)", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // 4. Customer/Supplier Picker
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
                                         else "Select Person"
                        
                        TextField(
                            value = currentName ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(if (isCustomer) "Customer" else "Supplier") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = entityExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = entityExpanded,
                            onDismissRequest = { entityExpanded = false }
                        ) {
                            if (entities.isEmpty()) {
                                DropdownMenuItem(text = { Text("No matches. Go to tab to add.") }, onClick = { })
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
                    singleLine = true
                )
                OutlinedTextField(
                    value = note, 
                    onValueChange = { note = it }, 
                    label = { Text("Note / Memo") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
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
                enabled = amount.isNotBlank()
            ) {
                Text("Save Entry")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
