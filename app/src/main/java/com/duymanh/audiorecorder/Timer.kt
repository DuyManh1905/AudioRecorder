package com.duymanh.audiorecorder

import android.os.Handler
import android.os.Looper
import kotlin.time.Duration


class Timer(listener: OnTimerTickListener) {

    interface OnTimerTickListener{
        fun onTimerTick(duration: String)
    }

    private var handler = Handler(Looper.getMainLooper())
    private lateinit var runable : Runnable

    private var duration = 0L
    private var delay = 100L

    init {
        runable = Runnable {
            duration += delay
            handler.postDelayed(runable,delay)
            listener.onTimerTick(fomat())
        }
    }

    fun start(){
        handler.postDelayed(runable,delay)
    }

    fun pause(){
        handler.removeCallbacks(runable)
    }

    fun stop(){
        handler.removeCallbacks(runable)
        duration = 0L
    }

    fun fomat() : String{
        val millis : Long = duration % 1000
        val seconds : Long = (duration/1000)%60
        val minutes = (duration/(1000*60)) % 60
        val hours = (duration/(1000*60*60))

        var formatted:String = if(hours>0)
            "%02d:%02d:%02d.%02d".format(hours,minutes,seconds,millis/10)
        else
           "%02d:%02d.%02d".format(minutes,seconds,millis/10)

        return formatted
    }
}