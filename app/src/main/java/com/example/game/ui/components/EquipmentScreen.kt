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
fun EquipmentScreen(viewModel: GameViewModel) {
    val saveData by viewModel.saveData.collectAsState()
    val party by viewModel.party.collectAsState()
    var selectedHeroId by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                "Equipment",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Sparks: ${saveData.sparks}  |  Yoga Lv: ${saveData.yogaLevel}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )

            Spacer(Modifier.height(16.dp))

            // Hero selector
            Text(
                "Select Hero",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(party) { hero ->
                    FilterChip(
                        selected = hero.heroId == selectedHeroId,
                        onClick = { selectedHeroId = hero.heroId },
                        label = {
                            Text(
                                hero.name,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = elementToColor(hero.element).copy(alpha = 0.3f)
                        )
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (selectedHeroId != null) {
                val hero = party.find { it.heroId == selectedHeroId }
                if (hero != null) {
                    Text(
                        "${hero.name}'s Equipment",
                        style = MaterialTheme.typography.titleSmall,
                        color = elementToColor(hero.element),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))

                    val equipped = viewModel.getEquippedItems(hero.heroId)

                    // Equipment slots grid (2x2)
                    val slots = listOf(
                        EquipmentSlot.WEAPON to "Weapon",
                        EquipmentSlot.ARMOR to "Armor",
                        EquipmentSlot.ACCESSORY to "Acc.1",
                        EquipmentSlot.ACCESSORY to "Acc.2"
                    )
                    val slotItems = slots.map { (slot, label) ->
                        val item = equipped.find { it.slot == slot }
                        SlotData(label, item)
                    }

                    // 2x2 grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SlotCard(slotItems[0], Modifier.weight(1f))
                        SlotCard(slotItems[1], Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SlotCard(slotItems[2], Modifier.weight(1f))
                        SlotCard(slotItems[3], Modifier.weight(1f))
                    }

                    Spacer(Modifier.height(16.dp))

                    // Inventory
                    Text(
                        "Inventory",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(8.dp))

                    val filteredEquipment = EquipmentDefinitions.allEquipment.filter { eq ->
                        eq.id in saveData.inventory &&
                                eq.yogaLevelRequired <= saveData.yogaLevel &&
                                (eq.heroId == null || eq.heroId == hero.heroId)
                    }

                    if (equipped.isEmpty() && filteredEquipment.isEmpty()) {
                        Text(
                            "No items available.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(filteredEquipment) { item ->
                                val alreadyEquipped = hero.equippedItems.any { eid ->
                                    EquipmentDefinitions.getEquipment(eid)?.slot == item.slot
                                }
                                InventoryItemCard(
                                    item = item,
                                    canEquip = !alreadyEquipped,
                                    onEquip = { viewModel.equipItem(hero.heroId, item.id) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))
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

private data class SlotData(val label: String, val item: Equipment?)

@Composable
private fun SlotCard(slot: SlotData, modifier: Modifier = Modifier) {
    GlassCard(
        modifier = modifier,
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                slot.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(4.dp))
            if (slot.item != null) {
                Text(
                    slot.item.name,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD740),
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
                slot.item.bonusDescription?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Text(
                    "Empty",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
private fun InventoryItemCard(item: Equipment, canEquip: Boolean, onEquip: () -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "[${item.slot.name}] ${item.tier.name}  Lv.${item.yogaLevelRequired}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                item.bonusDescription?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            FilledTonalButton(
                onClick = onEquip,
                enabled = canEquip,
                modifier = Modifier.height(36.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    "Equip",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
