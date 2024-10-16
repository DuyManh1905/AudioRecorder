package com.duymanh.audiorecorder

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface AudioRecordDao {
    @Query("SELECT * FROM audioRecords")
    fun getAll(): List<AudioRecord>

    @Insert
    fun insert(vararg audioRecord: AudioRecord)

    @Delete
    fun delete(audioRecord: AudioRecord)

    @Delete
    fun delete(audioRecord: Array<AudioRecord>)

    @Update
    fun update(audioRecord: AudioRecord)

    @Query("SELECT * FROM audioRecords WHERE fileName LIKE :query")
    fun searchDatabase(query: String):List<AudioRecord>

    @Query("SELECT * FROM audioRecords WHERE category = :category")
    fun searchByCategory(category: String): List<AudioRecord>
}