package com.Abubeker.cashpilot.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Abubeker.cashpilot.ui.BusinessViewModel
import com.Abubeker.cashpilot.ui.InsightType
import com.Abubeker.cashpilot.data.TransactionType
import com.Abubeker.cashpilot.data.Transaction
import com.Abubeker.cashpilot.data.BusinessProfile
import java.text.NumberFormat
import java.util.*
import kotlin.math.max

@Composable
fun DashboardScreen(
    viewModel: BusinessViewModel,
    onNavigateToScreen: (String) -> Unit
) {
    val transactions by viewModel.allTransactions.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalExpense by viewModel.totalExpense.collectAsState()
    val customers by viewModel.allCustomers.collectAsState()
    val suppliers by viewModel.allSuppliers.collectAsState()
    val insights by viewModel.insights.collectAsState()
    val profile by viewModel.businessProfile.collectAsState()
    val healthScore by viewModel.healthScore.collectAsState()
    
    var showQuickSaleDialog by remember { mutableStateOf(false) }
    var showQuickExpenseDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }

    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val today = calendar.timeInMillis

    val todaySales = transactions.filter { it.date >= today && (it.type == TransactionType.CASH_SALE || it.type == TransactionType.DIGITAL_PAYMENT) }.sumOf { it.amount }
    val todayExpenses = transactions.filter { it.date >= today && (it.type == TransactionType.EXPENSE || it.type == TransactionType.STOCK_PURCHASE) }.sumOf { it.amount }
    
    val totalDebt = customers.sumOf { it.totalDebt }
    val totalOwed = suppliers.sumOf { it.totalOwed }
    val cashAvailable = totalIncome - totalExpense

    val currencyFormatter = remember(profile?.currency) {
        NumberFormat.getCurrencyInstance().apply {
            try {
                currency = Currency.getInstance(profile?.currency ?: "ETB")
            } catch (e: Exception) {
                currency = Currency.getInstance("ETB")
            }
        }
    }

    if (profile == null) {
        LaunchedEffect(Unit) { showProfileDialog = true }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { Spacer(modifier = Modifier.height(16.dp)) }
            
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = getGreeting(),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = profile?.shopName ?: "Business Pilot",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { showProfileDialog = true },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Health Meter Section
            item {
                BusinessHealthMeter(score = healthScore)
            }

            if (transactions.isEmpty() && customers.isEmpty() && suppliers.isEmpty()) {
                item {
                    OnboardingCard()
                }
            }

            // High-Impact Summary Row
            item {
                Column {
                    Text(
                        text = "TODAY'S SNAPSHOT",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            SummaryCardItem(
                                title = "Sales",
                                amount = currencyFormatter.format(todaySales),
                                color = Color(0xFF2E7D32),
                                icon = Icons.AutoMirrored.Filled.TrendingUp
                            )
                        }
                        item {
                            SummaryCardItem(
                                title = "Cash",
                                amount = currencyFormatter.format(cashAvailable),
                                color = Color(0xFF1B3D6D),
                                icon = Icons.Default.AccountBalanceWallet
                            )
                        }
                        item {
                            SummaryCardItem(
                                title = "Expenses",
                                amount = currencyFormatter.format(todayExpenses),
                                color = Color(0xFFC62828),
                                icon = Icons.AutoMirrored.Filled.TrendingDown
                            )
                        }
                    }
                }
            }

            // Sales Trend Graph
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Weekly Sales Velocity", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(16.dp))
                        DashboardBarChart(transactions)
                    }
                }
            }

            // Core Metrics - Debt and Owed
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard(
                        label = "Customers Owe",
                        value = currencyFormatter.format(totalDebt),
                        color = Color(0xFFC62828),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToScreen("customers") }
                    )
                    MetricCard(
                        label = "You Owe",
                        value = currencyFormatter.format(totalOwed),
                        color = Color(0xFF1B3D6D),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToScreen("suppliers") }
                    )
                }
            }

            // Quick Actions Section
            item {
                Column {
                    Text(
                        text = "QUICK OPERATIONS",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ActionBtn(label = "New Sale", icon = Icons.Default.AddShoppingCart, color = Color(0xFF2E7D32), onClick = { showQuickSaleDialog = true })
                        ActionBtn(label = "Add Exp", icon = Icons.AutoMirrored.Filled.TrendingDown, color = Color(0xFFC62828), onClick = { showQuickExpenseDialog = true })
                        ActionBtn(label = "Check Stock", icon = Icons.Default.Inventory2, color = Color(0xFF7B1FA2), onClick = { onNavigateToScreen("inventory") })
                        ActionBtn(label = "View Ledger", icon = Icons.Default.AccountBalanceWallet, color = Color(0xFF1565C0), onClick = { onNavigateToScreen("cash") })
                    }
                }
            }

            // Smart Insights
            if (insights.isNotEmpty()) {
                item {
                    Text(
                        text = "PILOT ADVISORY",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                items(insights) { insight ->
                    InsightItemImproved(insight.title, insight.description, insight.type)
                }
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }

    // Dialogs
    if (showQuickSaleDialog) {
        QuickSaleDialogImproved(
            onDismiss = { showQuickSaleDialog = false },
            onConfirm = { amount ->
                viewModel.addTransaction(amount, TransactionType.CASH_SALE, "Direct Dashboard Sale")
                showQuickSaleDialog = false
            }
        )
    }

    if (showQuickExpenseDialog) {
        QuickExpenseDialogImproved(
            onDismiss = { showQuickExpenseDialog = false },
            onConfirm = { amount, note ->
                viewModel.addTransaction(amount, TransactionType.EXPENSE, note)
                showQuickExpenseDialog = false
            }
        )
    }

    if (showProfileDialog) {
        BusinessProfileDialogImproved(
            currentProfile = profile,
            onDismiss = { if (profile != null) showProfileDialog = false },
            onConfirm = { name, owner, currency ->
                viewModel.updateBusinessProfile(name, owner, currency)
                showProfileDialog = false
            }
        )
    }
}

private fun getGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 0..11 -> "Good Morning 👋"
        in 12..16 -> "Good Afternoon 👋"
        else -> "Good Evening 👋"
    }
}

@Composable
fun BusinessHealthMeter(score: Float) {
    val color = when {
        score > 0.7f -> Color(0xFF2E7D32)
        score > 0.4f -> Color(0xFFEF6C00)
        else -> Color(0xFFC62828)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Analytics, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Business Vitality", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { score },
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                color = color,
                trackColor = color.copy(alpha = 0.1f)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = when {
                    score > 0.7f -> "PERFECT: Your profit margins and debts are optimized."
                    score > 0.4f -> "STABLE: Keep a close eye on your weekly expenses."
                    else -> "ACTION REQUIRED: High debt or expenses detected. Review your insights."
                },
                style = MaterialTheme.typography.bodySmall,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun OnboardingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Ready to start? 🚀", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Tap 'New Sale' to record your first transaction. Your business cockpit will come alive!", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun DashboardBarChart(transactions: List<Transaction>) {
    val dayMillis = 24 * 60 * 60 * 1000L
    val now = System.currentTimeMillis()
    val salesByDay = (0..6).map { i ->
        val start = now - ((i + 1) * dayMillis)
        val end = now - (i * dayMillis)
        transactions.filter { it.date in start until end && (it.type == TransactionType.CASH_SALE || it.type == TransactionType.DIGITAL_PAYMENT) }.sumOf { it.amount }
    }.reversed()

    val maxVal = max(salesByDay.maxOrNull() ?: 1.0, 1.0)

    Row(
        modifier = Modifier.fillMaxWidth().height(100.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        val labels = listOf("M", "T", "W", "T", "F", "S", "S")
        val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        salesByDay.forEachIndexed { index, value ->
            val heightFactor = (value / maxVal).toFloat()
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .fillMaxHeight(max(0.1f, heightFactor))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                )
                Text(text = labels[(currentDay + index) % 7], style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
fun SummaryCardItem(title: String, amount: String, color: Color, icon: ImageVector) {
    Card(
        modifier = Modifier.width(160.dp).height(115.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.8f))
            Column {
                Text(title, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f))
                Text(amount, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun MetricCard(label: String, value: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = color)
        }
    }
}

@Composable
fun ActionBtn(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Surface(modifier = Modifier.size(60.dp).shadow(4.dp, RoundedCornerShape(20.dp)), shape = RoundedCornerShape(20.dp), color = color.copy(alpha = 0.1f)) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = label, tint = color) }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun InsightItemImproved(title: String, description: String, type: InsightType) {
    val color = when (type) {
        InsightType.WARNING -> Color(0xFFC62828)
        InsightType.SUCCESS -> Color(0xFF2E7D32)
        InsightType.INFO -> Color(0xFF1565C0)
        InsightType.TIP -> Color(0xFFEF6C00)
    }
    val icon = when (type) {
        InsightType.WARNING -> Icons.Default.WarningAmber
        InsightType.SUCCESS -> Icons.Default.AutoGraph
        else -> Icons.Default.TipsAndUpdates
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.05f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
            }
        }
    }
}

@Composable
fun QuickSaleDialogImproved(onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var amount by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Instant Cash Sale", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column {
                Text("Fast-record a transaction directly to your ledger.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) amount = it },
                    label = { Text("Total Received") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    prefix = { Text("ETB ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { amount.toDoubleOrNull()?.let { onConfirm(it) } },
                shape = RoundedCornerShape(14.dp),
                enabled = amount.isNotBlank()
            ) {
                Text("Record Entry")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun QuickExpenseDialogImproved(onDismiss: () -> Unit, onConfirm: (Double, String) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log New Expense", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) amount = it },
                    label = { Text("Amount (ETB)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Category / Description") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { amount.toDoubleOrNull()?.let { onConfirm(it, note) } },
                shape = RoundedCornerShape(14.dp),
                enabled = amount.isNotBlank()
            ) {
                Text("Confirm Expense")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun BusinessProfileDialogImproved(
    currentProfile: BusinessProfile?,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var shopName by remember { mutableStateOf(currentProfile?.shopName ?: "") }
    var ownerName by remember { mutableStateOf(currentProfile?.ownerName ?: "") }
    var currency by remember { mutableStateOf(currentProfile?.currency ?: "ETB") }

    AlertDialog(
        onDismissRequest = { if (currentProfile != null) onDismiss() },
        title = { Text("Business Cockpit Setup", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Identify your business to customize reports and dashboard analytics.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                OutlinedTextField(value = shopName, onValueChange = { shopName = it }, label = { Text("Business Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), singleLine = true)
                OutlinedTextField(value = ownerName, onValueChange = { ownerName = it }, label = { Text("Owner Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), singleLine = true)
                OutlinedTextField(value = currency, onValueChange = { currency = it }, label = { Text("Currency Code (e.g. ETB, USD)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(shopName, ownerName, currency) },
                shape = RoundedCornerShape(14.dp),
                enabled = shopName.isNotBlank()
            ) {
                Text("Initialize Pilot")
            }
        },
        dismissButton = { if (currentProfile != null) { TextButton(onClick = onDismiss) { Text("Dismiss") } } }
    )
}
