package com.duymanh.audiorecorder

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class WaveformView(context: Context?, attrs: AttributeSet?): View(context, attrs) {

    private var paint = Paint()
    private var amplitudes = ArrayList<Float>()
    private var spikes = ArrayList<RectF>()

    private var radius = 6f //do bo goc cua thanh spikes
    private var w = 9f //do rong cua thanh spikes
    private var d = 6f // khoang cach giua cac thanh spikes

    private var sw = 0f //chieu rong cua tat ca cac thanh spike
    private var sh = 400f // do cao toi da cua thanh spike

    private var maxSpikes = 0 //so luong cac thanh spike max

    init {
        paint.color = Color.rgb(244, 81, 30)
        sw = resources.displayMetrics.widthPixels.toFloat() //chieu rong cua thiet bi
        maxSpikes = (sw/(w+d)).toInt()
    }


    fun addAmplitude(amp: Float) {
        var norm: Float = Math.min(amp.toInt()/14, 400).toFloat()
        amplitudes.add(norm)

        spikes.clear()
        var amps = amplitudes.takeLast(maxSpikes)
        for(i in amps.indices){
            var left = sw - i*(w+d)
            var top = sh/2 - amps[i]/2
            var right = left + w
            var bottom = top + amps[i]
            spikes.add(RectF(left,top,right,bottom))
        }

        invalidate()
    }

    fun clear(): ArrayList<Float>{
        var amps = amplitudes.clone() as ArrayList<Float>
        amplitudes.clear()
        spikes.clear()
        invalidate()

        return amps
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        spikes.forEach({
            canvas?.drawRoundRect(it,radius,radius,paint)
        })
    }

}