package com.example.game.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.*
import com.example.game.persistence.DataLoader
import androidx.compose.ui.window.Dialog
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val SEGMENT_HEIGHT = 260.dp
private val HEADER_AREA_HEIGHT = 0.dp
private val PATH_AMPLITUDE = 70.dp

@Composable
fun MonsterRoadSelection(
    monsters: List<Monster>,
    defeatedIds: Set<String>,
    partyMembers: List<PartyMemberData>,
    onMonsterSelected: (Monster) -> Unit,
    onBack: () -> Unit
) {
    val sortedMonsters = remember { monsters.reversed() }
    val totalCount = sortedMonsters.size
    if (totalCount == 0) return

    val density = LocalDensity.current
    val scrollState = rememberScrollState()

    val topSpacer = 0.dp
    val bottomSpacer = 0.dp
    val totalContentHeight = topSpacer + HEADER_AREA_HEIGHT + SEGMENT_HEIGHT * totalCount.toFloat() + bottomSpacer

    var selectedMonster by remember { mutableStateOf<Monster?>(null) }

    val normDefeated = remember(defeatedIds) {
        defeatedIds.map { it.lowercase() }.toSet()
    }

    val activeIndex = remember(sortedMonsters, normDefeated) {
        val idx = sortedMonsters.indexOfLast { !normDefeated.contains(it.id.lowercase()) }
        if (idx < 0) totalCount else idx
    }

    LaunchedEffect(Unit) {
        if (scrollState.maxValue > 0) {
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    val infiniteTransition = rememberInfiniteTransition()
    val pulseAnim by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse)
    )
    val driftAnim by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8000), RepeatMode.Restart)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1B2A))
    ) {
        MapHeader(onBack = onBack)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 56.dp)
            .verticalScroll(scrollState)
        ) {
            val canvasWidthDp = maxWidth
            val dpScaleRatio = canvasWidthDp / 360.dp
            val adjustedHeight = totalContentHeight * dpScaleRatio

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(adjustedHeight)
            ) {
                val sw = size.width
                val sh = size.height
                val dpScale = sw / 360f

                drawBiomeBackground(totalCount, dpScale, sh,
                    topSpacer.value * dpScale,
                    HEADER_AREA_HEIGHT.value * dpScale,
                    SEGMENT_HEIGHT.value * dpScale)

                drawBiomeDecorations(totalCount, dpScale, sh,
                    topSpacer, HEADER_AREA_HEIGHT, SEGMENT_HEIGHT, pulseAnim)

                val pathPoints = drawPath(dpScale, sh)

                drawPathSegments(totalCount, dpScale, sortedMonsters, normDefeated, activeIndex, pathPoints,
                    topSpacer, HEADER_AREA_HEIGHT, SEGMENT_HEIGHT)

                drawSkyElements(dpScale, sh, driftAnim)

                drawNodes(sortedMonsters, normDefeated, activeIndex, dpScale, pulseAnim,
                    topSpacer, HEADER_AREA_HEIGHT, SEGMENT_HEIGHT)

                drawFogOfWar(activeIndex, dpScale, sh,
                    topSpacer, HEADER_AREA_HEIGHT, SEGMENT_HEIGHT)
            }

            sortedMonsters.forEachIndexed { index, monster ->
                val isDefeated = normDefeated.contains(monster.id.lowercase())
                val isUnlocked = index == sortedMonsters.lastIndex || normDefeated.contains(sortedMonsters[index + 1].id.lowercase())
                val nodeSize = getNodeSizeDp(monster.difficultyTier)
                val centerYDp = (topSpacer + HEADER_AREA_HEIGHT + SEGMENT_HEIGHT * index + SEGMENT_HEIGHT * 0.5f) * dpScaleRatio
                val centerXDp = canvasWidthDp / 2
                val cxDp = centerXDp + PATH_AMPLITUDE * dpScaleRatio * sin(index * 0.8f)
                val topLeftXDp = cxDp - nodeSize * 0.5f
                val topLeftYDp = centerYDp - nodeSize * 0.5f

                Box(
                    modifier = Modifier
                        .offset(x = topLeftXDp, y = topLeftYDp)
                        .size(nodeSize)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = isUnlocked
                        ) { selectedMonster = monster }
                        .zIndex(1f)
                )
            }
        }
    }

    if (selectedMonster != null) {
        MonsterConfirmDialog(
            monster = selectedMonster!!,
            partyMembers = partyMembers,
            onConfirm = {
                onMonsterSelected(selectedMonster!!)
                selectedMonster = null
            },
            onDismiss = { selectedMonster = null }
        )
    }
}

@Composable
private fun MapHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0D1B2A).copy(alpha = 0.92f))
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .zIndex(10f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                .size(40.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text("The Path of Zen", style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, color = Color(0xFFE8F5E9))
            Text("Walk the path of enlightenment", style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFA5D6A7))
        }
    }
}

// ─── Biome background ───────────────────────────────────────────────────

private data class BiomePalette(
    val sky1: Color, val sky2: Color, val ground1: Color, val ground2: Color,
    val accent: Color
)

private fun getBiome(ratio: Float): BiomePalette = when {
    ratio < 0.25f -> BiomePalette(
        Color(0xFF1B5E20), Color(0xFF388E3C), Color(0xFF2E7D32), Color(0xFF4CAF50), Color(0xFF81C784))
    ratio < 0.50f -> BiomePalette(
        Color(0xFFE65100), Color(0xFFBF360C), Color(0xFF5D4037), Color(0xFF8D6E63), Color(0xFFFFCC80))
    ratio < 0.75f -> BiomePalette(
        Color(0xFF1A237E), Color(0xFF283593), Color(0xFF37474F), Color(0xFF455A64), Color(0xFF9FA8DA))
    else -> BiomePalette(
        Color(0xFF3E2723), Color(0xFF4E342E), Color(0xFF2C0E00), Color(0xFF5D4037), Color(0xFFFF6F00))
}

@Suppress("DEPRECATION")
private fun DrawScope.drawBiomeBackground(
    totalCount: Int, dpScale: Float, sh: Float,
    topSp: Float, headerH: Float, segH: Float
) {
    val startY = topSp + headerH
    val totalH = totalCount * segH
    if (totalH <= 0f) return

    for (i in 0 until totalCount) {
        val ratio = i.toFloat() / totalCount.toFloat()
        val b = getBiome(ratio)
        val y0 = startY + i * segH
        val y1 = y0 + segH

        drawRect(
            brush = Brush.verticalGradient(
                listOf(b.sky1, b.sky2),
                startY = y0, endY = y1),
            topLeft = Offset(0f, y0), size = Size(size.width, segH))
    }

    // Deep space above first biome band
    drawRect(Color(0xFF0A0E27), topLeft = Offset(0f, 0f), size = Size(size.width, startY))
}

// ─── Biome decorations ──────────────────────────────────────────────────

private fun DrawScope.drawBiomeDecorations(
    totalCount: Int, dpScale: Float, sh: Float,
    topSp: Dp, headerH: Dp, segH: Dp, pulse: Float
) {
    val tSp = topSp.value * dpScale
    val hH = headerH.value * dpScale
    val sH = segH.value * dpScale
    val startY = tSp + hH
    val rng = Random(123)

    for (i in 0 until totalCount) {
        val ratio = i.toFloat() / totalCount.toFloat()
        val yOff = startY + i * sH
        val w = size.width

        when {
            ratio < 0.25f -> drawForestBand(yOff, sH, w, rng, dpScale)
            ratio < 0.50f -> drawDesertBand(yOff, sH, w, rng, dpScale)
            ratio < 0.75f -> drawMountainBand(yOff, sH, w, rng, dpScale, pulse)
            else -> drawVolcanicBand(yOff, sH, w, rng, dpScale, pulse)
        }
    }
}

private fun DrawScope.drawForestBand(yOff: Float, sH: Float, w: Float, rng: Random, s: Float) {
    val cx = w / 2f
    for (t in 0..3) {
        val tx = if (t % 2 == 0) 40f * s + rng.nextFloat() * 60f * s else w - 40f * s - rng.nextFloat() * 60f * s
        val ty = yOff + 20f * s + rng.nextFloat() * (sH - 60f * s)
        if (kotlin.math.abs(tx - cx) < 70f * s) continue
        drawTree(tx, ty, (16f + rng.nextFloat() * 10f) * s, Color(0xFF1B5E20))
    }
    for (g in 0..8) {
        val gx = rng.nextFloat() * w
        val gy = yOff + 20f * s + rng.nextFloat() * (sH - 40f * s)
        if (kotlin.math.abs(gx - cx) < 60f * s) continue
        drawLine(Color(0xFF66BB6A), Offset(gx, gy), Offset(gx - 4f * s, gy - 10f * s), 2f * s)
        drawLine(Color(0xFF66BB6A), Offset(gx, gy), Offset(gx + 4f * s, gy - 10f * s), 2f * s)
    }
}

private fun DrawScope.drawDesertBand(yOff: Float, sH: Float, w: Float, rng: Random, s: Float) {
    val cx = w / 2f
    for (c in 0..3) {
        val cxx = if (c % 2 == 0) 30f * s + rng.nextFloat() * 40f * s else w - 30f * s - rng.nextFloat() * 40f * s
        val cyy = yOff + 30f * s + rng.nextFloat() * (sH - 60f * s)
        if (kotlin.math.abs(cxx - cx) < 60f * s) continue
        drawCactus(cxx, cyy, (14f + rng.nextFloat() * 8f) * s, Color(0xFF33691E))
    }
    for (d in 0..4) {
        val dx = rng.nextFloat() * w
        val dy = yOff + 20f * s + rng.nextFloat() * (sH - 40f * s)
        if (kotlin.math.abs(dx - cx) < 50f * s) continue
        drawLine(Color(0xFFD7CCC8).copy(alpha = 0.3f), Offset(dx, dy), Offset(dx + 30f * s, dy), 1f * s)
    }
}

private fun DrawScope.drawMountainBand(yOff: Float, sH: Float, w: Float, rng: Random, s: Float, pulse: Float) {
    val cx = w / 2f
    for (cr in 0..3) {
        val crx = if (cr % 2 == 0) 30f * s + rng.nextFloat() * 40f * s else w - 30f * s - rng.nextFloat() * 40f * s
        val cry = yOff + 30f * s + rng.nextFloat() * (sH - 60f * s)
        if (kotlin.math.abs(crx - cx) < 60f * s) continue
        val crystalColors = listOf(Color(0xFF7E57C2), Color(0xFF5C6BC0), Color(0xFF9575CD))
        drawCrystal(crx, cry, (10f + rng.nextFloat() * 8f) * s, crystalColors[cr % 3])
    }
    for (sn in 0..3) {
        val snx = rng.nextFloat() * w
        val sny = yOff + 10f * s + rng.nextFloat() * sH * 0.3f
        if (kotlin.math.abs(snx - cx) < 60f * s) continue
        drawCircle(Color.White.copy(alpha = 0.15f), (2f + rng.nextFloat() * 4f) * s, Offset(snx, sny))
    }
    for (fl in 0..3) {
        val fll = fl * 0.3f
        val flx = rng.nextFloat() * w
        val fly = yOff + ((pulse + fll) % 1f) * sH
        drawCircle(Color.White.copy(alpha = 0.25f), 1.2f * s, Offset(flx, fly))
    }
}

private fun DrawScope.drawVolcanicBand(yOff: Float, sH: Float, w: Float, rng: Random, s: Float, pulse: Float) {
    val lavaGlow = Color(0xFFFF6F00).copy(alpha = 0.5f + 0.3f * sin(pulse * PI.toFloat() * 4f))
    for (lc in 0..2) {
        val lcx = 30f * s + lc * w / 3f
        val lcy = yOff + sH * 0.4f + rng.nextFloat() * sH * 0.3f
        val crack = Path().apply {
            moveTo(lcx, lcy)
            quadraticTo(lcx + 12f * s, lcy - 12f * s, lcx + 28f * s, lcy + 8f * s)
            quadraticTo(lcx + 40f * s, lcy - 4f * s, lcx + 55f * s, lcy + 4f * s)
        }
        drawPath(crack, lavaGlow, style = Stroke(width = 3f * s, cap = StrokeCap.Round))
    }
    for (em in 0..4) {
        val emx = rng.nextFloat() * w
        val emy = yOff + ((pulse * 0.6f + em * 0.15f) % 1f) * sH
        val emberAlpha = 0.4f + 0.3f * sin(pulse * PI.toFloat() * 5f + em)
        drawCircle(Color(0xFFFF9800).copy(alpha = emberAlpha), 2f * s, Offset(emx, emy))
        drawCircle(Color(0xFFFF4500).copy(alpha = emberAlpha * 0.5f), 3f * s, Offset(emx, emy))
    }
}

// ─── Decoration primitives ──────────────────────────────────────────────

private fun DrawScope.drawTree(cx: Float, cy: Float, size: Float, color: Color) {
    drawRect(color.copy(alpha = 0.5f), Offset(cx - size * 0.12f, cy), Size(size * 0.24f, size * 0.35f))
    drawCircle(color.copy(alpha = 0.6f), size * 0.45f, Offset(cx - size * 0.15f, cy - size * 0.25f))
    drawCircle(color.copy(alpha = 0.7f), size * 0.5f, Offset(cx + size * 0.08f, cy - size * 0.3f))
    drawCircle(color.copy(alpha = 0.55f), size * 0.35f, Offset(cx, cy - size * 0.08f))
}

private fun DrawScope.drawCactus(cx: Float, cy: Float, size: Float, color: Color) {
    val r1 = size * 0.1f
    drawRoundRect(color, topLeft = Offset(cx - size * 0.15f, cy - size * 0.5f), size = Size(size * 0.3f, size * 0.7f), cornerRadius = CornerRadius(r1, r1))
    val r2 = size * 0.08f
    drawRoundRect(color, topLeft = Offset(cx - size * 0.5f, cy - size * 0.45f), size = Size(size * 0.35f, size * 0.18f), cornerRadius = CornerRadius(r2, r2))
    drawRoundRect(color, topLeft = Offset(cx - size * 0.5f, cy - size * 0.6f), size = Size(size * 0.18f, size * 0.25f), cornerRadius = CornerRadius(r2, r2))
    drawRoundRect(color, topLeft = Offset(cx + size * 0.15f, cy - size * 0.35f), size = Size(size * 0.35f, size * 0.18f), cornerRadius = CornerRadius(r2, r2))
    drawRoundRect(color, topLeft = Offset(cx + size * 0.32f, cy - size * 0.5f), size = Size(size * 0.18f, size * 0.25f), cornerRadius = CornerRadius(r2, r2))
}

private fun DrawScope.drawCrystal(cx: Float, cy: Float, size: Float, color: Color) {
    val path = Path().apply {
        moveTo(cx, cy - size)
        lineTo(cx + size * 0.4f, cy - size * 0.1f)
        lineTo(cx + size * 0.15f, cy + size * 0.7f)
        lineTo(cx - size * 0.15f, cy + size * 0.7f)
        lineTo(cx - size * 0.4f, cy - size * 0.1f)
        close()
    }
    drawPath(path, color.copy(alpha = 0.55f))
    drawPath(path, color.copy(alpha = 0.7f), style = Stroke(width = 1.2f))
    drawLine(Color.White.copy(alpha = 0.25f), Offset(cx - size * 0.08f, cy - size * 0.2f), Offset(cx, cy + size * 0.1f), 1f)
}

// ─── Sky elements ───────────────────────────────────────────────────────

private fun DrawScope.drawSkyElements(dpScale: Float, sh: Float, drift: Float) {
    val rng = Random(456)
    val skyH = sh * 0.5f

    for (i in 0..40) {
        val sx = rng.nextFloat() * size.width
        val sy = rng.nextFloat() * skyH
        val twinkle = 0.3f + 0.4f * sin(i * 1.7f + drift * PI.toFloat() * 4f)
        drawCircle(Color.White.copy(alpha = twinkle * 0.5f), 1f + (i % 3) * 0.3f, Offset(sx, sy))
    }

    val moonX = size.width * 0.78f
    val moonY = skyH * 0.15f
    drawCircle(Color(0xFFFFF9C4).copy(alpha = 0.5f), 18f * dpScale, Offset(moonX, moonY))
    drawCircle(Color(0xFFFFF176).copy(alpha = 0.15f), 28f * dpScale, Offset(moonX, moonY))
    drawCircle(Color(0xFFFDD835).copy(alpha = 0.3f), 5f * dpScale, Offset(moonX - 5f * dpScale, moonY - 3f * dpScale))
}

// ─── Path ───────────────────────────────────────────────────────────────

private data class RoadPoint(val x: Float, val y: Float)

private fun DrawScope.drawPath(dpScale: Float, sh: Float): List<RoadPoint> {
    val points = mutableListOf<RoadPoint>()
    val centerX = size.width / 2f
    val roadWidth = 60f * dpScale
    val step = 5f

    var y = 0f
    while (y <= sh) {
        val xOff = sin(y * 0.014f) * PATH_AMPLITUDE.value * dpScale
        points.add(RoadPoint(centerX + xOff, y))
        y += step
    }

    val roadPath = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
    }

    drawPath(roadPath, Color(0xFF4E342E), style = Stroke(width = roadWidth * 1.2f, cap = StrokeCap.Round))
    drawPath(roadPath, Color(0xFF6D4C41), style = Stroke(width = roadWidth, cap = StrokeCap.Round))
    drawPath(roadPath, Color(0xFFA1887F).copy(alpha = 0.2f), style = Stroke(width = 2f * dpScale, cap = StrokeCap.Round))

    return points
}

private fun DrawScope.drawPathSegments(
    totalCount: Int, dpScale: Float, sortedMonsters: List<Monster>,
    defeatedIds: Set<String>, activeIndex: Int, roadPoints: List<RoadPoint>,
    topSp: Dp, headerH: Dp, segH: Dp
) {
    if (totalCount < 2) return
    val tS = topSp.value * dpScale
    val hH = headerH.value * dpScale
    val sH = segH.value * dpScale
    val startY = tS + hH

    for (i in 0 until totalCount - 1) {
        val y1 = startY + i * sH + sH / 2f
        val y2 = startY + (i + 1) * sH + sH / 2f
        val segment = roadPoints.filter { it.y in (y1 - sH * 0.4f)..(y2 + sH * 0.4f) }
        if (segment.size < 2) continue

        val segPath = Path().apply {
            moveTo(segment[0].x, segment[0].y)
            for (p in 1 until segment.size) lineTo(segment[p].x, segment[p].y)
        }

        val m1Defeated = defeatedIds.contains(sortedMonsters[i].id.lowercase())
        val m2Defeated = defeatedIds.contains(sortedMonsters[i + 1].id.lowercase())
        val isCompleted = m1Defeated && m2Defeated
        val isCurrent = activeIndex in (i + 1)..(i + 1)

        when {
            isCompleted -> {
                val glowColor = elementToColor(sortedMonsters[i].element)
                drawPath(segPath, glowColor.copy(alpha = 0.5f), style = Stroke(width = 5f * dpScale, cap = StrokeCap.Round))
                drawPath(segPath, glowColor.copy(alpha = 0.2f), style = Stroke(width = 12f * dpScale, cap = StrokeCap.Round))
            }
            i == activeIndex - 1 -> {
                drawPath(segPath, Color.White.copy(alpha = 0.25f), style = Stroke(width = 4f * dpScale, cap = StrokeCap.Round))
            }
            else -> {
                drawPath(segPath, Color(0xFF2C2C2C).copy(alpha = 0.5f), style = Stroke(width = 3f * dpScale, cap = StrokeCap.Round))
            }
        }
    }
}

// ─── Nodes ──────────────────────────────────────────────────────────────

private fun DrawScope.drawNodes(
    sortedMonsters: List<Monster>, defeatedIds: Set<String>, activeIndex: Int,
    dpScale: Float, pulseAnim: Float, topSp: Dp, headerH: Dp, segH: Dp
) {
    val tS = topSp.value * dpScale
    val hH = headerH.value * dpScale
    val sH = segH.value * dpScale
    val startY = tS + hH

    sortedMonsters.forEachIndexed { index, monster ->
        val isDefeated = defeatedIds.contains(monster.id.lowercase())
        val isActive = index == activeIndex
        val isUnlocked = index == sortedMonsters.lastIndex || defeatedIds.contains(sortedMonsters[index + 1].id.lowercase())
        val isLocked = !isUnlocked && !isDefeated
        val isBoss = monster.isBoss
        val difficulty = monster.difficultyTier
        val elColor = elementToColor(monster.element)

        val cy = startY + index * sH + sH / 2f
        val cx = size.width / 2f + sin(index * 0.8f) * PATH_AMPLITUDE.value * dpScale

        val nodeScale = when (difficulty) {
            DifficultyTier.EASY -> 34f
            DifficultyTier.MEDIUM -> 42f
            DifficultyTier.HARD -> 50f
            DifficultyTier.BOSS -> 62f
            DifficultyTier.SUPERBOSS -> 70f
        } * dpScale

        // Platform
        val platPath = Path().apply {
            moveTo(cx - nodeScale * 1.1f, cy + nodeScale * 0.55f)
            quadraticTo(cx - nodeScale * 1.3f, cy + nodeScale * 0.95f, cx, cy + nodeScale * 1.05f)
            quadraticTo(cx + nodeScale * 1.3f, cy + nodeScale * 0.95f, cx + nodeScale * 1.1f, cy + nodeScale * 0.55f)
            quadraticTo(cx, cy + nodeScale * 0.65f, cx - nodeScale * 1.1f, cy + nodeScale * 0.55f)
            close()
        }
        drawPath(platPath, Color(0xFF5D4037).copy(alpha = 0.75f))
        drawPath(platPath, Color(0xFF3E2723).copy(alpha = 0.5f), style = Stroke(width = 2f * dpScale))

        // Element glow (undefeated unlocked)
        if (isUnlocked && !isDefeated) {
            val glowR = nodeScale * 1.4f
            val ga = if (isActive) 0.25f + 0.15f * sin(pulseAnim * PI.toFloat() * 2f) else 0.15f
            drawCircle(elColor.copy(alpha = ga), glowR, Offset(cx, cy))
            drawCircle(elColor.copy(alpha = ga * 0.4f), glowR * 1.4f, Offset(cx, cy))
        }

        // Difficulty indicators
        when (difficulty) {
            DifficultyTier.EASY -> {}
            DifficultyTier.MEDIUM -> {
                for (sp in 0..5) {
                    val a = sp * PI.toFloat() / 3f
                    val sx = cx + nodeScale * 1.1f * cos(a)
                    val sy = cy + nodeScale * 0.85f + nodeScale * 1.1f * sin(a)
                    drawCircle(elColor.copy(alpha = 0.35f), 3f * dpScale, Offset(sx, sy))
                }
            }
            DifficultyTier.HARD -> {
                for (ru in 0..7) {
                    val a = ru * PI.toFloat() / 4f + pulseAnim * 0.2f
                    val rx = cx + nodeScale * 1.25f * cos(a)
                    val ry = cy + nodeScale * 0.85f + nodeScale * 1.25f * sin(a)
                    val ra = 0.25f + 0.2f * sin(pulseAnim * PI.toFloat() * 2f + ru)
                    drawCircle(elColor.copy(alpha = ra), 3.5f * dpScale, Offset(rx, ry),
                        style = Stroke(width = 1.5f * dpScale))
                }
            }
            DifficultyTier.BOSS, DifficultyTier.SUPERBOSS -> {
                val crownPath = Path().apply {
                    moveTo(cx - nodeScale * 1.4f, cy - nodeScale * 1.1f)
                    lineTo(cx - nodeScale * 1.2f, cy - nodeScale * 0.7f)
                    lineTo(cx - nodeScale * 0.7f, cy - nodeScale * 1.3f)
                    lineTo(cx - nodeScale * 0.25f, cy - nodeScale * 0.8f)
                    lineTo(cx, cy - nodeScale * 1.4f)
                    lineTo(cx + nodeScale * 0.25f, cy - nodeScale * 0.8f)
                    lineTo(cx + nodeScale * 0.7f, cy - nodeScale * 1.3f)
                    lineTo(cx + nodeScale * 1.2f, cy - nodeScale * 0.7f)
                    lineTo(cx + nodeScale * 1.4f, cy - nodeScale * 1.1f)
                }
                val crownA = if (isActive) 0.6f + 0.3f * sin(pulseAnim * PI.toFloat() * 2f) else 0.4f
                drawPath(crownPath, Color(0xFFFFD700).copy(alpha = crownA), style = Stroke(width = 2.5f * dpScale))

                val borderP = Path().apply {
                    moveTo(cx - nodeScale * 1.2f, cy + nodeScale * 0.7f)
                    quadraticTo(cx - nodeScale * 1.6f, cy - nodeScale * 0.15f, cx - nodeScale * 0.9f, cy - nodeScale * 0.7f)
                    quadraticTo(cx, cy - nodeScale * 1.5f, cx + nodeScale * 0.9f, cy - nodeScale * 0.7f)
                    quadraticTo(cx + nodeScale * 1.6f, cy - nodeScale * 0.15f, cx + nodeScale * 1.2f, cy + nodeScale * 0.7f)
                }
                drawPath(borderP, Color(0xFFFFD700).copy(alpha = 0.5f), style = Stroke(width = 2f * dpScale))

                if (isUnlocked && !isDefeated) {
                    val bp = 0.12f + 0.08f * sin(pulseAnim * PI.toFloat() * 3f)
                    drawCircle(Color(0xFFFFD700).copy(alpha = bp), nodeScale * 2f, Offset(cx, cy))
                }
            }
        }

        // Monster name label
        if (!isLocked) {
            val namePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 11f * dpScale
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
                isFakeBoldText = true
            }
            val defeatedPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.GRAY
                textSize = 10f * dpScale
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
                isStrikeThruText = isDefeated
            }
            val paint = if (isDefeated) defeatedPaint else namePaint
            val labelY = cy - nodeScale * 1.4f
            drawContext.canvas.nativeCanvas.drawText(
                monster.name, cx, labelY, paint
            )
        }

        // Monster silhouette
        if (!isLocked) {
            val silAlpha = if (isDefeated) 0.35f else 1f
            val silColor = if (isDefeated) Color.Gray else elColor
            drawCircle(Color.Green, nodeScale * 0.5f, Offset(cx, cy))
            drawMonsterShape(cx = cx, cy = cy - nodeScale * 0.05f, s = nodeScale * 0.65f,
                name = monster.name, tint = silColor.copy(alpha = silAlpha))
        }

        // Element orb
        if (isUnlocked && !isDefeated) {
            drawCircle(elColor, 3.5f * dpScale, Offset(cx + nodeScale * 0.65f, cy - nodeScale * 0.65f))
            drawCircle(Color.White.copy(alpha = 0.4f), 1.5f * dpScale, Offset(cx + nodeScale * 0.65f, cy - nodeScale * 0.65f))
        }

        // Defeated medal
        if (isDefeated) {
            val starPath = Path().apply {
                for (st in 0..9) {
                    val a = st * PI.toFloat() / 5f - PI.toFloat() / 2f
                    val r = if (st % 2 == 0) nodeScale * 0.3f else nodeScale * 0.15f
                    val mx = cx + r * cos(a)
                    val my = cy - nodeScale * 0.35f + r * sin(a)
                    if (st == 0) moveTo(mx, my) else lineTo(mx, my)
                }
                close()
            }
            drawPath(starPath, Color(0xFFFFD700).copy(alpha = 0.75f))
            drawPath(starPath, Color(0xFFFFA000).copy(alpha = 0.5f), style = Stroke(width = 1.2f * dpScale))
            drawCircle(Color(0xFFFFD700).copy(alpha = 0.15f), nodeScale * 0.5f, Offset(cx, cy - nodeScale * 0.35f))
        }

        // Locked fog
        if (isLocked) {
            drawCircle(Color(0xFF37474F).copy(alpha = 0.65f), nodeScale * 1.3f, Offset(cx, cy))
            drawCircle(Color(0xFF455A64).copy(alpha = 0.25f), nodeScale * 1.5f, Offset(cx, cy))

            val lockPath = Path().apply {
                moveTo(cx - nodeScale * 0.22f, cy - nodeScale * 0.1f)
                quadraticTo(cx - nodeScale * 0.22f, cy - nodeScale * 0.45f, cx, cy - nodeScale * 0.45f)
                quadraticTo(cx + nodeScale * 0.22f, cy - nodeScale * 0.45f, cx + nodeScale * 0.22f, cy - nodeScale * 0.1f)
                // lock body
                moveTo(cx - nodeScale * 0.25f, cy - nodeScale * 0.1f)
                lineTo(cx - nodeScale * 0.25f, cy + nodeScale * 0.2f)
                lineTo(cx + nodeScale * 0.25f, cy + nodeScale * 0.2f)
                lineTo(cx + nodeScale * 0.25f, cy - nodeScale * 0.1f)
                close()
            }
            drawPath(lockPath, Color.White.copy(alpha = 0.6f), style = Stroke(width = 2f * dpScale))
            drawCircle(Color.White.copy(alpha = 0.5f), 2.5f * dpScale, Offset(cx, cy + nodeScale * 0.05f))
        }

        // Active pulsing pointer
        if (isActive) {
            val arrowA = 0.5f + 0.4f * sin(pulseAnim * PI.toFloat() * 2f)
            val arrowY = cy + nodeScale * 1.3f + 6f * dpScale * sin(pulseAnim * PI.toFloat() * 2f)
            val arrPath = Path().apply {
                moveTo(cx - 8f * dpScale, arrowY + 10f * dpScale)  // bottom-left
                lineTo(cx, arrowY)                                    // tip (up)
                lineTo(cx + 8f * dpScale, arrowY + 10f * dpScale)    // bottom-right
                close()
            }
            drawPath(arrPath, Color(0xFFFFF176).copy(alpha = arrowA))
            drawCircle(Color(0xFFFFF176).copy(alpha = arrowA * 0.15f), 12f * dpScale, Offset(cx, arrowY + 5f * dpScale))
        }
    }
}

// ─── Fog of War ─────────────────────────────────────────────────────────

@Suppress("DEPRECATION")
private fun DrawScope.drawFogOfWar(
    activeIndex: Int, dpScale: Float, sh: Float,
    topSp: Dp, headerH: Dp, segH: Dp
) {
    if (activeIndex >= Int.MAX_VALUE) return

    val tS = topSp.value * dpScale
    val hH = headerH.value * dpScale
    val sH = segH.value * dpScale
    val fogY = tS + hH + sH * activeIndex

    drawRect(Color(0xFF000000).copy(alpha = 0.55f),
        topLeft = Offset(0f, 0f), size = Size(size.width, fogY))
    val fogTop = (fogY - 50f * dpScale).coerceAtLeast(0f)
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent),
            startY = fogTop, endY = fogY),
        topLeft = Offset(0f, fogTop),
        size = Size(size.width, 50f * dpScale))
}

// ─── Helpers ────────────────────────────────────────────────────────────

private fun getNodeSizeDp(tier: DifficultyTier): Dp = when (tier) {
    DifficultyTier.EASY -> 56.dp
    DifficultyTier.MEDIUM -> 64.dp
    DifficultyTier.HARD -> 72.dp
    DifficultyTier.BOSS -> 88.dp
    DifficultyTier.SUPERBOSS -> 96.dp
}

// ─── Confirmation dialog ─────────────────────────────────────────────────

@Composable
private fun MonsterConfirmDialog(
    monster: Monster,
    partyMembers: List<PartyMemberData>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val elColor = elementToColor(monster.element)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── Header: monster name + tier badge ──
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(Modifier.size(12.dp).background(elColor, CircleShape))

                    Text(
                        monster.englishName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when (monster.difficultyTier) {
                            DifficultyTier.BOSS, DifficultyTier.SUPERBOSS -> Color(0xFFFFD700).copy(alpha = 0.2f)
                            else -> elColor.copy(alpha = 0.15f)
                        }
                    ) {
                        Text(
                            monster.difficultyTier.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = when (monster.difficultyTier) {
                                DifficultyTier.BOSS, DifficultyTier.SUPERBOSS -> Color(0xFFFFD700)
                                else -> elColor
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                // ── Flavor Text ──
                val flavor = if (monster.flavorText.isNotBlank()) monster.flavorText else monster.mechanicDescription
                Text(
                    flavor,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                // ── Stats ──
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatChip("HP", "${monster.baseHp}", Color(0xFF4CAF50))
                    StatChip("ATK", "${monster.baseAtk}", Color(0xFFF44336))
                    StatChip("SPD", "${monster.baseSpd}", Color(0xFF2196F3))
                }

                // ── Party heroes ──
                Text(
                    "Active Party",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (partyMembers.isEmpty()) {
                    Text(
                        "No heroes in party!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    partyMembers.forEach { pm ->
                        val heroDef = DataLoader.heroes.find { it.id == pm.heroId }
                        if (heroDef != null) {
                            PartyHeroRow(heroDef = heroDef, pm = pm)
                        }
                    }
                }

                // ── Confirm / Cancel ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancel") }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        enabled = partyMembers.isNotEmpty()
                    ) { Text("Enter Battle") }
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
    }
}

@Composable
private fun PartyHeroRow(heroDef: Hero, pm: PartyMemberData) {
    val heroColor = elementToColor(heroDef.element)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Canvas(Modifier.size(28.dp)) {
            val c = size.width / 2f
            drawCircle(heroColor.copy(alpha = 0.2f), c, Offset(c, c))
            drawMonsterShape(
                cx = c, cy = c * 0.9f, s = c * 0.8f,
                name = heroDef.name, tint = heroColor.copy(alpha = 0.8f)
            )
        }

        Column {
            Text(heroDef.name, style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium)
            Text("Lv.${pm.level} ${heroDef.element.name}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
    }
}
