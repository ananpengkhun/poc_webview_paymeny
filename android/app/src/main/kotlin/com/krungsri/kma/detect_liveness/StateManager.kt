package com.krungsri.kma.detect_liveness

import tech.ainu.facial.liveness.LivenessState
import com.krungsri.kma.R


// object สำหรับจัดการ livenessState
internal object StateManager {

    /**
     * message Pair("TH", "EN")
    * */
    private val message = hashMapOf(
        LivenessState.NOTSTART to Pair(
            "กำลังเริ่มตรวจสอบใบหน้า",
            "Starting to verify your face"
        ),
        LivenessState.NORMAL to Pair(
            "กรุณามองตรงไปที่กล้อง",
            "Please look straight at the camera"
        ),
        LivenessState.FACE_NOT_FOUND to Pair(
            "ไม่พบใบหน้า",
            "Face not found"
        ),
        LivenessState.NOT_CENTER to Pair(
            "ขยับใบหน้าให้อยู่ตรงกลาง",
            "Move your face to the center"
        ),
        LivenessState.FACE_NOT_FORWARD to Pair(
            "กรุณามองตรงไปที่กล้อง",
            "Look straight"
        ),
        LivenessState.TOO_CLOSE to Pair(
            "ขยับใบหน้าห่างออกไป",
            "Move your face further away"
        ),
        LivenessState.TOO_FAR to Pair(
            "ขยับใบหน้าใกล้ขึ้น",
            "Move your face closer"
        ),
        LivenessState.CHANGE_ENVIRONMENT to Pair(
            "ใบหน้าไม่ชัดเจน กรุณาเปลี่ยนที่สแกนหน้า",
            "Face not clear. Please change location"
        ),
        LivenessState.TOO_LITTLE_BRIGHT to Pair(
            "ใบหน้ามืดเกินไป",
            "Your face is too dark"
        ),
        LivenessState.TOO_BRIGHT to Pair(
            "ใบหน้าสว่างเกินไป",
            "Your face is too bright"
        ),
        LivenessState.NO_MOUTH to Pair(
            "ไม่พบปาก",
            "Mouth not found"
        ),
        LivenessState.NO_EYE_LEFT to Pair(
            "ไม่พบตาซ้ายหรือตาซ้ายปิดอยู่",
            "Left eye not found or closed"
        ),
        LivenessState.NO_EYE_RIGHT to Pair(
            "ไม่พบตาขวาหรือตาขวาปิดอยู่",
            "Right eye not found or closed"
        ),
        LivenessState.NO_EYE to Pair(
            "ไม่พบดวงตาหรือดวงตาปิดอยู่",
            "Eyes not found or closed"
        ),
        LivenessState.MULTIPLE_FACE to Pair(
            "พบมากกว่า 1 ใบหน้า",
            "Detect multiple faces"
        ),
        LivenessState.BACKGROUND_BRIGHT to Pair(
            "แสงด้านหลังสว่างเกินไป",
            "Background is too bright"
        ),
        LivenessState.MOUTH_NOT_CLOSE to Pair(
            "กรุณาไม่อ้าปาก",
            "Please close your mouth"
        ),
        LivenessState.NOSE_NOT_FOUND to Pair(
            "ไม่พบจมูก",
            "Nose not found"
        ),
        LivenessState.TURN_FACE_LEFT to Pair(
            "หันหน้าไปทางซ้าย",
            "Turn your face to the left"
        ),
        LivenessState.TURN_FACE_RIGHT to Pair(
            "หันหน้าไปทางขวา",
            "Turn your face to the right"
        ),
        LivenessState.BLINK to Pair(
            "กระพริบตาช้าๆ",
            "Blink slowly"
        ),
        LivenessState.FACE_NOD to Pair(
            "พยักหน้า",
            "Nod your head"
        ),
    )

    fun getMessage(livenessState: LivenessState): Pair<String, String> {
        return message[livenessState] ?: Pair(
            "กรุณามองตรงไปที่กล้อง",
            "Look straight"
        )
    }

    fun mapAnimation(livenessState: LivenessState): Int {
        return when (livenessState) {
            LivenessState.NORMAL -> R.raw.head_in_frame
            LivenessState.FACE_NOT_FORWARD -> R.raw.head_in_frame
            LivenessState.NOTSTART -> R.raw.head_in_frame
            LivenessState.BLINK -> R.raw.head_blink
            LivenessState.FACE_NOD -> R.raw.head_tilt_up_down
            LivenessState.SMILE -> R.raw.head_smile
            LivenessState.TURN_FACE_LEFT_OR_RIGHT -> R.raw.head_turn_left
            LivenessState.TURN_FACE_LEFT -> R.raw.head_turn_left
            LivenessState.TURN_FACE_RIGHT -> R.raw.head_turn_right
            else -> R.raw.error
        }
    }

    fun isWarning(livenessState: LivenessState): Boolean {
        return when (livenessState) {
            LivenessState.NORMAL -> false
            LivenessState.FACE_NOT_FORWARD -> false
            LivenessState.BLINK -> false
            LivenessState.FACE_NOD -> false
            LivenessState.SMILE -> false
            LivenessState.TURN_FACE_LEFT -> false
            LivenessState.TURN_FACE_LEFT_OR_RIGHT -> false
            LivenessState.TURN_FACE_RIGHT -> false
            else -> true
        }
    }

    fun isAction(livenessState: LivenessState): Boolean {
        return when (livenessState) {
            LivenessState.TURN_FACE_LEFT_OR_RIGHT -> true
            LivenessState.TURN_FACE_LEFT -> true
            LivenessState.TURN_FACE_RIGHT -> true
            LivenessState.BLINK -> true
            LivenessState.SMILE -> true
            LivenessState.FACE_NOD -> true
            else -> false
        }
    }
}