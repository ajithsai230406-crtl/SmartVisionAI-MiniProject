package com.smartvision.ai.detector.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.smartvision.ai.detector.domain.*
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

// ─── TFLite Object Detection Repository ──────────────────────────────────────
@Singleton
class ObjectDetectionRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()
    private var imageProcessor: ImageProcessor? = null

    // Number of output detections — determined dynamically from the model's tensor shapes
    private var numDetections: Int = 25

    private val initScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val initJob: Job by lazy {
        initScope.launch {
            try {
                val modelBuffer = FileUtil.loadMappedFile(context, "efficientdet_lite4.tflite")
                val options = Interpreter.Options().apply {
                    setNumThreads(4)
                }
                interpreter = Interpreter(modelBuffer, options)
                labels = FileUtil.loadLabels(context, "coco_labels.txt")
                android.util.Log.d("ObjectDetector", "Labels loaded: ${labels.size} classes successfully.")

                val tfl = interpreter!!
                val boxShape = tfl.getOutputTensor(0).shape()
                if (boxShape.size >= 2) numDetections = boxShape[1]

                val inputTensor = tfl.getInputTensor(0)
                val inputShape = inputTensor.shape()
                val inputDataType = inputTensor.dataType()
                val targetHeight = if (inputShape.size >= 3) inputShape[1] else 640
                val targetWidth = if (inputShape.size >= 3) inputShape[2] else 640

                val processorBuilder = ImageProcessor.Builder()
                    .add(ResizeOp(targetHeight, targetWidth, ResizeOp.ResizeMethod.BILINEAR))
                
                if (inputDataType == DataType.FLOAT32) {
                    processorBuilder.add(org.tensorflow.lite.support.common.ops.NormalizeOp(0.0f, 255.0f))
                    android.util.Log.d("ObjectDetector", "Float32 input tensor detected, adding NormalizeOp(0f, 255f) to pipeline")
                }
                imageProcessor = processorBuilder.build()

                android.util.Log.d(
                    "ObjectDetector",
                    "Model loaded. Input: ${inputShape.contentToString()} | " +
                    "Output 0: ${tfl.getOutputTensor(0).shape().contentToString()} type=${tfl.getOutputTensor(0).dataType()} | " +
                    "Output 1: ${tfl.getOutputTensor(1).shape().contentToString()} | " +
                    "Output 2: ${tfl.getOutputTensor(2).shape().contentToString()} | " +
                    "Output 3: ${tfl.getOutputTensor(3).shape().contentToString()} | " +
                    "numDetections=$numDetections"
                )
            } catch (e: Exception) {
                android.util.Log.e("ObjectDetector", "Failed to load model", e)
            }
        }
    }

    private suspend fun ensureInitialized() {
        initJob.join()
    }

    private fun cleanLabel(label: String): String {
        return when (label.lowercase().trim()) {
            "cell phone"     -> "Phone"
            "tv"             -> "Monitor"
            "person"         -> "Person"
            "laptop"         -> "Laptop"
            "keyboard"       -> "Keyboard"
            "mouse"          -> "Mouse"
            "bottle"         -> "Bottle"
            "chair"          -> "Chair"
            "book"           -> "Book"
            "eye glasses"    -> "Eyeglasses"
            "dining table"   -> "Table"
            "potted plant"   -> "Plant"
            "hair drier"     -> "Hair Dryer"
            "sports ball"    -> "Ball"
            "teddy bear"     -> "Teddy Bear"
            "fire hydrant"   -> "Fire Hydrant"
            "street sign"    -> "Street Sign"
            "stop sign"      -> "Stop Sign"
            "parking meter"  -> "Parking Meter"
            "traffic light"  -> "Traffic Light"
            "baseball bat"   -> "Baseball Bat"
            "baseball glove" -> "Baseball Glove"
            "wine glass"     -> "Wine Glass"
            "hot dog"        -> "Hot Dog"
            "tennis racket"  -> "Tennis Racket"
            "backpack"       -> "Backpack"
            "handbag"        -> "Handbag"
            "suitcase"       -> "Suitcase"
            "scissors"       -> "Scissors"
            "toothbrush"     -> "Toothbrush"
            "remote"         -> "Remote Control"
            "clock"          -> "Clock"
            "vase"           -> "Vase"
            "umbrella"       -> "Umbrella"
            "frisbee"        -> "Frisbee"
            "skis"           -> "Ski Board"
            "snowboard"      -> "Snowboard"
            "surfboard"      -> "Surfboard"
            "kite"           -> "Kite"
            "skateboard"     -> "Skateboard"
            else -> label.split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
        }
    }

    /**
     * Sports/outdoor labels that EfficientDet-Lite commonly confuses with cylindrical
     * or reflective objects (bottles, flasks, phones, pens, etc.).
     * If such a label wins on a detection that is large/centered in frame, we suppress it.
     */
    private val likelyMislabeledSports = setOf(
        "tennis racket", "baseball bat", "frisbee", "surfboard",
        "skateboard", "snowboard", "skis", "kite", "sports ball",
        "baseball glove", "golf club"
    )

    /**
     * Applies a post-processing sanity check on a raw TFLite label:
     * If the detected label is a sports/outdoor item that is unlikely to appear
     * filling most of the camera frame in everyday use, and the bounding box covers
     * a large portion of the image (>30% area), override the label to "bottle"
     * (the most common large-frame false-positive in the COCO model).
     */
    private fun sanitizeLabel(
        rawLabel: String,
        boxArea: Float   // normalized area: (right-left)*(bottom-top), 0.0–1.0
    ): String {
        val lower = rawLabel.lowercase().trim()
        return if (lower in likelyMislabeledSports && boxArea > 0.30f) {
            android.util.Log.d(
                "ObjectDetector",
                "Sanity override: '$rawLabel' (area=$boxArea) is likely a bottle/object — overriding label to 'bottle'"
            )
            "bottle"
        } else {
            rawLabel
        }
    }

    /**
     * Detect objects from an in-memory [Bitmap].
     * Runs TFLite EfficientDet-Lite4 COCO model.
     *
     * EfficientDet-Lite output tensor order:
     *   0 → detection_boxes     [1, N, 4]  float32  [ymin, xmin, ymax, xmax] (0.0–1.0)
     *   1 → detection_classes   [1, N]     float32  class index
     *   2 → detection_scores    [1, N]     float32  confidence (0.0–1.0)
     *   3 → num_detections      [1]        float32  count of valid detections
     */
    suspend fun detectFromBitmap(bitmap: Bitmap): Result<List<DetectedItem>> =
        withContext(Dispatchers.Default) {
            runCatching {
                ensureInitialized()
                val tflite = interpreter ?: return@runCatching emptyList<DetectedItem>()
                val processor = imageProcessor ?: return@runCatching emptyList<DetectedItem>()

                // ── Pre-process image dynamically based on model input shape and type ──
                val inputTensor = tflite.getInputTensor(0)
                val inputDataType = inputTensor.dataType()

                val tensorImage = TensorImage(inputDataType)
                tensorImage.load(bitmap)
                val processedImage = processor.process(tensorImage)

                // ── Allocate output buffers ────────────────────────────────────
                val n = numDetections
                val outputBoxes   = Array(1) { Array(n) { FloatArray(4) } }   // [1,N,4]
                val outputClasses = Array(1) { FloatArray(n) }                 // [1,N]
                val outputScores  = Array(1) { FloatArray(n) }                 // [1,N]
                val outputCount   = FloatArray(1)                               // [1]

                // Standard EfficientDet-Lite TFLite output index mapping
                val outputs = mapOf(
                    0 to outputBoxes,
                    1 to outputClasses,
                    2 to outputScores,
                    3 to outputCount
                )

                tflite.runForMultipleInputsOutputs(
                    arrayOf(processedImage.buffer),
                    outputs
                )

                val actualCount = outputCount[0].toInt().coerceIn(0, n)
                val detectedItems = mutableListOf<DetectedItem>()

                for (i in 0 until actualCount) {
                    val confidence = outputScores[0][i]

                    // Raise threshold to 0.60 for reliable results; low-confidence detections
                    // from EfficientDet-Lite4 tend to produce wrong labels (e.g. bottle → tennis racket)
                    if (confidence < 0.60f) continue

                    val classId = outputClasses[0][i].toInt()
                    val rawLabel = labels.getOrElse(classId) { "Object" }

                    // Compute normalized bounding-box area for the sanity check
                    val ymin = outputBoxes[0][i][0].coerceIn(0f, 1f)
                    val xmin = outputBoxes[0][i][1].coerceIn(0f, 1f)
                    val ymax = outputBoxes[0][i][2].coerceIn(0f, 1f)
                    val xmax = outputBoxes[0][i][3].coerceIn(0f, 1f)
                    val boxArea = (xmax - xmin) * (ymax - ymin)

                    // Apply sanity override (e.g. large "tennis racket" → "bottle")
                    val sanitizedRaw = sanitizeLabel(rawLabel, boxArea)
                    val label = cleanLabel(sanitizedRaw)
                    val category = label.toCategory()

                    // EfficientDet box coordinates: [ymin, xmin, ymax, xmax] normalized 0–1
                    val box = DetectionBox(
                        left   = xmin,
                        top    = ymin,
                        right  = xmax,
                        bottom = ymax
                    )

                    android.util.Log.d(
                        "ObjectDetector",
                        "Inference result: id=$i | rawLabel=$rawLabel | mappedLabel=$label | confidence=${(confidence*100).toInt()}% | box=[left=${box.left}, top=${box.top}, right=${box.right}, bottom=${box.bottom}]"
                    )

                    detectedItems.add(
                        DetectedItem(
                            id          = i,
                            label       = label,
                            confidence  = confidence,
                            box         = box,
                            category    = category,
                            accentColor = category.colorArgb
                        )
                    )
                }

                android.util.Log.d(
                    "ObjectDetector",
                    "Detected $actualCount raw, ${detectedItems.size} above 50% threshold"
                )

                // Sort by confidence descending — highest confidence object first
                detectedItems.sortedByDescending { it.confidence }
            }
        }

    /**
     * Detect objects from a gallery [Uri].
     */
    suspend fun detectFromUri(context: Context, uri: Uri): Result<List<DetectedItem>> =
        withContext(Dispatchers.Default) {
            runCatching {
                ensureInitialized()
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: return@runCatching emptyList<DetectedItem>()
                val bitmap = BitmapFactory.decodeStream(inputStream)
                    ?: return@runCatching emptyList<DetectedItem>()
                detectFromBitmap(bitmap).getOrThrow()
            }
        }

    fun close() {
        initScope.cancel()
        interpreter?.close()
    }
}
