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
        
        totalIncome = repository.totalIncome
            .map { it ?: 0.0 }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

        totalExpense = repository.totalExpense
            .map { it ?: 0.0 }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    }

    private val flowGroup1 = combine(allTransactions, allCustomers, allSuppliers) { t, c, s -> Triple(t, c, s) }
    private val flowGroup2 = combine(allProducts, totalIncome, totalExpense) { p, inc, exp -> Triple(p, inc, exp) }

    val insights: StateFlow<List<BusinessInsight>> = combine(flowGroup1, flowGroup2) { g1, g2 ->
        val (transactions, customers, suppliers) = g1
        val (products, income, expense) = g2
        
        val list = mutableListOf<BusinessInsight>()
        val totalDebt = customers.sumOf { it.totalDebt }
        val totalOwed = suppliers.sumOf { it.totalOwed }
        
        if (income > 0.0 && expense > income * 0.75) {
            list.add(BusinessInsight("Spending Warning", "Expenses are ${(expense / income * 100).toInt()}% of your income. Look for ways to reduce costs.", InsightType.WARNING))
        }

        if (totalDebt > totalOwed && totalDebt > 1000.0) {
            list.add(BusinessInsight("Cash Opportunity", "You have ${totalDebt.toInt()} ETB in customer debt. Collecting half would pay all your suppliers.", InsightType.INFO))
        }

        val lowStockCount = products.count { it.stock <= it.lowStockThreshold }
        if (lowStockCount > 0) {
            list.add(BusinessInsight("Restock Alert", "$lowStockCount items are running low. Check Stock tab to avoid losing sales.", InsightType.WARNING))
        }

        val week = 7 * 24 * 60 * 60 * 1000L
        val now = System.currentTimeMillis()
        val thisWeekSales = transactions.filter { it.date > now - week && it.type.name.contains("SALE") }.sumOf { it.amount }
        val lastWeekSales = transactions.filter { it.date in (now - 2 * week) until (now - week) && it.type.name.contains("SALE") }.sumOf { it.amount }
        if (thisWeekSales > lastWeekSales && lastWeekSales > 0.0) {
            val growth = ((thisWeekSales - lastWeekSales) / lastWeekSales * 100).toInt()
            list.add(BusinessInsight("Growth Detected", "Sales are up $growth% compared to last week. Great job!", InsightType.SUCCESS))
        }

        list.toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addTransaction(amount: Double, type: TransactionType, note: String = "", customerId: Long? = null, supplierId: Long? = null, isCredit: Boolean = false, productId: Long? = null) {
        viewModelScope.launch {
            repository.addTransaction(Transaction(amount = amount, type = type, note = note, customerId = customerId, supplierId = supplierId, productId = productId, isCredit = isCredit))
            if (productId != null && (type == TransactionType.CASH_SALE || type == TransactionType.DIGITAL_PAYMENT)) {
                allProducts.value.find { it.id == productId }?.let {
                    updateProduct(it.copy(stock = it.stock - 1))
                }
            }
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            // Reverse account balance updates correctly using DAO
            val database = AppDatabase.getDatabase(getApplication())
            val dao = database.businessDao()
            if (transaction.isCredit) {
                transaction.customerId?.let { dao.updateCustomerDebt(it, -transaction.amount) }
                transaction.supplierId?.let { dao.updateSupplierOwed(it, -transaction.amount) }
            } else if (transaction.type == TransactionType.CUSTOMER_DEBT_PAYMENT) {
                transaction.customerId?.let { dao.updateCustomerDebt(it, transaction.amount) }
            } else if (transaction.type == TransactionType.SUPPLIER_DEBT_PAYMENT) {
                transaction.supplierId?.let { dao.updateSupplierOwed(it, transaction.amount) }
            }
            repository.deleteTransaction(transaction)
            
            // Restore stock if it was a sale
            transaction.productId?.let { pid ->
                allProducts.value.find { it.id == pid }?.let {
                    updateProduct(it.copy(stock = it.stock + 1))
                }
            }
        }
    }

    fun addCustomer(name: String, phone: String = "") = viewModelScope.launch { repository.addCustomer(Customer(name = name, phone = phone)) }
    fun deleteCustomer(customer: Customer) = viewModelScope.launch { repository.deleteCustomer(customer) }
    fun addSupplier(name: String, phone: String = "", paymentDate: Long? = null) = viewModelScope.launch { repository.addSupplier(Supplier(name = name, phone = phone, nextPaymentDate = paymentDate)) }
    fun deleteSupplier(supplier: Supplier) = viewModelScope.launch { repository.deleteSupplier(supplier) }
    fun addProduct(name: String, stock: Int, threshold: Int, price: Double) = viewModelScope.launch { repository.addProduct(Product(name = name, stock = stock, lowStockThreshold = threshold, price = price)) }
    fun updateProduct(product: Product) = viewModelScope.launch { repository.updateProduct(product) }
    fun deleteProduct(product: Product) = viewModelScope.launch { repository.deleteProduct(product) }
}
