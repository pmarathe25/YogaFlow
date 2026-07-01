package com.example.model

import android.content.Context
import org.json.JSONObject

object FlowLoader {
    private var cachedFlows: List<YogaFlow>? = null
    private var cachedPoses: Map<Int, YogaPose>? = null

    fun loadPoses(context: Context): Map<Int, YogaPose> {
        cachedPoses?.let { return it }

        val json = context.assets.open("poses.json").bufferedReader().use { it.readText() }
        val root = JSONObject(json)
        val posesArray = root.getJSONArray("poses")
        val posesMap = mutableMapOf<Int, YogaPose>()

        for (i in 0 until posesArray.length()) {
            val poseObj = posesArray.getJSONObject(i)
            val instructionsArray = poseObj.getJSONArray("instructions")
            val instructions = mutableListOf<String>()
            for (k in 0 until instructionsArray.length()) {
                instructions.add(instructionsArray.getString(k))
            }
            val id = poseObj.getInt("id")
            posesMap[id] = YogaPose(
                id = id,
                sanskritName = poseObj.getString("sanskritName"),
                englishName = poseObj.getString("englishName"),
                description = poseObj.optString("description", ""),
                benefits = poseObj.optString("benefits", ""),
                instructions = instructions,
                voicePrompt = poseObj.optString("voicePrompt", ""),
                sanskritInstructions = poseObj.optString("sanskritInstructions", ""),
                holdDurationSec = poseObj.optInt("holdDurationSec", 30),
            )
        }

        cachedPoses = posesMap
        return posesMap
    }

    fun loadFlows(context: Context): List<YogaFlow> {
        cachedFlows?.let { return it }

        val posesMap = loadPoses(context)
        val json = context.assets.open("flows.json").bufferedReader().use { it.readText() }
        val root = JSONObject(json)
        val flowsArray = root.getJSONArray("flows")
        val flows = mutableListOf<YogaFlow>()

        for (i in 0 until flowsArray.length()) {
            val flowObj = flowsArray.getJSONObject(i)
            val posesIdsArray = flowObj.getJSONArray("poses")
            val poses = mutableListOf<YogaPose>()

            for (j in 0 until posesIdsArray.length()) {
                val poseId = posesIdsArray.getInt(j)
                posesMap[poseId]?.let { poses.add(it) }
            }

            flows.add(
                YogaFlow(
                    id = flowObj.getString("id"),
                    name = flowObj.getString("name"),
                    description = flowObj.optString("description", ""),
                    difficulty = flowObj.optString("difficulty", ""),
                    totalDurationMinutes = flowObj.getInt("totalDurationMinutes"),
                    poses = poses
                )
            )
        }

        cachedFlows = flows
        return flows
    }

    fun getPoseById(context: Context, id: Int): YogaPose? {
        return loadPoses(context)[id]
    }

    fun getFlowById(context: Context, id: String): YogaFlow? {
        return loadFlows(context).find { it.id == id }
    }
}
