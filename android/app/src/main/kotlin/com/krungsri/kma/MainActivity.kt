package com.krungsri.kma

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import com.krungsri.kma.constants.Channel
import com.krungsri.kma.constants.Channel.Companion.livenessChannel
import com.krungsri.kma.constants.Method.BaseChannelMethod.EXECUTE_AINU_LIVELINESS
import com.krungsri.kma.constants.Method.BaseChannelMethod.EXECUTE_AINU_LIVELINESS_PASSIVE
import com.krungsri.kma.detect_liveness.AinuLivenessActivity
import com.krungsri.kma.detect_liveness.AinuLivenessConstants
import com.krungsri.kma.detect_liveness.ShareAinuLivenessResult
import io.flutter.embedding.android.FlutterFragmentActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.StandardMethodCodec
import tech.ainu.facial.liveness.AinuLiveness
import tech.ainu.facial.liveness.ResultCode

class MainActivity: FlutterFragmentActivity() {
    private var isBreakerOverlay = false
    private var isDeviceInWhiteList = false
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
//        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),99)

        setUpForAinuLiessness(flutterEngine)

        getAllPackageName()


    }
    var mapGrad = hashMapOf<Int,String>()
    fun getAllPackageName(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            if(checkSelfPermission(Manifest.permission.QUERY_ALL_PACKAGES) == PackageManager.PERMISSION_GRANTED){
//                var listPackage = packageManager.getInstalledPackages(0)
                var listPackage = packageManager.getInstalledPackages(PackageManager.PERMISSION_GRANTED)
                println("========================================= getInstalledPackages : ${listPackage.size}")
                listPackage.forEach{ item ->
                    println("========================================= start")
                    println("query package name  : ${item.packageName}")

                    item.requestedPermissions.forEach {
                        println("---${it}")
                    }
                    println("========================================= end")
                }
            }else{
                // no permitted
            }
        }else{
            var listPackage = packageManager.getInstalledPackages(PackageManager.PERMISSION_GRANTED)

            println("========================================= getInstalledPackages : ${listPackage.size}")
            listPackage.forEach{ item ->
                println("========================================= start")
                if(item.packageName.contains("droidcam")){
//                    packageManager.checkPermission()
                    val pm = packageManager.getPackageInfo(item.packageName, PackageManager.GET_PERMISSIONS)

                    println("size requestedPermissionsFlags :${pm.requestedPermissionsFlags.size}")
                    println("size requestedPermissions :${pm.requestedPermissions.size}")
                    if(pm.requestedPermissionsFlags != null){

                        var i = 0
                        pm.requestedPermissionsFlags.forEach {
//                            var isGrant = PermissionChecker.checkCallingPermission(this, packageManager.requestedPermissions[i],item.packageName)
//                            var isGrant = ActivityCompat.checkSelfPermission(this, packageManager.requestedPermissions[i])
                            var isGrant = packageManager.checkPermission( pm.requestedPermissions[i], item.packageName)
//                            PackageManager().checkPermission()
                            mapGrad[i] = "isGrant :${isGrant == PackageManager.PERMISSION_GRANTED}, ${pm.requestedPermissions[i]}"
                            i++
//                            println("---packageManager data requestedPermissionsFlags :${it}")
                        }
                    }

                    mapGrad.forEach {
                        println("result ====> ${it.value}")
                    }


//                    if(packageManager.requestedPermissions != null){
//                        packageManager.requestedPermissions.forEach {
//                            println("---packageManager data requestedPermissions :${it}")
//                        }
//                    }

                    println("========================================= end")
                }

            }
        }

    }

    // register activity result สำหรับรับค่าจาก AinuLivenessActivity
    private val facialLivenessActivityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult() // Example contract
    ) { result: ActivityResult ->
        // Handle the result here
        val resultCode = result.resultCode
        val data = result.data
        if (resultCode == Activity.RESULT_OK) {
            // Data was successfully returned from SecondActivity
            val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                data?.getSerializableExtra(
                        AinuLivenessActivity.EXTRA_LIVENESS_RESULT_CODE,
                        ResultCode::class.java
                )
            } else {
                data?.getSerializableExtra(AinuLivenessActivity.EXTRA_LIVENESS_RESULT_CODE) as ResultCode
            }

            val resultLivenessCodeStr = resultData?.name ?: ""

            if (resultData == ResultCode.OK) {
                sendMessageToFlutter(
                        livenessChannel,
                        "success",
                        mapOf(
                                "resultCode" to resultLivenessCodeStr,
                                "imageByte" to (ShareAinuLivenessResult.imageData ?: ByteArray(0)),
                                "imageBase64" to (Base64.encodeToString(ShareAinuLivenessResult.imageData, Base64.DEFAULT)),
                                "metadata" to (ShareAinuLivenessResult.metadata ?: ""),
                                "signature" to (ShareAinuLivenessResult.signature ?: ""),
                                "keyId" to (ShareAinuLivenessResult.keyId ?: ""),
                                "log" to (ShareAinuLivenessResult.log ?: "")
                        )
                )
            } else {
                sendMessageToFlutter(
                        livenessChannel,
                        "livenessFailed",
                        mapOf(
                                "resultCode" to resultLivenessCodeStr,
                                "imageByte" to ByteArray(0),
                                "metadata" to "",
                                "signature" to "",
                                "keyId" to "",
                                "log" to (ShareAinuLivenessResult.log ?: "")
                        )
                )
            }
            // Process the resultData as needed
        } else if (resultCode == Activity.RESULT_CANCELED) {
            sendMessageToFlutter(
                    livenessChannel,
                    "livenessFailed",
                    mapOf(
                            "resultCode" to "CANCELED",
                            "imageByte" to ByteArray(0),
                            "metadata" to "",
                            "signature" to "",
                            "keyId" to "",
                            "log" to ""
                    )
            )
        }else if(resultCode == 666){ //reject denied permission
            sendMessageToFlutter(
                    livenessChannel,
                    "denied_permission",
                    mapOf(
                            "resultCode" to "CANCELED",
                            "imageByte" to ByteArray(0),
                            "metadata" to "",
                            "signature" to "",
                            "keyId" to "",
                            "log" to ""
                    )
            )
        }
        else if(resultCode == 777){ //reject back press
            sendMessageToFlutter(
                    livenessChannel,
                    "back",
                    mapOf(
                            "resultCode" to "CANCELED",
                            "imageByte" to ByteArray(0),
                            "metadata" to "",
                            "signature" to "",
                            "keyId" to "",
                            "log" to ""
                    )
            )
        }
        else if(resultCode == 999){ //reject accessibility
            sendMessageToFlutter(
                    livenessChannel,
                    "detect_accessibility",
                    mapOf(
                            "resultCode" to "CANCELED",
                            "imageByte" to ByteArray(0),
                            "metadata" to "",
                            "signature" to "",
                            "keyId" to "",
                            "log" to ""
                    )
            )
        }else if(resultCode == 888){ //reject overlay
            sendMessageToFlutter(
                    livenessChannel,
                    "detect_overlay",
                    mapOf(
                            "resultCode" to "CANCELED",
                            "imageByte" to ByteArray(0),
                            "metadata" to "",
                            "signature" to "",
                            "keyId" to "",
                            "log" to ""
                    )
            )
        }
    }

    var flutterEngineInstance: FlutterEngine? = null

    private fun sendMessageToFlutter(channelName: String, methodName: String, data: Map<String, Any>) {
        flutterEngineInstance?.let {
            MethodChannel(
                    it.dartExecutor.binaryMessenger, channelName
            ).invokeMethod(
                    methodName,
                    data
            )
        }
    }
    private fun setUpForAinuLiessness(flutterEngine: FlutterEngine){
        val ainuMethodChannel: MethodChannel
        val messenger: BinaryMessenger = flutterEngine.dartExecutor.binaryMessenger
        val taskQueue = messenger.makeBackgroundTaskQueue()
        flutterEngineInstance = flutterEngine
        ainuMethodChannel = MethodChannel(messenger, Channel.livenessChannel, StandardMethodCodec.INSTANCE, taskQueue)

        ainuMethodChannel.setMethodCallHandler { call, result ->
            val isEn = call.argument("isEn") ?: false

            when (call.method) {
                EXECUTE_AINU_LIVELINESS -> {
                    startAinuLiveness(0, isEn)
                }
                EXECUTE_AINU_LIVELINESS_PASSIVE -> {
                    startAinuLiveness(0, isEn)
                }
                else -> {
                    result.notImplemented()
                }
            }
        }
    }

    fun startAinuLiveness(numOfAction : Int, isEn : Boolean){
        // start AinuLivenessActivity สำหรับ scan facial liveness
        val intent = Intent(this, AinuLivenessActivity::class.java)

        // pass value for set config SDK
        intent.putExtra(AinuLivenessConstants.EXTRA_OUTPUT_IMAGE_SIGNATURE, true)
        intent.putExtra(AinuLivenessConstants.EXTRA_OUTPUT_IMAGE_TYPE, AinuLiveness.ImageOutputType.JPEG)
        intent.putExtra(AinuLivenessConstants.EXTRA_OUTPUT_IMAGE_FACE_HEIGHT_PERCENTAGE, 55)
        intent.putExtra(AinuLivenessConstants.EXTRA_OUTPUT_IMAGE_MAX_WIDTH, 480)
        intent.putExtra(AinuLivenessConstants.EXTRA_OUTPUT_IMAGE_MAX_FILE_SIZE, 50)
        intent.putExtra(AinuLivenessConstants.EXTRA_CAMERA, AinuLiveness.Camera.FRONT)
        // if camera set FRONT. SDK not allow use AinuLiveness.Action.LEFT_OR_RIGHT
        // if camera set BACK. SDK not allow use AinuLiveness.Action.LEFT and AinuLiveness.Action.RIGHT
        intent.putStringArrayListExtra(AinuLivenessConstants.EXTRA_ACTION, arrayListOf(
                AinuLiveness.Action.BLINK.name,
                AinuLiveness.Action.NOD.name,
                AinuLiveness.Action.LEFT.name,
                AinuLiveness.Action.RIGHT.name,
        ))
        intent.putExtra(AinuLivenessConstants.EXTRA_NUM_OF_ACTION, numOfAction)
        intent.putExtra(AinuLivenessConstants.EXTRA_QUALITY_MULTIPLE_FACE, true)
        intent.putExtra(AinuLivenessConstants.EXTRA_QUALITY_MOUTH_VISIBLE, true)
        intent.putExtra(AinuLivenessConstants.EXTRA_QUALITY_MOUTH_OPEN, true)
        intent.putExtra(AinuLivenessConstants.EXTRA_QUALITY_NOSE_VISIBLE, true)
        intent.putExtra(AinuLivenessConstants.EXTRA_QUALITY_EYE, true)
        intent.putExtra(AinuLivenessConstants.EXTRA_QUALITY_BRIGHTNESS, true)
        intent.putExtra(AinuLivenessConstants.EXTRA_QUALITY_DARK, true)
        intent.putExtra("isEn", isEn)
        intent.putExtra("isBreakerOverlay", isBreakerOverlay)
        intent.putExtra("isDeviceInWhiteList", isDeviceInWhiteList)
        facialLivenessActivityResultLauncher.launch(intent)
    }
}
