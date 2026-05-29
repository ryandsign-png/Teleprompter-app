package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScriptDao {
    @Query("SELECT * FROM scripts ORDER BY lastModified DESC")
    fun getAllScripts(): Flow<List<Script>>

    @Query("SELECT * FROM scripts WHERE id = :id")
    fun getScriptById(id: Int): Flow<Script?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScript(script: Script): Long

    @Delete
    suspend fun deleteScript(script: Script)

    @Query("DELETE FROM scripts WHERE id = :id")
    suspend fun deleteScriptById(id: Int)
}
