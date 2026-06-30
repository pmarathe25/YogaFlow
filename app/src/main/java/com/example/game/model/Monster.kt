package com.example.game.model

import com.example.game.model.Element.*
import com.example.game.model.DamageType.*
import com.example.game.model.TargetType.*
import com.example.game.model.StatusEffectType.*

data class Monster(
    val id: String,
    val name: String,
    val englishName: String,
    val element: Element,
    val baseHp: Int,
    val baseAtk: Int,
    val baseSpd: Int,
    val specialAttack: Skill,
    val mechanicDescription: String,
    val aiBehavior: AIBehavior,
    val difficultyTier: DifficultyTier,
    val phases: List<MonsterPhase> = listOf(MonsterPhase(1f, emptyList())),
    val isBoss: Boolean = false
)

enum class DifficultyTier {
    EASY, MEDIUM, HARD, BOSS, SUPERBOSS
}

data class MonsterPhase(
    val hpThreshold: Float,  // below this % HP the phase activates (1.0 = start, 0.0 = never)
    val triggers: List<PhaseTrigger>
)

data class PhaseTrigger(
    val type: PhaseTriggerType,
    val value: Float = 0f,
    val summonMonsterId: String? = null
)

enum class PhaseTriggerType {
    REFLECT_DAMAGE,
    SUMMON_ADD,
    GAIN_SHIELD,
    DOUBLE_ACTIONS,
    BECOME_UNTARGETABLE,
    NULLIFY_ELEMENT,
    EXTRA_ACTION
}

data class AIBehavior(
    val specialChance: Float = 0.3f,
    val tauntPreference: Boolean = true,
    val targetStrategy: TargetStrategy = TargetStrategy.RANDOM
)

enum class TargetStrategy {
    RANDOM, LOWEST_HP, HIGHEST_HP, MOST_BUFFS, RANDOM_HERO
}

data class MonsterInstance(
    val monsterId: String,
    val name: String,
    val englishName: String,
    val element: Element,
    val maxHp: Int,
    var currentHp: Int,
    val atk: Int,
    val spd: Int,
    val specialAttack: Skill,
    val aiBehavior: AIBehavior,
    val phases: List<MonsterPhase>,
    var shield: Int = 0,
    var activePhase: Int = 0,
    var isBoss: Boolean = false,
    var isDead: Boolean = false,
    var turnsSinceLastSpecial: Int = 0,
    var extraActionsThisRound: Int = 0
) {
    val hpPercent: Float get() = if (maxHp > 0) currentHp.toFloat() / maxHp else 0f
}

object MonsterDefinitions {
    private val bhayaSpecial = Skill("bhaya_special", "Terrifying Shadow",
        "Stops positive energy, dealing dark damage.",
        SINGLE_ENEMY, damageComponents = listOf(DamageComponent(ELEMENTAL, SHADOW, 100)),
        baseDamage = 11, statusEffects = listOf(StatusEffectInfliction(ATK_DOWN, 0.30f, 3)))

    private val tandraSpecial = Skill("tandra_special", "Drowsing Mist",
        "AOE SPD- debuff.",
        ALL_ENEMIES, baseDamage = 9,
        statusEffects = listOf(StatusEffectInfliction(SPD_DOWN, 1f, 3)))

    private val chintaSpecial = Skill("chinta_special", "Anxious Swarm",
        "Multi-hit electric attack.",
        ALL_ENEMIES, damageComponents = listOf(DamageComponent(ELEMENTAL, ELECTRIC, 100)),
        baseDamage = 10, hits = 3)

    private val alasyaSpecial = Skill("alasya_special", "Crushing Weight",
        "Massive single attack after building up.",
        SINGLE_ENEMY, damageComponents = listOf(DamageComponent(PHYSICAL)),
        baseDamage = 22)  // 2x normal

    private val matsaryaSpecial = Skill("matsarya_special", "Covetous Gaze",
        "Steals a random buff.",
        SINGLE_ENEMY, baseDamage = 16)

    private val krodhaSpecial = Skill("krodha_special", "Furious Burn",
        "Heavy fire damage.",
        SINGLE_ENEMY, damageComponents = listOf(DamageComponent(ELEMENTAL, FIRE, 100)),
        baseDamage = 25)

    private val dveshaSpecial = Skill("dvesha_special", "Vicious Strike",
        "Bonus damage vs highest HP target.",
        SINGLE_ENEMY, damageComponents = listOf(DamageComponent(PHYSICAL)),
        baseDamage = 18)

    private val mohaSpecial = Skill("moha_special", "Illusion Mist",
        "Confuses target.",
        SINGLE_ENEMY, baseDamage = 22,
        statusEffects = listOf(StatusEffectInfliction(CONFUSE, 0.30f, 2)))

    private val lobhaSpecial = Skill("lobha_special", "Grasping Hunger",
        "Damage with shield gain.",
        SINGLE_ENEMY, damageComponents = listOf(DamageComponent(ELEMENTAL, EARTH, 100)),
        baseDamage = 20)

    private val abhimanaSpecial = Skill("abhimana_special", "Arrogant Beam",
        "Consumes buffs for power.",
        SINGLE_ENEMY, damageComponents = listOf(DamageComponent(ELEMENTAL, LIGHT, 100)),
        baseDamage = 26)

    private val madaSpecial = Skill("mada_special", "Radiant Slam",
        "Powerful light attack.",
        SINGLE_ENEMY, damageComponents = listOf(DamageComponent(ELEMENTAL, LIGHT, 100)),
        baseDamage = 28)

    private val irsyaSpecial = Skill("irsya_special", "Mirror Strike",
        "Copies the last hero action.",
        SINGLE_ENEMY, damageComponents = listOf(DamageComponent(ELEMENTAL, ELECTRIC, 100)),
        baseDamage = 24)

    private val ahankaraSpecial = Skill("ahankara_special", "Arrogant Slam",
        "Massive all-element damage.",
        SINGLE_ENEMY, damageComponents = listOf(DamageComponent(ELEMENTAL, LIGHT, 100)),
        baseDamage = 30)

    private val mayaSpecial = Skill("maya_special", "Deceptive Shadows",
        "Dark damage.",
        ALL_ENEMIES, damageComponents = listOf(DamageComponent(ELEMENTAL, DARK, 100)),
        baseDamage = 20)

    private val kleshSpecial = Skill("klesh_special", "Chaos Waves",
        "Void damage with random status.",
        ALL_ENEMIES, damageComponents = listOf(DamageComponent(ELEMENTAL, VOID, 100)),
        baseDamage = 26)

    private val samsaraSpecial = Skill("samsara_special", "Karmic Retribution",
        "Devastating void damage.",
        ALL_ENEMIES, damageComponents = listOf(DamageComponent(ELEMENTAL, VOID, 100)),
        baseDamage = 40)

    val allMonsters: List<Monster> = listOf(
        Monster("Bhaya", "Bhaya", "Self-Doubt & Fear", SHADOW,
            80, 8, 16, bhayaSpecial,
            "Applies ATK- debuff. Fast but fragile.",
            AIBehavior(0.3f, true, TargetStrategy.RANDOM), DifficultyTier.EASY),
        Monster("Tandra", "Tandra", "Fatigue & Lethargy", WATER,
            100, 9, 10, tandraSpecial,
            "AOE SPD- debuff. Slow but tanky.",
            AIBehavior(0.3f, true, TargetStrategy.RANDOM), DifficultyTier.EASY),
        Monster("Chinta", "Chinta", "Anxiety & Worry", ELECTRIC,
            110, 10, 14, chintaSpecial,
            "Multi-hit attack. Small SPD- chance per hit.",
            AIBehavior(0.25f, true, TargetStrategy.RANDOM), DifficultyTier.EASY),
        Monster("Alasya", "Alasya", "Sloth & Laziness", EARTH,
            130, 11, 6, alasyaSpecial,
            "Skips every other turn to build up, then big hit.",
            AIBehavior(0f, true, TargetStrategy.RANDOM), DifficultyTier.EASY),

        Monster("Matsarya", "Matsarya", "Envy & Jealousy", DARK,
            180, 16, 12, matsaryaSpecial,
            "Steals one random buff from target hero.",
            AIBehavior(0.3f, true, TargetStrategy.MOST_BUFFS), DifficultyTier.MEDIUM),
        Monster("Krodha", "Krodha", "Anger & Rage", FIRE,
            220, 20, 8, krodhaSpecial,
            "Below 50% HP: Berserk (ATK×2.5, DEF×0.5).",
            AIBehavior(0.3f, true, TargetStrategy.RANDOM), DifficultyTier.MEDIUM,
            phases = listOf(MonsterPhase(1f, emptyList()), MonsterPhase(0.5f,
                listOf(PhaseTrigger(PhaseTriggerType.DOUBLE_ACTIONS, 2.5f))))),  // atk multiplier
        Monster("Dvesha", "Dvesha", "Aversion & Hatred", DARK,
            250, 18, 10, dveshaSpecial,
            "Targets highest-HP hero, bonus damage = 15% max HP.",
            AIBehavior(0.3f, true, TargetStrategy.HIGHEST_HP), DifficultyTier.MEDIUM),

        Monster("Moha", "Moha", "Delusion & Attachment", DARK,
            350, 22, 11, mohaSpecial,
            "30% confuse chance. Sometimes dodges. Gets one extra action per round.",
            AIBehavior(0.35f, true, TargetStrategy.RANDOM), DifficultyTier.HARD,
            phases = listOf(MonsterPhase(1f, listOf(PhaseTrigger(PhaseTriggerType.EXTRA_ACTION))))),
        Monster("Lobha", "Lobha", "Greed & Attachment", EARTH,
            400, 20, 9, lobhaSpecial,
            "Gains shield = 40% damage dealt. Extremely tanky.",
            AIBehavior(0.3f, true, TargetStrategy.RANDOM), DifficultyTier.HARD),
        Monster("Abhimana", "Abhimana", "Conceit & Pride", LIGHT,
            380, 26, 11, abhimanaSpecial,
            "Consumes buffs → gains ATK+ per buff. Punishes buffers.",
            AIBehavior(0.4f, true, TargetStrategy.MOST_BUFFS), DifficultyTier.HARD),
        Monster("Mada", "Mada", "Pride & Vanity", LIGHT,
            420, 28, 12, madaSpecial,
            "Heals 20% max HP when heroes heal. Anti-healer.",
            AIBehavior(0.3f, true, TargetStrategy.RANDOM), DifficultyTier.HARD),
        Monster("Irsya", "Irsya", "Jealousy & Resentment", ELECTRIC,
            360, 24, 15, irsyaSpecial,
            "Copies last hero action. Gains +5 SPD per copy. Gets faster each turn.",
            AIBehavior(0.3f, true, TargetStrategy.RANDOM), DifficultyTier.HARD),

        Monster("Ahankara", "Ahankara", "Ego & Vanity (Boss)", LIGHT,
            600, 30, 12, ahankaraSpecial,
            "P1: Reflects 35% damage. P2: Summons mirror images.",
            AIBehavior(0.3f, true, TargetStrategy.RANDOM), DifficultyTier.BOSS, isBoss = true,
            phases = listOf(
                MonsterPhase(1f, listOf(PhaseTrigger(PhaseTriggerType.REFLECT_DAMAGE, 0.35f))),
                MonsterPhase(0.5f, listOf(PhaseTrigger(PhaseTriggerType.SUMMON_ADD, summonMonsterId = "Ahankara_Mirror")))
            )),
        Monster("Maya", "Maya", "Illusion & Deception (Boss)", DARK,
            700, 28, 13, mayaSpecial,
            "P1: Creates shadow copies of heroes. P2: Untargetable while copies remain.",
            AIBehavior(0.3f, true, TargetStrategy.RANDOM), DifficultyTier.BOSS, isBoss = true,
            phases = listOf(
                MonsterPhase(1f, listOf(PhaseTrigger(PhaseTriggerType.SUMMON_ADD, summonMonsterId = "Maya_Shadow"))),
                MonsterPhase(0.5f, listOf(PhaseTrigger(PhaseTriggerType.BECOME_UNTARGETABLE)))
            )),
        Monster("Klesh", "Klesh", "Turmoil & Affliction (Final Boss)", VOID,
            900, 35, 14, kleshSpecial,
            "P1: Random status. P2: Shield + summon. P3: Double actions.",
            AIBehavior(0.35f, true, TargetStrategy.RANDOM), DifficultyTier.BOSS, isBoss = true,
            phases = listOf(
                MonsterPhase(1f, listOf(PhaseTrigger(PhaseTriggerType.NULLIFY_ELEMENT))),
                MonsterPhase(0.66f, listOf(PhaseTrigger(PhaseTriggerType.GAIN_SHIELD, 0.6f),
                    PhaseTrigger(PhaseTriggerType.SUMMON_ADD, summonMonsterId = "Bhaya"))),
                MonsterPhase(0.33f, listOf(PhaseTrigger(PhaseTriggerType.DOUBLE_ACTIONS, 2f)))
            )),
        Monster("Samsara", "Samsara", "Cycle of Suffering (Superboss)", VOID,
            1500, 40, 16, samsaraSpecial,
            "5 phases, each nullifies one element. P5: Only Strike works.",
            AIBehavior(0.4f, true, TargetStrategy.RANDOM), DifficultyTier.SUPERBOSS, isBoss = true,
            phases = listOf(
                MonsterPhase(1f, listOf(PhaseTrigger(PhaseTriggerType.NULLIFY_ELEMENT, 0f, "FIRE"))),
                MonsterPhase(0.8f, listOf(PhaseTrigger(PhaseTriggerType.NULLIFY_ELEMENT, 0f, "LIGHT"))),
                MonsterPhase(0.6f, listOf(PhaseTrigger(PhaseTriggerType.NULLIFY_ELEMENT, 0f, "AIR"))),
                MonsterPhase(0.4f, listOf(PhaseTrigger(PhaseTriggerType.NULLIFY_ELEMENT, 0f, "EARTH"))),
                MonsterPhase(0.2f, listOf(PhaseTrigger(PhaseTriggerType.NULLIFY_ELEMENT, 0f, "ALL")))
            ))
    )

    fun getMonster(id: String): Monster? = allMonsters.find { it.id == id }

    fun createInstance(monster: Monster): MonsterInstance {
        return MonsterInstance(
            monsterId = monster.id,
            name = monster.name,
            englishName = monster.englishName,
            element = monster.element,
            maxHp = monster.baseHp,
            currentHp = monster.baseHp,
            atk = monster.baseAtk,
            spd = monster.baseSpd,
            specialAttack = monster.specialAttack,
            aiBehavior = monster.aiBehavior,
            phases = monster.phases,
            isBoss = monster.isBoss
        )
    }
}
