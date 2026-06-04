package com.example.sentix

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.example.sentix.ml.EmotionImageClassifier
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
class EvaluacionCamaraActivity : BaseMenuActivity() {

    private lateinit var previewCamara: PreviewView
    private lateinit var txtEstadoCamara: TextView
    private lateinit var txtResultadoDev: TextView
    private lateinit var btnCapturarImagen: Button
    private lateinit var btnContinuarTest: Button

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var emotionClassifier: EmotionImageClassifier
    private lateinit var faceDetector: FaceDetector
    private var resultadoFacialInterno: EmotionImageClassifier.ResultadoEmocion? = null

    /*
     * Modo desarrollador:
     * true  = muestra emoción y confianza para pruebas.
     * false = oculta el resultado parcial al usuario final.
     */
    private val modoDesarrollador = true
    private var procesandoImagen = false
    private val pedirPermisoCamara =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { permitido ->
            if (permitido) {
                iniciarCamara()
            } else {
                txtEstadoCamara.text = "Permiso requerido"
                btnCapturarImagen.isEnabled = false
                btnContinuarTest.isEnabled = false

                Toast.makeText(
                    this,
                    "Para realizar la evaluación completa, es necesario permitir el uso de la cámara.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun getContenidoLayoutId(): Int {
        return R.layout.activity_evaluacion_camara
    }

    override fun onContenidoCreado() {
        enlazarVistas()
        configurarEventos()
        cargarDatosUsuarioLocal()

        cameraExecutor = Executors.newSingleThreadExecutor()
        emotionClassifier = EmotionImageClassifier(this)

        val opcionesDetectorRostro = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(0.18f)
            .enableTracking()
            .build()

        faceDetector = FaceDetection.getClient(opcionesDetectorRostro)

        if (!modoDesarrollador) {
            txtResultadoDev.visibility = View.GONE
        }

        verificarPermisoCamara()
    }

    override fun onUsuarioActualizado(cache: UsuarioCache) {
        /*
         * Este método se ejecuta cuando BaseMenuActivity carga datos desde caché
         * o Firebase. Lo dejamos preparado para futuras pantallas de evaluación.
         */
        if (uidActual.isBlank() && cache.uid.isNotBlank()) {
            uidActual = cache.uid
        }

        if (emailActual.isBlank() && cache.email.isNotBlank()) {
            emailActual = cache.email
        }
    }

    private fun enlazarVistas() {
        previewCamara = findViewById(R.id.previewCamara)
        txtEstadoCamara = findViewById(R.id.txtEstadoCamara)
        txtResultadoDev = findViewById(R.id.txtResultadoDev)
        btnCapturarImagen = findViewById(R.id.btnCapturarImagen)
        btnContinuarTest = findViewById(R.id.btnContinuarTest)
    }

    private fun configurarEventos() {
        btnCapturarImagen.setOnClickListener {
            capturarImagen()
        }

        btnContinuarTest.setOnClickListener {
            val resultado = resultadoFacialInterno

            if (resultado == null) {
                Toast.makeText(
                    this,
                    "Primero debes capturar la imagen facial.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            /*
             * Aquí luego abriremos EvaluacionTestActivity.
             * Por ahora confirmamos que el resultado facial ya quedó listo.
             */
            Toast.makeText(
                this,
                "Imagen registrada. Siguiente paso: test emocional.",
                Toast.LENGTH_LONG
            ).show()

            Log.d(
                "EVALUACION_FLUJO",
                "UID: $uidActual, Email: $emailActual, Resultado facial: ${resultado.etiqueta} - ${resultado.confianza}%"
            )
        }
    }

    private fun cargarDatosUsuarioLocal() {
        val cache = UsuarioCacheManager.obtener(this)

        if (uidActual.isBlank() && cache.uid.isNotBlank()) {
            uidActual = cache.uid
        }

        if (emailActual.isBlank() && cache.email.isNotBlank()) {
            emailActual = cache.email
        }

        Log.d("EVALUACION_CACHE", "UID actual: $uidActual")
        Log.d("EVALUACION_CACHE", "Email actual: $emailActual")
    }

    private fun verificarPermisoCamara() {
        val permiso = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        )

        if (permiso == PackageManager.PERMISSION_GRANTED) {
            iniciarCamara()
        } else {
            pedirPermisoCamara.launch(Manifest.permission.CAMERA)
        }
    }

    private fun iniciarCamara() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewCamara.surfaceProvider)
                    }

                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
                )

                txtEstadoCamara.text = "Cámara lista"
                btnCapturarImagen.isEnabled = true

                Log.d("CAMARA_SENTIX", "Cámara frontal iniciada correctamente")

            } catch (e: Exception) {
                Log.e("CAMARA_SENTIX", "Error al iniciar cámara", e)

                txtEstadoCamara.text = "Error de cámara"
                btnCapturarImagen.isEnabled = false

                Toast.makeText(
                    this,
                    "No se pudo iniciar la cámara.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun capturarImagen() {
        if (procesandoImagen) {
            return
        }

        val imageCaptureActual = imageCapture

        if (imageCaptureActual == null) {
            txtEstadoCamara.text = "Cámara no lista"
            return
        }

        procesandoImagen = true
        btnCapturarImagen.isEnabled = false
        btnContinuarTest.isEnabled = false
        txtEstadoCamara.text = "Analizando..."

        val archivoFoto = File(
            cacheDir,
            "sentix_eval_${System.currentTimeMillis()}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(archivoFoto).build()

        imageCaptureActual.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    analizarImagenCapturada(archivoFoto)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CAMARA_SENTIX", "Error al capturar imagen", exception)

                    runOnUiThread {
                        procesandoImagen = false
                        btnCapturarImagen.isEnabled = true
                        btnContinuarTest.isEnabled = resultadoFacialInterno != null
                        txtEstadoCamara.text = "Intenta de nuevo"
                    }
                }
            }
        )
    }

    private fun analizarImagenCapturada(archivoFoto: File) {
        try {
            val bitmapOriginal = BitmapFactory.decodeFile(archivoFoto.absolutePath)

            if (bitmapOriginal == null) {
                runOnUiThread {
                    procesandoImagen = false
                    btnCapturarImagen.isEnabled = true
                    txtEstadoCamara.text = "Imagen no válida"
                }
                return
            }

            val bitmapOrientado = corregirOrientacionImagen(
                bitmap = bitmapOriginal,
                rutaImagen = archivoFoto.absolutePath
            )

            val inputImage = InputImage.fromBitmap(bitmapOrientado, 0)

            faceDetector.process(inputImage)
                .addOnSuccessListener { rostros ->
                    if (rostros.isEmpty()) {
                        runOnUiThread {
                            procesandoImagen = false
                            btnCapturarImagen.isEnabled = true
                            btnContinuarTest.isEnabled = false
                            txtEstadoCamara.text = "Rostro no detectado"

                            if (modoDesarrollador) {
                                txtResultadoDev.text =
                                    "DEV\nNo se detectó ningún rostro.\nIntenta mirar de frente y acercarte al marco."
                            }
                        }
                        return@addOnSuccessListener
                    }

                    val rostroPrincipal = obtenerRostroPrincipal(rostros)

                    val bitmapRostro = recortarRostroDetectado(
                        bitmap = bitmapOrientado,
                        rectRostro = rostroPrincipal.boundingBox
                    )

                    val resultado = emotionClassifier.clasificar(bitmapRostro)
                    resultadoFacialInterno = resultado

                    Log.d("CNN_SENTIX", "Rostro detectado: ${rostroPrincipal.boundingBox}")
                    Log.d("CNN_SENTIX", "Clase facial: ${resultado.etiqueta}")
                    Log.d("CNN_SENTIX", "Clase traducida: ${resultado.etiquetaTraducida}")
                    Log.d("CNN_SENTIX", "Confianza: ${resultado.confianza}")
                    Log.d("CNN_SENTIX", "Probabilidades: ${resultado.probabilidades}")

                    runOnUiThread {
                        txtEstadoCamara.text = "Imagen registrada"
                        btnCapturarImagen.isEnabled = true
                        btnContinuarTest.isEnabled = true
                        procesandoImagen = false

                        if (modoDesarrollador) {
                            txtResultadoDev.text =
                                "DEV\n" +
                                        "Usuario: ${emailActual.ifBlank { "sin correo" }}\n" +
                                        "Rostro detectado: sí\n" +
                                        "Resultado facial: ${resultado.etiquetaTraducida}\n" +
                                        "Clase interna: ${resultado.etiqueta}\n" +
                                        "Confianza: ${"%.2f".format(resultado.confianza)}%\n\n" +
                                        "Probabilidades:\n" +
                                        resultado.probabilidades.entries.joinToString("\n") {
                                            "${it.key}: ${"%.2f".format(it.value)}%"
                                        }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MLKIT_SENTIX", "Error detectando rostro", e)

                    runOnUiThread {
                        procesandoImagen = false
                        btnCapturarImagen.isEnabled = true
                        btnContinuarTest.isEnabled = false
                        txtEstadoCamara.text = "Error al detectar rostro"

                        if (modoDesarrollador) {
                            txtResultadoDev.text =
                                "DEV\nError en ML Kit Face Detection:\n${e.message}"
                        }
                    }
                }

        } catch (e: Exception) {
            Log.e("CNN_SENTIX", "Error al analizar imagen", e)

            runOnUiThread {
                procesandoImagen = false
                btnCapturarImagen.isEnabled = true
                btnContinuarTest.isEnabled = false
                txtEstadoCamara.text = "Error de análisis"

                if (modoDesarrollador) {
                    txtResultadoDev.text =
                        "DEV\nError general al analizar imagen:\n${e.message}"
                }
            }
        }
    }
    private fun corregirOrientacionImagen(bitmap: Bitmap, rutaImagen: String): Bitmap {
        return try {
            val exif = ExifInterface(rutaImagen)

            val orientacion = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            val grados = when (orientacion) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }

            if (grados == 0f) {
                bitmap
            } else {
                val matrix = Matrix()
                matrix.postRotate(grados)

                Bitmap.createBitmap(
                    bitmap,
                    0,
                    0,
                    bitmap.width,
                    bitmap.height,
                    matrix,
                    true
                )
            }

        } catch (e: Exception) {
            Log.e("CAMARA_SENTIX", "No se pudo corregir orientación", e)
            bitmap
        }
    }

    private fun obtenerRostroPrincipal(rostros: List<Face>): Face {
        return rostros.maxByOrNull { rostro ->
            rostro.boundingBox.width() * rostro.boundingBox.height()
        } ?: rostros.first()
    }

    private fun recortarRostroDetectado(bitmap: Bitmap, rectRostro: Rect): Bitmap {
        val anchoImagen = bitmap.width
        val altoImagen = bitmap.height

        val centroX = rectRostro.centerX()
        val centroY = rectRostro.centerY()

        /*
         * Margen para que el recorte no sea solo ojos/nariz/boca.
         * Incluye frente, mentón y un poco de contexto, parecido al dataset.
         */
        val ladoBase = maxOf(rectRostro.width(), rectRostro.height())
        val ladoRecorte = (ladoBase * 1.55f).toInt()

        var x = centroX - ladoRecorte / 2
        var y = centroY - ladoRecorte / 2

        /*
         * Subimos un poco el recorte para incluir mejor frente y ojos.
         */
        y -= (ladoRecorte * 0.08f).toInt()

        x = x.coerceIn(0, (anchoImagen - ladoRecorte).coerceAtLeast(0))
        y = y.coerceIn(0, (altoImagen - ladoRecorte).coerceAtLeast(0))

        val anchoFinal = ladoRecorte.coerceAtMost(anchoImagen - x)
        val altoFinal = ladoRecorte.coerceAtMost(altoImagen - y)

        val ladoFinal = minOf(anchoFinal, altoFinal)

        Log.d(
            "MLKIT_SENTIX",
            "Recorte rostro -> x:$x y:$y lado:$ladoFinal rectOriginal:$rectRostro"
        )

        return Bitmap.createBitmap(
            bitmap,
            x,
            y,
            ladoFinal,
            ladoFinal
        )
    }

    override fun onMenuEvaluacionSeleccionada() {
        ocultarMenu()
    }

    override fun onDestroy() {
        super.onDestroy()

        if (::cameraExecutor.isInitialized) {
            cameraExecutor.shutdown()
        }

        if (::emotionClassifier.isInitialized) {
            emotionClassifier.cerrar()
        }
        if (::faceDetector.isInitialized) {
            faceDetector.close()
        }
    }
}