package com.ncorti.kotlin.template.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class PlayerActivity : AppCompatActivity() {

    private lateinit var playerView: PlayerView
    private var player: ExoPlayer? = null
    private var videoUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        videoUrl = intent.getStringExtra("VIDEO_URL")
        playerView = findViewById(R.id.playerView)
    }

    override fun onStart() {
        super.onStart()
        if (player == null) {
            player = ExoPlayer.Builder(this).build()
            playerView.player = player
            
            videoUrl?.let {
                val mediaItem = MediaItem.fromUri(it)
                player?.setMediaItem(mediaItem)
                player?.prepare()
                player?.play()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        player?.release()
        player = null
    }
}