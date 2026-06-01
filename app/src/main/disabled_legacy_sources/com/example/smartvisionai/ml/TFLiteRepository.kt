package com.example.smartvisionai.ml

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.label.TensorLabel
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import javax.inject.Inject
import javax.inject.Singleton

data class ClassificationResult(
    val label: String,
    val confidence: Float
)

@Singleton
class TFLiteRepository @Inject constructor(
    private val context: Context
) {
    // Waste classification model (place waste_classifier.tflite in assets/)
    private var wasteInterpreter: Interpreter? = null
    private var wasteLabels: List<String> = emptyList()

    // Object detection model (place object_detection.tflite in assets/)
    private var objectInterpreter: Interpreter? = null
    private var objectLabels: List<String> = emptyList()

    private val wasteModelFile = "waste_classifier.tflite"
    private val wasteLabelsFile = "waste_labels.txt"
    private val objectModelFile = "efficientdet_lite0.tflite"
    private val objectLabelsFile = "coco_labels.txt"

    init {
        loadModels()
    }

    private fun loadModels() {
        // Load waste classifier
        try {
            val wasteModel = FileUtil.loadMappedFile(context, wasteModelFile)
            wasteInterpreter = Interpreter(wasteModel)
            wasteLabels = FileUtil.loadLabels(context, wasteLabelsFile)
        } catch (_: Exception) {
            // Model not present — will fallback to Gemini
        }

        // Load object detection
        try {
            val objectModel = FileUtil.loadMappedFile(context, objectModelFile)
            objectInterpreter = Interpreter(objectModel)
            objectLabels = FileUtil.loadLabels(context, objectLabelsFile)
        } catch (_: Exception) {
            // Model not present — will fallback to ML Kit
        }
    }

    /**
     * Classify waste from a bitmap.
     * Returns top-3 results sorted by confidence descending.
     */
    fun classifyWaste(bitmap: Bitmap): List<ClassificationResult> {
        val interpreter = wasteInterpreter ?: return emptyList()

        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
            .build()

        val tensorImage = TensorImage.fromBitmap(bitmap)
        val processedImage = imageProcessor.process(tensorImage)

        // Output buffer: [1, numLabels]
        val outputShape = interpreter.getOutputTensor(0).shape()
        val outputBuffer = TensorBuffer.createFixedSize(outputShape,
            interpreter.getOutputTensor(0).dataType())

        interpreter.run(processedImage.buffer, outputBuffer.buffer.rewind())

        val labelOutput = TensorLabel(wasteLabels, outputBuffer)
        return labelOutput.mapWithFloatValue
            .entries
            .map { ClassificationResult(it.key, it.value) }
            .sortedByDescending { it.confidence }
            .take(3)
    }

    /**
     * Run EfficientDet object detection on a bitmap.
     * Returns top results with labels and confidence.
     */
    fun detectObjectsTFLite(bitmap: Bitmap): List<ClassificationResult> {
        val interpreter = objectInterpreter ?: return emptyList()
        // EfficientDet-Lite0 input: [1, 320, 320, 3]
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(320, 320, ResizeOp.ResizeMethod.BILINEAR))
            .build()
        val tensorImage = TensorImage.fromBitmap(bitmap)
        val processedImage = imageProcessor.process(tensorImage)

        // EfficientDet outputs: [boxes, classes, scores, count]
        val outputScores  = Array(1) { FloatArray(25) }
        val outputClasses = Array(1) { FloatArray(25) }
        val outputCount   = FloatArray(1)

        val outputs = mapOf(
            0 to Array(1) { Array(25) { FloatArray(4) } }, // boxes
            1 to outputClasses,
            2 to outputScores,
            3 to outputCount
        )

        interpreter.runForMultipleInputsOutputs(
            arrayOf(processedImage.buffer), outputs
        )

        val count = outputCount[0].toInt().coerceAtMost(25)
        return (0 until count)
            .filter { outputScores[0][it] > 0.4f }
            .map { i ->
                val labelIdx = outputClasses[0][i].toInt()
                val label = objectLabels.getOrElse(labelIdx) { "Unknown" }
                ClassificationResult(label, outputScores[0][i])
            }
            .sortedByDescending { it.confidence }
    }

    fun close() {
        wasteInterpreter?.close()
        objectInterpreter?.close()
    }
}
