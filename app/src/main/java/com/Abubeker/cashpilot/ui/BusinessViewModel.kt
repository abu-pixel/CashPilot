package com.Abubeker.cashpilot.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.Abubeker.cashpilot.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

data class BusinessInsight(
    val title: String,
    val description: String,
    val type: InsightType
)

enum class InsightType { WARNING, INFO, SUCCESS, TIP }

class BusinessViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: BusinessRepository

    val allTransactions: StateFlow<List<Transaction>>
    val allCustomers: StateFlow<List<Customer>>
    val allSuppliers: StateFlow<List<Supplier>>
    val allProducts: StateFlow<List<Product>>
    val businessProfile: StateFlow<BusinessProfile?>
    val totalIncome: StateFlow<Double>
    val totalExpense: StateFlow<Double>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = BusinessRepository(database.businessDao())

        allTransactions = repository.allTransactions
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        allCustomers = repository.allCustomers
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        allSuppliers = repository.allSuppliers
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        allProducts = repository.allProducts
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        businessProfile = repository.businessProfile
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
        
        totalIncome = repository.totalIncome
            .map { it ?: 0.0 }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

        totalExpense = repository.totalExpense
            .map { it ?: 0.0 }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    }

    // High-end Business Health Score (0.0 to 1.0)
    val healthScore: StateFlow<Float> = combine(totalIncome, totalExpense, allCustomers, allSuppliers, allProducts) { inc, exp, cust, supp, prod ->
        var score = 0.6f 
        
        // 1. Profitability
        if (inc > exp && inc > 0) score += 0.2f
        else if (exp > inc) score -= 0.2f
        
        // 2. Debt Management
        val totalDebt = cust.sumOf { it.totalDebt }
        val totalOwed = supp.sumOf { it.totalOwed }
        val debtRatio = if (inc > 0) totalDebt / inc else if (totalDebt > 0) 1.0 else 0.0
        
        if (debtRatio < 0.2) score += 0.15f
        else if (debtRatio > 0.5) score -= 0.15f
        
        // 3. Stock Health
        val lowStock = prod.count { it.stock <= it.lowStockThreshold }
        if (lowStock > 0) score -= (lowStock * 0.02f)
        
        score.coerceIn(0.1f, 1.0f)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.6f)

    // Using explicit combination to avoid inference issues with many flows
    val insights: StateFlow<List<BusinessInsight>> = combine(
        listOf(allTransactions, allCustomers, allSuppliers, allProducts, totalIncome, totalExpense)
    ) { args ->
        @Suppress("UNCHECKED_CAST") val transactions = args[0] as List<Transaction>
        @Suppress("UNCHECKED_CAST") val customers = args[1] as List<Customer>
        @Suppress("UNCHECKED_CAST") val suppliers = args[2] as List<Supplier>
        @Suppress("UNCHECKED_CAST") val products = args[3] as List<Product>
        val income = args[4] as Double
        val expense = args[5] as Double
        
        val list = mutableListOf<BusinessInsight>()
        val totalDebt = customers.sumOf { it.totalDebt }
        val totalOwed = suppliers.sumOf { it.totalOwed }
        val now = System.currentTimeMillis()
        val day = 24 * 60 * 60 * 1000L

        // 1. Critical Spending Analysis
        if (income > 0.0 && expense > income * 0.75) {
            list.add(BusinessInsight("Spending Alert", "Expenses are ${(expense / income * 100).toInt()}% of your income. Check non-essential costs.", InsightType.WARNING))
        }

        // 2. Weekly Expense Average comparison
        if (transactions.size > 15) {
            val fourWeeksAgo = now - (28 * day)
            val monthTx = transactions.filter { it.date > fourWeeksAgo }
            val avgWeeklyExp = monthTx.filter { it.type.name.contains("EXPENSE") || it.type == TransactionType.STOCK_PURCHASE }.sumOf { it.amount } / 4.0
            val thisWeekExp = transactions.filter { it.date > now - (7 * day) && (it.type.name.contains("EXPENSE") || it.type == TransactionType.STOCK_PURCHASE) }.sumOf { it.amount }
            
            if (thisWeekExp > avgWeeklyExp * 1.22 && avgWeeklyExp > 0) {
                list.add(BusinessInsight("Efficiency Alert", "Expenses are 22% higher than your normal weekly average.", InsightType.WARNING))
            }
        }

        // 3. Debt Recovery Insight
        if (totalDebt > totalOwed && totalDebt > 1000.0) {
            list.add(BusinessInsight("Cash Opportunity", "You have ${totalDebt.toInt()} ETB in customer debt. Collecting half would pay all your supplier bills.", InsightType.INFO))
        }

        // 4. Low Stock Protection
        val lowStockItems = products.filter { it.stock <= it.lowStockThreshold }
        if (lowStockItems.isNotEmpty()) {
            list.add(BusinessInsight("Stock Warning", "${lowStockItems.size} items are low. Replenish now to prevent lost sales.", InsightType.WARNING))
        }

        // 5. Growth Trend
        val thisWeekSales = transactions.filter { it.date > now - (7 * day) && (it.type == TransactionType.CASH_SALE || it.type == TransactionType.DIGITAL_PAYMENT) }.sumOf { it.amount }
        val lastWeekSales = transactions.filter { it.date in (now - (14 * day)) until (now - (7 * day)) && (it.type == TransactionType.CASH_SALE || it.type == TransactionType.DIGITAL_PAYMENT) }.sumOf { it.amount }
        if (thisWeekSales > lastWeekSales && lastWeekSales > 0.0) {
            val growth = ((thisWeekSales - lastWeekSales) / lastWeekSales * 100).toInt()
            list.add(BusinessInsight("Growth Detected", "Sales increased by $growth% compared to last week. Keep it up!", InsightType.SUCCESS))
        }

        list.toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Pro-Level Transaction & Stock Automation
    fun addTransaction(amount: Double, type: TransactionType, note: String = "", customerId: Long? = null, supplierId: Long? = null, isCredit: Boolean = false, productId: Long? = null) {
        viewModelScope.launch {
            repository.addTransaction(
                Transaction(
                    amount = amount, type = type, note = note, 
                    customerId = customerId, supplierId = supplierId, 
                    productId = productId, isCredit = isCredit
                )
            )
            // Automatic inventory adjustment for sales
            if (productId != null) {
                allProducts.value.find { it.id == productId }?.let { prod ->
                    when (type) {
                        TransactionType.CASH_SALE, TransactionType.DIGITAL_PAYMENT -> {
                            updateProduct(prod.copy(stock = (prod.stock - 1).coerceAtLeast(0)))
                        }
                        TransactionType.STOCK_PURCHASE -> {
                            updateProduct(prod.copy(stock = prod.stock + 1))
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            val database = AppDatabase.getDatabase(getApplication())
            val dao = database.businessDao()
            // Reverse account balance logic
            if (transaction.isCredit) {
                transaction.customerId?.let { dao.updateCustomerDebt(it, -transaction.amount) }
                transaction.supplierId?.let { dao.updateSupplierOwed(it, -transaction.amount) }
            } else if (transaction.type == TransactionType.CUSTOMER_DEBT_PAYMENT) {
                transaction.customerId?.let { dao.updateCustomerDebt(it, transaction.amount) }
            } else if (transaction.type == TransactionType.SUPPLIER_DEBT_PAYMENT) {
                transaction.supplierId?.let { dao.updateSupplierOwed(it, transaction.amount) }
            }
            
            repository.deleteTransaction(transaction)
            
            // Pro-Level Automation: Reverse stock impact
            transaction.productId?.let { pid ->
                allProducts.value.find { it.id == pid }?.let { prod ->
                    when (transaction.type) {
                        TransactionType.CASH_SALE, TransactionType.DIGITAL_PAYMENT -> {
                            updateProduct(prod.copy(stock = prod.stock + 1))
                        }
                        TransactionType.STOCK_PURCHASE -> {
                            updateProduct(prod.copy(stock = (prod.stock - 1).coerceAtLeast(0)))
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    // Entity Management
    fun addCustomer(name: String, phone: String = "") = viewModelScope.launch { repository.addCustomer(Customer(name = name, phone = phone)) }
    fun deleteCustomer(customer: Customer) = viewModelScope.launch { repository.deleteCustomer(customer) }
    
    fun addSupplier(name: String, phone: String = "", paymentDate: Long? = null) = viewModelScope.launch { 
        repository.addSupplier(Supplier(name = name, phone = phone, nextPaymentDate = paymentDate)) 
    }
    fun deleteSupplier(supplier: Supplier) = viewModelScope.launch { repository.deleteSupplier(supplier) }
    
    fun addProduct(name: String, stock: Int, threshold: Int, price: Double) = viewModelScope.launch { 
        repository.addProduct(Product(name = name, stock = stock, lowStockThreshold = threshold, price = price)) 
    }
    fun updateProduct(product: Product) = viewModelScope.launch { repository.updateProduct(product) }
    fun deleteProduct(product: Product) = viewModelScope.launch { repository.deleteProduct(product) }

    fun updateBusinessProfile(shopName: String, ownerName: String, currency: String) = viewModelScope.launch {
        repository.updateBusinessProfile(BusinessProfile(shopName = shopName, ownerName = ownerName, currency = currency))
    }
}
