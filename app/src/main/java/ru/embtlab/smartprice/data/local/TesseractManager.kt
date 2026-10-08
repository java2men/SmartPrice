package ru.embtlab.smartprice.data.local

import android.content.Context
import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class TesseractManager(private val context: Context) {

    private var tessApi: TessBaseAPI? = null
    private var isInitialized = false

    suspend fun init(): Boolean = withContext(Dispatchers.IO) {
        if (isInitialized) return@withContext true

        val tessDir = File(context.filesDir, "tessdata")
        if (!tessDir.exists()) {
            tessDir.mkdirs()
        }

        // Копируем обе языковые модели: русскую и английскую
        val requiredModels = listOf("rus.traineddata", "eng.traineddata")
        for (modelName in requiredModels) {
            val modelFile = File(tessDir, modelName)
            if (!modelFile.exists() || modelFile.length() == 0L) {
                try {
                    context.assets.open("tessdata/$modelName").use { input ->
                        FileOutputStream(modelFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        tessApi = TessBaseAPI().apply {
            // Инициализируем двуязычный режим (rus+eng)
            val success = init(context.filesDir.absolutePath, "rus+eng")
            if (success) {
                pageSegMode = TessBaseAPI.PageSegMode.PSM_SPARSE_TEXT
            }
            isInitialized = success
        }

        isInitialized
    }

    suspend fun recognizeText(bitmap: Bitmap): String = withContext(Dispatchers.Default) {
        val api = tessApi ?: return@withContext ""
        try {
            api.setImage(bitmap)
            api.utF8Text ?: ""
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun close() {
        tessApi?.recycle()
        tessApi = null
        isInitialized = false
    }
}