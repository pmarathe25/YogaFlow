package com.example.model

data class ZoneLevel(
    val level: Int,
    val name: String,
    val costXp: Int,
    val costSparks: Int,
    val visitorsProvided: Int,
    val iconEmoji: String
)

data class GardenZone(
    val id: String,
    val name: String,
    val description: String,
    val levels: List<ZoneLevel>
)

object GardenShop {
    val zones = listOf(
        GardenZone(
            id = "entrance",
            name = "Welcoming Path",
            description = "The entrance to your sanctuary.",
            levels = listOf(
                ZoneLevel(1, "Dirt Path", 50, 0, 1, "🟫"),
                ZoneLevel(2, "Pebble Walkway", 150, 1, 3, "🪨"),
                ZoneLevel(3, "Stone Steps", 400, 3, 7, "🪜"),
                ZoneLevel(4, "Torii Gate", 1000, 5, 15, "⛩️")
            )
        ),
        GardenZone(
            id = "meditation",
            name = "Meditation Area",
            description = "A quiet place to sit and reflect.",
            levels = listOf(
                ZoneLevel(1, "Woven Mat", 100, 0, 2, "🧎"),
                ZoneLevel(2, "Wooden Deck", 300, 2, 5, "🪵"),
                ZoneLevel(3, "Bamboo Pavilion", 800, 4, 12, "🎋"),
                ZoneLevel(4, "Grand Dojo", 2000, 10, 30, "🏯")
            )
        ),
        GardenZone(
            id = "water",
            name = "Water Feature",
            description = "The soothing sound of flowing water.",
            levels = listOf(
                ZoneLevel(1, "Stone Basin", 200, 1, 3, "🥣"),
                ZoneLevel(2, "Bamboo Fountain", 500, 3, 8, "🎍"),
                ZoneLevel(3, "Lily Pond", 1200, 6, 20, "🪷"),
                ZoneLevel(4, "Koi Lake", 3000, 12, 50, "🐟")
            )
        ),
        GardenZone(
            id = "flora",
            name = "Flora & Trees",
            description = "Life blooming in your garden.",
            levels = listOf(
                ZoneLevel(1, "Tall Grass", 150, 0, 2, "🌾"),
                ZoneLevel(2, "Potted Bonsai", 400, 2, 6, "🪴"),
                ZoneLevel(3, "Cherry Blossom", 1000, 5, 18, "🌸"),
                ZoneLevel(4, "Ancient Pine", 2500, 15, 40, "🌲")
            )
        )
    )

    fun getCost(type: String): Int {
        // type is expected to be "zoneId_level" e.g., "entrance_1"
        val parts = type.split("_")
        if (parts.size == 2) {
            val zoneId = parts[0]
            val level = parts[1].toIntOrNull() ?: 1
            val zone = zones.find { it.id == zoneId }
            val zoneLevel = zone?.levels?.find { it.level == level }
            return zoneLevel?.costXp ?: 0
        }
        return 0 // Refund old items
    }
}

