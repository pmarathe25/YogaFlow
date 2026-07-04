package com.example.game.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.*
import com.example.game.persistence.DataLoader
import com.example.game.viewmodel.GameViewModel
import com.example.ui.components.GlassCard

@Composable
fun PartyScreen(viewModel: GameViewModel) {
    val party by viewModel.party.collectAsState()
    val saveData by viewModel.saveData.collectAsState()
    val allHeroes = DataLoader.heroes
    
    var detailHeroId by remember { mutableStateOf<String?>(null) }

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
                IconButton(onClick = { viewModel.navigateBack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                    val isUnlocked = heroDef.id.lowercase() in saveData.unlockedHeroIds
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
            Column(modifier = Modifier.padding(24.dp)) {
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
                
                // Stats Row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    DetailStat("HP", stats.maxHp.toString(), Icons.Default.Favorite, Color.Red)
                    DetailStat("ATK", stats.atk.toString(), Icons.Default.Bolt, Color(0xFFFFA500))
                    DetailStat("SPD", stats.spd.toString(), Icons.Default.Speed, Color.Cyan)
                }

                Spacer(Modifier.height(20.dp))
                
                // Level Up
                Button(
                    onClick = { viewModel.levelUpHero(hero.id) },
                    enabled = canLevelUp,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = heroColor)
                ) {
                    Text("Level Up (${levelUpCost} \u2726)", fontWeight = FontWeight.Bold)
                }
                
                Spacer(Modifier.height(24.dp))
                
                // Gear Section
                Text("Equipped Gear", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                
                val equipped = viewModel.getEquippedItems(hero.id)
                val slots = listOf(EquipmentSlot.WEAPON, EquipmentSlot.ARMOR, EquipmentSlot.ACCESSORY)
                
                slots.forEach { slot ->
                    val item = equipped.find { it.slot == slot }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(Modifier.size(36.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(item?.getIcon() ?: "", fontSize = 18.sp)
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(item?.name ?: "Empty $slot", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                item?.let { Text(it.bonusDescription, style = MaterialTheme.typography.labelSmall, color = Color.Gray, maxLines = 1) }
                            }
                        }
                        if (item != null) {
                            IconButton(onClick = { viewModel.unequipItem(hero.id, item.id) }) {
                                Icon(Icons.Default.LinkOff, contentDescription = "Unequip", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Inventory (Available for this hero)
                Text("Available Inventory", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                val available = DataLoader.equipment.filter { eq ->
                    eq.id in saveData.inventory && (eq.heroId == null || eq.heroId == hero.id) &&
                    equipped.none { it.slot == eq.slot }
                }
                
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(available) { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Text(item.getIcon(), fontSize = 24.sp)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(item.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    Text(item.bonusDescription, style = MaterialTheme.typography.labelSmall, color = Color.Gray, maxLines = 1)
                                }
                            }
                            TextButton(onClick = { viewModel.equipItem(hero.id, item.id) }) {
                                Text("EQUIP", fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
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
