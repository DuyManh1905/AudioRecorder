package com.duymanh.audiorecorder

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class OnnxModel(context: Context, modelName: String) {
    private val session: OrtSession
    init {
        // Tải model từ assets
        val modelPath = loadModelFromAssets(context, modelName).absolutePath

        // Tạo môi trường ONNX
        val env = OrtEnvironment.getEnvironment()
        session = env.createSession(modelPath) // Sử dụng modelPath
    }

    fun predict(inputData: FloatArray): Int {
        val inputTensor = OnnxTensor.createTensor(OrtEnvironment.getEnvironment(), inputData)

        val inputs = mapOf("input" to inputTensor)
        val results = session.run(inputs)

        val outputTensor = results[0] as OnnxTensor
        val outputArray = outputTensor.value as Array<FloatArray>

        // Tìm chỉ số của giá trị lớn nhất
        var maxIndex = 0
        var maxValue = outputArray[0][0]
        for (i in outputArray[0].indices) {
            if (outputArray[0][i] > maxValue) {
                maxValue = outputArray[0][i]
                maxIndex = i
            }
        }

        return maxIndex
    }
    fun loadModelFromAssets(context: Context, modelName: String): File {
        val modelFile = File(context.cacheDir, modelName) // Tạo file trong cache dir
        if (!modelFile.exists()) {
            val inputStream: InputStream = context.assets.open(modelName)
            val outputStream = FileOutputStream(modelFile)

            inputStream.copyTo(outputStream) // Sao chép file từ assets sang cache
            outputStream.close()
            inputStream.close()
        }
        return modelFile
    }
}
