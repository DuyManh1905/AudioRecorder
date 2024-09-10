package com.duymanh.audiorecorder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

const val REQUEST_CODE = 200

class MainActivity : AppCompatActivity(),Timer.OnTimerTickListener {

    private lateinit var btnRecord: ImageButton
    private lateinit var tvTimer: TextView

    private var permissions = arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.VIBRATE)
    private var permissionGranted = false

    private lateinit var recorder : MediaRecorder
    private var dirPath = ""
    private var fileName = ""
    private var isRecording = false
    private var isPaused = false

    private lateinit var vibrator: Vibrator

    private lateinit var timer : Timer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnRecord = findViewById(R.id.btnRecord)
        tvTimer = findViewById(R.id.tvTimer)
        timer = Timer(this)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        permissionGranted = ActivityCompat.checkSelfPermission(this,permissions[0]) == PackageManager.PERMISSION_GRANTED

        if(!permissionGranted){
            ActivityCompat.requestPermissions(this,permissions, REQUEST_CODE)
        }


        btnRecord.setOnClickListener {
            when{
                isPaused -> resumeRecorder()
                isRecording -> pauseRecorder()
                else -> startRecording()
            }

            vibrator.vibrate(VibrationEffect.createOneShot(50,VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == REQUEST_CODE){
            permissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun pauseRecorder(){
        recorder.pause()
        isPaused = true
        btnRecord.setImageResource(R.drawable.ic_record)

        timer.pause()
    }

    private fun resumeRecorder(){
        recorder.resume()
        isPaused = false
        btnRecord.setImageResource(R.drawable.ic_pause)

        timer.start()
    }

    private fun startRecording(){
        if(!permissionGranted){
            ActivityCompat.requestPermissions(this,permissions, REQUEST_CODE)
            return
        }
        //start recording
        recorder = MediaRecorder()
        dirPath = "${externalCacheDir?.absolutePath}/"

        var simpleDateFormat = SimpleDateFormat("yyyy.MM.DD_hh.mm.ss")
        var date = simpleDateFormat.format(Date())
        fileName = "audio_record_$date"

        recorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile("$dirPath$fileName.mp3")

            try {
                prepare()
            }catch (e: IOException){
                e.printStackTrace()
            }
            start()
        }
        btnRecord.setImageResource(R.drawable.ic_pause)
        isRecording = true
        isPaused = false

        timer.start()
    }

    private fun stopRecorder(){
        timer.stop()
    }

    override fun onTimerTick(duration: String) {
        tvTimer.setText(duration)
    }
}