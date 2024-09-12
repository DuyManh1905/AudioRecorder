package com.duymanh.audiorecorder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import com.duymanh.audiorecorder.databinding.ActivityMainBinding
import com.duymanh.audiorecorder.databinding.BottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.io.File

const val REQUEST_CODE = 200

class MainActivity : AppCompatActivity(), Timer.OnTimerTickListener {

    private lateinit var binding: ActivityMainBinding // View Binding object
    private lateinit var amplitudes: ArrayList<Float>

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

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>

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


        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottomSheet))
        bottomSheetBehavior.peekHeight = 0
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        // Set up button click listener
        binding.btnRecord.setOnClickListener {
            when {
                isPaused -> resumeRecorder()
                isRecording -> pauseRecorder()
                else -> startRecording()
            }

            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        }
        binding.btnList.setOnClickListener{
            // TODO
            Toast.makeText(this,"List button",Toast.LENGTH_SHORT).show()
        }

        binding.btnDone.setOnClickListener{
            stopRecorder()
            Toast.makeText(this,"Record saved",Toast.LENGTH_SHORT).show()

            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            binding.bottomSheetBG.visibility = View.VISIBLE
            findViewById<TextInputEditText>(R.id.fileNameInput).setText(fileName)
        }



        findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener{
            File("$dirPath$fileName.mp3").delete()
            dismiss()
        }

        findViewById<MaterialButton>(R.id.btnOk).setOnClickListener{
            dismiss()
            save()
        }

        binding.bottomSheetBG.setOnClickListener{
            File("$dirPath$fileName.mp3").delete()
            dismiss()
        }

        binding.btnDelete.setOnClickListener{
            stopRecorder()
            File("$dirPath$fileName.mp3").delete()
            Toast.makeText(this,"Record delete",Toast.LENGTH_SHORT).show()
        }

        binding.btnDelete.isClickable = false;
    }

    private fun save(){
        val newFilename = findViewById<TextInputEditText>(R.id.fileNameInput).text.toString()
        if(newFilename!=fileName){
            var newFile = File("$dirPath$newFilename.mp3")
            File("$dirPath$fileName.mp3").renameTo(newFile)
        }
    }
    private fun dismiss(){
        binding.bottomSheetBG.visibility = View.GONE
        hideKeyboard(findViewById<TextInputEditText>(R.id.fileNameInput))

        Handler(Looper.getMainLooper()).postDelayed({
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        },100)
    }
    private fun hideKeyboard(view: View){
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken,0)
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
        binding.btnDelete.isClickable = true
        binding.btnDelete.setImageResource(R.drawable.ic_delete) // Use binding

        binding.btnList.visibility = View.GONE
        binding.btnDone.visibility = View.VISIBLE

    }

    private fun stopRecorder() {
        timer.stop()

        recorder.apply {
            stop()
            release()
        }

        isPaused = false
        isRecording = false

        binding.btnList.visibility = View.VISIBLE
        binding.btnDone.visibility = View.GONE

        binding.btnDelete.isClickable = false
        binding.btnDelete.setImageResource(R.drawable.ic_delete_disabled)
        binding.btnRecord.setImageResource(R.drawable.ic_record)
        binding.tvTimer.setText("00:00.00")
        amplitudes = binding.waveformView.clear()
    }

    override fun onTimerTick(duration: String) {
        binding.tvTimer.text = duration // Use binding
        binding.waveformView.addAmplitude(recorder.maxAmplitude.toFloat())
    }
}
