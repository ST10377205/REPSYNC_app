package com.example.myapplication

import android.content.Context

// Translate display labels only. Canonical exercise names remain stable in storage.
fun Context.localizedText(value: String): String {
    val resource = when (value) {
        "Home" -> R.string.ui_home
        "Bodyweight" -> R.string.ui_bodyweight
        "Flexibility" -> R.string.ui_flexibility
        "Home Full Body" -> R.string.ui_home_full_body
        "Build Muscle" -> R.string.ui_build_muscle
        "Home (No Equipment)" -> R.string.ui_home_no_equipment
        "3 Days / Week" -> R.string.ui_3_days_week
        "Home Workout" -> R.string.ui_home_workout
        "Lose Weight" -> R.string.ui_lose_weight
        "Improve Endurance" -> R.string.ui_improve_endurance
        "Dumbbells Only" -> R.string.ui_dumbbells_only
        "Full Gym" -> R.string.ui_full_gym
        "Resistance Bands" -> R.string.ui_resistance_bands
        "2 Days / Week" -> R.string.ui_2_days_week
        "4 Days / Week" -> R.string.ui_4_days_week
        "5+ Days / Week" -> R.string.ui_5_days_week
        "Custom Routine" -> R.string.custom_routine
        "Bodyweight Power" -> R.string.bodyweight_power
        "Morning Flow" -> R.string.morning_flow
        "Workout" -> R.string.workout
        "Push-ups" -> R.string.label_6
        "Bodyweight Squats" -> R.string.label_7
        "Squats" -> R.string.label_8
        "Plank" -> R.string.label_9
        "Mountain Climbers" -> R.string.label_10
        "Lunges" -> R.string.label_11
        "Burpees" -> R.string.label_12
        "Dumbbell Curls" -> R.string.label_13
        "Dumbbell Rows" -> R.string.label_14
        "Lateral Raises" -> R.string.label_15
        "Cat-Cow Stretch" -> R.string.label_16
        "Downward Dog" -> R.string.label_17
        "Cobra Stretch" -> R.string.label_18
        "Bench Press" -> R.string.label_19
        "Deadlift" -> R.string.label_20
        "Glute Bridges" -> R.string.label_21
        "Dumbbell Goblet Squats" -> R.string.label_22
        "Dumbbell Bench Press" -> R.string.label_23
        "Barbell Squats" -> R.string.label_24
        "Child's Pose" -> R.string.label_25
        "Cat-Cow" -> R.string.label_26
        "Warrior II" -> R.string.label_27
        "Air Squats" -> R.string.label_28
        "Crunches" -> R.string.label_29
        "Chest" -> R.string.label_30
        "Legs" -> R.string.label_31
        "Core" -> R.string.label_32
        "Cardio" -> R.string.label_33
        "Glutes" -> R.string.label_34
        "Arms" -> R.string.label_35
        "Back" -> R.string.label_36
        "Shoulders" -> R.string.label_37
        "Full Body" -> R.string.label_38
        "Abs" -> R.string.label_39
        "Barbell" -> R.string.label_40
        "Dumbbells" -> R.string.label_41
        "Yoga" -> R.string.label_42
        "Place hands shoulder-width apart, lower chest to the floor keeping your core tight and elbows at a 45-degree angle, then push back up fully." -> R.string.guide_1
        "Keep feet shoulder-width apart, lower hips back and down like sitting in a chair, keep chest high, and push through your heels to return." -> R.string.guide_2
        "Place forearms on the ground, align elbows under shoulders, keep body in a perfect straight line from head to heels, and squeeze core tightly." -> R.string.guide_3
        "Start in a top push-up position, rapidly drive your knees toward your chest one at a time in an alternating running motion." -> R.string.guide_4
        "Step forward with one leg, lower your hips until both knees are bent at a 90-degree angle, keep torso upright, then push back to start." -> R.string.guide_5
        "Drop into a squat, jump feet back into a push-up position, do a push-up, jump feet back into a squat, and explosively jump up with hands overhead." -> R.string.guide_6
        "Stand tall, keep elbows close to your torso, curl dumbbells up while contracting biceps, squeeze at the top, and lower with absolute control." -> R.string.guide_7
        "Hinge forward at the hips with flat back, pull dumbbells up toward your waist keeping elbows tucked close to your ribcage, then lower." -> R.string.guide_8
        "Stand tall with dumbbells at sides, raise arms out horizontally with a slight bend in the elbows until parallel to the floor, then lower slowly." -> R.string.guide_9
        "On all fours, inhale to arch your back downward and look up (Cow), then exhale to round your spine up toward the ceiling (Cat)." -> R.string.guide_10
        "From hands and knees, push hips up and back to form an inverted 'V' shape, press heels down toward the floor, and relax your head." -> R.string.guide_11
        "Lie flat face down, place hands under your shoulders, and gently press up to lift your chest off the ground while keeping hips on the floor." -> R.string.guide_12
        "Lie flat on the bench, grip the barbell slightly wider than shoulder-width, lower the bar to mid-chest with control, then press it explosively back up." -> R.string.guide_13
        "Stand with feet mid-bar, hinge at hips to grip the bar, keep spine neutral and chest up, drive upward through your hips and legs to stand upright." -> R.string.guide_14
        else -> null
    }
    if (resource != null) return getString(resource)
    return when {
        value.contains(" • ") -> value.split(" • ").joinToString(" • ") { localizedText(it) }
        value.contains(" - ") -> value.split(" - ").joinToString(" - ") { localizedText(it) }
        else -> value
    }
}
