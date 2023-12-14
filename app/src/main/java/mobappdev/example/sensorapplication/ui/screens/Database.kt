package mobappdev.example.sensorapplication.ui.screens

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues
import android.database.Cursor
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

//import dagger.hilt.android.scopes.SingletonComponent
import javax.inject.Singleton
import com.google.gson.Gson


@Singleton
class Database @Inject constructor (@ApplicationContext context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "local_data.db"
        const val DATABASE_VERSION = 1
        const val TABLE_NAME = "local_data_table"

        // Define table columns
        const val COLUMN_ID = "id"
        const val COLUMN_POLAR_ALG1 = "polar_alg1"
        const val COLUMN_POLAR_ALG2 = "polar_alg2"
        const val COLUMN_INTERNAL_ALG1 = "internal_alg1"
        const val COLUMN_INTERNAL_ALG2 = "internal_alg2"
        const val COLUMN_TIME_ALG1 = "time_alg1"
        const val COLUMN_TIME_INT_ALG1 = "time_int_alg1"
        const val COLUMN_TIME_INT_ALG2 = "time_int_alg2"
    }

    // Create the table
    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_POLAR_ALG1 REAL,
                $COLUMN_POLAR_ALG2 REAL,
                $COLUMN_INTERNAL_ALG1 REAL,
                $COLUMN_INTERNAL_ALG2 REAL,
                $COLUMN_TIME_ALG1 INTEGER,
                $COLUMN_TIME_INT_ALG1 INTEGER,
                $COLUMN_TIME_INT_ALG2 INTEGER
            )
        """.trimIndent()

        db.execSQL(createTableQuery)
    }

    // Upgrade the database (if needed)
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Handle database upgrade if needed

    }

    // Function to insert data into the database
    fun insertData(
        polarAlg1: List<Float?>,
        polarAlg2: List<Float?>,
        internalAlg1: List<Float?>,
        internalAlg2: List<Float?>,
        timeAlg1: List<Long?>,
        timeIntAlg1: List<Long?>,
        timeIntAlg2: List<Long?>
    ) {
        val db = writableDatabase

        val values = ContentValues()
        values.put(COLUMN_POLAR_ALG1, Gson().toJson(polarAlg1))
        values.put(COLUMN_POLAR_ALG2, Gson().toJson(polarAlg2))
        values.put(COLUMN_INTERNAL_ALG1, Gson().toJson(internalAlg1))
        values.put(COLUMN_INTERNAL_ALG2, Gson().toJson(internalAlg2))
        values.put(COLUMN_TIME_ALG1, Gson().toJson(timeAlg1))
        values.put(COLUMN_TIME_INT_ALG1, Gson().toJson(timeIntAlg1))
        values.put(COLUMN_TIME_INT_ALG2, Gson().toJson(timeIntAlg2))

        // Insert the new row, returning the primary key value of the new row
        db.insert(TABLE_NAME, null, values)
    }




    //this is to get the data from the database:
    fun getAllData(): List<DataItem> {
        val dataList = mutableListOf<DataItem>()


        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME", null)

        try {
            while (cursor.moveToNext()) {
                val timestampIndex = cursor.getColumnIndex(COLUMN_POLAR_ALG1)

                if (timestampIndex >= 0) {
                    val timestamp = cursor.getLong(timestampIndex)
                    val dataItem = DataItem(timestamp)

                    val polarAlg1Index = cursor.getColumnIndex(COLUMN_POLAR_ALG1)
                    if (polarAlg1Index >= 0) {
                        dataItem.setColumnValue(COLUMN_POLAR_ALG1, cursor.getFloat(polarAlg1Index))
                    }

                    val polarAlg2Index = cursor.getColumnIndex(COLUMN_POLAR_ALG2)
                    if (polarAlg2Index >= 0) {
                        dataItem.setColumnValue(COLUMN_POLAR_ALG2, cursor.getFloat(polarAlg2Index))
                    }

                    val internalAlg1Index = cursor.getColumnIndex(COLUMN_INTERNAL_ALG1)
                    if (internalAlg1Index >= 0) {
                        dataItem.setColumnValue(
                            COLUMN_INTERNAL_ALG1,
                            cursor.getFloat(internalAlg1Index)
                        )
                    }

                    val internalAlg2Index = cursor.getColumnIndex(COLUMN_INTERNAL_ALG2)
                    if (internalAlg2Index >= 0) {
                        dataItem.setColumnValue(
                            COLUMN_INTERNAL_ALG2,
                            cursor.getFloat(internalAlg2Index)
                        )
                    }

                    val timeAlg1Index = cursor.getColumnIndex(COLUMN_TIME_ALG1)
                    if (timeAlg1Index >= 0) {
                        dataItem.setColumnValue(COLUMN_TIME_ALG1, cursor.getLong(timeAlg1Index))
                    }


                    val timeIntAlg1Index = cursor.getColumnIndex(COLUMN_TIME_INT_ALG1)
                    if (timeIntAlg1Index >= 0) {
                        dataItem.setColumnValue(
                            COLUMN_TIME_INT_ALG1,
                            cursor.getLong(timeIntAlg1Index)
                        )
                    }

                    val timeIntAlg2Index = cursor.getColumnIndex(COLUMN_TIME_INT_ALG2)
                    if (timeIntAlg2Index >= 0) {
                        dataItem.setColumnValue(
                            COLUMN_TIME_INT_ALG2,
                            cursor.getLong(timeIntAlg2Index)
                        )
                    }

                    dataList.add(dataItem)
                }
            }


        } catch (e: Exception) {
            // Handle the exception or log it
            e.printStackTrace()
        } finally {
            cursor.close()
        }
        return dataList
    }
    fun getAllTables(): List<String> {
        val tableNames = mutableListOf<String>()

        val db = readableDatabase
        val cursor: Cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null)

        try {
            while (cursor.moveToNext()) {
                val tableName = cursor.getString(0)
                if (tableName != "android_metadata" && tableName != "sqlite_sequence") {
                    tableNames.add(tableName)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor.close()
        }

        return tableNames
    }



}

data class DataItem @Inject constructor(val timestamp: Long) {
    private val columnValues = mutableMapOf<String, Any>()

    fun setColumnValue(columnName: String, value: Any) {
        columnValues[columnName] = value
    }

    fun getColumnValue(columnName: String): Any? {
        return columnValues[columnName]
    }
}




