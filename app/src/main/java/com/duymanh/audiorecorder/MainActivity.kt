package com.duymanh.audiorecorder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import com.duymanh.audiorecorder.databinding.ActivityMainBinding

const val REQUEST_CODE = 200

class MainActivity : AppCompatActivity(), Timer.OnTimerTickListener {

    private lateinit var binding: ActivityMainBinding // View Binding object

    private var permissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.VIBRATE
    )
    private var permissionGranted = false

    private lateinit var recorder: MediaRecorder
    private var dirPath = ""
    private var fileName = ""
    private var isRecording = false
    private var isPaused = false

    private lateinit var vibrator: Vibrator
    private lateinit var timer: Timer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        timer = Timer(this)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        permissionGranted = ActivityCompat.checkSelfPermission(
            this, permissions[0]
        ) == PackageManager.PERMISSION_GRANTED

        if (!permissionGranted) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE)
        }

        // Set up button click listener
        binding.btnRecord.setOnClickListener {
            when {
                isPaused -> resumeRecorder()
                isRecording -> pauseRecorder()
                else -> startRecording()
            }

            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE) {
            permissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun pauseRecorder() {
        recorder.pause()
        isPaused = true
        binding.btnRecord.setImageResource(R.drawable.ic_record) // Use binding

        timer.pause()
    }

    private fun resumeRecorder() {
        recorder.resume()
        isPaused = false
        binding.btnRecord.setImageResource(R.drawable.ic_pause) // Use binding

        timer.start()
    }

    private fun startRecording() {
        if (!permissionGranted) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE)
            return
        }

        recorder = MediaRecorder()
        dirPath = "${externalCacheDir?.absolutePath}/"

        val simpleDateFormat = SimpleDateFormat("yyyy.MM.dd_hh.mm.ss")
        val date = simpleDateFormat.format(Date())
        fileName = "audio_record_$date"

        recorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile("$dirPath$fileName.mp3")

            try {
                prepare()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            start()
        }

        binding.btnRecord.setImageResource(R.drawable.ic_pause) // Use binding
        isRecording = true
        isPaused = false

        timer.start()
    }

    private fun stopRecorder() {
        timer.stop()
    }

    override fun onTimerTick(duration: String) {
        binding.tvTimer.text = duration // Use binding
    }
}
