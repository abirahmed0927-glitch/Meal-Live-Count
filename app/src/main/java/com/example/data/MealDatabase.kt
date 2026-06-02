package com.example.data

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Entity(tableName = "meal_records")
data class MealRecord(
    @PrimaryKey val date: Int, // Day of the month: 1..31
    val breakfast: Int,
    val lunch: Int,
    val dinner: Int
)

@Dao
interface MealDao {
    @Query("SELECT * FROM meal_records ORDER BY date ASC")
    fun getAllRecords(): Flow<List<MealRecord>>

    @Query("SELECT * FROM meal_records WHERE date = :date LIMIT 1")
    suspend fun getRecordForDate(date: Int): MealRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(record: MealRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<MealRecord>)
    
    @Query("UPDATE meal_records SET breakfast = :breakfast, lunch = :lunch, dinner = :dinner WHERE date = :date")
    suspend fun updateMealCounts(date: Int, breakfast: Int, lunch: Int, dinner: Int)
}

@Database(entities = [MealRecord::class], version = 1, exportSchema = false)
abstract class MealDatabase : RoomDatabase() {
    abstract fun mealDao(): MealDao

    companion object {
        @Volatile
        private var INSTANCE: MealDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): MealDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MealDatabase::class.java,
                    "meal_database"
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Populate database with default realistic counts for 31 days
                        scope.launch(Dispatchers.IO) {
                            val dao = getDatabase(context, scope).mealDao()
                            val defaults = (1..31).map { day ->
                                // Generate nice random look-alike food tracking stats
                                val breakfast = (3..12).random()
                                val lunch = (5..18).random()
                                val dinner = (8..22).random()
                                MealRecord(date = day, breakfast = breakfast, lunch = lunch, dinner = dinner)
                            }
                            dao.insertAll(defaults)
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
