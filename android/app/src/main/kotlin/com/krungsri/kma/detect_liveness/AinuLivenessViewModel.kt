package com.krungsri.kma.detect_liveness

import android.app.Application
import android.content.Context
import android.content.res.Resources
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
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.krungsri.kma.R
import tech.ainu.facial.liveness.AinuLiveness
import tech.ainu.facial.liveness.LivenessState
import tech.ainu.facial.liveness.OnLivenessListener
import tech.ainu.facial.liveness.ResultCode
import com.krungsri.kma.detect_liveness.StateManager.getMessage
import com.krungsri.kma.detect_liveness.StateManager.isAction
import com.krungsri.kma.detect_liveness.StateManager.isWarning
import com.krungsri.kma.detect_liveness.StateManager.mapAnimation


class AinuLivenessViewModel(
    application: Application,
) : AndroidViewModel(application) {

    val backgroundWithMask: MutableLiveData<Bitmap> =
        MutableLiveData(Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888))
    val statusFirst: MutableLiveData<String> = MutableLiveData("")
    val statusSecond: MutableLiveData<String> = MutableLiveData("")
    val animateId: MutableLiveData<Int> = MutableLiveData(R.raw.head_in_frame)
    val resultLiveness: MutableLiveData<ResultCode?> =
        MutableLiveData(null)
    val isAlert = MutableLiveData(false)
    val isStart = MutableLiveData(false)

    private var status = LivenessState.NOTSTART
    private var isInitFacialLiveness = false
    private var previousAction = LivenessState.NOTSTART

    fun initSDKLiveness(
        context: Context,
        key: String,
        env: AinuLiveness.Env?,
        numOfAction: Int?,
        actions: ArrayList<AinuLiveness.Action>?,
        enableSignature: Boolean?,
        outputType: AinuLiveness.ImageOutputType?,
        faceHeightPercentage: Int?,
        maxWidth: Int?,
        maxFileSize: Int?,
        rectMask: Rect,
    ) {
        if (isInitFacialLiveness) return
        this.resultLiveness.value = null
        isStart.postValue(false)
        val listener = object : OnLivenessListener {
            override fun onStateUpdate(livenessState: LivenessState) {
                if (status != livenessState) {
                    status = livenessState
                    isStart.postValue(true)

                    handleMessage(status)
                    animateId.postValue(mapAnimation(livenessState))

                    updateAlert(livenessState)
                    modifyBackgroundWithMask(
                        true,
                        context.resources,
                        isWarning(livenessState),
                        rectMask
                    )
                }
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
                isStart.postValue(false)
                ShareAinuLivenessResult.resultCode = resultCode
                ShareAinuLivenessResult.log = log
                if (resultCode == ResultCode.OK) {
                    ShareAinuLivenessResult.imageData = imageData
                    ShareAinuLivenessResult.metadata = metadata
                    ShareAinuLivenessResult.signature = signature
                    ShareAinuLivenessResult.keyId = keyId
                }
                this@AinuLivenessViewModel.resultLiveness.postValue(
                    resultCode
                )
            }
        }

        // set default
        handleMessage(status)
        animateId.postValue(mapAnimation(status))

        AinuLiveness.Builder(
            context,
            key,
            env ?: AinuLiveness.Env.UAT,
            listener,
        ).apply {
            if (numOfAction != null) {
                setNumOfAction(numOfAction)
            }
            if (actions != null) {
                setAction(actions)
            }
            setOutputImageProperties(enableSignature, outputType, faceHeightPercentage, maxWidth, maxFileSize)

        }.build()
        isInitFacialLiveness = true
    }

    fun startSDKLiveness() {
        AinuLiveness.start()
    }

    fun processSDKLiveness(image: Bitmap, rotationDegree: Int, rectCam: Rect, rectMask: Rect) {
        AinuLiveness.process(image, rotationDegree, rectCam, rectMask)
    }

    fun closeSDKLiveness() {
        AinuLiveness.close()
    }

    fun generateBackgroundWithMask(resources: Resources, rectCam: Rect, rectMask: Rect) {
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
        backgroundWithMask.value = outputBitmap
    }

    fun modifyBackgroundWithMask(
        isStart: Boolean,
        resources: Resources,
        statusWarning: Boolean,
        rectMask: Rect,
    ) {

        val valueBitmap = backgroundWithMask.value ?: return
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

        backgroundWithMask.postValue(output)
    }

    private fun updateAlert(action: LivenessState) {

        var statusAlert = false
        if (isAction(previousAction)) {
            if (action == LivenessState.NORMAL || action == LivenessState.FACE_NOT_FORWARD) {
                statusAlert = true
            }
        }

        isAlert.postValue(statusAlert)
        previousAction = action
    }

    private fun handleMessage(status: LivenessState) {
        val (statusMessageTH, statusMessageEN) = getMessage(status)
        statusFirst.postValue(statusMessageEN)
        statusSecond.postValue(statusMessageTH)
    }
}