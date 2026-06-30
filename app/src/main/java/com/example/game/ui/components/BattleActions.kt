package com.example.game.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    onDefend: () -> Unit,
    onCombo: (Set<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var showComboSelector by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xCC000000))
            .padding(8.dp)
    ) {
        // Skill buttons row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(currentHero.skills) { skill ->
                SkillButton(
                    skill = skill,
                    onClick = { onSkill(skill) },
                    modifier = Modifier.width(100.dp)
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // Action row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Ultimate button
            val ultReady = currentHero.ultimateGauge >= 100
            Button(
                onClick = onUltimate,
                enabled = ultReady,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (ultReady) Color(0xFFFFD700) else Color(0xFF555555),
                    disabledContainerColor = Color(0xFF333333)
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "ULTIMATE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (ultReady) Color.Black else Color.Gray
                )
            }

            // Defend button
            OutlinedButton(
                onClick = onDefend,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("DEFEND", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            // Combo button
            if (comboAvailable) {
                Button(
                    onClick = { showComboSelector = !showComboSelector },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF9C27B0)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("COMBO", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Combo selector
        if (showComboSelector) {
            Spacer(Modifier.height(6.dp))
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

@Composable
private fun SkillButton(
    skill: Skill,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        skill.healScaling != null -> Color(0xFF2E7D32).copy(alpha = 0.8f)
        skill.shieldScaling != null -> Color(0xFF1565C0).copy(alpha = 0.8f)
        skill.damageComponents.isNotEmpty() || skill.baseDamage > 0 -> Color(0xFFB71C1C).copy(alpha = 0.8f)
        else -> Color(0xFF4A148C).copy(alpha = 0.8f)
    }

    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = skill.name,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
            Text(
                text = skill.description.take(40) + if (skill.description.length > 40) "..." else "",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 7.sp,
                textAlign = TextAlign.Center,
                maxLines = 2
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xDD1A0033))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                "Select combo participants:",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                heroes.filter { !it.isDead }.forEach { hero ->
                    val isSelected = hero.heroId in selected
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selected = if (isSelected) selected - hero.heroId
                            else selected + hero.heroId
                        },
                        label = { Text(hero.name, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF9C27B0)
                        )
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = { selected.let(onSelect) },
                    enabled = selected.size >= 2,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0))
                ) {
                    Text("Execute!", fontSize = 11.sp)
                }
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel", fontSize = 11.sp)
                }
            }
        }
    }
}
