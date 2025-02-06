package com.krungsri.kma.detect_liveness

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.drawable.VectorDrawable
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.text.TextUtils
import android.util.Size
import android.view.accessibility.AccessibilityManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.example.poc_webview_payment.R
import com.example.poc_webview_payment.databinding.ActivityFacialLivenessScanBinding
import com.krungsri.kma.detect_liveness.StateManager.getMessage
import com.krungsri.kma.detect_liveness.StateManager.isAction
import com.krungsri.kma.detect_liveness.StateManager.isWarning
import com.krungsri.kma.detect_liveness.StateManager.mapAnimation
import tech.ainu.facial.liveness.AinuLiveness
import tech.ainu.facial.liveness.AinuLiveness.Env
import tech.ainu.facial.liveness.AinuLiveness.ImageOutputType
import tech.ainu.facial.liveness.LivenessState
import tech.ainu.facial.liveness.OnLivenessListener
import tech.ainu.facial.liveness.ResultCode
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

import android.view.MotionEvent


@androidx.annotation.OptIn(androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)
internal open class AinuLivenessActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_LIVENESS_RESULT_CODE = "extra.liveness.result.code"
        const val REQUEST_ACTIVITY_CODE = 200123

        private const val REQUEST_PERMISSION_CODE = 200
        private const val TIME_VIBRATOR = 200L
    }

    private val binding: ActivityFacialLivenessScanBinding by lazy {
        ActivityFacialLivenessScanBinding.inflate(layoutInflater)
    }

    private var executors: ExecutorService? = null
    private var imageAnalysis: ImageAnalysis? = null
    private lateinit var vibrator: Vibrator
    private lateinit var rectCam: Rect
    private lateinit var rectMask: Rect
    private var isUICreated = false
    // config variable
    private var env: Env = Env.UAT
    private var outputType: ImageOutputType? = null
    private var actions: ArrayList<AinuLiveness.Action>? = null
    private var numOfAction: Int? = null
    private var enableSignature: Boolean? = null
    private var faceHeightPercentage: Int? = null
    private var maxWidth: Int? = null
    private var maxFileSize: Int? = null
    private var camera: AinuLiveness.Camera? = null

    private var multipleFace = true
    private var mouthVisible = true
    private var mouthOpen = true
    private var noseVisible = true
    private var eye = true
    private var brightness = true
    private var dark = true

    // UI handler
    private var backgroundWithMask: Bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
    private var animateId: Int = R.raw.head_in_frame
    private var isAlert = false
    private var isStart = false

    private var isInitFacialLiveness = false
    private var previousAction = LivenessState.NOTSTART

    private var isEn = false
    private var isBreakerOverlay = false
    private var isDeviceInWhiteList = false

    private val listener = object : OnLivenessListener {
        override fun onStateUpdate(livenessState: LivenessState) {
            handleMessage(livenessState)
//            animateId = mapAnimation(livenessState)
            runOnUiThread {
                binding.animeStatus.setAnimation(animateId)
                binding.animeStatus.playAnimation()
            }

            updateAlert(livenessState)
            modifyBackgroundWithMask(
                true,
                this@AinuLivenessActivity.resources,
                isWarning(livenessState),
                rectMask
            )
        }

        override fun onDetectionComplete(
            resultCode: ResultCode,
            image: Bitmap?,
            imageData: ByteArray?,
            metadata: String?,
            signature: String?,
            keyId: String?,
            log: String
        ) {
            isStart = false

            // set ค่าไปที่ ShareAinuLivenessResult สำหรับใช้งานที่หน้า MainActivity
            ShareAinuLivenessResult.resultCode = resultCode
            ShareAinuLivenessResult.log = log
            if (resultCode == ResultCode.OK) {
                ShareAinuLivenessResult.imageData = imageData?.clone()
                ShareAinuLivenessResult.metadata = metadata
                ShareAinuLivenessResult.signature = signature
                ShareAinuLivenessResult.keyId = keyId
            }

            // clear memory SDK
            isStart = false
            AinuLiveness.close()

            // set activity result เพื่อส่งกลับไปที่ MainActivity
            val errorCode = resultCode
            val newIntent = Intent()
            newIntent.putExtra(EXTRA_LIVENESS_RESULT_CODE, errorCode)
            setResult(Activity.RESULT_OK, newIntent)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initExtra()
        initFacialLiveness()
        grantPermission()
        initialUI()
    }

    val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            val accessibilityServiceEnabled: Boolean = getEnabledAccessibilityServiceList().size > 0
            if(accessibilityServiceEnabled){
                rejectAccessibilty()
            }

        }
    }

    override fun onResume() {
        super.onResume()

        var uri: android.net.Uri =
            android.provider.Settings.Secure.getUriFor(android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        contentResolver.registerContentObserver(uri, false, observer)
    }

    override fun onPause() {
        super.onPause()
        contentResolver.unregisterContentObserver(observer);
    }

    private fun initExtra() {
        enableSignature = intent.getBooleanExtra(AinuLivenessConstants.EXTRA_OUTPUT_IMAGE_SIGNATURE, true)
        outputType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(AinuLivenessConstants.EXTRA_OUTPUT_IMAGE_TYPE, ImageOutputType::class.java)
        } else {
            intent.getSerializableExtra(AinuLivenessConstants.EXTRA_OUTPUT_IMAGE_TYPE) as ImageOutputType
        }
        val actionListString = intent.getStringArrayListExtra(AinuLivenessConstants.EXTRA_ACTION)
        actionListString?.forEach {
            if (actions == null) {
                actions = arrayListOf(AinuLiveness.Action.valueOf(it))
            } else {
                actions?.add(AinuLiveness.Action.valueOf(it))
            }
        }

        numOfAction = intent.getIntExtra(AinuLivenessConstants.EXTRA_NUM_OF_ACTION, 0)

        faceHeightPercentage = intent.getIntExtra(AinuLivenessConstants.EXTRA_OUTPUT_IMAGE_FACE_HEIGHT_PERCENTAGE, 70)
        maxWidth = intent.getIntExtra(AinuLivenessConstants.EXTRA_OUTPUT_IMAGE_MAX_WIDTH, 1000)
        maxFileSize = intent.getIntExtra(AinuLivenessConstants.EXTRA_OUTPUT_IMAGE_MAX_FILE_SIZE, 200)
        camera = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(AinuLivenessConstants.EXTRA_CAMERA, AinuLiveness.Camera::class.java)
        } else {
            intent.getSerializableExtra(AinuLivenessConstants.EXTRA_CAMERA) as AinuLiveness.Camera
        }

        multipleFace = intent.getBooleanExtra(AinuLivenessConstants.EXTRA_QUALITY_MULTIPLE_FACE, true)
        mouthVisible = intent.getBooleanExtra(AinuLivenessConstants.EXTRA_QUALITY_MOUTH_VISIBLE, true)
        mouthOpen = intent.getBooleanExtra(AinuLivenessConstants.EXTRA_QUALITY_MOUTH_OPEN, true)
        noseVisible = intent.getBooleanExtra(AinuLivenessConstants.EXTRA_QUALITY_NOSE_VISIBLE, true)
        eye = intent.getBooleanExtra(AinuLivenessConstants.EXTRA_QUALITY_EYE, true)
        brightness = intent.getBooleanExtra(AinuLivenessConstants.EXTRA_QUALITY_BRIGHTNESS, true)
        dark = intent.getBooleanExtra(AinuLivenessConstants.EXTRA_QUALITY_DARK, true)

        isEn = intent.getBooleanExtra("isEn", false)
        isBreakerOverlay = intent.getBooleanExtra("isBreakerOverlay", false)
        isDeviceInWhiteList = intent.getBooleanExtra("isDeviceInWhiteList", false)
    }

    private fun initFacialLiveness() {
        // set default
        handleMessage(LivenessState.NOTSTART)
        animateId = mapAnimation(LivenessState.NOTSTART)

        AinuLiveness.Builder(
            this,
            "X7pNrmAqctUe9VdevGCwE8zxe5tVs6hm",
            env ?: Env.UAT,
            listener,
        ).apply {

            // config SDK here
            if (this@AinuLivenessActivity.numOfAction != null) {
                setNumOfAction(this@AinuLivenessActivity.numOfAction!!)
            }

            if (this@AinuLivenessActivity.actions != null) {
                setAction(this@AinuLivenessActivity.actions!!)
            }

            if (this@AinuLivenessActivity.camera != null) {
                setCamera(this@AinuLivenessActivity.camera!!)
            }

            setOutputImageProperties(
                enableSignature = this@AinuLivenessActivity.enableSignature,
                outputType = this@AinuLivenessActivity.outputType,
                faceHeightPercentage = this@AinuLivenessActivity.faceHeightPercentage,
                maxWidth = this@AinuLivenessActivity.maxWidth,
                maxFileSize = this@AinuLivenessActivity.maxFileSize
            )

            configQuality(
                multipleFace = this@AinuLivenessActivity.multipleFace,
                mouthVisible = this@AinuLivenessActivity.mouthVisible,
                noseVisible = this@AinuLivenessActivity.noseVisible,
                mouthOpen = this@AinuLivenessActivity.mouthOpen,
                eye = this@AinuLivenessActivity.eye,
                brightness = this@AinuLivenessActivity.brightness,
                dark = this@AinuLivenessActivity.dark
            )

            setLoggingEnable(false)

        }.build()
        isInitFacialLiveness = true
    }

    private fun grantPermission() {
        @Suppress("DEPRECATION")
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.CAMERA,
                ), REQUEST_PERMISSION_CODE
            )
        }
    }

    fun backPressed(){
        setResult(777)
        finish()
    }

    private fun initialUI() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                isStart = false
                AinuLiveness.close()
                backPressed()
            }
        })

        binding.backBtn.setOnClickListener {
            isStart = false
            AinuLiveness.close()
            backPressed()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_PERMISSION_CODE -> {
                // If request is cancelled, the result arrays are empty.
                grantResults.forEachIndexed { index, it ->
                    if ((grantResults.isNotEmpty() && it == PackageManager.PERMISSION_GRANTED)) {
                        setupCameraProvider()
                    } else if (it == PackageManager.PERMISSION_DENIED) {
                        rejectDeniedPermission()
                    }
                }

                return
            }
        }
    }

    private fun setupCameraProvider() {
        ProcessCameraProvider.getInstance(this).also { provider ->
            provider.addListener(
                { bindPreview(provider.get()) },
                ContextCompat.getMainExecutor(this)
            )
        }
    }


    // setup camera
    private fun bindPreview(cameraProvider: ProcessCameraProvider) {

        val sizeCamera = Size(1440, 1920)
        val previewBuilder = Preview.Builder()

        val focusDistance = 3.0f // example: infinite focus
        val extender: Camera2Interop.Extender<*> = Camera2Interop.Extender(previewBuilder)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            extender.setCaptureRequestOption(
                CaptureRequest.CONTROL_AF_MODE,
                CameraMetadata.CONTROL_AF_MODE_OFF
            )
        }

        extender.setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, focusDistance)

        val preview = previewBuilder.setResolutionSelector(
            ResolutionSelector.Builder()
                .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                .setAspectRatioStrategy(
                    AspectRatioStrategy(
                        AspectRatio.RATIO_4_3, AspectRatioStrategy.FALLBACK_RULE_AUTO
                    )
                ).build()
        ).build()

        imageAnalysis = ImageAnalysis.Builder().setResolutionSelector(
            ResolutionSelector.Builder().setResolutionStrategy(
                ResolutionStrategy(
                    sizeCamera, ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
                )
            ).build()
        ).setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()

        val cameraSelector: CameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
            .build()

        executors = Executors.newSingleThreadExecutor()
        imageAnalysis?.setAnalyzer(
            executors!!
        ) { image ->
            val rotation = image.imageInfo.rotationDegrees
            if (isUICreated) {
                AinuLiveness.process(
                    image.toBitmap(),
                    rotation,
                    rectCam,
                    rectMask
                )
            }
            image.close()
        }

        val camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
        camera.cameraControl.cancelFocusAndMetering()
        camera.let {
            preview.setSurfaceProvider(binding.previewView.surfaceProvider)
        }
    }

    // calculate frame mask guid line
    private fun initialFrameCameraUI() {
        val maskDistance = 0.45
        val positionCamera = IntArray(2)
        binding.previewView.getLocationInWindow(positionCamera)

        val maskH = (binding.previewView.height * maskDistance).toInt()
        val maskW = (maskH * 0.85).toInt()

        rectCam = Rect(
            positionCamera[0],
            positionCamera[1],
            positionCamera[0] + binding.previewView.width,
            positionCamera[1] + binding.previewView.height
        )
        val positionMask = IntArray(2)
        binding.maskImageView.getLocationInWindow(positionMask)

        val xC = positionMask[0] + binding.maskImageView.width / 2
        val yC = positionMask[1] + binding.maskImageView.height / 2
        val rMaskH = maskH / 2
        val rMaskW = maskW / 2
        val startX = xC - rMaskW
        val startY = yC - rMaskH

        rectMask = Rect(
            startX,
            startY,
            startX + maskW,
            startY + maskH
        )

        generateBackgroundWithMask(resources, rectCam, rectMask)

        if (!isStart) {
            AinuLiveness.start()
            isStart = true
        }

        isUICreated = true
    }

    private fun activateVibrator() {
        val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, TIME_VIBRATOR.toInt())
        if (vibrator.hasVibrator()) {
            // Device supports vibration
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Vibration effects for Android Oreo (API 26) and above
                val vibrationEffect =
                    VibrationEffect.createOneShot(TIME_VIBRATOR, VibrationEffect.DEFAULT_AMPLITUDE)
                vibrator.vibrate(vibrationEffect)
            } else {
                // Vibration effects for older versions of Android
                vibrator.vibrate(TIME_VIBRATOR)
            }
        } else {
            // Device does not have a vibrator
            // Handle fallback or alternative feedback mechanisms
        }
    }

    private fun generateBackgroundWithMask(resources: Resources, rectCam: Rect, rectMask: Rect) {
        val catPosition = Point()
        catPosition.x = (rectCam.right - rectCam.left) / 2
        catPosition.y = rectMask.top - rectCam.top + (rectMask.bottom - rectMask.top) / 2

        val bitmap = Bitmap.createBitmap(
            rectCam.right - rectCam.left,
            rectCam.bottom - rectCam.top,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        canvas.drawColor(ResourcesCompat.getColor(resources, R.color.bg_facial_scan, null))

        val drawableMask = ResourcesCompat.getDrawable(
            resources,
            R.drawable.mask_cut,
            null
        ) as VectorDrawable
        val drawableMaskApply = ResourcesCompat.getDrawable(
            resources,
            R.drawable.mask_prepare,
            null
        ) as VectorDrawable

        val shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        val transparentPaint = Paint().apply {
            isAntiAlias = true
            this.shader = shader
        }
        transparentPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OUT)

        val outputBitmap = Bitmap.createBitmap(
            rectCam.right - rectCam.left,
            rectCam.bottom - rectCam.top,
            Bitmap.Config.ARGB_8888
        )
        val canvasMask = Canvas(outputBitmap)
        canvasMask.drawColor(Color.TRANSPARENT)

        drawableMask.bounds = rectMask
        drawableMaskApply.bounds = rectMask
        drawableMask.draw(canvasMask)

        canvasMask.drawBitmap(bitmap, 0f, 0f, transparentPaint)
        drawableMaskApply.draw(canvasMask)
        backgroundWithMask = outputBitmap
        runOnUiThread {
            binding.bgPreview.setImageBitmap(backgroundWithMask)
        }
    }

    fun modifyBackgroundWithMask(
        isStart: Boolean,
        resources: Resources,
        statusWarning: Boolean,
        rectMask: Rect,
    ) {

        val valueBitmap = backgroundWithMask
        val output = Bitmap.createBitmap(valueBitmap)
        val canvasMask = Canvas(output)

        val drawableMaskApply = if (!isStart) {
            ResourcesCompat.getDrawable(
                resources,
                R.drawable.mask_prepare,
                null
            ) as VectorDrawable
        } else if (!statusWarning) {
            ResourcesCompat.getDrawable(
                resources,
                R.drawable.mask_normal,
                null
            ) as VectorDrawable
        } else {
            ResourcesCompat.getDrawable(
                resources,
                R.drawable.mask_warning,
                null
            ) as VectorDrawable
        }

        drawableMaskApply.bounds = rectMask
        drawableMaskApply.draw(canvasMask)

        backgroundWithMask = output
        runOnUiThread {
            binding.bgPreview.setImageBitmap(backgroundWithMask)
        }
    }

    private fun updateAlert(action: LivenessState) {

        var statusAlert = false
        if (isAction(previousAction)) {
            if (action == LivenessState.NORMAL || action == LivenessState.FACE_NOT_FORWARD) {
                statusAlert = true
            }
        }

        isAlert = statusAlert
        if (isAlert) activateVibrator()
        previousAction = action
    }

    private fun handleMessage(status: LivenessState) {
        val (statusMessageTH, statusMessageEN) = getMessage(status)
        runOnUiThread {
            binding.statusFirstTextView.text = if(isEn) statusMessageEN else statusMessageTH
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) initialFrameCameraUI()

        if(getEnabledAccessibilityServiceList().size > 0){
            rejectAccessibilty()
        }
    }

    fun rejectDeniedPermission(){
        setResult(666)
        finish()
    }

    fun rejectAccessibilty(){
        setResult(999)
        finish()
    }

    fun rejectOverlay(){
        setResult(888)
        finish()
    }

    private fun getEnabledAccessibilityServiceList(): ArrayList<String> {
        val accessibilityManager = applicationContext?.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        // val enabledAccessibilityServiceList: List<AccessibilityServiceInfo>? = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)

        // Get Active AccessibilityList by package, (fix issue some device can't get EnableAccessibilitySettingsList) for example com.lgeha.nuts
        val accessibilityServiceInfoList : List<String>  = getEnableAccessibilitySettingsList()

        // Get Active AccessibilityList by package with another method, the result might be the same as the first one (some device can't get values) for example com.lgeha.nuts
        val accessibilityEnableServiceList : List<String>  = accessibilityManager
            .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .map { it.resolveInfo.serviceInfo.packageName }

        // Merge enable service AccessibilityList
        val enabledServices = ArrayList((accessibilityServiceInfoList + accessibilityEnableServiceList).distinct())
        println("enabledServices = ${enabledServices.toString()}")

        return enabledServices
    }

    private fun getEnableAccessibilitySettingsList(): List<String> {
        var accessibilityEnabled = 0
        val enableList: MutableList<String> = mutableListOf()
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                applicationContext?.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
        } catch (e: Exception) { }
        val stringColonSplitter = TextUtils.SimpleStringSplitter(':')
        if (accessibilityEnabled == 1) {
            val settingValue = Settings.Secure.getString(
                applicationContext?.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            if (settingValue != null) {
                stringColonSplitter.setString(settingValue)
                while (stringColonSplitter.hasNext()) {
                    val accessibilityService = stringColonSplitter.next()
                    enableList.add(accessibilityService.substringBefore("/"))
                }
            }
        }
        return enableList
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        //check android < 12
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            if(!isBreakerOverlay && ev != null && !isDeviceInWhiteList){
                val theBadTouch = ev.flags and MotionEvent.FLAG_WINDOW_IS_PARTIALLY_OBSCURED != 0 || ev.flags and MotionEvent.FLAG_WINDOW_IS_OBSCURED != 0
                if (theBadTouch){
                    rejectOverlay()
//                    mc.invokeMethod("overlayAndroidActionMethod",null)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}