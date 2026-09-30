package com.Abubeker.cashpilot.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType {
    CASH_SALE, 
    DIGITAL_PAYMENT, 
    CUSTOMER_DEBT_PAYMENT, 
    OTHER_INCOME,
    OPENING_CASH,
    EXPENSE, 
    STOCK_PURCHASE, 
    TRANSPORT, 
    RENT, 
    SALARY, 
    UTILITIES,
    SUPPLIER_DEBT_PAYMENT
}

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val totalDebt: Double = 0.0
)

@Entity(tableName = "suppliers")
data class Supplier(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val totalOwed: Double = 0.0,
    val nextPaymentDate: Long? = null
)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val type: TransactionType,
    val date: Long = System.currentTimeMillis(),
    val note: String = "",
    val customerId: Long? = null,
    val supplierId: Long? = null,
    val productId: Long? = null,
    val isCredit: Boolean = false,
    val itemsJson: String? = null
)

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val stock: Int = 0,
    val lowStockThreshold: Int = 10,
    val price: Double = 0.0
)
