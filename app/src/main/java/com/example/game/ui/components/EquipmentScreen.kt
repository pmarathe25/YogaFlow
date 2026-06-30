package com.example.game.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.*
import com.example.game.viewmodel.GameViewModel

@Composable
fun EquipmentScreen(viewModel: GameViewModel) {
    val saveData by viewModel.saveData.collectAsState()
    val party by viewModel.party.collectAsState()
    var selectedHeroId by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(Color(0xFF0D1B2A), Color(0xFF1B2838))
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                "EQUIPMENT",
                color = Color(0xFF4488FF),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Sparks: ${saveData.sparks}  |  Yoga Lv: ${saveData.yogaLevel}",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp
            )

            Spacer(Modifier.height(16.dp))

            // Hero selector
            Text("Select Hero", color = Color.White, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                party.forEach { hero ->
                    FilterChip(
                        selected = hero.heroId == selectedHeroId,
                        onClick = { selectedHeroId = hero.heroId },
                        label = { Text(hero.name, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = elementToColor(hero.element).copy(alpha = 0.3f)
                        )
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (selectedHeroId != null) {
                // Equipped items
                val hero = party.find { it.heroId == selectedHeroId }
                if (hero != null) {
                    Text(
                        "${hero.name}'s Equipment",
                        color = elementToColor(hero.element),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))

                    val equipped = viewModel.getEquippedItems(hero.heroId)
                    if (equipped.isEmpty()) {
                        Text("No items equipped.", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
                    } else {
                        equipped.forEach { item ->
                            EquippedItemCard(item, onUnequip = {
                                viewModel.unequipItem(hero.heroId, item.id)
                            })
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Inventory
                    Text("Inventory", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))

                    val filteredEquipment = EquipmentDefinitions.allEquipment.filter { eq ->
                        eq.id in saveData.inventory &&
                                eq.yogaLevelRequired <= saveData.yogaLevel &&
                                (eq.heroId == null || eq.heroId == hero.heroId)
                    }

                    if (filteredEquipment.isEmpty()) {
                        Text("No available equipment.", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
                    } else {
                        LazyColumn {
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
            Button(
                onClick = { viewModel.navigateBack() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back")
            }
        }
    }
}

@Composable
private fun EquippedItemCard(item: Equipment, onUnequip: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x40000000))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "[${item.slot.name}] ${item.name}",
                    color = Color(0xFFFFD740),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                item.bonusDescription?.let {
                    Text(it, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                }
            }
            OutlinedButton(onClick = onUnequip) {
                Text("X", fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun InventoryItemCard(item: Equipment, canEquip: Boolean, onEquip: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x40000000))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${item.name}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "[${item.slot.name}] ${item.tier.name}  Lv.${item.yogaLevelRequired}",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp
                )
                item.bonusDescription?.let {
                    Text(it, color = Color(0xFFFFD740).copy(alpha = 0.7f), fontSize = 10.sp)
                }
            }
            Button(
                onClick = onEquip,
                enabled = canEquip,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Equip", fontSize = 10.sp)
            }
        }
    }
}
