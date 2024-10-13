package com.duymanh.audiorecorder

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.room.Room
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.duymanh.audiorecorder.databinding.ActivityMainBinding
import com.duymanh.audiorecorder.databinding.BottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.ObjectOutputStream
import java.text.SimpleDateFormat
import java.util.Date

const val REQUEST_CODE = 200

class MainActivity : AppCompatActivity(), Timer.OnTimerTickListener {

    private lateinit var binding: ActivityMainBinding // View Binding object
    private lateinit var binding2: BottomSheetBinding

    private lateinit var amplitudes: ArrayList<Float>

    private var recordDuration: Long = 0 // Thời gian ghi âm tính bằng mili giây

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

    private var duration = ""

    private lateinit var vibrator: Vibrator
    private lateinit var timer: Timer


    private lateinit var db: AppDatabase

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Khoi tao pluggin Python

        if (! Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        // Initialize the View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        binding2 = BottomSheetBinding.bind(binding.root.findViewById(R.id.bottomSheet))

        timer = Timer(this)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        permissionGranted = ActivityCompat.checkSelfPermission(
            this, permissions[0]
        ) == PackageManager.PERMISSION_GRANTED

        if (!permissionGranted) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE)
        }

        db = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "audioRecords"
        ).build()


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
            startActivity(Intent(this, GalleryActivity::class.java))
        }


        binding.btnDone.setOnClickListener{

            if (recordDuration < 10000) { // kiểm tra nếu thời gian ghi âm nhỏ hơn 10 giây
                Toast.makeText(this, "Vui lòng ghi âm ít nhất 10 giây!", Toast.LENGTH_SHORT).show()
                stopRecorder()
                return@setOnClickListener
            }

            stopRecorder()

            binding.progressBar.visibility = View.VISIBLE
            binding.progressText.visibility = View.VISIBLE



            CoroutineScope(Dispatchers.Main).launch {
                var categoryInd = classification()
                if (categoryInd >= 0) {
                    binding2.categorySpinner.setSelection(categoryInd)  // Thiết lập giá trị mặc định
                }

                binding.progressBar.visibility = View.GONE
                binding.progressText.visibility = View.GONE

                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                binding.bottomSheetBG.visibility = View.VISIBLE
                binding2.filenameInput.setText(fileName)
            }
        }



        binding2.btnCancel.setOnClickListener{
            File("$dirPath$fileName.wav").delete()
            dismiss()
        }

        binding2.btnOk.setOnClickListener{
            Toast.makeText(this@MainActivity, "Record saved", Toast.LENGTH_SHORT).show()
            dismiss()
            save()
        }

        var categories = resources.getStringArray(R.array.category)
        var spinnerAdapter = ArrayAdapter(this,R.layout.item_spinner, categories)
        spinnerAdapter.setDropDownViewResource(R.layout.item_spinner)
        binding2.categorySpinner.adapter = spinnerAdapter

        binding.bottomSheetBG.setOnClickListener{
            File("$dirPath$fileName.wav").delete()
            dismiss()
        }

        binding.btnDelete.setOnClickListener{
            stopRecorder()
            File("$dirPath$fileName.wav").delete()
            Toast.makeText(this,"Record delete",Toast.LENGTH_SHORT).show()
        }

        binding.btnDelete.isClickable = false;
    }

    private suspend fun classification(): Int {
        val modelClassifier = InstrumentClassifier(this)
        var classifiedCategoryId = 0
        classifiedCategoryId = modelClassifier.inference("$dirPath$fileName.wav")
        return classifiedCategoryId
    }

    private fun save(){
        val newFilename = binding2.filenameInput.text.toString()
        if(newFilename!=fileName){
            //            uuuuuuuuuuuu
            var newFile = File("$dirPath$newFilename.wav")
            File("$dirPath$fileName.wav").renameTo(newFile)
        }

        var filePath = "$dirPath$newFilename.wav"
        var timestamp: Long = Date().time
        var ampsPath = "$dirPath$newFilename"

        try {
            var fos = FileOutputStream(ampsPath)
            var out = ObjectOutputStream(fos)
            out.writeObject(amplitudes)//ghi doi tuong amplitudes vao duong dan ampsPath
            fos.close()
            out.close()
        }catch (e: IOException){
            e.printStackTrace()
        }
        var category = binding2.categorySpinner.selectedItem.toString()
        var record = AudioRecord(newFilename, filePath, timestamp,duration,ampsPath,category)

        GlobalScope.launch {//thao tac bat dong bo
            db.audioRecordDao().insert(record)
        }
    }
    private fun dismiss(){
        binding.bottomSheetBG.visibility = View.GONE
        hideKeyboard(binding2.filenameInput)

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
            setAudioChannels(1)
            setAudioSamplingRate(22050)
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile("$dirPath$fileName.wav")

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

        recordDuration = 0
        timer.start()
        binding.btnDelete.isClickable = true
        binding.btnDelete.setImageResource(R.drawable.ic_delete) // Use binding

        binding.btnList.visibility = View.GONE
        binding.btnDone.visibility = View.VISIBLE

    }

    private fun stopRecorder() {
        timer.stop()
        recordDuration = 0

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
        this.duration = duration.dropLast(3)
        binding.waveformView.addAmplitude(recorder.maxAmplitude.toFloat())
        println(recordDuration)
        recordDuration += 100
    }
}
