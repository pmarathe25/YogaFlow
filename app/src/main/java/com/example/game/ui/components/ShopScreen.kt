package com.example.game.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.game.model.*
import com.example.game.viewmodel.GameViewModel

@Composable
fun ShopScreen(viewModel: GameViewModel) {
    val saveData by viewModel.saveData.collectAsState()
    val party by viewModel.party.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header with sparks
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Shop",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(16.dp),
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
                val available = EquipmentDefinitions.allEquipment.filter { eq ->
                    eq.slot == selectedCategory &&
                            eq.yogaLevelRequired <= saveData.yogaLevel
                }
                items(available) { item ->
                    val owned = item.id in saveData.inventory
                    val canAfford = saveData.sparks >= item.sparksCost
                    val partyHasHero = item.heroId == null || party.any { it.heroId == item.heroId }

                    ShopItemCard(
                        item = item,
                        owned = owned,
                        canAfford = canAfford,
                        partyHasHero = partyHasHero,
                        onPurchase = { viewModel.purchaseItem(item.id) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { viewModel.navigateBack() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Back",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun ShopItemCard(
    item: Equipment,
    owned: Boolean,
    canAfford: Boolean,
    partyHasHero: Boolean,
    onPurchase: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                    // Tier badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when (item.tier) {
                            EquipmentTier.UNIQUE -> Color(0xFFFFD740).copy(alpha = 0.2f)
                            EquipmentTier.CLASS_SPECIFIC -> Color(0xFFB388FF).copy(alpha = 0.2f)
                            EquipmentTier.GENERIC -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = item.tier.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = when (item.tier) {
                                EquipmentTier.UNIQUE -> Color(0xFFFFD740)
                                EquipmentTier.CLASS_SPECIFIC -> Color(0xFFB388FF)
                                EquipmentTier.GENERIC -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "[${item.slot.name}]  Lv.${item.yogaLevelRequired}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                item.bonusDescription?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                if (!partyHasHero && item.heroId != null) {
                    Text(
                        "Requires: ${item.heroId}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (owned) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
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
                        color = if (canAfford) Color(0xFFFFD740) else MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(4.dp))
                    FilledTonalButton(
                        onClick = onPurchase,
                        enabled = canAfford && partyHasHero,
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
