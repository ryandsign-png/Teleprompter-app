package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scripts")
data class Script(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val lastModified: Long = System.currentTimeMillis(),
    val scrollSpeed: Float = 3f,
    val fontSize: Float = 24f,
    val isMirrored: Boolean = false,
    val alignment: String = "LEFT" // "LEFT", "CENTER", "RIGHT"
)
