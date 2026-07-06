# Plan 26: Replace "Your Journey" Page With Expanded Path of Zen Portal

## Problem

The "Your Journey" page (`ExpandedDashboardScreen`) is a stack of panels topped by a header bar. The Path of Zen portal card — the key entry point to battles — is nested deep at the bottom. The page should be replaced by the portal card itself, filling the full screen.

## Changes

### 1. Portal card becomes the entire page

**`JourneyScreen.kt`** — replace the entire `ExpandedDashboardScreen` content. Remove the top bar (back arrow + "Your Journey" title + trophy icon). The portal card takes over the full screen.

The new page structure:

```
 ┌─────────────────────────────────┐
 │                                 │
 │       [spiral orb, 160dp]       │
 │                                 │
 │      Path of Zen                │
 │  Walk the path of enlightenment │
 │                                 │
 │   Encounters ahead:             │
 │   ◉ ◉ ◉ ◉ ◉ ◉ ...              │
 │                                 │
 │   ┌─── LEVEL CARD ───┐         │
 │   │ LVL 5 • Warrior   │         │
 │   │ ████████░░░░ XP   │         │
 │   └───────────────────┘         │
 │                                 │
 │   [  ENTER THE PATH  ]          │
 │                                 │
 │   [Party] [Shop] [Trophies]     │
 │                                 │
 └─────────────────────────────────┘
```

#### Full composable rewrite

```kotlin
@Composable
fun ExpandedDashboardScreen(
    viewModel: YogaViewModel,
    gameViewModel: GameViewModel,
    onBack: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onNavigateToBattle: () -> Unit,
    onNavigateToParty: () -> Unit,
    onNavigateToShop: () -> Unit,
    onNavigateToTrophies: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalSessions by viewModel.totalSessions.collectAsState()
    val totalXp by viewModel.totalXp.collectAsState()
    val currentLevel by viewModel.currentLevel.collectAsState()
    val currentLevelName by viewModel.currentLevelName.collectAsState()
    val levelProgress by viewModel.levelProgress.collectAsState()
    val gameSaveData by gameViewModel.saveData.collectAsState()
    val party by gameViewModel.party.collectAsState()
    val sparks = gameSaveData.sparks
    val gold = gameSaveData.gold

    var showMonsterRoad by remember { mutableStateOf(false) }
    var showLevelsDialog by remember { mutableStateOf(false) }

    // Level info dialog
    if (showLevelsDialog) {
        LevelsInfoDialog(
            currentLevel = currentLevel,
            totalXp = totalXp,
            onDismiss = { showLevelsDialog = false }
        )
    }

    // Monster road selection (opened by "ENTER THE PATH")
    if (showMonsterRoad) {
        MonsterRoadSelection(
            monsters = DataLoader.monsters,
            defeatedIds = gameSaveData.defeatedMonsterIds,
            onMonsterSelected = { monster ->
                gameViewModel.startBattle(monster.id)
                onNavigateToBattle()
                showMonsterRoad = false
            },
            onBack = { showMonsterRoad = false }
        )
    } else {
        // ── Full-screen portal ──
        val infiniteTransition = rememberInfiniteTransition()
        val pulse by infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse)
        )
        val glow by infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(3000), RepeatMode.Reverse)
        )
        val previewMonsters = remember { DataLoader.monsters.take(6) }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0D1B2A))
        ) {
            // Background animation
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width; val h = size.height
                val cx = w / 2f; val cy = h / 2f
                drawCircle(Color(0xFF4CAF50).copy(alpha = 0.08f + 0.04f * sin(pulse * 3f)), w * 0.5f, Offset(cx, cy))
                drawCircle(Color(0xFF81C784).copy(alpha = 0.05f + 0.03f * sin(glow * 2f)), w * 0.65f, Offset(cx, cy))
                for (i in 0..25) {
                    val sx = (i * 137.5f) % w; val sy = (i * 97.3f) % (h * 0.7f)
                    val twinkle = 0.3f + 0.4f * sin(i * 1.3f + glow * 4f)
                    drawCircle(Color.White.copy(alpha = twinkle * 0.4f), 1.2f, Offset(sx, sy))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Sparks + Gold (top right-ish area) ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⚡", fontSize = 16.sp)
                    Spacer(Modifier.width(4.dp))
                    Text("$sparks",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Icon(Icons.Default.MonetizationOn, contentDescription = null,
                        tint = Color(0xFFFFD700), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("$gold",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFFFD700))
                }

                Spacer(Modifier.height(24.dp))

                // ── Level card (compact, clickable) ──
                LevelCardCompact(
                    currentLevel = currentLevel,
                    currentLevelName = currentLevelName,
                    totalXp = totalXp,
                    levelProgress = levelProgress,
                    onClick = { showLevelsDialog = true }
                )

                Spacer(Modifier.height(32.dp))

                // ── Spiral orb ──
                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(160.dp)) {
                        val c = size.width / 2f; val r = size.width * 0.45f
                        val innerR = size.width * 0.25f
                        drawCircle(Color(0xFF4CAF50).copy(alpha = 0.2f + 0.1f * sin(pulse * 2f)), r * 1.2f, Offset(c, c))
                        drawCircle(Color(0xFF66BB6A).copy(alpha = 0.15f), r * 1.0f, Offset(c, c), style = Stroke(width = 4f))
                        drawCircle(Color(0xFF388E3C).copy(alpha = 0.2f), innerR, Offset(c, c), style = Stroke(width = 2f))
                        val path = Path()
                        for (i in 0..359) {
                            val a = i * 0.01745f
                            val sr = innerR * (0.3f + 0.7f * (i / 360f))
                            val px = c + sr * kotlin.math.cos(a)
                            val py = c + sr * kotlin.math.sin(a)
                            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                        }
                        drawPath(path, Color(0xFFA5D6A7).copy(alpha = 0.5f), style = Stroke(width = 2f))
                        val orbPulse = 0.6f + 0.4f * sin(pulse * 3f)
                        drawCircle(Color(0xFFE8F5E9).copy(alpha = orbPulse * 0.8f), innerR * 0.35f, Offset(c, c))
                        drawCircle(Color.White.copy(alpha = orbPulse * 0.3f), innerR * 0.5f, Offset(c, c))
                    }
                }

                Spacer(Modifier.height(32.dp))

                // ── Title ──
                Text(
                    "Path of Zen",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold, color = Color(0xFFE8F5E9)
                )
                Text(
                    "Walk the path of enlightenment",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFA5D6A7),
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(Modifier.height(32.dp))

                // ── Monster preview row ──
                Text(
                    "Encounters ahead:",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF81C784)
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    previewMonsters.forEach { monster ->
                        MonsterPreviewCircle(monster, size = 52.dp)
                    }
                    Text("...", color = Color(0xFF81C784), fontSize = 24.sp)
                }

                Spacer(Modifier.height(36.dp))

                // ── ENTER THE PATH button ──
                Button(
                    onClick = { showMonsterRoad = true },
                    modifier = Modifier.width(240.dp).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C).copy(alpha = 0.9f))
                ) {
                    Text("ENTER THE PATH", fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 2.sp)
                }

                Spacer(Modifier.height(32.dp))

                // ── Nav buttons ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NavCard(
                        title = "Party", icon = "👥",
                        subtitle = "${party.size} Heroes",
                        onClick = onNavigateToParty,
                        modifier = Modifier.weight(1f)
                    )
                    NavCard(
                        title = "Shop", icon = "🛒",
                        subtitle = "New Items",
                        onClick = onNavigateToShop,
                        modifier = Modifier.weight(1f)
                    )
                    NavCard(
                        title = "Trophies", icon = "🏆",
                        subtitle = "Achievements",
                        onClick = onNavigateToTrophies,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
```

### 2. Add compact level card composable

```kotlin
@Composable
private fun LevelCardCompact(
    currentLevel: Int,
    currentLevelName: String,
    totalXp: Int,
    levelProgress: Float,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Level badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        brush = Brush.radialGradient(
                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("LVL", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onPrimary)
                    Text("$currentLevel", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimary)
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(currentLevelName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface)
                Text("$totalXp XP", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { levelProgress },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    strokeCap = StrokeCap.Round
                )
            }

            // Total sessions
            Text(
                "→",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}
```

### 3. Remove old content

Remove from `JourneyScreen.kt`:
- `StatisticsPanel` composable entirely (lines 143-577)
- Top bar Row (lines 92-121) with back arrow, "Your Journey" title, trophy icon
- `showTrophies` state and `TrophyModal` dialog/handler
- All unused imports: `AnimatedVisibility`, `expandVertically`, `shrinkVertically`, `LazyRow` (if no longer used), `LevelDefinitions`, `TrophyModal`, `testTag`

### 4. Update `MonsterPreviewCircle`

Add an optional `size` parameter:

```diff
 @Composable
-private fun MonsterPreviewCircle(monster: Monster) {
+private fun MonsterPreviewCircle(monster: Monster, size: Dp = 44.dp) {
     val elColor = elementToColor(monster.element)
     Box(
-        modifier = Modifier.size(44.dp),
+        modifier = Modifier.size(size),
```

### 5. Update navigation callers

**`YogaNavHost.kt:114-131`** — add the new nav callbacks:

```diff
 composable(Screen.ExpandedDashboard.route) {
     ExpandedDashboardScreen(
         viewModel = viewModel,
         gameViewModel = gameViewModel,
         onBack = { navController.popBackStack() },
         onNavigateToHistory = {
             navController.navigate(Screen.History.route)
         },
         onOpenSettings = {
             navController.navigate(Screen.Settings.route) {
                 popUpTo(Screen.Dashboard.route)
             }
         },
         onNavigateToBattle = {
             navController.navigate(Screen.ZenBattle.route) { launchSingleTop = true }
         },
+        onNavigateToParty = {
+            gameViewModel.navigateTo(GameScreen.PARTY)
+            navController.navigate(Screen.ZenBattle.route) { launchSingleTop = true }
+        },
+        onNavigateToShop = {
+            gameViewModel.navigateTo(GameScreen.SHOP)
+            navController.navigate(Screen.ZenBattle.route) { launchSingleTop = true }
+        },
+        onNavigateToTrophies = {
+            gameViewModel.navigateTo(GameScreen.TROPHIES)
+            navController.navigate(Screen.ZenBattle.route) { launchSingleTop = true }
+        },
     )
 }
```

### 6. Spark icon change

Remove `Icons.Default.Favorite` from the codebase. The sparks display in the new page uses `"⚡"` emoji. Search other `.kt` files for any remaining `Icons.Default.Favorite` references related to sparks and replace with `Icons.Default.AutoAwesome`.

## Files to modify

| File | Changes |
|---|---|
| `JourneyScreen.kt` | Rewrite `ExpandedDashboardScreen` — portal card IS the page; remove top bar/title/back-arrow; add compact `LevelCardCompact`; remove `StatisticsPanel`, daily quest box, old stat cards; update `MonsterPreviewCircle` with size param |
| `YogaNavHost.kt` | Add `onNavigateToParty`, `onNavigateToShop`, `onNavigateToTrophies` callbacks |
| All files with `Icons.Default.Favorite` | Replace with `Icons.Default.AutoAwesome` or `"⚡"` |

## Dependencies

- None. Standalone redesign of the Journey page.
