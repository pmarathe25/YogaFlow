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
fun ShopScreen(viewModel: GameViewModel) {
    val saveData by viewModel.saveData.collectAsState()
    val party by viewModel.party.collectAsState()

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
                "SHOP",
                color = Color(0xFF4488FF),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Sparks: ${saveData.sparks}  |  Yoga Lv: ${saveData.yogaLevel}",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp
            )

            Spacer(Modifier.height(16.dp))

            // Filter tabs
            var selectedCategory by remember { mutableStateOf(EquipmentSlot.WEAPON) }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                EquipmentSlot.values().forEach { slot ->
                    FilterChip(
                        selected = slot == selectedCategory,
                        onClick = { selectedCategory = slot },
                        label = { Text(slot.name.take(4), fontSize = 9.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF1565C0)
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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back")
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
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x40000000))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.name,
                    color = when (item.tier) {
                        EquipmentTier.UNIQUE -> Color(0xFFFFD740)
                        EquipmentTier.CLASS_SPECIFIC -> Color(0xFFB388FF)
                        EquipmentTier.GENERIC -> Color.White
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    "[${item.slot.name}] ${item.tier.name}  Lv.${item.yogaLevelRequired}",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp
                )
                item.bonusDescription?.let {
                    Text(it, color = Color(0xFFFFD740).copy(alpha = 0.7f), fontSize = 10.sp)
                }
                if (!partyHasHero && item.heroId != null) {
                    Text(
                        "Requires: ${item.heroId}",
                        color = Color(0xFFFF4444).copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (owned) {
                    Text("OWNED", color = Color(0xFF66BB6A), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text("${item.sparksCost} ✦", color = if (canAfford) Color(0xFFFFD740) else Color(0xFFFF4444), fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = onPurchase,
                        enabled = canAfford && partyHasHero,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E7D32),
                            disabledContainerColor = Color(0xFF333333)
                        ),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Buy", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}
