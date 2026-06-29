package com.example.model

import android.content.Context
import org.json.JSONObject

object FlowLoader {
    private var cachedFlows: List<YogaFlow>? = null
    private var cachedSanskritPrompts: Map<Int, String>? = null

    fun loadFlows(context: Context): List<YogaFlow> {
        cachedFlows?.let { return it }

        val json = context.assets.open("flows.json").bufferedReader().use { it.readText() }
        val root = JSONObject(json)
        val flowsArray = root.getJSONArray("flows")
        val flows = mutableListOf<YogaFlow>()

        for (i in 0 until flowsArray.length()) {
            val flowObj = flowsArray.getJSONObject(i)
            val posesArray = flowObj.getJSONArray("poses")
            val poses = mutableListOf<YogaPose>()

            for (j in 0 until posesArray.length()) {
                val poseObj = posesArray.getJSONObject(j)
                val instructionsArray = poseObj.getJSONArray("instructions")
                val instructions = mutableListOf<String>()
                for (k in 0 until instructionsArray.length()) {
                    instructions.add(instructionsArray.getString(k))
                }
                poses.add(
                    YogaPose(
                        id = poseObj.getInt("id"),
                        sanskritName = poseObj.getString("sanskritName"),
                        englishName = poseObj.getString("englishName"),
                        description = poseObj.optString("description", ""),
                        benefits = poseObj.optString("benefits", ""),
                        instructions = instructions,
                        voicePrompt = poseObj.optString("voicePrompt", ""),
                        holdDurationSec = poseObj.optInt("holdDurationSec", 30)
                    )
                )
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

    fun loadSanskritPrompts(context: Context): Map<Int, String> {
        cachedSanskritPrompts?.let { return it }
        
        val json = context.assets.open("sanskrit_prompts.json").bufferedReader().use { it.readText() }
        val root = JSONObject(json)
        val prompts = mutableMapOf<Int, String>()
        
        val keys = root.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            prompts[key.toInt()] = root.getString(key)
        }
        
        cachedSanskritPrompts = prompts
        return prompts
    }

    fun getSanskritPrompt(context: Context, poseId: Int): String? {
        return loadSanskritPrompts(context)[poseId]
    }

    fun getFlowById(context: Context, id: String): YogaFlow? {
        return loadFlows(context).find { it.id == id }
    }
}
