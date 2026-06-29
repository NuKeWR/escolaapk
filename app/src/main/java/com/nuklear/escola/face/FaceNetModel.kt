package com.nuklear.escola.face

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * FaceNet via TensorFlow Lite.
 * 
 * Detecta automaticamente o tamanho de input e output do modelo.
 * Funciona com facenet.tflite de 128-d ou 512-d.
 */
class FaceNetModel private constructor(context: Context) {

    companion object {
        private const val TAG = "FaceNet"

        @Volatile
        private var instance: FaceNetModel? = null

        fun get(context: Context): FaceNetModel {
            return instance ?: synchronized(this) {
                instance ?: FaceNetModel(context.applicationContext).also { instance = it }
            }
        }
    }

    private var interpreter: Interpreter? = null
    var inputSize: Int = 160
        private set
    var embeddingDim: Int = 128
        private set
    var isReady: Boolean = false
        private set

    init {
        try {
            val model = FileUtil.loadMappedFile(context, "facenet.tflite")
            val options = Interpreter.Options().apply {
                setNumThreads(4)
            }
            val interp = Interpreter(model, options)

            // Detectar dimensões automaticamente do modelo
            val inputShape = interp.getInputTensor(0).shape()   // ex: [1, 160, 160, 3]
            val outputShape = interp.getOutputTensor(0).shape() // ex: [1, 128] ou [1, 512]

            inputSize = inputShape[1]   // 160
            embeddingDim = outputShape[1] // 128 ou 512

            interpreter = interp
            isReady = true

            Log.d(TAG, "✅ FaceNet carregado! Input: ${inputSize}x${inputSize}, Output: ${embeddingDim}-d")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro carregando facenet.tflite: ${e.message}")
            isReady = false
        }
    }

    /**
     * Gera embedding a partir de um rosto recortado.
     * Retorna null se o modelo não está carregado.
     */
    fun getEmbedding(faceBitmap: Bitmap): FloatArray? {
        val interp = interpreter ?: return null

        // Redimensionar para o tamanho esperado pelo modelo
        val resized = Bitmap.createScaledBitmap(faceBitmap, inputSize, inputSize, true)

        // Converter para ByteBuffer float32 normalizado [-1, 1]
        val bufferSize = inputSize * inputSize * 3 * 4 // 4 bytes per float
        val inputBuffer = ByteBuffer.allocateDirect(bufferSize)
        inputBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputSize * inputSize)
        resized.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        for (pixel in pixels) {
            val r = ((pixel shr 16) and 0xFF)
            val g = ((pixel shr 8) and 0xFF)
            val b = (pixel and 0xFF)
            inputBuffer.putFloat((r - 127.5f) / 127.5f)
            inputBuffer.putFloat((g - 127.5f) / 127.5f)
            inputBuffer.putFloat((b - 127.5f) / 127.5f)
        }

        // Output array
        val outputArray = Array(1) { FloatArray(embeddingDim) }

        inputBuffer.rewind()
        interp.run(inputBuffer, outputArray as Any)

        // Normalizar L2
        val embedding = outputArray[0]
        var sumSq = 0f
        for (v in embedding) sumSq += v * v
        val norm = kotlin.math.sqrt(sumSq).coerceAtLeast(0.0001f)
        for (i in embedding.indices) embedding[i] /= norm

        return embedding
    }
}
