package com.example.model

data class YogaPose(
    val id: Int,
    val sanskritName: String,
    val englishName: String,
    val description: String,
    val benefits: String,
    val instructions: List<String>,
    val voicePrompt: String,
    val holdDurationSec: Int = 30
)

data class YogaFlow(
    val id: String,
    val name: String,
    val description: String,
    val difficulty: String,
    val totalDurationMinutes: Int,
    val poses: List<YogaPose>
)

object YogaFlowRepository {
    @Deprecated("Flows are now loaded from assets/flows.json via FlowLoader")
    val sunSalutationFlow = YogaFlow(
        id = "sun_salutation",
        name = "Sun Salutation",
        description = "A standard 12-step sequence of yoga postures synchronized with breath (Surya Namaskar) to energize the body and mind.",
        difficulty = "Intermediate",
        totalDurationMinutes = 6,
        poses = listOf(
            YogaPose(
                id = 1,
                sanskritName = "Pranamasana",
                englishName = "Prayer Pose",
                description = "Stand upright with feet together and palms pressed together at the heart center.",
                benefits = "Establishes calm, centers attention, and prepares the mind.",
                instructions = listOf(
                    "Stand at the front edge of your mat with feet together.",
                    "Distribute your weight evenly on both feet.",
                    "Inhale, expand your chest, and relax your shoulders.",
                    "Exhale and press your palms together in front of your chest."
                ),
                voicePrompt = "Step 1: Prayer Pose, or Pranamasana. Stand tall at the front of your mat. Bring your hands together at your heart center. Exhale, grounding yourself, and set a calm intention."
            ),
            YogaPose(
                id = 2,
                sanskritName = "Hastauttanasana",
                englishName = "Raised Arms Pose",
                description = "Inhale and reach arms upward and backward, stretching the whole body.",
                benefits = "Stretches chest and abdomen, improves digestion, tones arm muscles.",
                instructions = listOf(
                    "Inhale and lift your arms up and backward.",
                    "Keep your biceps close to your ears.",
                    "Gently push your hips forward and arch back slightly.",
                    "Stretches the whole body from the heels to the tips of the fingers."
                ),
                voicePrompt = "Step 2: Raised Arms Pose, Hastauttanasana. Inhale deeply, sweep your arms up and slightly back. Reach through your fingertips, stretching your entire front body."
            ),
            YogaPose(
                id = 3,
                sanskritName = "Uttanasana",
                englishName = "Standing Forward Bend",
                description = "Exhale, hinge at the hips, and fold forward to touch the ground or shins.",
                benefits = "Stretches hamstrings and spine, relieves tension, increases circulation.",
                instructions = listOf(
                    "Exhale and bend forward from the hips, keeping the spine long.",
                    "Bring the hands down next to the feet on the mat.",
                    "Relax your head and neck downward.",
                    "Softly bend knees if hamstrings are tight."
                ),
                voicePrompt = "Step 3: Standing Forward Bend, Uttanasana. Exhale, hinge from your hips, and fold forward completely. Let your head hand heavy and touch the mat or your shins."
            ),
            YogaPose(
                id = 4,
                sanskritName = "Ashwa Sanchalanasana",
                englishName = "Equestrian / Lunge Pose",
                description = "Inhale, step your right leg far back, drop knee down, and lift chest up.",
                benefits = "Stretches hip flexors, strengthens legs, opens chest.",
                instructions = listOf(
                    "Inhale and step your right leg as far back as comfortable.",
                    "Lower your right knee to the mat and untuck the toes.",
                    "Keep your left knee aligned directly over your left ankle.",
                    "Lift your chest, roll shoulders back, and look forward or slightly up."
                ),
                voicePrompt = "Step 4: Lunge Pose, Ashwa Sanchalanasana. Inhale, step your right leg far back, lowering the knee. Lift your chest and gaze forward, opening your hips."
            ),
            YogaPose(
                id = 5,
                sanskritName = "Phalakasana",
                englishName = "Plank Pose",
                description = "Step left leg back into a strong, straight-line plank posture.",
                benefits = "Strengthens core, arms, shoulders, and wrists.",
                instructions = listOf(
                    "Hold your breath and step your left foot back next to the right foot.",
                    "Align your shoulders directly over your wrists.",
                    "Keep your body in a straight line from head to heels.",
                    "Engage your core and thighs, do not let your lower back sag."
                ),
                voicePrompt = "Step 5: Plank Pose, Phalakasana. Step your left foot back to meet the right. Hold your breath and support your body in a solid, straight line from head to heels."
            ),
            YogaPose(
                id = 6,
                sanskritName = "Ashtanga Namaskara",
                englishName = "Eight-Limbed Pose",
                description = "Lower knees, chest, and chin down to the floor, hips slightly raised.",
                benefits = "Strengthens back, shoulders, arms, and chest.",
                instructions = listOf(
                    "Exhale and gently lower your knees to the mat.",
                    "Lower your chest and chin to the floor between your hands.",
                    "Keep your hips slightly elevated off the ground.",
                    "Eight points of the body should touch the floor."
                ),
                voicePrompt = "Step 6: Eight-Limbed Pose, Ashtanga Namaskara. Exhale and lower your knees, chest, and chin to the mat. Keep your hips slightly elevated."
            ),
            YogaPose(
                id = 7,
                sanskritName = "Bhujangasana",
                englishName = "Cobra Pose",
                description = "Inhale, slide forward and lift chest up with bent elbows.",
                benefits = "Strengthens spine, opens shoulders and chest, tones back.",
                instructions = listOf(
                    "Slide your chest forward, untuck your toes, and press tops of feet into mat.",
                    "Inhale and lift your chest, keeping elbows bent and tucked in.",
                    "Keep your shoulders rolled away from your ears.",
                    "Gaze forward or slightly upward."
                ),
                voicePrompt = "Step 7: Cobra Pose, Bhujangasana. Inhale, slide forward and lift your chest up. Keep your elbows bent and close to your body, opening your heart."
            ),
            YogaPose(
                id = 8,
                sanskritName = "Adho Mukha Svanasana",
                englishName = "Downward-Facing Dog",
                description = "Exhale, tuck toes, lift hips up and back into an inverted V-shape.",
                benefits = "Energizes body, stretches hamstrings, calves, and shoulders.",
                instructions = listOf(
                    "Exhale, tuck your toes, and push your hips up and backward.",
                    "Form an inverted 'V' shape with your body.",
                    "Press your palms firmly into the mat, fingers spread wide.",
                    "Try to lower your heels toward the ground, relaxing your neck."
                ),
                voicePrompt = "Step 8: Downward-Facing Dog, Adho Mukha Svanasana. Exhale, tuck your toes, and lift your hips high and back. Press your hands into the mat, stretching your spine."
            ),
            YogaPose(
                id = 9,
                sanskritName = "Ashwa Sanchalanasana",
                englishName = "Lunge Pose (Left knee down)",
                description = "Inhale, step your right foot forward between your hands, left knee down.",
                benefits = "Opens hip flexors and pelvis, improves posture.",
                instructions = listOf(
                    "Inhale and step your right foot forward between your hands.",
                    "Lower your left knee to the mat.",
                    "Roll your shoulders back, lift your chest, and look forward.",
                    "Ensure your right knee is stacked directly above your right ankle."
                ),
                voicePrompt = "Step 9: Lunge Pose, Ashwa Sanchalanasana. Inhale, step your right foot forward between your hands, and drop your left knee. Lift your gaze and shine your chest."
            ),
            YogaPose(
                id = 10,
                sanskritName = "Uttanasana",
                englishName = "Standing Forward Bend",
                description = "Exhale, step left foot forward next to right, and fold down.",
                benefits = "Calms mind, stretches calves and spine.",
                instructions = listOf(
                    "Exhale and step your left foot forward next to your right foot.",
                    "Extend your legs and fold forward from the hips.",
                    "Touch your fingertips or palms to the floor, if possible.",
                    "Allow your head to hang completely relaxed."
                ),
                voicePrompt = "Step 10: Standing Forward Bend, Uttanasana. Exhale, step your left foot forward next to the right, and fold deeply. Let all tension drain from your neck."
            ),
            YogaPose(
                id = 11,
                sanskritName = "Hastauttanasana",
                englishName = "Raised Arms Pose",
                description = "Inhale, reach up and back, lengthening the spine.",
                benefits = "Expands lungs, energizes nervous system.",
                instructions = listOf(
                    "Inhale, roll your spine up slowly, and reach your arms overhead.",
                    "Keep biceps by your ears and stretch your arms up and backward.",
                    "Push hips slightly forward and open your heart to the sky.",
                    "Feel the breath fill your chest."
                ),
                voicePrompt = "Step 11: Raised Arms Pose, Hastauttanasana. Inhale, rise all the way up, sweep your arms overhead, and gently arch back. Lift your spirit and stretch."
            ),
            YogaPose(
                id = 12,
                sanskritName = "Pranamasana",
                englishName = "Prayer Pose",
                description = "Exhale, lower arms, and return palms together at chest.",
                benefits = "Restores baseline heart rate, returns focus to center.",
                instructions = listOf(
                    "Exhale and slowly lower your arms.",
                    "Bring your palms together in front of your chest.",
                    "Close your eyes for a moment, observing the flow of energy.",
                    "Stand quiet and steady in Tadasana / Prayer Pose."
                ),
                voicePrompt = "Step 12: Prayer Pose, Pranamasana. Exhale and lower your hands back to your heart center. Stand still, breathe, and feel the vibrant energy flowing through your entire body."
            )
        )
    )

    val warriorFlow = YogaFlow(
        id = "warrior_flow",
        name = "Warrior Strength",
        description = "A powerful flow of standing postures to build core stability, leg strength, and focused concentration.",
        difficulty = "Intermediate",
        totalDurationMinutes = 3,
        poses = listOf(
            YogaPose(
                id = 201,
                sanskritName = "Tadasana",
                englishName = "Mountain Pose",
                description = "Ground your feet, stand upright with arms by your side, and breathe steadily.",
                benefits = "Improves posture, strengthens thighs, knees, and ankles.",
                instructions = listOf(
                    "Stand with big toes touching, heels slightly apart.",
                    "Engage your thighs and lift your kneecaps.",
                    "Let your arms hang naturally with palms facing forward.",
                    "Inhale deeply, expanding your torso."
                ),
                voicePrompt = "Warrior Flow Step 1: Mountain Pose, Tadasana. Ground your feet firmly into the earth. Stand tall, relax your shoulders, and breathe with power and clarity."
            ),
            YogaPose(
                id = 202,
                sanskritName = "Virabhadrasana I",
                englishName = "Warrior I",
                description = "Step your left leg back, bend your front knee, and sweep your arms high.",
                benefits = "Stretches chest and lungs, strengthens shoulders, arms, and back.",
                instructions = listOf(
                    "Step your left foot back about 3 to 4 feet.",
                    "Turn your left foot out 45 degrees and align your hips to the front.",
                    "Bend your right knee to 90 degrees.",
                    "Inhale, sweep your arms high with palms facing."
                ),
                voicePrompt = "Warrior Flow Step 2: Warrior 1. Step your left leg back and sweep your arms high. Square your hips forward, bend your front knee, and stand strong."
            ),
            YogaPose(
                id = 203,
                sanskritName = "Virabhadrasana II",
                englishName = "Warrior II",
                description = "Open your arms wide parallel to the floor, gazing forward over your right hand.",
                benefits = "Strengthens legs and ankles, stretches chest and shoulders.",
                instructions = listOf(
                    "From Warrior I, open your hips and arms to the side.",
                    "Extend your arms straight in opposite directions.",
                    "Keep your right knee bent at 90 degrees.",
                    "Gaze steadily over your right fingertips."
                ),
                voicePrompt = "Warrior Flow Step 3: Warrior 2. Open your arms and hips to the side. Extend your energy through both fingertips, looking forward past your front hand."
            ),
            YogaPose(
                id = 204,
                sanskritName = "Trikonasana",
                englishName = "Triangle Pose",
                description = "Straighten your front leg, hinge at your hip, and reach down to your shin or floor.",
                benefits = "Stretches spine, chest, hips, and hamstrings; relieves stress.",
                instructions = listOf(
                    "Straighten your front right leg.",
                    "Hinge at the front hip, reaching forward with your right hand.",
                    "Tilt your torso down, bringing your right hand to your shin or floor.",
                    "Extend your left arm straight up toward the sky."
                ),
                voicePrompt = "Warrior Flow Step 4: Triangle Pose, Trikonasana. Straighten your front leg, lean forward, and open your arms vertically. Feel the deep side stretch and spine lengthening."
            ),
            YogaPose(
                id = 205,
                sanskritName = "Balasana",
                englishName = "Child's Pose",
                description = "Rest your knees on the floor, push your hips to your heels, and relax your forehead down.",
                benefits = "Calms brain, relieves back and neck pain, gently stretches hips.",
                instructions = listOf(
                    "Drop your knees to the mat and sit back on your heels.",
                    "Extend your arms fully in front of you.",
                    "Lower your chest and forehead to the floor.",
                    "Breathe deeply into your lower back."
                ),
                voicePrompt = "Warrior Flow Step 5: Child's Pose, Balasana. Sink your hips back to your heels and rest your forehead. Let go of all effort and breathe deeply."
            )
        )
    )

    val restorativeYinFlow = YogaFlow(
        id = "restorative_yin",
        name = "Deep Restorative Yin",
        description = "A relaxing, low-intensity series of holds designed to release deep connective tissues and soothe the nervous system.",
        difficulty = "Beginner",
        totalDurationMinutes = 4,
        poses = listOf(
            YogaPose(
                id = 301,
                sanskritName = "Balasana",
                englishName = "Child's Pose",
                description = "Rest on your shins and fold your chest forward, letting go of all tension.",
                benefits = "Deeply calming, releases upper and lower back stress.",
                instructions = listOf(
                    "Bring knees wide, big toes touching.",
                    "Lower your hips to your heels and fold forward.",
                    "Rest your arms next to your body or extended forward.",
                    "Let your shoulders relax completely."
                ),
                voicePrompt = "Restorative Yin Step 1: Child's Pose, Balasana. Bring your knees wide, sink your hips back, and relax your forehead on the mat. Feel the breath soothing your back."
            ),
            YogaPose(
                id = 302,
                sanskritName = "Salamba Bhujangasana",
                englishName = "Sphinx Pose",
                description = "Lie on your belly, resting on your forearms with chest gently lifted.",
                benefits = "Gently stimulates the lumbar curve, opens chest, stimulates thyroid.",
                instructions = listOf(
                    "Lie face down on the mat.",
                    "Bring elbows directly under your shoulders, forearms flat on the floor.",
                    "Inhale, press into your forearms, and gently lift your chest.",
                    "Relax your glutes and lower back completely."
                ),
                voicePrompt = "Restorative Yin Step 2: Sphinx Pose. Prop yourself onto your forearms, relaxing your lower body completely. Lift your chest gently and enjoy this mild back-bend."
            ),
            YogaPose(
                id = 303,
                sanskritName = "Baddha Konasana",
                englishName = "Butterfly Pose",
                description = "Sit upright, bring soles of your feet together, and let your knees fall out wide.",
                benefits = "Opens groin and inner thighs, relieves fatigue, improves hip flexibility.",
                instructions = listOf(
                    "Sit tall and draw your feet in toward your pelvis.",
                    "Press the soles of your feet together.",
                    "Allow your knees to fall open toward the sides.",
                    "Gently fold forward if comfortable."
                ),
                voicePrompt = "Restorative Yin Step 3: Butterfly Pose, Baddha Konasana. Bring the soles of your feet together, letting your knees open wide. Fold forward slightly to release the hips."
            ),
            YogaPose(
                id = 304,
                sanskritName = "Supta Matsyendrasana",
                englishName = "Reclined Spinal Twist",
                description = "Lie on your back, draw knees to chest, and drop them to the side.",
                benefits = "Massages abdominal organs, stretches spine and glutes, calms mind.",
                instructions = listOf(
                    "Lie flat on your back.",
                    "Bring your knees to a 90-degree bend above your hips.",
                    "Lower both knees to the left side while keeping shoulders flat.",
                    "Extend your right arm to the side and look over your right shoulder."
                ),
                voicePrompt = "Restorative Yin Step 4: Reclined Spinal Twist. Lay on your back and drop your knees to the side. Open your chest flat toward the sky, breathing ease into your spine."
            ),
            YogaPose(
                id = 305,
                sanskritName = "Savasana",
                englishName = "Corpse Pose",
                description = "Lie flat on your back, feet wide, arms relaxed with palms up.",
                benefits = "Reduces heart rate, deeply integrates physical practice, calms central nervous system.",
                instructions = listOf(
                    "Lie fully flat on your back.",
                    "Let your feet turn out naturally.",
                    "Bring arms slightly away from the body with palms up.",
                    "Close your eyes and sink into complete stillness."
                ),
                voicePrompt = "Restorative Yin Step 5: Corpse Pose, Savasana. Lie completely flat on your back, letting your whole body surrender. Close your eyes, be absolutely still, and rest. Namaste."
            )
        )
    )

    val morningEnergizerFlow = YogaFlow(
        id = "morning_energizer",
        name = "Morning Energizer",
        description = "Start your day with a gentle, refreshing sequence to awaken your senses, stretch your limbs, and bring vibrant focus to your morning.",
        difficulty = "Beginner",
        totalDurationMinutes = 5,
        poses = listOf(
            YogaPose(
                id = 201,
                sanskritName = "Tadasana",
                englishName = "Mountain Pose",
                description = "Stand tall with feet grounded, palms facing forward, and breath steady.",
                benefits = "Awakens the nervous system, improves posture, and builds mental clarity.",
                instructions = listOf(
                    "Press all corners of your feet into the floor.",
                    "Engage your thighs and lift your kneecaps.",
                    "Inhale deeply, lengthening your spine and relaxing your shoulders."
                ),
                voicePrompt = "Morning Energizer Step 1: Mountain Pose, or Tadasana. Wake up your body by standing tall and breathing deeply. Ground your feet and prepare your mind for the day."
            ),
            YogaPose(
                id = 2,
                sanskritName = "Hastauttanasana",
                englishName = "Raised Arms Pose",
                description = "Reach arms high, pressing palms together and arching slightly backward.",
                benefits = "Stretches the belly and arms, expands lung capacity, and invites fresh energy.",
                instructions = listOf(
                    "Inhale, sweeping arms up next to your ears.",
                    "Reach high through your fingers and lift your gaze.",
                    "Arch gently backward from your upper spine."
                ),
                voicePrompt = "Morning Energizer Step 2: Raised Arms Pose. Sweep your arms high to greet the morning. Stretch toward the sky and lift your heart."
            ),
            YogaPose(
                id = 3,
                sanskritName = "Uttanasana",
                englishName = "Standing Forward Bend",
                description = "Hinge at the hips and fold forward, relaxing head and neck downward.",
                benefits = "Stretches the back of the legs and releases spine tension accumulated during sleep.",
                instructions = listOf(
                    "Exhale, bending forward from your hips with a long spine.",
                    "Bring fingers to the floor or shins.",
                    "Relax your head and neck fully."
                ),
                voicePrompt = "Morning Energizer Step 3: Forward Bend. Exhale and hinge from your hips. Fold forward, letting your neck relax completely and releasing any stiffness from sleep."
            ),
            YogaPose(
                id = 8,
                sanskritName = "Adho Mukha Svanasana",
                englishName = "Downward-Facing Dog",
                description = "Push hips up and back, creating an inverted V-shape with the body.",
                benefits = "Lengthens the spine, stretches calves, and increases blood flow to the brain.",
                instructions = listOf(
                    "Press palms firmly into the mat, spread fingers wide.",
                    "Tuck toes and lift hips high and back.",
                    "Gently press your heels toward the ground."
                ),
                voicePrompt = "Morning Energizer Step 4: Downward Dog. Step back and press your hips up and back. Stretch your spine and awaken your hamstrings with a gentle pedaling motion."
            ),
            YogaPose(
                id = 205,
                sanskritName = "Balasana",
                englishName = "Child's Pose",
                description = "Sit on your heels, extend arms forward, and rest forehead on the mat.",
                benefits = "Brings calm, aligns breathing, and centers your mind.",
                instructions = listOf(
                    "Lower your knees to the mat, big toes touching.",
                    "Sit back on your heels and extend arms forward.",
                    "Rest your forehead down and take long, slow breaths."
                ),
                voicePrompt = "Morning Energizer Step 5: Child's Pose. Bring your knees wide and sink your hips to your heels. Breathe deeply and feel a sense of calm gratitude for the day ahead."
            )
        )
    )

    val bedtimeWindDownFlow = YogaFlow(
        id = "bedtime_wind_down",
        name = "Bedtime Wind Down",
        description = "A slow, calming sequence of grounded holds to release the tension of the day and prepare your mind and body for a deep, restful sleep.",
        difficulty = "Beginner",
        totalDurationMinutes = 5,
        poses = listOf(
            YogaPose(
                id = 301,
                sanskritName = "Balasana",
                englishName = "Child's Pose",
                description = "Rest on your shins and fold your chest forward, letting go of all tension.",
                benefits = "Relieves back and neck tension, calms the central nervous system.",
                instructions = listOf(
                    "Bring your knees wide and let your hips sink to your heels.",
                    "Walk your hands out in front of you.",
                    "Let your chest melt toward the floor."
                ),
                voicePrompt = "Bedtime Wind Down Step 1: Child's Pose. Rest your body on the mat. Take slow, soothing breaths and feel the busyness of the day fade away."
            ),
            YogaPose(
                id = 303,
                sanskritName = "Baddha Konasana",
                englishName = "Butterfly Pose",
                description = "Sit with soles of feet together, knees open wide, folding gently forward.",
                benefits = "Gently opens the inner hips and lower back, releasing emotional tension.",
                instructions = listOf(
                    "Bring the soles of your feet together, drawing heels in.",
                    "Hold your feet and sit up tall.",
                    "Exhale and slowly fold forward over your feet."
                ),
                voicePrompt = "Bedtime Wind Down Step 2: Butterfly Pose. Bring your feet together and let your knees relax outward. Lean forward slightly and release any tension in your hips."
            ),
            YogaPose(
                id = 304,
                sanskritName = "Supta Matsyendrasana",
                englishName = "Reclined Spinal Twist",
                description = "Lie flat, draw knees to chest, and let them drop to one side.",
                benefits = "Releases the spine, stimulates digestion, and relieves back tension.",
                instructions = listOf(
                    "Lie on your back and hug your knees to your chest.",
                    "Drop your knees to the left side.",
                    "Extend your right arm to the right and turn your head."
                ),
                voicePrompt = "Bedtime Wind Down Step 3: Reclined Spinal Twist. Lay back and let your knees drift to the side. Breathe deeply into your back, letting your body decompress."
            ),
            YogaPose(
                id = 305,
                sanskritName = "Savasana",
                englishName = "Corpse Pose",
                description = "Lie flat on your back, feet wide, arms relaxed with palms facing up.",
                benefits = "Induces complete physical and mental relaxation, ideal before sleep.",
                instructions = listOf(
                    "Extend your legs flat on the mat, letting feet turn outward.",
                    "Bring your arms to your sides, palms up.",
                    "Close your eyes and let your breath become natural and soft."
                ),
                voicePrompt = "Bedtime Wind Down Step 4: Savasana, or Corpse Pose. Lie completely flat, letting every muscle go limp. Close your eyes, sink into the mat, and drift into a peaceful rest."
            )
        )
    )

    val coreBalanceFlow = YogaFlow(
        id = "core_balance",
        name = "Core & Balance Builder",
        description = "Focus your energy on stability and core strength with this sequence that challenges your balance and tones your core muscles.",
        difficulty = "Intermediate",
        totalDurationMinutes = 5,
        poses = listOf(
            YogaPose(
                id = 201,
                sanskritName = "Tadasana",
                englishName = "Mountain Pose",
                description = "Ground your feet, stand upright with arms by your side, and breathe steadily.",
                benefits = "Improves posture, strengthens thighs, knees, and ankles.",
                instructions = listOf(
                    "Stand with big toes touching, heels slightly apart.",
                    "Engage your thighs and lift your kneecaps.",
                    "Let your arms hang naturally with palms facing forward."
                ),
                voicePrompt = "Core Balance Step 1: Mountain Pose. Find your balance and establish a strong, tall stance. Ground your feet into the earth."
            ),
            YogaPose(
                id = 202,
                sanskritName = "Virabhadrasana I",
                englishName = "Warrior I",
                description = "Step your left leg back, bend your front knee, and sweep your arms high.",
                benefits = "Stretches chest and lungs, strengthens shoulders, arms, and back.",
                instructions = listOf(
                    "Step your left foot back about 3 to 4 feet.",
                    "Turn your left foot out 45 degrees.",
                    "Bend your front knee to 90 degrees.",
                    "Reach arms up parallel."
                ),
                voicePrompt = "Core Balance Step 2: Warrior 1. Step your left leg back, reach high, and lift your chest. Stand solid and strong."
            ),
            YogaPose(
                id = 203,
                sanskritName = "Virabhadrasana II",
                englishName = "Warrior II",
                description = "Open your arms wide parallel to the floor, gazing forward over your right hand.",
                benefits = "Strengthens legs and ankles, stretches chest and shoulders.",
                instructions = listOf(
                    "Open your hips and arms to the side.",
                    "Extend your arms straight in opposite directions.",
                    "Keep your front knee bent over the ankle."
                ),
                voicePrompt = "Core Balance Step 3: Warrior 2. Open your arms and body to the side. Look forward past your hand, finding absolute stability."
            ),
            YogaPose(
                id = 5,
                sanskritName = "Phalakasana",
                englishName = "Plank Pose",
                description = "Align your shoulders over wrists and hold your body in a straight line.",
                benefits = "Ignites the deep abdominal muscles and strengthens wrists and arms.",
                instructions = listOf(
                    "Step back into a high push-up position.",
                    "Keep your head, hips, and heels in one long line.",
                    "Engage your navel upward toward your spine."
                ),
                voicePrompt = "Core Balance Step 4: Plank Pose. Lower into a strong plank posture. Pull your belly button in tight and support your body with core power."
            ),
            YogaPose(
                id = 8,
                sanskritName = "Adho Mukha Svanasana",
                englishName = "Downward-Facing Dog",
                description = "Push your hips up and back into an inverted V-shape, stretching your spine.",
                benefits = "Stretches the whole back body and restores calm breathing.",
                instructions = listOf(
                    "Tuck your toes and elevate your hips high.",
                    "Relax your neck and gaze toward your shins.",
                    "Press your hands firmly into the mat."
                ),
                voicePrompt = "Core Balance Step 5: Downward-Facing Dog. Elevate your hips and release your heels downward. Let your back lengthen and take deep, centering breaths."
            )
        )
    )

    val heartOpeningVinyasaFlow = YogaFlow(
        id = "heart_opening",
        name = "Heart Opening Vinyasa",
        description = "An uplifting flow centering on backbends and chest expansion to open your heart, release upper body stiffness, and boost your energy.",
        difficulty = "Intermediate",
        totalDurationMinutes = 5,
        poses = listOf(
            YogaPose(
                id = 201,
                sanskritName = "Tadasana",
                englishName = "Mountain Pose",
                description = "Ground your feet, stand upright with arms by your side, and breathe steadily.",
                benefits = "Improves posture, strengthens thighs, knees, and ankles.",
                instructions = listOf(
                    "Stand with big toes touching, heels slightly apart.",
                    "Engage your thighs and lift your kneecaps.",
                    "Let your arms hang naturally with palms facing forward."
                ),
                voicePrompt = "Heart Opening Step 1: Mountain Pose. Stand tall and ground yourself, feeling the space around your chest expand."
            ),
            YogaPose(
                id = 2,
                sanskritName = "Hastauttanasana",
                englishName = "Raised Arms Pose",
                description = "Inhale and reach arms upward and backward, stretching the whole body.",
                benefits = "Stretches chest and abdomen, improves digestion, tones back.",
                instructions = listOf(
                    "Inhale and lift your arms up and backward.",
                    "Keep biceps close to your ears.",
                    "Push your hips forward and gently arch your back."
                ),
                voicePrompt = "Heart Opening Step 2: Raised Arms Pose. Reach up and arch back slightly, stretching your chest toward the light."
            ),
            YogaPose(
                id = 302,
                sanskritName = "Salamba Bhujangasana",
                englishName = "Sphinx Pose",
                description = "Lie on your belly, resting on your forearms with chest gently lifted.",
                benefits = "Gently stimulates the lumbar curve, opens chest, stimulates thyroid.",
                instructions = listOf(
                    "Lie face down on the mat.",
                    "Bring elbows directly under your shoulders, forearms flat on the floor.",
                    "Inhale, press into your forearms, and gently lift your chest."
                ),
                voicePrompt = "Heart Opening Step 3: Sphinx Pose. Rest on your forearms, lift your chest gently, and feel your collarbones broaden."
            ),
            YogaPose(
                id = 7,
                sanskritName = "Bhujangasana",
                englishName = "Cobra Pose",
                description = "Inhale, slide forward and lift chest up with bent elbows.",
                benefits = "Strengthens spine, opens shoulders and chest, tones back.",
                instructions = listOf(
                    "Slide your chest forward, untuck your toes.",
                    "Inhale and lift your chest, keeping elbows bent and tucked in.",
                    "Keep your shoulders rolled away from your ears."
                ),
                voicePrompt = "Heart Opening Step 4: Cobra Pose. Press into your palms and roll your shoulders back. Lift your heart and breathe energy into your upper back."
            ),
            YogaPose(
                id = 205,
                sanskritName = "Balasana",
                englishName = "Child's Pose",
                description = "Rest your knees on the floor, push your hips to your heels, and relax your forehead down.",
                benefits = "Calms brain, relieves back and neck pain, gently stretches hips.",
                instructions = listOf(
                    "Drop your knees to the mat and sit back on your heels.",
                    "Extend your arms fully in front of you.",
                    "Lower your chest and forehead to the floor."
                ),
                voicePrompt = "Heart Opening Step 5: Child's Pose. Relax down completely. Absorb the openness of your chest and rest your heart on the mat."
            )
        )
    )

    val powerVinyasaAscentFlow = YogaFlow(
        id = "power_vinyasa",
        name = "Power Vinyasa Ascent",
        description = "An intense, fast-paced advanced vinyasa flow that builds stamina, muscle strength, and deep athletic concentration.",
        difficulty = "Advanced",
        totalDurationMinutes = 6,
        poses = listOf(
            YogaPose(
                id = 5,
                sanskritName = "Phalakasana",
                englishName = "Plank Pose",
                description = "Step back into a strong, straight-line plank posture.",
                benefits = "Strengthens core, arms, shoulders, and wrists.",
                instructions = listOf(
                    "Hold your body in a straight line from head to heels.",
                    "Align your shoulders directly over your wrists.",
                    "Engage your core and thighs."
                ),
                voicePrompt = "Power Vinyasa Step 1: Plank Pose. Ignite your core immediately. Hold this posture strong, building heat in your belly and arms."
            ),
            YogaPose(
                id = 6,
                sanskritName = "Ashtanga Namaskara",
                englishName = "Eight-Limbed Pose",
                description = "Lower knees, chest, and chin down to the floor, hips slightly raised.",
                benefits = "Strengthens back, shoulders, arms, and chest.",
                instructions = listOf(
                    "Exhale and lower knees, chest, and chin to the floor.",
                    "Keep your elbows tucked tightly next to your ribs.",
                    "Keep hips slightly elevated."
                ),
                voicePrompt = "Power Vinyasa Step 2: Eight-Limbed Pose. Lower with control, keeping your elbows close to your chest and your focus sharp."
            ),
            YogaPose(
                id = 7,
                sanskritName = "Bhujangasana",
                englishName = "Cobra Pose",
                description = "Inhale, slide forward and lift chest up with bent elbows.",
                benefits = "Strengthens spine, opens shoulders and chest.",
                instructions = listOf(
                    "Slide forward and lift your chest up.",
                    "Press the tops of your feet into the mat.",
                    "Keep shoulders rolled away from ears."
                ),
                voicePrompt = "Power Vinyasa Step 3: Cobra Pose. Inhale and lift up, opening your front body. Feel the strength of your spine working."
            ),
            YogaPose(
                id = 8,
                sanskritName = "Adho Mukha Svanasana",
                englishName = "Downward-Facing Dog",
                description = "Exhale, tuck toes, lift hips up and back into an inverted V-shape.",
                benefits = "Energizes body, stretches hamstrings, calves, and shoulders.",
                instructions = listOf(
                    "Tuck your toes and push your hips high and back.",
                    "Press your palms firmly into the mat.",
                    "Let your head hang heavy."
                ),
                voicePrompt = "Power Vinyasa Step 4: Downward Dog. Push back and breathe deeply. Use this pose to center yourself and stretch out your spine."
            ),
            YogaPose(
                id = 202,
                sanskritName = "Virabhadrasana I",
                englishName = "Warrior I",
                description = "Step your left leg back, bend your front knee, and sweep your arms high.",
                benefits = "Stretches chest, strengthens shoulders, arms, and back.",
                instructions = listOf(
                    "Step your left foot back 3 to 4 feet.",
                    "Bend your front knee to 90 degrees.",
                    "Reach arms upward and square your hips."
                ),
                voicePrompt = "Power Vinyasa Step 5: Warrior 1. Step up with power. Reach high, bend your front knee, and sink into your hips with complete focus."
            ),
            YogaPose(
                id = 203,
                sanskritName = "Virabhadrasana II",
                englishName = "Warrior II",
                description = "Open your arms wide parallel to the floor, gazing forward over your right hand.",
                benefits = "Strengthens legs, stretches chest and shoulders.",
                instructions = listOf(
                    "Open your arms and hips to the side.",
                    "Extend your arms straight in opposite directions.",
                    "Gaze over your front hand."
                ),
                voicePrompt = "Power Vinyasa Step 6: Warrior 2. Open up with strength. Direct your energy through both hands, standing solid as a rock."
            )
        )
    )

    val ashtangaCorePowerFlow = YogaFlow(
        id = "ashtanga_core",
        name = "Ashtanga Core Power",
        description = "An advanced, core-centric sequence of strong postures requiring intense muscle activation, breath control, and persistent focus.",
        difficulty = "Advanced",
        totalDurationMinutes = 6,
        poses = listOf(
            YogaPose(
                id = 5,
                sanskritName = "Phalakasana",
                englishName = "Plank Pose",
                description = "Step back into a strong, straight-line plank posture.",
                benefits = "Strengthens core, arms, shoulders, and wrists.",
                instructions = listOf(
                    "Hold your body in a straight line from head to heels.",
                    "Align your shoulders directly over your wrists.",
                    "Engage your core and thighs."
                ),
                voicePrompt = "Ashtanga Core Step 1: Plank Pose. Establish a firm core. Breathe steadily and let your muscles engage."
            ),
            YogaPose(
                id = 202,
                sanskritName = "Virabhadrasana I",
                englishName = "Warrior I",
                description = "Step your left leg back, bend your front knee, and sweep your arms high.",
                benefits = "Stretches chest, strengthens shoulders, arms, and back.",
                instructions = listOf(
                    "Step your left foot back 3 to 4 feet.",
                    "Bend your front knee to 90 degrees.",
                    "Reach arms upward."
                ),
                voicePrompt = "Ashtanga Core Step 2: Warrior 1. Step up with determination. Press your feet down, lift your arms, and center your focus."
            ),
            YogaPose(
                id = 203,
                sanskritName = "Virabhadrasana II",
                englishName = "Warrior II",
                description = "Open your arms wide parallel to the floor, gazing forward over your right hand.",
                benefits = "Strengthens legs, stretches chest and shoulders.",
                instructions = listOf(
                    "Open your arms and hips to the side.",
                    "Extend your arms straight in opposite directions."
                ),
                voicePrompt = "Ashtanga Core Step 3: Warrior 2. Open into a powerful stance. Gaze forward and find absolute clarity."
            ),
            YogaPose(
                id = 7,
                sanskritName = "Bhujangasana",
                englishName = "Cobra Pose",
                description = "Inhale, slide forward and lift chest up with bent elbows.",
                benefits = "Strengthens spine, opens shoulders and chest.",
                instructions = listOf(
                    "Slide forward and lift your chest up.",
                    "Press the tops of your feet into the mat."
                ),
                voicePrompt = "Ashtanga Core Step 4: Cobra Pose. Slide onto your belly and lift your chest, engaging your entire back body."
            ),
            YogaPose(
                id = 8,
                sanskritName = "Adho Mukha Svanasana",
                englishName = "Downward-Facing Dog",
                description = "Exhale, tuck toes, lift hips up and back into an inverted V-shape.",
                benefits = "Energizes body, stretches hamstrings, calves, and shoulders.",
                instructions = listOf(
                    "Tuck your toes and push your hips high and back.",
                    "Press your palms firmly into the mat."
                ),
                voicePrompt = "Ashtanga Core Step 5: Downward Dog. Push back, stretching your legs and centering your mind for the final hold."
            ),
            YogaPose(
                id = 205,
                sanskritName = "Balasana",
                englishName = "Child's Pose",
                description = "Rest your knees on the floor, push your hips to your heels, and relax your forehead down.",
                benefits = "Calms brain, relieves back and neck pain, gently stretches hips.",
                instructions = listOf(
                    "Drop your knees to the mat and sit back on your heels.",
                    "Extend your arms fully in front of you."
                ),
                voicePrompt = "Ashtanga Core Step 6: Child's Pose. Relax and surrender. Let your heartbeat slow down and enjoy the sweet release."
            )
        )
    )

    val advancedBalanceMasteryFlow = YogaFlow(
        id = "balance_mastery",
        name = "Advanced Balance Mastery",
        description = "Push the boundaries of your stability and deep focus with a sequence of challenging standing balances and grounding postures.",
        difficulty = "Advanced",
        totalDurationMinutes = 7,
        poses = listOf(
            YogaPose(
                id = 201,
                sanskritName = "Tadasana",
                englishName = "Mountain Pose",
                description = "Ground your feet, stand upright with arms by your side, and breathe steadily.",
                benefits = "Improves posture, strengthens thighs, knees, and ankles.",
                instructions = listOf(
                    "Stand with big toes touching, heels slightly apart.",
                    "Engage your thighs and lift your kneecaps.",
                    "Let your arms hang naturally with palms facing forward."
                ),
                voicePrompt = "Balance Mastery Step 1: Mountain Pose. Stand tall and completely still. Ground your mind and body."
            ),
            YogaPose(
                id = 202,
                sanskritName = "Virabhadrasana I",
                englishName = "Warrior I",
                description = "Step your left leg back, bend your front knee, and sweep your arms high.",
                benefits = "Stretches chest, strengthens shoulders, arms, and back.",
                instructions = listOf(
                    "Step your left foot back 3 to 4 feet.",
                    "Bend your front knee to 90 degrees.",
                    "Reach arms upward and square your hips."
                ),
                voicePrompt = "Balance Mastery Step 2: Warrior 1. Ground your back foot firmly. Elevate your spine and settle your hips deep."
            ),
            YogaPose(
                id = 203,
                sanskritName = "Virabhadrasana II",
                englishName = "Warrior II",
                description = "Open your arms wide parallel to the floor, gazing forward over your right hand.",
                benefits = "Strengthens legs, stretches chest and shoulders.",
                instructions = listOf(
                    "Open your arms and hips to the side.",
                    "Extend your arms straight in opposite directions.",
                    "Gaze over your front hand."
                ),
                voicePrompt = "Balance Mastery Step 3: Warrior 2. Stretch wide and stay steady, focusing your gaze forward with intent."
            ),
            YogaPose(
                id = 204,
                sanskritName = "Trikonasana",
                englishName = "Triangle Pose",
                description = "Straighten your front leg, hinge at your hip, and reach down to your shin or floor.",
                benefits = "Stretches spine, chest, hips, and hamstrings.",
                instructions = listOf(
                    "Straighten your front leg.",
                    "Reach forward and down, touching your shin or a block.",
                    "Open your chest and extend your opposite arm up."
                ),
                voicePrompt = "Balance Mastery Step 4: Triangle Pose. Hinge forward and open your heart to the side. Maintain absolute balance."
            ),
            YogaPose(
                id = 205,
                sanskritName = "Balasana",
                englishName = "Child's Pose",
                description = "Rest your knees on the floor, push your hips to your heels, and relax your forehead down.",
                benefits = "Calms brain, relieves back and neck pain.",
                instructions = listOf(
                    "Drop your knees to the mat and sit back on your heels.",
                    "Extend your arms fully in front of you."
                ),
                voicePrompt = "Balance Mastery Step 5: Child's Pose. Rest and breathe, releasing all physical effort."
            ),
            YogaPose(
                id = 305,
                sanskritName = "Savasana",
                englishName = "Corpse Pose",
                description = "Lie flat on your back, feet wide, arms relaxed with palms up.",
                benefits = "Reduces heart rate, deeply integrates physical practice.",
                instructions = listOf(
                    "Lie fully flat on your back.",
                    "Let your feet turn out naturally.",
                    "Close your eyes and sink into complete stillness."
                ),
                voicePrompt = "Balance Mastery Step 6: Savasana. Complete your balance practice with pure relaxation. Rest deeply."
            )
        )
    )

    @Deprecated("Use FlowLoader.loadFlows(context) instead")
    val allFlows = listOf(
        restorativeYinFlow,
        morningEnergizerFlow,
        bedtimeWindDownFlow,
        sunSalutationFlow,
        warriorFlow,
        coreBalanceFlow,
        heartOpeningVinyasaFlow,
        powerVinyasaAscentFlow,
        ashtangaCorePowerFlow,
        advancedBalanceMasteryFlow
    )

    @Deprecated("Use FlowLoader.loadFlows(context) instead")
    fun getAllFlows(context: android.content.Context): List<YogaFlow> {
        return FlowLoader.loadFlows(context)
    }
}
