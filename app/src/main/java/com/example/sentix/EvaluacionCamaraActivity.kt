package com.example.sentix
import android.content.Intent
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
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import java.util.Locale

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
    private val modoDesarrollador = false
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

            val intent = Intent(this, EvaluacionTestActivity::class.java)
            intent.putExtra("uid", uidActual)
            intent.putExtra("email", emailActual)

            intent.putExtra("emocionFacial", resultado.etiqueta)
            intent.putExtra("emocionFacialTraducida", resultado.etiquetaTraducida)
            intent.putExtra("confianzaFacial", resultado.confianza)

            Log.d(
                "EVALUACION_FLUJO",
                "Pasando al test -> UID: $uidActual, Email: $emailActual, Facial: ${resultado.etiqueta} - ${resultado.confianza}%"
            )

            startActivity(intent)
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
        resultadoFacialInterno = null
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
    private fun irAlTestConResultado(resultado: EmotionImageClassifier.ResultadoEmocion) {
        val intent = Intent(this, EvaluacionTestActivity::class.java)

        intent.putExtra("uid", uidActual)
        intent.putExtra("email", emailActual)

        intent.putExtra("emocionFacial", resultado.etiqueta)
        intent.putExtra("emocionFacialTraducida", resultado.etiquetaTraducida)
        intent.putExtra("confianzaFacial", resultado.confianza)

        Log.d(
            "EVALUACION_FLUJO",
            "Pasando al test -> UID: $uidActual, Email: $emailActual, Facial: ${resultado.etiqueta} - ${resultado.confianza}%"
        )

        startActivity(intent)
    }
    private fun analizarImagenCapturada(archivoFoto: File) {
        try {
            val bitmapOriginal = BitmapFactory.decodeFile(archivoFoto.absolutePath)

            if (bitmapOriginal == null) {
                runOnUiThread {
                    procesandoImagen = false
                    resultadoFacialInterno = null
                    btnCapturarImagen.isEnabled = true
                    btnContinuarTest.isEnabled = false
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
                            resultadoFacialInterno = null
                            btnCapturarImagen.isEnabled = true
                            btnContinuarTest.isEnabled = false
                            txtEstadoCamara.text = "Rostro no detectado"

                            Toast.makeText(
                                this,
                                "Ubica tu rostro dentro del marco e intenta nuevamente.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        return@addOnSuccessListener
                    }

                    val rostroPrincipal = obtenerRostroPrincipal(rostros)

                    val bitmapRostro = recortarRostroDetectado(
                        bitmap = bitmapOrientado,
                        rectRostro = rostroPrincipal.boundingBox
                    )

                    val preparacion = prepararRostroParaCnn(bitmapRostro)
                    val bitmapParaCnn = preparacion.first
                    val estadoIluminacion = preparacion.second

                    if (bitmapParaCnn == null) {
                        Log.d("LUZ_SENTIX", "Imagen rechazada: $estadoIluminacion")

                        runOnUiThread {
                            procesandoImagen = false
                            resultadoFacialInterno = null
                            btnCapturarImagen.isEnabled = true
                            btnContinuarTest.isEnabled = false
                            txtEstadoCamara.text = "Mejora la iluminación"

                            Toast.makeText(
                                this,
                                "Busca mejor iluminación e intenta nuevamente.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        return@addOnSuccessListener
                    }

                    val resultado = emotionClassifier.clasificar(bitmapParaCnn)
                    resultadoFacialInterno = resultado

                    Log.d("CNN_SENTIX", "Rostro detectado: ${rostroPrincipal.boundingBox}")
                    Log.d("CNN_SENTIX", "Iluminación: $estadoIluminacion")
                    Log.d("CNN_SENTIX", "Clase facial: ${resultado.etiqueta}")
                    Log.d("CNN_SENTIX", "Clase traducida: ${resultado.etiquetaTraducida}")
                    Log.d("CNN_SENTIX", "Confianza: ${resultado.confianza}")
                    Log.d("CNN_SENTIX", "Probabilidades: ${resultado.probabilidades}")

                    runOnUiThread {
                        txtEstadoCamara.text = "Imagen registrada"
                        btnCapturarImagen.isEnabled = true
                        btnContinuarTest.isEnabled = false
                        procesandoImagen = false

                        /*
                         * Modo usuario final:
                         * No mostramos resultado CNN.
                         * Pasamos directamente al test emocional.
                         */
                        irAlTestConResultado(resultado)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MLKIT_SENTIX", "Error detectando rostro", e)

                    runOnUiThread {
                        procesandoImagen = false
                        resultadoFacialInterno = null
                        btnCapturarImagen.isEnabled = true
                        btnContinuarTest.isEnabled = false
                        txtEstadoCamara.text = "Intenta nuevamente"

                        Toast.makeText(
                            this,
                            "No se pudo validar el rostro. Intenta otra vez.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

        } catch (e: Exception) {
            Log.e("CNN_SENTIX", "Error al analizar imagen", e)

            runOnUiThread {
                procesandoImagen = false
                resultadoFacialInterno = null
                btnCapturarImagen.isEnabled = true
                btnContinuarTest.isEnabled = false
                txtEstadoCamara.text = "Intenta nuevamente"

                Toast.makeText(
                    this,
                    "No se pudo analizar la imagen. Intenta otra vez.",
                    Toast.LENGTH_SHORT
                ).show()
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
    private fun calcularBrilloPromedio(bitmap: Bitmap): Float {
        /*
         * Calcula brillo promedio del rostro.
         * Rango aproximado:
         * 0   = negro
         * 255 = muy claro
         */
        val ancho = bitmap.width
        val alto = bitmap.height

        val escala = 4
        val bitmapReducido = Bitmap.createScaledBitmap(
            bitmap,
            (ancho / escala).coerceAtLeast(1),
            (alto / escala).coerceAtLeast(1),
            true
        )

        var sumaBrillo = 0.0
        var totalPixeles = 0

        val pixels = IntArray(bitmapReducido.width * bitmapReducido.height)

        bitmapReducido.getPixels(
            pixels,
            0,
            bitmapReducido.width,
            0,
            0,
            bitmapReducido.width,
            bitmapReducido.height
        )

        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF

            /*
             * Fórmula de luminancia perceptual.
             * Da más peso al verde porque el ojo humano lo percibe más.
             */
            val brillo = 0.299 * r + 0.587 * g + 0.114 * b

            sumaBrillo += brillo
            totalPixeles++
        }

        return if (totalPixeles > 0) {
            (sumaBrillo / totalPixeles).toFloat()
        } else {
            0f
        }
    }

    private fun ajustarIluminacionAdaptativa(bitmap: Bitmap, calidad: CalidadIluminacion): Bitmap {
        /*
         * Ajuste adaptativo:
         * - Si está bajo de luz real, sube brillo suavemente.
         * - Si sale con brillo alto por autoexposición, baja un poco brillo.
         * - Mantiene colores naturales.
         */
        val bitmapAjustado = Bitmap.createBitmap(
            bitmap.width,
            bitmap.height,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmapAjustado)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val contraste: Float
        val brillo: Float

        when {
            calidad.brilloPromedio < 90f -> {
                contraste = 1.12f
                brillo = 14f
            }

            calidad.brilloPromedio > 118f -> {
                contraste = 1.08f
                brillo = -12f
            }

            else -> {
                contraste = 1.04f
                brillo = 4f
            }
        }

        val translate = (-0.5f * contraste + 0.5f) * 255f + brillo

        val colorMatrix = ColorMatrix(
            floatArrayOf(
                contraste, 0f, 0f, 0f, translate,
                0f, contraste, 0f, 0f, translate,
                0f, 0f, contraste, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return bitmapAjustado
    }
    private fun obtenerZonaCentralRostro(bitmap: Bitmap): Bitmap {
        /*
         * Medimos solo la zona central del recorte, porque ahí está el rostro.
         * Evitamos que pared, fondo o ropa alteren el promedio de iluminación.
         */
        val porcentaje = 0.62f

        val anchoZona = (bitmap.width * porcentaje).toInt()
        val altoZona = (bitmap.height * porcentaje).toInt()

        val x = ((bitmap.width - anchoZona) / 2).coerceAtLeast(0)
        val y = ((bitmap.height - altoZona) / 2).coerceAtLeast(0)

        return Bitmap.createBitmap(
            bitmap,
            x,
            y,
            anchoZona.coerceAtMost(bitmap.width - x),
            altoZona.coerceAtMost(bitmap.height - y)
        )
    }
    private fun analizarCalidadIluminacion(bitmap: Bitmap): CalidadIluminacion {
        /*
         * Importante:
         * Analizamos solo el centro del rostro, no todo el recorte.
         */
        val zonaRostro = obtenerZonaCentralRostro(bitmap)

        val escala = 4

        val bitmapReducido = Bitmap.createScaledBitmap(
            zonaRostro,
            (zonaRostro.width / escala).coerceAtLeast(1),
            (zonaRostro.height / escala).coerceAtLeast(1),
            true
        )

        val pixels = IntArray(bitmapReducido.width * bitmapReducido.height)

        bitmapReducido.getPixels(
            pixels,
            0,
            bitmapReducido.width,
            0,
            0,
            bitmapReducido.width,
            bitmapReducido.height
        )

        val luminancias = FloatArray(pixels.size)

        var suma = 0f
        var sombras = 0
        var quemados = 0

        for (i in pixels.indices) {
            val pixel = pixels[i]

            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF

            val luminancia = (0.299f * r + 0.587f * g + 0.114f * b)

            luminancias[i] = luminancia
            suma += luminancia

            if (luminancia < 55f) {
                sombras++
            }

            if (luminancia > 225f) {
                quemados++
            }
        }

        val total = luminancias.size.coerceAtLeast(1)
        val promedio = suma / total

        var sumaDiferencias = 0f

        for (valor in luminancias) {
            val diferencia = valor - promedio
            sumaDiferencias += diferencia * diferencia
        }

        val contraste = kotlin.math.sqrt(sumaDiferencias / total)

        val porcentajeSombras = (sombras.toFloat() / total.toFloat()) * 100f
        val porcentajeQuemados = (quemados.toFloat() / total.toFloat()) * 100f

        return CalidadIluminacion(
            brilloPromedio = promedio,
            contraste = contraste,
            porcentajeSombras = porcentajeSombras,
            porcentajeQuemados = porcentajeQuemados
        )
    }
    private fun prepararRostroParaCnn(bitmapRostro: Bitmap): Pair<Bitmap?, String> {
        val calidad = analizarCalidadIluminacion(bitmapRostro)

        val detalleCalidad = String.format(
            Locale.US,
            "brillo=%.1f contraste=%.1f sombras=%.1f%% quemados=%.1f%%",
            calidad.brilloPromedio,
            calidad.contraste,
            calidad.porcentajeSombras,
            calidad.porcentajeQuemados
        )

        Log.d("LUZ_SENTIX", detalleCalidad)

        /*
         * Reglas usando zona central del rostro:
         *
         * <= 118: imagen aceptable, pasa directo.
         * > 118: puede ser autoexposición o luz rara, se ajusta suavemente.
         *
         * Se rechaza si:
         * - hay demasiadas sombras reales en el rostro
         * - hay zonas muy quemadas
         * - el contraste es muy bajo
         */
        return when {
            calidad.porcentajeSombras > 48f -> {
                null to "rechazada:sombras_altas:$detalleCalidad"
            }

            calidad.porcentajeQuemados > 22f -> {
                null to "rechazada:sobreexpuesta:$detalleCalidad"
            }

            calidad.contraste < 16f -> {
                null to "rechazada:bajo_contraste:$detalleCalidad"
            }

            calidad.brilloPromedio <= 118f -> {
                bitmapRostro to "normal:$detalleCalidad"
            }

            else -> {
                val rostroAjustado = ajustarIluminacionAdaptativa(bitmapRostro, calidad)
                rostroAjustado to "ajustada:$detalleCalidad"
            }
        }
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

data class CalidadIluminacion(
    val brilloPromedio: Float,
    val contraste: Float,
    val porcentajeSombras: Float,
    val porcentajeQuemados: Float
)