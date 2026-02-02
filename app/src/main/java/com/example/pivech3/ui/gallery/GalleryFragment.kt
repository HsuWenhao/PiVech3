package com.example.pivech3.ui.gallery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import com.example.pivech3.databinding.FragmentGalleryBinding

class GalleryFragment : Fragment() {

    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!

    private var player: ExoPlayer? = null

    private val rtspUrl = "rtsp://192.168.1.10:8554/wmv"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    @UnstableApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Fullscreen playback UI: PlayerView covers the whole fragment.
        binding.playerView.useController = false

        val context = requireContext()
        val newPlayer = ExoPlayer.Builder(context).build()
        player = newPlayer
        binding.playerView.player = newPlayer

        val mediaItem = MediaItem.fromUri(rtspUrl)

        // Prefer RTSP media source for rtsp:// URIs.
        val mediaSource = RtspMediaSource.Factory().createMediaSource(mediaItem)

        newPlayer.setMediaSource(mediaSource)
        newPlayer.playWhenReady = true
        newPlayer.prepare()
    }

    override fun onStop() {
        super.onStop()
        // Pause when not visible.
        player?.pause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.playerView.player = null
        player?.release()
        player = null
        _binding = null
    }
}