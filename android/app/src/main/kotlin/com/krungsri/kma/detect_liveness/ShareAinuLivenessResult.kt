package com.krungsri.kma.detect_liveness

import tech.ainu.facial.liveness.ResultCode

// object สำหรับ เก็บค่าต่างๆ จากหน้า AinuLivenessActivity เนื่องจาก activity result ไม่สามารถส่งข้อมูลขนาดใหญ่
internal object ShareAinuLivenessResult {
    var resultCode: ResultCode? = null
    var imageData: ByteArray? = null
    var metadata: String? = null
    var signature: String? = null
    var keyId: String? = null
    var log: String? = null
}