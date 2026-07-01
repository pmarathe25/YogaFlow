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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.*
import com.example.game.persistence.DataLoader
import com.example.game.viewmodel.GameViewModel

@Composable
fun ShopScreen(viewModel: GameViewModel) {
    val saveData by viewModel.saveData.collectAsState()
    val party by viewModel.party.collectAsState()
    
    var selectedItemForDetail by remember { mutableStateOf<Equipment?>(null) }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateBack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    "Shop",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "\u2726",
                            color = Color(0xFFFFD740),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "${saveData.sparks}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD740)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Filter tabs
            var selectedCategory by remember { mutableStateOf(EquipmentSlot.WEAPON) }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(EquipmentSlot.values()) { slot ->
                    FilterChip(
                        selected = slot == selectedCategory,
                        onClick = { selectedCategory = slot },
                        label = {
                            Text(
                                slot.name,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                val available = DataLoader.equipment.filter { eq ->
                    eq.slot == selectedCategory
                }
                items(available) { item ->
                    val owned = item.id in saveData.inventory
                    val canAfford = saveData.sparks >= item.sparksCost
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
        
        selectedItemForDetail?.let { item ->
            GearDetailsDialog(
                item = item,
                onDismiss = { selectedItemForDetail = null }
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
    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).alpha(if (levelLocked) 0.6f else 1f).clickable { onShowDetail() },
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Text(item.getIcon(), fontSize = 28.sp, modifier = Modifier.padding(end = 12.dp))
            
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
                        color = when (item.tier) {
                            EquipmentTier.UNIQUE -> Color(0xFFFFD740)
                            EquipmentTier.CLASS_SPECIFIC -> Color(0xFFB388FF)
                            EquipmentTier.GENERIC -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    if (levelLocked) "Requires Yoga Lv.${item.yogaLevelRequired}" else "Yoga Lv.${item.yogaLevelRequired}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (levelLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
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
                        "${item.sparksCost} \u2726",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (canAfford && !levelLocked) Color(0xFFFFD740) else MaterialTheme.colorScheme.error
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
                Text(item.getIcon(), fontSize = 48.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    item.name, 
                    style = MaterialTheme.typography.titleLarge, 
                    fontWeight = FontWeight.Bold,
                    color = when (item.tier) {
                        EquipmentTier.UNIQUE -> Color(0xFFFFD740)
                        EquipmentTier.CLASS_SPECIFIC -> Color(0xFFB388FF)
                        else -> MaterialTheme.colorScheme.onSurface
                    }
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
                
                Spacer(Modifier.height(24.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Close")
                }
            }
        }
    }
}
