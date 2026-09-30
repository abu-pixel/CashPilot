package com.Abubeker.cashpilot.data

import kotlinx.coroutines.flow.Flow

class BusinessRepository(private val businessDao: BusinessDao) {
    val allTransactions: Flow<List<Transaction>> = businessDao.getAllTransactions()
    val allCustomers: Flow<List<Customer>> = businessDao.getAllCustomers()
    val allSuppliers: Flow<List<Supplier>> = businessDao.getAllSuppliers()
    val totalIncome: Flow<Double?> = businessDao.getTotalIncome()
    val totalExpense: Flow<Double?> = businessDao.getTotalExpense()
    val allProducts: Flow<List<Product>> = businessDao.getAllProducts()

    suspend fun addTransaction(transaction: Transaction) {
        businessDao.insertTransaction(transaction)
        
        // Update customer/supplier totals based on transaction type and credit status
        when {
            transaction.isCredit -> {
                transaction.customerId?.let { 
                    businessDao.updateCustomerDebt(it, transaction.amount) 
                }
                transaction.supplierId?.let { 
                    businessDao.updateSupplierOwed(it, transaction.amount) 
                }
            }
            transaction.type == TransactionType.CUSTOMER_DEBT_PAYMENT -> {
                transaction.customerId?.let {
                    businessDao.updateCustomerDebt(it, -transaction.amount)
                }
            }
            transaction.type == TransactionType.SUPPLIER_DEBT_PAYMENT -> {
                transaction.supplierId?.let {
                    businessDao.updateSupplierOwed(it, -transaction.amount)
                }
            }
            else -> {}
        }
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        businessDao.deleteTransaction(transaction)
    }

    suspend fun addCustomer(customer: Customer) = businessDao.insertCustomer(customer)
    suspend fun deleteCustomer(customer: Customer) = businessDao.deleteCustomer(customer)
    suspend fun updateCustomerDebt(customerId: Long, amount: Double) = businessDao.updateCustomerDebt(customerId, amount)
    
    suspend fun addSupplier(supplier: Supplier) = businessDao.insertSupplier(supplier)
    suspend fun deleteSupplier(supplier: Supplier) = businessDao.deleteSupplier(supplier)
    suspend fun updateSupplierOwed(supplierId: Long, amount: Double) = businessDao.updateSupplierOwed(supplierId, amount)

    suspend fun addProduct(product: Product) = businessDao.insertProduct(product)
    suspend fun updateProduct(product: Product) = businessDao.updateProduct(product)
    suspend fun deleteProduct(product: Product) = businessDao.deleteProduct(product)
}
