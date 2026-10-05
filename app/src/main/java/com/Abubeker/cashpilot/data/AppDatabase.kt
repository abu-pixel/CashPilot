package com.Abubeker.cashpilot.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Insert
    suspend fun insertTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("SELECT * FROM customers")
    fun getAllCustomers(): Flow<List<Customer>>

    @Insert
    suspend fun insertCustomer(customer: Customer)

    @Delete
    suspend fun deleteCustomer(customer: Customer)

    @Query("UPDATE customers SET totalDebt = totalDebt + :amount WHERE id = :customerId")
    suspend fun updateCustomerDebt(customerId: Long, amount: Double)

    @Query("SELECT * FROM suppliers")
    fun getAllSuppliers(): Flow<List<Supplier>>

    @Insert
    suspend fun insertSupplier(supplier: Supplier)

    @Delete
    suspend fun deleteSupplier(supplier: Supplier)

    @Query("UPDATE suppliers SET totalOwed = totalOwed + :amount WHERE id = :supplierId")
    suspend fun updateSupplierOwed(supplierId: Long, amount: Double)
    
    @Query("SELECT SUM(amount) FROM transactions WHERE isCredit = 0 AND type IN ('CASH_SALE', 'DIGITAL_PAYMENT', 'CUSTOMER_DEBT_PAYMENT', 'OTHER_INCOME', 'OPENING_CASH')")
    fun getTotalIncome(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE isCredit = 0 AND type IN ('EXPENSE', 'STOCK_PURCHASE', 'TRANSPORT', 'RENT', 'SALARY', 'UTILITIES', 'SUPPLIER_DEBT_PAYMENT')")
    fun getTotalExpense(): Flow<Double?>

    @Query("SELECT * FROM products")
    fun getAllProducts(): Flow<List<Product>>

    @Insert
    suspend fun insertProduct(product: Product)

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("SELECT * FROM business_profile WHERE id = 1")
    fun getBusinessProfile(): Flow<BusinessProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBusinessProfile(profile: BusinessProfile)
}

@Database(entities = [Customer::class, Supplier::class, Transaction::class, Product::class, BusinessProfile::class], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun businessDao(): BusinessDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cashpilot_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
