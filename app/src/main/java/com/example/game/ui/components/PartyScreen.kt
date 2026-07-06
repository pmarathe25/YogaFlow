package com.example.game.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.game.model.*
import com.example.game.persistence.DataLoader
import com.example.game.viewmodel.GameViewModel
import com.example.ui.components.GlassCard

@Composable
fun PartyScreen(viewModel: GameViewModel, onBack: () -> Unit = { viewModel.navigateBack() }) {
    val party by viewModel.party.collectAsState()
    val saveData by viewModel.saveData.collectAsState()
    val allHeroes = DataLoader.heroes
    
    var detailHeroId by remember { mutableStateOf<Int?>(null) }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back",
                         tint = MaterialTheme.colorScheme.onBackground)
                }
                Text(
                    "Your Heroes",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(Modifier.height(8.dp))
            Text(
                "Yoga Level: ${saveData.yogaLevel} | Sparks: ${saveData.sparks} \u2726",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 12.dp)
            )
            Spacer(Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(allHeroes) { heroDef ->
                    val partyMember = party.find { it.heroId == heroDef.id }
                    val isUnlocked = heroDef.id in saveData.unlockedHeroIds
                    val canPurchase = heroDef.unlockYogaLevel <= saveData.yogaLevel && !isUnlocked
                    
                    if (canPurchase) {
                        PurchasableHeroItem(
                            hero = heroDef,
                            sparks = saveData.sparks,
                            onPurchase = { viewModel.purchaseHero(heroDef.id) }
                        )
                    } else {
                        HeroListItem(
                            hero = heroDef,
                            partyMember = partyMember,
                            isUnlocked = isUnlocked,
                            onClick = { if (isUnlocked) detailHeroId = heroDef.id }
                        )
                    }
                }
            }
        }
        
        // Hero Details Modal
        detailHeroId?.let { id ->
            val heroDef = allHeroes.find { it.id == id }
            val partyMember = party.find { it.heroId == id }
            if (heroDef != null && partyMember != null) {
                HeroDetailsDialog(
                    hero = heroDef,
                    partyMember = partyMember,
                    saveData = saveData,
                    viewModel = viewModel,
                    onDismiss = { detailHeroId = null }
                )
            }
        }
    }
}

private data class HeroStats(val maxHp: Int, val atk: Int, val spd: Int)

private fun computeHeroStats(hero: Hero, level: Int): HeroStats {
    val mult = 1f + (level - 1) * 0.15f
    return HeroStats(
        maxHp = (hero.baseHp * mult).toInt(),
        atk = (hero.baseAtk * mult).toInt(),
        spd = (hero.baseSpd * mult).toInt()
    )
}

@Composable
private fun HeroListItem(hero: Hero, partyMember: PartyMemberData?, isUnlocked: Boolean, onClick: () -> Unit) {
    val heroColor = if (isUnlocked) elementToColor(hero.element) else Color.Gray

    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).alpha(if (isUnlocked) 1f else 0.5f).clickable(enabled = isUnlocked) { onClick() },
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        elevation = 3.dp,
        useDefaultPadding = false
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(heroColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (!isUnlocked) {
                    Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color.Gray)
                } else {
                    HeroPortrait(hero.id, heroColor, Modifier.size(48.dp))
                }
            }
            
            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    hero.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = heroColor,
                    fontWeight = FontWeight.Bold
                )
                if (isUnlocked && partyMember != null) {
                    val stats = computeHeroStats(hero, partyMember.level)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Lv.${partyMember.level}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text("HP ${stats.maxHp}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                } else {
                    Text("Requires Yoga Lv.${hero.unlockYogaLevel}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }
            
            if (isUnlocked) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Details", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
            }
        }
    }
}

@Composable
private fun PurchasableHeroItem(hero: Hero, sparks: Int, onPurchase: () -> Unit) {
    val heroColor = elementToColor(hero.element)
    val cost = hero.unlockYogaLevel
    val canAfford = sparks >= cost

    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        elevation = 3.dp,
        useDefaultPadding = false
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(heroColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(hero.name.take(1), fontWeight = FontWeight.ExtraBold, color = heroColor, fontSize = 24.sp)
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    hero.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = heroColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Recruit for ${cost} \u2726",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (canAfford) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.error
                )
            }

            FilledTonalButton(
                onClick = onPurchase,
                enabled = canAfford,
                modifier = Modifier.height(36.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = heroColor.copy(alpha = 0.3f),
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text("Buy", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun HeroDetailsDialog(
    hero: Hero,
    partyMember: PartyMemberData,
    saveData: GameProgress,
    viewModel: GameViewModel,
    onDismiss: () -> Unit
) {
    val heroColor = elementToColor(hero.element)
    val levelUpCost = viewModel.getHeroLevelUpCost(hero.id)
    val canLevelUp = saveData.sparks >= levelUpCost
    val stats = computeHeroStats(hero, partyMember.level)

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(max = 650.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(64.dp).background(heroColor.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        HeroPortrait(hero.id, heroColor, Modifier.size(56.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(hero.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                        Text("Level ${partyMember.level} ${hero.role.name}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close") }
                }
                
                Spacer(Modifier.height(12.dp))
                
                Text(
                    hero.flavorQuote, 
                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                HorizontalDivider(Modifier.padding(vertical = 16.dp))
                
                // Stats
                Column(modifier = Modifier.fillMaxWidth()) {
                    StatBar("HP", stats.maxHp, 500, Color.Red, "\u2764\uFE0F")
                    StatBar("ATK", stats.atk, 80, Color(0xFFFFA500), "\u2694\uFE0F")
                    StatBar("SPD", stats.spd, 25, Color.Cyan, "\uD83D\uDCA8")
                }

                HorizontalDivider(Modifier.padding(vertical = 16.dp))

                // Level Up
                var showLevelUpDialog by remember { mutableStateOf(false) }
                var equipSlot by remember { mutableStateOf<EquipmentSlot?>(null) }

                Button(
                    onClick = { showLevelUpDialog = true },
                    enabled = canLevelUp,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = heroColor)
                ) {
                    Text("Level Up (${levelUpCost} \u2726)", fontWeight = FontWeight.Bold)
                }

                if (showLevelUpDialog) {
                    LevelUpDialog(
                        hero = hero,
                        partyMember = partyMember,
                        heroColor = heroColor,
                        onConfirm = {
                            viewModel.levelUpHero(hero.id)
                            showLevelUpDialog = false
                        },
                        onDismiss = { showLevelUpDialog = false }
                    )
                }
                
                Spacer(Modifier.height(24.dp))
                
                // Gear Section
                Text("Equipped Gear", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))

                val equipped = viewModel.getEquippedItems(hero.id)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier.size(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        HeroPortrait(hero.id, heroColor, Modifier.size(100.dp))
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val weaponItem = equipped.find { it.slot == EquipmentSlot.WEAPON }
                        val armorItem = equipped.find { it.slot == EquipmentSlot.ARMOR }
                        val accessoryItem = equipped.find { it.slot == EquipmentSlot.ACCESSORY }

                        EquipmentSlotCard(
                            slot = EquipmentSlot.WEAPON,
                            item = weaponItem,
                            heroColor = heroColor,
                            onUnequip = { viewModel.unequipItem(hero.id, it) },
                            onEquip = { equipSlot = EquipmentSlot.WEAPON }
                        )
                        EquipmentSlotCard(
                            slot = EquipmentSlot.ARMOR,
                            item = armorItem,
                            heroColor = heroColor,
                            onUnequip = { viewModel.unequipItem(hero.id, it) },
                            onEquip = { equipSlot = EquipmentSlot.ARMOR }
                        )
                        EquipmentSlotCard(
                            slot = EquipmentSlot.ACCESSORY,
                            item = accessoryItem,
                            heroColor = heroColor,
                            onUnequip = { viewModel.unequipItem(hero.id, it) },
                            onEquip = { equipSlot = EquipmentSlot.ACCESSORY }
                        )
                    }
                }
                
                equipSlot?.let { slot ->
                    val availableItems = DataLoader.equipment.filter { eq ->
                        eq.id in saveData.inventory && eq.slot == slot &&
                        (eq.heroId == null || eq.heroId == hero.id)
                    }

                    EquipItemDialog(
                        slot = slot,
                        items = availableItems,
                        currentlyEquipped = equipped.find { it.slot == slot },
                        heroColor = heroColor,
                        onEquip = { itemId ->
                            viewModel.equipItem(hero.id, itemId)
                            equipSlot = null
                        },
                        onDismiss = { equipSlot = null }
                    )
                }

                // Skills section
                Text("Skills", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))

                hero.skills.forEach { skill ->
                    SkillCard(skill = skill, hero = hero, level = partyMember.level, heroColor = heroColor)
                }

                // Ultimate
                Spacer(Modifier.height(12.dp))
                SkillCard(skill = hero.ultimate, hero = hero, level = partyMember.level, isUltimate = true, heroColor = heroColor)

                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun SkillCard(skill: Skill, hero: Hero, level: Int, isUltimate: Boolean = false, heroColor: Color = Color.Gray) {
    val typeColor = when {
        isUltimate -> heroColor
        skill.healScaling != null -> Color(0xFF4CAF50)
        skill.shieldScaling != null -> Color(0xFF2196F3)
        skill.buffs.isNotEmpty() -> Color(0xFFFFD740)
        skill.damageComponents.any { it.type == DamageType.PHYSICAL } -> MaterialTheme.colorScheme.onSurface
        skill.damageComponents.any { it.type == DamageType.ELEMENTAL } -> elementToColor(skill.damageComponents.firstNotNullOfOrNull { it.element } ?: Element.NEUTRAL)
        else -> MaterialTheme.colorScheme.onSurface
    }

    val typeLabel = when {
        isUltimate -> "Ultimate"
        skill.healScaling != null -> "Heal"
        skill.shieldScaling != null -> "Shield"
        skill.cleanse -> "Cleanse"
        skill.statusEffects.isNotEmpty() -> "Status"
        skill.buffs.isNotEmpty() -> "Buff"
        skill.damageComponents.any { it.type == DamageType.ELEMENTAL } ->
            skill.damageComponents.firstNotNullOfOrNull { it.element?.name } ?: "Damage"
        else -> "Physical"
    }

    val damage = skill.baseDamage + skill.damagePerLevel * (level - 1)
    val healAmount = skill.healScaling?.let {
        if (it.isPercentage) "${it.baseHeal}%" else "${it.baseHeal + it.healPerLevel * (level - 1)}"
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = typeColor.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
            // Type indicator
            Box(
                modifier = Modifier.size(8.dp, 36.dp)
                    .background(typeColor, RoundedCornerShape(4.dp))
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        skill.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = typeColor
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = typeColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            typeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = typeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                    if (isUltimate) {
                        Spacer(Modifier.width(4.dp))
                        Text("\u2B50", fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    skill.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (damage > 0) {
                        Text("${damage} dmg", style = MaterialTheme.typography.labelSmall, color = typeColor)
                    }
                    if (healAmount != null) {
                        Text("Heal $healAmount", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
                    }
                    if (skill.cooldown > 0) {
                        Text("CD: ${skill.cooldown}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                    if (skill.cleanse) {
                        Text("Cleanses", style = MaterialTheme.typography.labelSmall, color = Color(0xFFAB47BC))
                    }
                }
            }
            // Ultimate gauge gain
            if (!isUltimate) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("+${skill.ultimateGain}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text("ult gauge", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
private fun EquipmentSlotCard(
    slot: EquipmentSlot,
    item: Equipment?,
    heroColor: Color,
    onUnequip: (String) -> Unit,
    onEquip: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (item != null)
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (item != null) onUnequip(item.id) else onEquip() }
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                    item?.icon ?: when (slot) {
                    EquipmentSlot.WEAPON -> "\uD83D\uDDE1\uFE0F"
                    EquipmentSlot.ARMOR -> "\uD83D\uDEE1\uFE0F"
                    EquipmentSlot.ACCESSORY -> "\uD83D\uDC8D"
                },
                fontSize = 20.sp,
                modifier = Modifier.padding(end = 8.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item?.name ?: "Empty ${slot.name.lowercase().replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (item != null) FontWeight.Bold else FontWeight.Normal,
                    color = if (item != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                if (item != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        item.bonusDescription.split("\n").first(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (item != null) {
                IconButton(onClick = { onUnequip(item.id) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.LinkOff, contentDescription = "Unequip", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun DetailStat(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
    }
}

@Composable
private fun StatBar(label: String, value: Int, maxValue: Int, color: Color, icon: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(icon, fontSize = 16.sp, modifier = Modifier.width(24.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(48.dp))
        LinearProgressIndicator(
            progress = { value.toFloat() / maxValue.toFloat() },
            modifier = Modifier.weight(1f).height(12.dp).clip(RoundedCornerShape(6.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.15f)
        )
        Spacer(Modifier.width(8.dp))
        Text("$value", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun LevelUpDialog(
    hero: Hero,
    partyMember: PartyMemberData,
    heroColor: Color,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val currentStats = computeHeroStats(hero, partyMember.level)
    val nextStats = computeHeroStats(hero, partyMember.level + 1)

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Level Up ${hero.name}?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                Text("Stats", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                StatComparisonRow("HP", currentStats.maxHp, nextStats.maxHp, Color.Red)
                StatComparisonRow("ATK", currentStats.atk, nextStats.atk, Color(0xFFFFA500))
                StatComparisonRow("SPD", currentStats.spd, nextStats.spd, Color.Cyan)

                Spacer(Modifier.height(16.dp))

                Text("Skills", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                hero.skills.forEach { skill ->
                    SkillComparisonRow(skill, partyMember.level)
                }

                Spacer(Modifier.height(20.dp))

                Text("Cost: sparks \u2726", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(onClick = onConfirm, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = heroColor)) { Text("Confirm") }
                }
            }
        }
    }
}

@Composable
private fun StatComparisonRow(label: String, current: Int, next: Int, color: Color) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text("$current", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(" \u2192 ", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Text("$next", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
        Text(" (+${next - current})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(48.dp))
    }
}

@Composable
private fun SkillComparisonRow(skill: Skill, currentLevel: Int) {
    val current = skill.baseDamage + skill.damagePerLevel * (currentLevel - 1)
    val next = skill.baseDamage + skill.damagePerLevel * currentLevel
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
        Text(skill.name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        if (skill.damagePerLevel > 0) {
            Text("$current \u2192 $next dmg", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        } else if (skill.healScaling != null) {
            val curHeal = skill.healScaling.baseHeal + skill.healScaling.healPerLevel * (currentLevel - 1)
            val nxtHeal = skill.healScaling.baseHeal + skill.healScaling.healPerLevel * currentLevel
            if (skill.healScaling.healPerLevel > 0) {
                Text("$curHeal \u2192 $nxtHeal heal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            } else {
                Text("No change", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        } else {
            Text("No change", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

@Composable
private fun EquipItemDialog(
    slot: EquipmentSlot,
    items: List<Equipment>,
    currentlyEquipped: Equipment?,
    heroColor: Color,
    onEquip: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                Text(
                    "Select ${slot.name.lowercase().replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (currentlyEquipped != null) "Currently: ${currentlyEquipped.name}" else "No item equipped",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))

                if (items.isEmpty()) {
                    Text(
                        "No items available for this slot.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                } else {
                    items.forEach { item ->
                        EquipItemRow(
                            item = item,
                            currentlyEquipped = currentlyEquipped,
                            heroColor = heroColor,
                            onEquip = { onEquip(item.id) }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun EquipItemRow(
    item: Equipment,
    currentlyEquipped: Equipment?,
    heroColor: Color,
    onEquip: () -> Unit
) {
    val isAlreadyEquipped = item.id == currentlyEquipped?.id

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(
            alpha = if (isAlreadyEquipped) 0.3f else 0.5f
        ),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(item.icon, fontSize = 24.sp, modifier = Modifier.padding(end = 12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        item.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = item.getThemeColor()
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = item.getThemeColor().copy(alpha = 0.15f)
                    ) {
                        Text(
                            item.tier.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            color = item.getThemeColor(),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                if (currentlyEquipped != null && !isAlreadyEquipped) {
                    StatComparison(
                        currentItem = currentlyEquipped,
                        newItem = item
                    )
                } else if (isAlreadyEquipped) {
                    Text(
                        "Currently equipped",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        item.bonusDescription,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!isAlreadyEquipped) {
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = onEquip,
                    modifier = Modifier.height(36.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = heroColor.copy(alpha = 0.3f)
                    )
                ) {
                    Text("Equip", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun StatComparison(currentItem: Equipment, newItem: Equipment) {
    val allTypes = (currentItem.effects.map { it.type } + newItem.effects.map { it.type }).distinct()

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        allTypes.forEach { type ->
            val currentVal = currentItem.effects.find { it.type == type }?.value ?: 0f
            val newVal = newItem.effects.find { it.type == type }?.value ?: 0f

            if (currentVal != newVal) {
                val label = type.name.lowercase().replace("_", " ")
                val fmt = { v: Float -> if (v >= 1f) "+${v.toInt()}" else if (v > 0f) "+${(v * 100).toInt()}%" else "0" }
                val isBetter = newVal > currentVal

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "${fmt(currentVal)} \u2192 ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        fmt(newVal),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isBetter) Color(0xFF4CAF50) else Color(0xFFE53935)
                    )
                    Text(
                        " $label",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
