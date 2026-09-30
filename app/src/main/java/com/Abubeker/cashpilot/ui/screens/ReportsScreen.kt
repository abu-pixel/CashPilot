package com.Abubeker.cashpilot.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.Abubeker.cashpilot.ui.BusinessViewModel
import com.Abubeker.cashpilot.data.Transaction
import com.Abubeker.cashpilot.data.TransactionType
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

@Composable
fun ReportsScreen(viewModel: BusinessViewModel) {
    val transactions by viewModel.allTransactions.collectAsState()
    val customers by viewModel.allCustomers.collectAsState()
    val suppliers by viewModel.allSuppliers.collectAsState()
    val context = LocalContext.current

    val totalDebt = customers.sumOf { it.totalDebt }
    val totalOwed = suppliers.sumOf { it.totalOwed }

    val currencyFormatter = NumberFormat.getCurrencyInstance().apply {
        currency = Currency.getInstance("ETB")
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    val csv = "Date,Type,Amount,Credit,Note\n" + transactions.joinToString("\n") { 
                        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(it.date))
                        "$date,${it.type.name},${it.amount},${it.isCredit},\"${it.note}\""
                    }
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, csv)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Export Business Data"))
                },
                icon = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                text = { Text("Export CSV") },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
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
                Text("Business Performance", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            }

            // 1. Daily Section
            item {
                DetailedPeriodicReport(
                    title = "Daily Report",
                    transactions = transactions,
                    periodDays = 1,
                    formatter = currencyFormatter
                )
            }

            // 2. Weekly Section
            item {
                DetailedPeriodicReport(
                    title = "Weekly Report",
                    transactions = transactions,
                    periodDays = 7,
                    formatter = currencyFormatter,
                    showCreditStats = true
                )
            }

            // 3. Monthly Section
            item {
                DetailedPeriodicReport(
                    title = "Monthly Report",
                    transactions = transactions,
                    periodDays = 30,
                    formatter = currencyFormatter,
                    showBalances = true,
                    totalDebt = totalDebt,
                    totalOwed = totalOwed
                )
            }

            // 4. Sales Trend Graph
            item {
                ReportSection("Sales Trend (Last 7 Days)") {
                    SimpleBarChart(transactions)
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun DetailedPeriodicReport(
    title: String,
    transactions: List<Transaction>,
    periodDays: Int,
    formatter: NumberFormat,
    showCreditStats: Boolean = false,
    showBalances: Boolean = false,
    totalDebt: Double = 0.0,
    totalOwed: Double = 0.0
) {
    val periodStart = System.currentTimeMillis() - (periodDays * 24 * 60 * 60 * 1000L)
    val periodTransactions = transactions.filter { it.date >= periodStart }
    
    val sales = periodTransactions.filter { it.type == TransactionType.CASH_SALE || it.type == TransactionType.DIGITAL_PAYMENT }.sumOf { it.amount }
    val expenses = periodTransactions.filter { it.type == TransactionType.EXPENSE || it.type == TransactionType.STOCK_PURCHASE }.sumOf { it.amount }
    val creditSales = periodTransactions.filter { it.isCredit && (it.type == TransactionType.CASH_SALE || it.type == TransactionType.DIGITAL_PAYMENT) }.sumOf { it.amount }
    val debtCollected = periodTransactions.filter { it.type == TransactionType.CUSTOMER_DEBT_PAYMENT }.sumOf { it.amount }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            
            ReportRow("Revenue / Sales", formatter.format(sales))
            ReportRow("Total Expenses", formatter.format(expenses))
            
            if (showCreditStats) {
                ReportRow("Credit Sales", formatter.format(creditSales), color = Color(0xFFC62828))
                ReportRow("Debt Collected", formatter.format(debtCollected), color = Color(0xFF2E7D32))
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            ReportRow("Net Movement", formatter.format(sales - expenses), isBold = true, color = if (sales >= expenses) Color(0xFF2E7D32) else Color(0xFFC62828))

            if (showBalances) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Overall Balances", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                ReportRow("Credit Outstanding", formatter.format(totalDebt), color = Color(0xFFC62828))
                ReportRow("Supplier Debt", formatter.format(totalOwed), color = Color(0xFFC62828))
            }
        }
    }
}

@Composable
fun SimpleBarChart(transactions: List<Transaction>) {
    val dayMillis = 24 * 60 * 60 * 1000L
    val now = System.currentTimeMillis()
    val salesByDay = (0..6).map { i ->
        val start = now - ((i + 1) * dayMillis)
        val end = now - (i * dayMillis)
        transactions.filter { it.date in start until end && (it.type == TransactionType.CASH_SALE || it.type == TransactionType.DIGITAL_PAYMENT) }.sumOf { it.amount }
    }.reversed()

    val maxVal = max(salesByDay.maxOrNull() ?: 1.0, 1.0)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(top = 8.dp),
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
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                )
                Text(text = labels[(currentDay + index) % 7], style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun ReportSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun ReportRow(label: String, value: String, color: Color = Color.Unspecified, isBold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal, color = color)
    }
}
