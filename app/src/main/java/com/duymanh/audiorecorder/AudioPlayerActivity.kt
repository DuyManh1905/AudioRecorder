package com.duymanh.audiorecorder

import android.media.MediaPlayer
import android.media.PlaybackParams
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import java.text.DecimalFormat
import java.text.NumberFormat

class AudioPlayerActivity : AppCompatActivity() {

    private lateinit var mediaPlayer: MediaPlayer

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvFilename: TextView

    private lateinit var tvTrackProgress: TextView
    private lateinit var tvTrackDuration: TextView

    private lateinit var playerView: PlayerWaveformView

    private lateinit var btnPlay: ImageButton
    private lateinit var btnBackward: ImageButton
    private lateinit var btnForwad: ImageButton
    private lateinit var speedChip: Chip
    private lateinit var seekBar: SeekBar

    private lateinit var runnable: Runnable
    private lateinit var handler: Handler
    private var delay = 10L

    private var jumpsValue = 1000

    private var playbackSpeed = 1.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_player)

        var filePath = intent.getStringExtra("filepath")
        var fileName = intent.getStringExtra("filename")

        toolbar = findViewById(R.id.toolbar)
        tvFilename = findViewById(R.id.tvFilename)

        tvTrackProgress = findViewById(R.id.tvTrackProgress)
        tvTrackDuration = findViewById(R.id.tvTrackDuration)
        playerView = findViewById(R.id.playerView)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener{
            onBackPressed()
        }

        tvFilename.text = fileName

        mediaPlayer = MediaPlayer()
        mediaPlayer.apply {
            setDataSource(filePath)
            prepare()
        }

        tvTrackDuration.text = dateFormat(mediaPlayer.duration)

        btnBackward = findViewById(R.id.btnBackward)
        btnForwad = findViewById(R.id.btnForWard)
        btnPlay = findViewById(R.id.btnPlay)
        speedChip = findViewById(R.id.chip)
        seekBar = findViewById(R.id.seekBar)

        handler = Handler(Looper.getMainLooper())
        runnable = Runnable {
            seekBar.progress = mediaPlayer.currentPosition
            tvTrackProgress.text = dateFormat(mediaPlayer.currentPosition)
            handler.postDelayed(runnable,delay)
        }


        btnPlay.setOnClickListener{
            playPausePlayer()
        }
        playPausePlayer()
        seekBar.max = mediaPlayer.duration
        mediaPlayer.setOnCompletionListener {
            btnPlay.background = ResourcesCompat.getDrawable(resources, R.drawable.ic_play_circle,theme)
            handler.removeCallbacks(runnable)
        }

        btnForwad.setOnClickListener{
            mediaPlayer.seekTo(mediaPlayer.currentPosition + jumpsValue)
            seekBar.progress += jumpsValue
        }

        btnBackward.setOnClickListener{
            mediaPlayer.seekTo(mediaPlayer.currentPosition - jumpsValue)
            seekBar.progress -= jumpsValue
        }

        speedChip.setOnClickListener{
            if(playbackSpeed != 2f){
                playbackSpeed+= 0.5f
            }
            else{
                playbackSpeed = 0.5f
            }
            mediaPlayer.playbackParams = PlaybackParams().setSpeed(playbackSpeed)
            speedChip.text = "x $playbackSpeed"
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if(fromUser){
                    mediaPlayer.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        } )

    }

    private fun playPausePlayer() {
        if(!mediaPlayer.isPlaying){
            mediaPlayer.start()
            btnPlay.background = ResourcesCompat.getDrawable(resources, R.drawable.ic_pause_circle,theme)

            runnable = Runnable {
                var progress = mediaPlayer.currentPosition
                seekBar.progress = progress

                var amp = 80 + Math.random()*300
                playerView.updateAmps(amp.toInt())

                handler.postDelayed(runnable, 100)
            }

            handler.postDelayed(runnable,delay)
        }
        else{
            mediaPlayer.pause()
            btnPlay.background = ResourcesCompat.getDrawable(resources, R.drawable.ic_play_circle,theme)
            handler.removeCallbacks(runnable)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        mediaPlayer.stop()
        mediaPlayer.release()
        handler.removeCallbacks(runnable)
    }

    private fun dateFormat(duration: Int): String {
        var d = duration/1000
        var s = d%60
        var m = (d/60 % 60)
        var h = ((d - m*60)/360).toInt()

        var f: NumberFormat = DecimalFormat("00")
        var str = "$m:${f.format(s)}"
        if(h>0){
            str = "$h:$str"
        }
        return str
    }
}