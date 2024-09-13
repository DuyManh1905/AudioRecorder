package com.duymanh.audiorecorder

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey


@Entity(tableName = "audioRecords")
data class AudioRecord (
    var fileName: String,
    var filePath: String,//duong dan den tep
    var timestamp: Long,//thoi gian luu
    var duration: String,//do dai
    var ampsPath: String//luu tru du lieu dang song
){
    @PrimaryKey(autoGenerate = true)
    var id = 0
    @Ignore
    var isChecked = false

}