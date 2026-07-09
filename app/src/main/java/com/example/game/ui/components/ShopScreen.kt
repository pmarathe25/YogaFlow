package com.example.game.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.border
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.*
import com.example.game.persistence.DataLoader
import com.example.game.viewmodel.GameViewModel
import com.example.ui.components.GlassCard

@Composable
fun ShopScreen(viewModel: GameViewModel, onBack: () -> Unit = { viewModel.navigateBack() }) {
    val saveData by viewModel.saveData.collectAsState()
    val party by viewModel.party.collectAsState()
    
    var selectedItemForDetail by remember { mutableStateOf<Equipment?>(null) }
    var selectedCategory by remember { mutableStateOf(EquipmentSlot.WEAPON) }

    Scaffold(
        bottomBar = {
            SlotNavigationBar(
                selectedSlot = selectedCategory,
                onSlotSelected = { selectedCategory = it }
            )
        },
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
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
                    "Shop",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                val availableGold = saveData.gold
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "\uD83E\uDE99",
                            color = Color(0xFFFFD700),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "$availableGold",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                    }
                }
            }

            // Tier filter
            var selectedTierFilter by remember { mutableStateOf<EquipmentTier?>(null) }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf(null) + EquipmentTier.values().toList()) { tier ->
                    val label = tier?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "All"
                    FilterChip(
                        selected = selectedTierFilter == tier,
                        onClick = { selectedTierFilter = tier },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            val available = DataLoader.equipment.filter { eq ->
                eq.slot == selectedCategory &&
                (selectedTierFilter == null || eq.tier == selectedTierFilter)
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                if (selectedTierFilter != null) {
                    items(available, key = { it.id }) { item ->
                        val owned = item.id in saveData.inventory
                        val availableGold = saveData.gold
                        val canAfford = availableGold >= item.goldCost
                        val partyHasHero = item.heroId == null || party.any { it.heroId == item.heroId }
                        val levelLocked = item.yogaLevelRequired > saveData.yogaLevel

                        ShopItemCard(
                            item = item,
                            owned = owned,
                            canAfford = canAfford,
                            partyHasHero = partyHasHero,
                            levelLocked = levelLocked,
                            onPurchase = { viewModel.purchaseItem(item.id) },
                            onShowDetail = { selectedItemForDetail = item }
                        )
                    }
                } else {
                    val grouped = available.groupBy { it.tier }
                    val tierOrder = listOf(
                        EquipmentTier.COMMON,
                        EquipmentTier.UNCOMMON,
                        EquipmentTier.RARE,
                        EquipmentTier.UNIQUE
                    )
                    for (tier in tierOrder) {
                        val tierItems = grouped[tier].orEmpty()
                        if (tierItems.isEmpty()) continue

                        item(key = "header_${tier.name}") {
                            TierSectionHeader(tier)
                        }
                        items(tierItems, key = { it.id }) { item ->
                            val owned = item.id in saveData.inventory
                            val availableGold = saveData.gold
                            val canAfford = availableGold >= item.goldCost
                            val partyHasHero = item.heroId == null || party.any { it.heroId == item.heroId }
                            val levelLocked = item.yogaLevelRequired > saveData.yogaLevel

                            ShopItemCard(
                                item = item,
                                owned = owned,
                                canAfford = canAfford,
                                partyHasHero = partyHasHero,
                                levelLocked = levelLocked,
                                onPurchase = { viewModel.purchaseItem(item.id) },
                                onShowDetail = { selectedItemForDetail = item }
                            )
                        }
                    }
                }
            }
        }
    }
    
    selectedItemForDetail?.let { item ->
        GearDetailsDialog(
            item = item,
            onDismiss = { selectedItemForDetail = null }
        )
    }
}

@Composable
private fun SlotNavigationBar(
    selectedSlot: EquipmentSlot,
    onSlotSelected: (EquipmentSlot) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        EquipmentSlot.WEAPON.let { slot ->
            NavigationBarItem(
                icon = { Text("\uD83D\uDDE1\uFE0F", fontSize = 20.sp) },
                label = { Text("Weapon") },
                selected = selectedSlot == slot,
                onClick = { onSlotSelected(slot) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFFF44336),
                    selectedTextColor = Color(0xFFF44336),
                    indicatorColor = Color(0xFFF44336).copy(alpha = 0.12f)
                )
            )
        }
        EquipmentSlot.ARMOR.let { slot ->
            NavigationBarItem(
                icon = { Text("\uD83D\uDEE1\uFE0F", fontSize = 20.sp) },
                label = { Text("Armor") },
                selected = selectedSlot == slot,
                onClick = { onSlotSelected(slot) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF4CAF50),
                    selectedTextColor = Color(0xFF4CAF50),
                    indicatorColor = Color(0xFF4CAF50).copy(alpha = 0.12f)
                )
            )
        }
        EquipmentSlot.ACCESSORY.let { slot ->
            NavigationBarItem(
                icon = { Text("\uD83D\uDC8D", fontSize = 20.sp) },
                label = { Text("Accessory") },
                selected = selectedSlot == slot,
                onClick = { onSlotSelected(slot) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF2196F3),
                    selectedTextColor = Color(0xFF2196F3),
                    indicatorColor = Color(0xFF2196F3).copy(alpha = 0.12f)
                )
            )
        }
    }
}

@Composable
private fun ShopItemCard(
    item: Equipment,
    owned: Boolean,
    canAfford: Boolean,
    partyHasHero: Boolean,
    levelLocked: Boolean,
    onPurchase: () -> Unit,
    onShowDetail: () -> Unit
) {
    val isUnique = item.tier == EquipmentTier.UNIQUE
    val shimmerTransition = rememberInfiniteTransition()
    val shimmerAlpha by shimmerTransition.animateFloat(
        initialValue = 0f, targetValue = 0.5f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse)
    )

    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).alpha(if (levelLocked) 0.6f else 1f).clickable { onShowDetail() }
            .then(
                if (isUnique) Modifier
                    .drawBehind {
                        val shimmerWidth = size.width * 0.4f
                        val shimmerX = (size.width * (shimmerAlpha * 2f)) % (size.width * 1.5f) - shimmerWidth
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.15f), Color.Transparent),
                                start = Offset(shimmerX, 0f),
                                end = Offset(shimmerX + shimmerWidth, size.height)
                            )
                        )
                    }
                    .border(width = 2.dp, color = item.getThemeColor().copy(alpha = 0.8f), shape = RoundedCornerShape(16.dp))
                else Modifier
            ),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        elevation = 2.dp,
        useDefaultPadding = false
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Text(item.icon, fontSize = 28.sp, modifier = Modifier.padding(end = 12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (levelLocked) {
                        Icon(Icons.Default.Lock, contentDescription = "Locked", modifier = Modifier.size(14.dp))
                    }
                    Text(
                        item.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = item.getThemeColor(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = item.getThemeColor().copy(alpha = 0.15f)
                    ) {
                        Text(
                            item.tier.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            color = item.getThemeColor(),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    item.bonusDescription.split("\n").first(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                if (owned) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                    ) {
                        Text(
                            "Owned",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Text(
                        "${item.goldCost} \uD83E\uDE99",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (canAfford && !levelLocked) Color(0xFFFFD700) else MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(4.dp))
                    FilledTonalButton(
                        onClick = onPurchase,
                        enabled = canAfford && partyHasHero && !levelLocked,
                        modifier = Modifier.height(32.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            "Buy",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GearDetailsDialog(item: Equipment, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(item.icon, fontSize = 48.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    item.name, 
                    style = MaterialTheme.typography.titleLarge, 
                    fontWeight = FontWeight.Bold,
                    color = item.getThemeColor()
                )
                Text(item.tier.name, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                
                Text(
                    item.description, 
                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(Modifier.height(16.dp))
                
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        item.bonusDescription,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Set bonus section for unique items
                if (item.tier == EquipmentTier.UNIQUE && item.heroId != null) {
                    val setBonus = item.heroId?.let { hid ->
                        DataLoader.setBonuses.find { it.heroId == hid }
                    }

                    if (setBonus != null) {
                        Spacer(Modifier.height(16.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "Set: ${setBonus.name}",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Spacer(Modifier.height(4.dp))
                                val otherItems = setBonus.requiredItems.filter { it != item.id }
                                Text(
                                    "Other items: ${otherItems.joinToString(", ") { id ->
                                        DataLoader.getEquipment(id)?.name ?: id
                                    }}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Bonus: ${setBonus.description}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun TierSectionHeader(tier: EquipmentTier) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = tier.getThemeColor().copy(alpha = 0.2f)
        ) {
            Text(
                tier.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = tier.getThemeColor(),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
        Spacer(Modifier.weight(1f))
    }
}
