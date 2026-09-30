package com.Abubeker.cashpilot.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.Abubeker.cashpilot.ui.BusinessViewModel
import com.Abubeker.cashpilot.ui.InsightType
import com.Abubeker.cashpilot.data.TransactionType
import java.text.NumberFormat
import java.util.*

@Composable
fun DashboardScreen(viewModel: BusinessViewModel) {
    val transactions by viewModel.allTransactions.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalExpense by viewModel.totalExpense.collectAsState()
    val customers by viewModel.allCustomers.collectAsState()
    val suppliers by viewModel.allSuppliers.collectAsState()
    val insights by viewModel.insights.collectAsState()
    
    var showQuickSaleDialog by remember { mutableStateOf(false) }

    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val today = calendar.timeInMillis

    // Weekly stats for trend card
    val weekStart = today - (7 * 24 * 60 * 60 * 1000L)
    val lastWeekStart = weekStart - (7 * 24 * 60 * 60 * 1000L)

    val thisWeekSales = transactions.filter { it.date >= weekStart && it.type.name.contains("SALE") }.sumOf { it.amount }
    val lastWeekSales = transactions.filter { it.date in lastWeekStart until weekStart && it.type.name.contains("SALE") }.sumOf { it.amount }
    
    val growth = if (lastWeekSales > 0) ((thisWeekSales - lastWeekSales) / lastWeekSales) * 100 else 0.0

    val todaySales = transactions.filter { it.date >= today && it.type.name.contains("SALE") }.sumOf { it.amount }
    val todayExpenses = transactions.filter { it.date >= today && (it.type == TransactionType.EXPENSE || it.type == TransactionType.STOCK_PURCHASE) }.sumOf { it.amount }
    
    val totalDebt = customers.sumOf { it.totalDebt }
    val totalOwed = suppliers.sumOf { it.totalOwed }
    val cashAvailable = totalIncome - totalExpense

    val currencyFormatter = NumberFormat.getCurrencyInstance().apply {
        currency = Currency.getInstance("ETB")
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showQuickSaleDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Record Sale") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = getGreeting(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                SummarySection(
                    sales = currencyFormatter.format(todaySales),
                    expenses = currencyFormatter.format(todayExpenses),
                    cash = currencyFormatter.format(cashAvailable),
                    debt = currencyFormatter.format(totalDebt),
                    owed = currencyFormatter.format(totalOwed)
                )
            }

            item {
                TrendCard(
                    thisWeek = currencyFormatter.format(thisWeekSales),
                    lastWeek = currencyFormatter.format(lastWeekSales),
                    growth = growth
                )
            }

            if (insights.isNotEmpty()) {
                item {
                    Text(text = "⚠️ ALERTS & INSIGHTS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(insights) { insight ->
                    InsightCard(insight.title, insight.description, insight.type)
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    if (showQuickSaleDialog) {
        QuickSaleDialog(
            onDismiss = { showQuickSaleDialog = false },
            onConfirm = { amount ->
                viewModel.addTransaction(amount, TransactionType.CASH_SALE, "Today's Sale")
                showQuickSaleDialog = false
            }
        )
    }
}

fun getGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 0..11 -> "GOOD MORNING 👋"
        in 12..16 -> "GOOD AFTERNOON 👋"
        else -> "GOOD EVENING 👋"
    }
}

@Composable
fun SummarySection(sales: String, expenses: String, cash: String, debt: String, owed: String) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MainCard("Today's Sales", sales, Color(0xFF2E7D32))
        MainCard("Today's Expenses", expenses, Color(0xFFC62828))
        MainCard("Cash Available", cash, MaterialTheme.colorScheme.primary)
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniCard("Customers Owe You", debt, Modifier.weight(1f))
            MiniCard("You Owe Suppliers", owed, Modifier.weight(1f))
        }
    }
}

@Composable
fun MainCard(title: String, amount: String, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelMedium, color = color)
            Text(text = amount, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun MiniCard(title: String, amount: String, modifier: Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelSmall)
            Text(text = amount, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TrendCard(thisWeek: String, lastWeek: String, growth: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("📈 SALES TREND", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("This week", style = MaterialTheme.typography.labelSmall)
                    Text(thisWeek, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Last week", style = MaterialTheme.typography.labelSmall)
                    Text(lastWeek, style = MaterialTheme.typography.bodyLarge)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (growth >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                    contentDescription = null,
                    tint = if (growth >= 0) Color(0xFF2E7D32) else Color(0xFFC62828),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = String.format(Locale.getDefault(), "%.1f%%", Math.abs(growth)),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (growth >= 0) Color(0xFF2E7D32) else Color(0xFFC62828),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@Composable
fun InsightCard(title: String, description: String, type: InsightType) {
    val color = when (type) {
        InsightType.WARNING -> MaterialTheme.colorScheme.errorContainer
        InsightType.SUCCESS -> Color(0xFFE8F5E9)
        InsightType.INFO -> MaterialTheme.colorScheme.primaryContainer
        InsightType.TIP -> MaterialTheme.colorScheme.secondaryContainer
    }
    val icon = when (type) {
        InsightType.WARNING -> Icons.Default.Warning
        InsightType.SUCCESS -> Icons.Default.CheckCircle
        else -> Icons.Default.Lightbulb
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(text = description, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun QuickSaleDialog(onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var amount by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick Cash Sale") },
        text = {
            TextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount (ETB)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = { amount.toDoubleOrNull()?.let { onConfirm(it) } }) {
                Text("Record")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
