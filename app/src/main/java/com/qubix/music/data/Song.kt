package com.qubix.music.data

data class Song(
    val videoId: String,
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val videoUrl: String,
    val durationSec: Long,
)
