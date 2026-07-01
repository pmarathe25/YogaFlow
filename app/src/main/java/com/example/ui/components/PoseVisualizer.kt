package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class PoseAngle {
    FRONT,
    SIDE,
    PERSPECTIVE
}

class PoseSkeleton(
    val head: Offset,
    val shoulder: Offset,
    val hip: Offset,
    val limbs: List<Pair<Offset, Offset>>,
    val joints: List<Offset> = emptyList()
)

fun getPoseSkeleton(poseId: Int, angle: PoseAngle, w: Float, h: Float, matY: Float): PoseSkeleton {
    var head = Offset(w * 0.5f, h * 0.25f)
    var shoulder = Offset(w * 0.5f, h * 0.41f)
    var hip = Offset(w * 0.5f, h * 0.65f)
    val limbs = mutableListOf<Pair<Offset, Offset>>()
    val joints = mutableListOf<Offset>()

    when (poseId) {
        1, 12 -> { // Prayer Pose
            if (angle == PoseAngle.SIDE) {
                head = Offset(w * 0.48f, h * 0.25f)
                shoulder = Offset(w * 0.48f, h * 0.41f)
                hip = Offset(w * 0.48f, h * 0.65f)
                limbs.add(hip to Offset(w * 0.48f, matY))
                limbs.add(shoulder to Offset(w * 0.58f, h * 0.50f))
                limbs.add(Offset(w * 0.58f, h * 0.50f) to Offset(w * 0.58f, h * 0.42f))
                joints.addAll(listOf(shoulder, hip, Offset(w * 0.58f, h * 0.50f)))
            } else {
                val dx = if (angle == PoseAngle.FRONT) 0f else 0.02f
                head = Offset(w * (0.5f + dx), h * 0.25f)
                shoulder = Offset(w * (0.5f + dx), h * 0.41f)
                hip = Offset(w * 0.5f, h * 0.65f)
                limbs.add(hip to Offset(w * 0.46f, matY))
                limbs.add(hip to Offset(w * 0.54f, matY))
                val elbowL = Offset(w * 0.38f, h * 0.52f)
                val elbowR = Offset(w * 0.62f, h * 0.52f)
                val hands = Offset(w * 0.5f, h * 0.48f)
                limbs.add(shoulder to elbowL)
                limbs.add(elbowL to hands)
                limbs.add(shoulder to elbowR)
                limbs.add(elbowR to hands)
                joints.addAll(listOf(shoulder, hip, elbowL, elbowR))
            }
        }
        2, 11 -> { // Raised Arms
            if (angle == PoseAngle.SIDE) {
                head = Offset(w * 0.42f, h * 0.26f)
                shoulder = Offset(w * 0.46f, h * 0.41f)
                hip = Offset(w * 0.53f, h * 0.67f)
                limbs.add(hip to Offset(w * 0.53f, matY))
                limbs.add(shoulder to Offset(w * 0.30f, h * 0.16f))
            } else {
                val dx = if (angle == PoseAngle.FRONT) 0f else -0.02f
                head = Offset(w * (0.5f + dx), h * 0.24f)
                shoulder = Offset(w * (0.5f + dx), h * 0.40f)
                hip = Offset(w * (0.5f + dx), h * 0.65f)
                limbs.add(hip to Offset(w * 0.48f, matY))
                limbs.add(hip to Offset(w * 0.54f, matY))
                limbs.add(shoulder to Offset(w * 0.32f, h * 0.15f))
                limbs.add(shoulder to Offset(w * 0.62f, h * 0.15f))
            }
            joints.addAll(listOf(shoulder, hip))
        }
        3, 10 -> { // Standing Forward Bend
            val dx = if (angle == PoseAngle.FRONT) 0f else if (angle == PoseAngle.SIDE) 0.05f else 0.03f
            hip = Offset(w * (0.5f + dx), h * 0.46f)
            shoulder = Offset(w * (0.5f - dx * 0.5f), h * 0.72f)
            head = Offset(w * (0.5f - dx), h * 0.82f)
            limbs.add(hip to Offset(w * (0.5f + dx), matY))
            limbs.add(shoulder to Offset(w * (0.45f - dx), h * 0.84f))
            joints.addAll(listOf(shoulder, hip))
        }
        4, 9 -> { // Lunge
            if (angle == PoseAngle.FRONT) {
                head = Offset(w * 0.5f, h * 0.35f)
                shoulder = Offset(w * 0.5f, h * 0.48f)
                hip = Offset(w * 0.5f, h * 0.68f)
                val kneeL = Offset(w * 0.42f, h * 0.78f)
                limbs.add(hip to kneeL)
                limbs.add(kneeL to Offset(w * 0.42f, matY))
                val kneeR = Offset(w * 0.58f, h * 0.78f)
                limbs.add(hip to kneeR)
                limbs.add(kneeR to Offset(w * 0.58f, matY))
                limbs.add(shoulder to Offset(w * 0.40f, h * 0.60f))
                limbs.add(shoulder to Offset(w * 0.60f, h * 0.60f))
                joints.addAll(listOf(shoulder, hip, kneeL, kneeR))
            } else {
                hip = Offset(w * 0.42f, h * 0.65f)
                shoulder = Offset(w * 0.44f, h * 0.45f)
                head = Offset(w * 0.45f, h * 0.32f)
                val kneeFront = Offset(w * 0.56f, h * 0.63f)
                limbs.add(hip to kneeFront)
                limbs.add(kneeFront to Offset(w * 0.62f, matY))
                limbs.add(hip to Offset(w * 0.25f, matY))
                limbs.add(shoulder to Offset(w * 0.5f, matY))
                joints.addAll(listOf(shoulder, hip, kneeFront))
            }
        }
        5 -> { // Plank
            if (angle == PoseAngle.FRONT) {
                head = Offset(w * 0.5f, h * 0.40f)
                shoulder = Offset(w * 0.5f, h * 0.50f)
                hip = Offset(w * 0.5f, h * 0.62f)
                limbs.add(shoulder to Offset(w * 0.44f, matY))
                limbs.add(shoulder to Offset(w * 0.56f, matY))
                limbs.add(hip to Offset(w * 0.5f, matY))
                joints.addAll(listOf(shoulder, hip))
            } else {
                hip = Offset(w * 0.45f, h * 0.60f)
                shoulder = Offset(w * 0.68f, h * 0.48f)
                head = Offset(w * 0.76f, h * 0.41f)
                limbs.add(hip to Offset(w * 0.22f, matY))
                limbs.add(shoulder to Offset(w * 0.68f, matY))
                joints.addAll(listOf(shoulder, hip))
            }
        }
        6 -> { // Eight-Limbed
            if (angle == PoseAngle.FRONT) {
                head = Offset(w * 0.5f, h * 0.55f)
                shoulder = Offset(w * 0.5f, h * 0.65f)
                hip = Offset(w * 0.5f, h * 0.58f)
                val elbowL = Offset(w * 0.38f, h * 0.72f)
                val elbowR = Offset(w * 0.62f, h * 0.72f)
                limbs.add(shoulder to elbowL)
                limbs.add(elbowL to Offset(w * 0.40f, matY))
                limbs.add(shoulder to elbowR)
                limbs.add(elbowR to Offset(w * 0.60f, matY))
                limbs.add(hip to Offset(w * 0.45f, matY))
                limbs.add(hip to Offset(w * 0.55f, matY))
                joints.addAll(listOf(shoulder, hip, elbowL, elbowR))
            } else {
                hip = Offset(w * 0.50f, h * 0.58f)
                shoulder = Offset(w * 0.66f, h * 0.70f)
                head = Offset(w * 0.75f, h * 0.62f)
                val knees = Offset(w * 0.38f, matY)
                limbs.add(knees to Offset(w * 0.22f, matY))
                limbs.add(hip to knees)
                val elbow = Offset(w * 0.58f, h * 0.66f)
                limbs.add(shoulder to elbow)
                limbs.add(elbow to Offset(w * 0.62f, matY))
                joints.addAll(listOf(shoulder, hip, knees))
            }
        }
        7, 301 -> { // Cobra / Sphinx
            if (angle == PoseAngle.FRONT) {
                head = Offset(w * 0.5f, h * 0.32f)
                shoulder = Offset(w * 0.5f, h * 0.48f)
                hip = Offset(w * 0.5f, matY)
                limbs.add(shoulder to Offset(w * 0.40f, matY))
                limbs.add(shoulder to Offset(w * 0.60f, matY))
                limbs.add(hip to shoulder)
                joints.addAll(listOf(shoulder, hip))
            } else {
                hip = Offset(w * 0.45f, matY)
                shoulder = Offset(w * 0.66f, h * 0.50f)
                head = Offset(w * 0.72f, h * 0.34f)
                limbs.add(hip to Offset(w * 0.22f, matY))
                val contactY = if (poseId == 301) h * 0.72f else matY
                limbs.add(shoulder to Offset(w * 0.64f, contactY))
                joints.addAll(listOf(shoulder, hip))
            }
        }
        8 -> { // Downward Dog
            if (angle == PoseAngle.FRONT) {
                hip = Offset(w * 0.5f, h * 0.42f)
                shoulder = Offset(w * 0.5f, h * 0.66f)
                head = Offset(w * 0.5f, h * 0.74f)
                limbs.add(hip to Offset(w * 0.42f, matY))
                limbs.add(hip to Offset(w * 0.58f, matY))
                limbs.add(shoulder to Offset(w * 0.40f, matY))
                limbs.add(shoulder to Offset(w * 0.60f, matY))
                joints.addAll(listOf(shoulder, hip))
            } else {
                hip = Offset(w * 0.50f, h * 0.40f)
                shoulder = Offset(w * 0.68f, h * 0.66f)
                head = Offset(w * 0.72f, h * 0.74f)
                limbs.add(hip to Offset(w * 0.28f, matY))
                limbs.add(shoulder to Offset(w * 0.78f, matY))
                joints.addAll(listOf(shoulder, hip))
            }
        }
        201 -> { // Mountain Pose
            if (angle == PoseAngle.SIDE) {
                head = Offset(w * 0.5f, h * 0.25f)
                shoulder = Offset(w * 0.5f, h * 0.40f)
                hip = Offset(w * 0.5f, h * 0.65f)
                limbs.add(hip to Offset(w * 0.5f, matY))
                limbs.add(shoulder to Offset(w * 0.5f, h * 0.55f))
                joints.addAll(listOf(shoulder, hip))
            } else {
                head = Offset(w * 0.5f, h * 0.25f)
                shoulder = Offset(w * 0.5f, h * 0.40f)
                hip = Offset(w * 0.5f, h * 0.65f)
                limbs.add(hip to Offset(w * 0.47f, matY))
                limbs.add(hip to Offset(w * 0.53f, matY))
                limbs.add(shoulder to Offset(w * 0.42f, h * 0.55f))
                limbs.add(shoulder to Offset(w * 0.58f, h * 0.55f))
                joints.addAll(listOf(shoulder, hip))
            }
        }
        202 -> { // Warrior I
            if (angle == PoseAngle.FRONT) {
                head = Offset(w * 0.5f, h * 0.20f)
                shoulder = Offset(w * 0.5f, h * 0.35f)
                hip = Offset(w * 0.5f, h * 0.60f)
                val kneeL = Offset(w * 0.40f, h * 0.68f)
                limbs.add(hip to kneeL)
                limbs.add(kneeL to Offset(w * 0.38f, matY))
                val kneeR = Offset(w * 0.60f, h * 0.68f)
                limbs.add(hip to kneeR)
                limbs.add(kneeR to Offset(w * 0.62f, matY))
                limbs.add(shoulder to Offset(w * 0.44f, h * 0.10f))
                limbs.add(shoulder to Offset(w * 0.56f, h * 0.10f))
                joints.addAll(listOf(shoulder, hip, kneeL, kneeR))
            } else {
                head = Offset(w * 0.48f, h * 0.20f)
                shoulder = Offset(w * 0.48f, h * 0.35f)
                hip = Offset(w * 0.45f, h * 0.60f)
                val kneeFront = Offset(w * 0.65f, h * 0.65f)
                limbs.add(hip to kneeFront)
                limbs.add(kneeFront to Offset(w * 0.65f, matY))
                limbs.add(hip to Offset(w * 0.25f, matY))
                limbs.add(shoulder to Offset(w * 0.44f, h * 0.10f))
                limbs.add(shoulder to Offset(w * 0.52f, h * 0.10f))
                joints.addAll(listOf(shoulder, hip, kneeFront))
            }
        }
        203 -> { // Warrior II
            if (angle == PoseAngle.FRONT) {
                head = Offset(w * 0.5f, h * 0.28f)
                shoulder = Offset(w * 0.5f, h * 0.42f)
                hip = Offset(w * 0.5f, h * 0.63f)
                val kneeL = Offset(w * 0.40f, h * 0.68f)
                limbs.add(hip to kneeL)
                limbs.add(kneeL to Offset(w * 0.38f, matY))
                val kneeR = Offset(w * 0.60f, h * 0.68f)
                limbs.add(hip to kneeR)
                limbs.add(kneeR to Offset(w * 0.62f, matY))
                limbs.add(shoulder to Offset(w * 0.25f, h * 0.42f))
                limbs.add(shoulder to Offset(w * 0.75f, h * 0.42f))
                joints.addAll(listOf(shoulder, hip, kneeL, kneeR))
            } else {
                head = Offset(w * 0.48f, h * 0.28f)
                shoulder = Offset(w * 0.48f, h * 0.42f)
                hip = Offset(w * 0.48f, h * 0.63f)
                val kneeFront = Offset(w * 0.62f, h * 0.68f)
                limbs.add(hip to kneeFront)
                limbs.add(kneeFront to Offset(w * 0.62f, matY))
                limbs.add(hip to Offset(w * 0.22f, matY))
                limbs.add(shoulder to Offset(w * 0.18f, h * 0.42f))
                limbs.add(shoulder to Offset(w * 0.78f, h * 0.42f))
                joints.addAll(listOf(shoulder, hip, kneeFront))
            }
        }
        204 -> { // Triangle
            if (angle == PoseAngle.FRONT) {
                hip = Offset(w * 0.5f, h * 0.58f)
                shoulder = Offset(w * 0.42f, h * 0.70f)
                head = Offset(w * 0.35f, h * 0.74f)
                limbs.add(hip to Offset(w * 0.35f, matY))
                limbs.add(hip to Offset(w * 0.65f, matY))
                limbs.add(shoulder to Offset(w * 0.38f, h * 0.85f))
                limbs.add(shoulder to Offset(w * 0.50f, h * 0.40f))
                joints.addAll(listOf(shoulder, hip))
            } else {
                hip = Offset(w * 0.48f, h * 0.58f)
                shoulder = Offset(w * 0.40f, h * 0.70f)
                head = Offset(w * 0.33f, h * 0.74f)
                limbs.add(hip to Offset(w * 0.32f, matY))
                limbs.add(hip to Offset(w * 0.68f, matY))
                limbs.add(shoulder to Offset(w * 0.36f, h * 0.85f))
                limbs.add(shoulder to Offset(w * 0.54f, h * 0.40f))
                joints.addAll(listOf(shoulder, hip))
            }
        }
        205, 301 -> { // Child's Pose
            if (angle == PoseAngle.FRONT) {
                head = Offset(w * 0.5f, matY - 10f)
                shoulder = Offset(w * 0.5f, matY - 18f)
                hip = Offset(w * 0.5f, matY - 6f)
                limbs.add(hip to Offset(w * 0.42f, matY))
                limbs.add(hip to Offset(w * 0.58f, matY))
                limbs.add(shoulder to Offset(w * 0.40f, matY - 22f))
                limbs.add(shoulder to Offset(w * 0.60f, matY - 22f))
                joints.addAll(listOf(shoulder, hip))
            } else {
                val knee = Offset(w * 0.45f, matY)
                hip = Offset(w * 0.28f, matY - 6f)
                shoulder = Offset(w * 0.62f, matY - 14f)
                head = Offset(w * 0.72f, matY - 8f)
                limbs.add(hip to knee)
                limbs.add(shoulder to Offset(w * 0.88f, matY))
                joints.addAll(listOf(shoulder, hip))
            }
        }
        302 -> { // Butterfly
            if (angle == PoseAngle.SIDE) {
                head = Offset(w * 0.45f, h * 0.35f)
                shoulder = Offset(w * 0.45f, h * 0.50f)
                hip = Offset(w * 0.45f, h * 0.74f)
                val knee = Offset(w * 0.58f, matY - 4f)
                limbs.add(hip to knee)
                limbs.add(knee to Offset(w * 0.52f, matY))
                limbs.add(shoulder to Offset(w * 0.52f, matY - 2f))
                joints.addAll(listOf(shoulder, hip))
            } else {
                head = Offset(w * 0.5f, h * 0.35f)
                shoulder = Offset(w * 0.5f, h * 0.50f)
                hip = Offset(w * 0.5f, h * 0.74f)
                val kneeL = Offset(w * 0.32f, matY - 4f)
                val kneeR = Offset(w * 0.68f, matY - 4f)
                val feet = Offset(w * 0.5f, matY)
                limbs.add(hip to kneeL)
                limbs.add(kneeL to feet)
                limbs.add(hip to kneeR)
                limbs.add(kneeR to feet)
                limbs.add(shoulder to Offset(w * 0.48f, matY - 2f))
                limbs.add(shoulder to Offset(w * 0.52f, matY - 2f))
                joints.addAll(listOf(shoulder, hip))
            }
        }
        303 -> { // Spinal Twist
            if (angle == PoseAngle.SIDE) {
                head = Offset(w * 0.48f, h * 0.40f)
                shoulder = Offset(w * 0.48f, h * 0.55f)
                hip = Offset(w * 0.48f, h * 0.78f)
                limbs.add(shoulder to Offset(w * 0.65f, h * 0.62f))
                val knee = Offset(w * 0.60f, h * 0.75f)
                limbs.add(hip to knee)
                limbs.add(knee to Offset(w * 0.52f, matY))
                joints.addAll(listOf(shoulder, hip))
            } else {
                head = Offset(w * 0.5f, h * 0.40f)
                shoulder = Offset(w * 0.5f, h * 0.55f)
                hip = Offset(w * 0.5f, h * 0.78f)
                limbs.add(shoulder to Offset(w * 0.22f, h * 0.55f))
                limbs.add(shoulder to Offset(w * 0.78f, h * 0.55f))
                val kneeL = Offset(w * 0.35f, h * 0.78f)
                limbs.add(hip to kneeL)
                limbs.add(kneeL to Offset(w * 0.38f, matY))
                joints.addAll(listOf(shoulder, hip))
            }
        }
        304 -> { // Savasana
            if (angle == PoseAngle.SIDE) {
                head = Offset(w * 0.25f, matY - 6f)
                shoulder = Offset(w * 0.35f, matY - 8f)
                hip = Offset(w * 0.55f, matY - 6f)
                limbs.add(shoulder to hip)
                limbs.add(hip to Offset(w * 0.85f, matY - 4f))
                limbs.add(shoulder to Offset(w * 0.42f, matY - 6f))
                joints.addAll(listOf(shoulder, hip))
            } else {
                head = Offset(w * 0.5f, h * 0.28f)
                shoulder = Offset(w * 0.5f, h * 0.43f)
                hip = Offset(w * 0.5f, h * 0.65f)
                limbs.add(hip to Offset(w * 0.43f, matY))
                limbs.add(hip to Offset(w * 0.57f, matY))
                limbs.add(shoulder to Offset(w * 0.38f, h * 0.55f))
                limbs.add(shoulder to Offset(w * 0.62f, h * 0.55f))
                joints.addAll(listOf(shoulder, hip))
            }
        }
        else -> { // Default meditation/succinct pose
            head = Offset(w * 0.5f, h * 0.32f)
            shoulder = Offset(w * 0.5f, h * 0.48f)
            hip = Offset(w * 0.5f, h * 0.72f)
            val kneeL = Offset(w * 0.35f, h * 0.78f)
            val kneeR = Offset(w * 0.65f, h * 0.78f)
            limbs.add(hip to kneeL)
            limbs.add(hip to kneeR)
            limbs.add(shoulder to kneeL)
            limbs.add(shoulder to kneeR)
            joints.addAll(listOf(shoulder, hip))
        }
    }

    limbs.add(0, shoulder to hip)

    return PoseSkeleton(head, shoulder, hip, limbs, joints)
}

@Composable
fun YogaPoseVisual(
    poseId: Int,
    angle: PoseAngle = PoseAngle.FRONT,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val matY = height * 0.88f

            val gridColor = Color(0xFF888888).copy(alpha = 0.15f)
            val gridSize = 20.dp.toPx()
            var x = 0f
            while (x < width) {
                drawLine(color = gridColor, start = Offset(x, 0f), end = Offset(x, height), strokeWidth = 0.5.dp.toPx())
                x += gridSize
            }
            var y = 0f
            while (y < height) {
                drawLine(color = gridColor, start = Offset(0f, y), end = Offset(width, y), strokeWidth = 0.5.dp.toPx())
                y += gridSize
            }

            val graphiteColor = Color(0xFF333333).copy(alpha = 0.7f)
            val groundY = matY
            drawLine(color = graphiteColor.copy(alpha = 0.25f), start = Offset(width * 0.15f, groundY), end = Offset(width * 0.85f, groundY), strokeWidth = 1.dp.toPx())

            val skeleton = getPoseSkeleton(poseId, angle, width, height, matY)

            val headRadius = 13.dp.toPx()
            val limbThickness = 12.dp.toPx()
            val torsoThickness = 18.dp.toPx()

            val primaryAccent = Color(0xFF3DDC84)

            skeleton.limbs.forEachIndexed { index, pair ->
                val start = pair.first
                val end = pair.second
                val isTorso = index == 0
                val thickness = if (isTorso) torsoThickness else limbThickness

                drawLine(color = primaryAccent.copy(alpha = 0.2f), start = start, end = end, strokeWidth = thickness, cap = StrokeCap.Round)
                drawLine(color = graphiteColor, start = start, end = end, strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
            }

            drawCircle(color = primaryAccent.copy(alpha = 0.2f), radius = headRadius, center = skeleton.head)
            drawCircle(color = graphiteColor, radius = headRadius, center = skeleton.head, style = Stroke(width = 1.5.dp.toPx()))

            val faceOffset = if (angle == PoseAngle.SIDE) {
                if (poseId in listOf(4, 9, 5, 6, 7, 302, 205, 301, 8)) headRadius * 0.8f else -headRadius * 0.8f
            } else 0f

            if (faceOffset != 0f) {
                drawLine(color = graphiteColor, start = skeleton.head + Offset(faceOffset * 0.5f, 0f), end = skeleton.head + Offset(faceOffset, 0f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
            }

            skeleton.joints.forEach { joint ->
                drawCircle(color = Color.White, radius = 3.5.dp.toPx(), center = joint)
                drawCircle(color = graphiteColor, radius = 3.5.dp.toPx(), center = joint, style = Stroke(width = 1.dp.toPx()))
            }
        }

        Text(
            text = if (angle == PoseAngle.FRONT) "FRONT" else "SIDE",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                fontSize = 10.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 8.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}
