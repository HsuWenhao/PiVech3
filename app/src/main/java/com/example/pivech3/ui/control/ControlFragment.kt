package com.example.pivech3.ui.control

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import com.example.pivech3.databinding.FragmentControlBinding
import com.example.pivech3.prefs.AppPreferences

class ControlFragment : Fragment() {

    private var _binding: FragmentControlBinding? = null
    private val binding get() = _binding!!

    private var player: ExoPlayer? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private var retryCount = 0
    private val maxRetries = 5
    private val retryDelayMs = 1500L

    private val playerListener = object : Player.Listener {
        @UnstableApi
        override fun onPlayerError(error: PlaybackException) {
            // Basic user feedback + auto retry.
            Toast.makeText(
                requireContext(),
                "播放失败，正在重连…(${retryCount + 1}/$maxRetries)",
                Toast.LENGTH_SHORT
            ).show()
            scheduleRetry()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentControlBinding.inflate(inflater, container, false)
        return binding.root
    }

    @UnstableApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.playerView.useController = false

        startPlayback()
    }

    @UnstableApi
    private fun startPlayback() {
        val context = requireContext()

        // Read RTSP URL from Settings.
        val rtspUrl = AppPreferences.getRtspUrl(context).trim().ifEmpty { AppPreferences.DEFAULT_RTSP_URL }

        val newPlayer = ExoPlayer.Builder(context).build()
        player = newPlayer
        binding.playerView.player = newPlayer

        newPlayer.addListener(playerListener)

        val mediaItem = MediaItem.fromUri(rtspUrl)
        val mediaSource = RtspMediaSource.Factory().createMediaSource(mediaItem)

        retryCount = 0
        newPlayer.setMediaSource(mediaSource)
        newPlayer.playWhenReady = true
        newPlayer.prepare()
    }

    @UnstableApi
    private fun scheduleRetry() {
        if (!isAdded) return
        if (retryCount >= maxRetries) {
            Toast.makeText(requireContext(), "重连失败，请检查 RTSP 地址/网络", Toast.LENGTH_LONG).show()
            return
        }
        retryCount += 1
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.postDelayed({
            if (!isAdded) return@postDelayed
            restartPlayback()
        }, retryDelayMs)
    }

    @UnstableApi
    private fun restartPlayback() {
        // Recreate player to ensure RTSP reconnect is clean.
        binding.playerView.player = null
        player?.removeListener(playerListener)
        player?.release()
        player = null

        startPlayback()
    }

    override fun onStop() {
        super.onStop()
        player?.pause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mainHandler.removeCallbacksAndMessages(null)
        binding.playerView.player = null
        player?.removeListener(playerListener)
        player?.release()
        player = null
        _binding = null
    }
}