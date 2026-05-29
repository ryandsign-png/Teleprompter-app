package com.example.data

import kotlinx.coroutines.flow.Flow

class ScriptRepository(private val scriptDao: ScriptDao) {
    val allScripts: Flow<List<Script>> = scriptDao.getAllScripts()

    fun getScriptById(id: Int): Flow<Script?> = scriptDao.getScriptById(id)

    suspend fun insert(script: Script): Long = scriptDao.insertScript(script)

    suspend fun delete(script: Script) = scriptDao.deleteScript(script)

    suspend fun deleteById(id: Int) = scriptDao.deleteScriptById(id)
}
