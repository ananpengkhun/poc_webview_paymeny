package com.krungsri.kma.constants

class Method {
    object BaseChannelMethod {
        const val EXECUTE_SCAN_ID_CARD = "executeScanIdcard"
        const val EXECUTE_LIVELINESS = "executeLiveness"
        const val EXECUTE_EYE_BLINKING = "executeEyeBlinking"
        const val EXECUTE_ADB_ANDROID_DEBUG = "executeADBAndroidDebug"
        const val EXECUTE_GENERATE_AUTHENTICATE_PUBLIC_KEY = "executeGenerateAuthenPublicKey"
        const val EXECUTE_REMOVE_USER_LOGIN = "executeRemoveUserLogin"
        const val EXECUTE_AUTHENTICATE_TOUCH_ID = "executeAuthenticateTouchID"
        const val APP_EXIT = "app_exit"
        const val GET_DEVICE_INFO = "getDeviceInfo"
        const val MIGRATE_DATA_FROM_V3 = "migrateDataFromV3";
        const val CHECK_GOOGLE_PLAY_INTEGRITY = "checkGooglePlayIntegrity"
        const val NETWORK_SECURITY_VPN_CHECKER = "network_security_vpn_checker"
        const val NETWORK_SECURITY_PROXY_CHECKER = "network_security_proxy_checker"
        const val NETWORK_ACTIVE_PROTOCOLS = "network_active_protocols"
        const val GET_BASE64_OF_APK_DIGEST_SHA256 = "getBase64OfApkDigestSha256"
        const val SAVE_FILE_TO_DOWNLOADS = "saveFileToDownloads"
        const val SEND_CIRCUIT_BREAKER_OVERLAY = "sendCircuitBreakersOverlay"

        const val EXECUTE_AINU_LIVELINESS = "executeAinuLiveness"
        const val EXECUTE_AINU_LIVELINESS_PASSIVE = "executeAinuLivenessPassive"
    }

    object SecurityChannelMethod {
        const val ENABLE_SECURE_CAPTURE = "enableSecureCapture"
        const val DISABLE_SECURE_CAPTURE = "disableSecureCapture"
    }

    object SecureStorageMethodChannel {
        const val GET_SECURE_STORAGE = "get_secure_storage"
        const val REMOVE_SECURE_STORAGE = "remove_secure_storage"
        const val GET_SHARED_PREF = "get_shared_pref"
        const val REMOVE_SHARED_PREF = "remove_shared_pref"
        const val GET_PROPERTY = "get_property"
    }
}