package com.example.model

import com.example.R

data class AudioTrack(
    val name: String,
    val resId: Int,
    val isMusic: Boolean
) {
    companion object {
        fun loadTracks(): List<AudioTrack> {
            val fields = R.raw::class.java.fields
            return fields.filter { it.name.startsWith("track_") }.map { field ->
                val resId = field.getInt(null)
                val nameParts = field.name.removePrefix("track_").split("_")
                val name = nameParts.filter { it.toIntOrNull() == null }
                    .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
                val isMusic = !name.contains("Breath", ignoreCase = true) && !name.contains("Ocean", ignoreCase = true)
                AudioTrack(name, resId, isMusic)
            }.sortedBy { it.name }
        }
    }
}
