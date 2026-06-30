package com.example.game.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.*

@Composable
fun ActionPanel(
    currentHero: HeroInstance,
    heroes: List<HeroInstance>,
    comboAvailable: Boolean,
    currentCombo: ComboSkill?,
    onSkill: (Skill) -> Unit,
    onUltimate: () -> Unit,
    onCombo: (Set<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var showComboSelector by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.offset(y = (-8).dp),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Skill pills row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(currentHero.skills) { skill ->
                    SkillPill(
                        skill = skill,
                        heroElement = currentHero.element,
                        onClick = { onSkill(skill) }
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // Action buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val ultReady = currentHero.ultimateGauge >= 100
                Button(
                    onClick = onUltimate,
                    enabled = ultReady,
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (ultReady) Color(0xFFFFD740) else MaterialTheme.colorScheme.surfaceVariant,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        "\uD83D\uDD25 ULTIMATE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (ultReady) Color.Black else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }

                if (comboAvailable) {
                    Button(
                        onClick = { showComboSelector = !showComboSelector },
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF9C27B0)
                        )
                    ) {
                        Text(
                            "\u2728 COMBO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Combo selector
            if (showComboSelector) {
                Spacer(Modifier.height(8.dp))
                ComboSelector(
                    heroes = heroes,
                    onSelect = { participants ->
                        onCombo(participants)
                        showComboSelector = false
                    },
                    onDismiss = { showComboSelector = false }
                )
            }
        }
    }
}

@Composable
private fun SkillPill(
    skill: Skill,
    heroElement: Element,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(stiffness = 500f)
    )

    val accentColor = when {
        skill.healScaling != null -> Color(0xFF2E7D32)
        skill.shieldScaling != null -> Color(0xFF1565C0)
        skill.damageComponents.isNotEmpty() || skill.baseDamage > 0 -> Color(0xFFB71C1C)
        else -> Color(0xFF4A148C)
    }

    Surface(
        modifier = Modifier
            .height(32.dp)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.height(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(MaterialTheme.shapes.small)
                    .background(accentColor)
            )
            Text(
                text = skill.name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
private fun ComboSelector(
    heroes: List<HeroInstance>,
    onSelect: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "Select combo participants:",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                heroes.filter { !it.isDead }.forEach { hero ->
                    val isSelected = hero.heroId in selected
                    val heroColor = elementToColor(hero.element)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .clickable {
                                selected = if (isSelected) selected - hero.heroId
                                else selected + hero.heroId
                            }
                            .background(
                                if (isSelected) heroColor.copy(alpha = 0.3f)
                                else Color.Transparent
                            )
                            .padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(
                                    if (isSelected) heroColor.copy(alpha = 0.6f)
                                    else heroColor.copy(alpha = 0.2f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(heroColor)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            hero.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) heroColor
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { selected.let(onSelect) },
                    enabled = selected.size >= 2,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF9C27B0)
                    )
                ) {
                    Text(
                        "Execute!",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        "Cancel",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
