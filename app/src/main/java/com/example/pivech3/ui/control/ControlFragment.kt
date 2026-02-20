package com.example.pivech3.ui.control

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.pivech3.databinding.FragmentControlBinding
import com.example.pivech3.prefs.AppPreferences
import kotlin.math.hypot
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer

class ControlFragment : Fragment() {

    private var _binding: FragmentControlBinding? = null
    private val binding get() = _binding!!

    private val mainHandler = Handler(Looper.getMainLooper())

    // RTSP URL
    private val rtspUrl: String by lazy {
        AppPreferences.migrateWebRtcToRtspIfNeeded(requireContext())
        AppPreferences.getRtspUrl(requireContext())
    }

    // Low-latency tuning. UDP is lower-latency than TCP but may be less reliable.
    private fun useRtspTcp(): Boolean = AppPreferences.getRtspUseTcp(requireContext())
    private fun networkCachingMs(): Int = AppPreferences.getRtspCacheMs(requireContext())
    private fun liveCachingMs(): Int = AppPreferences.getRtspCacheMs(requireContext())
    private fun rtspCachingMs(): Int = AppPreferences.getRtspCacheMs(requireContext())

    private var libVlc: LibVLC? = null
    private var vlcPlayer: MediaPlayer? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentControlBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(binding.controlRoot) { _, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.controlRoot.setPadding(sys.left, sys.top, sys.right, sys.bottom)
            insets
        }

        // Hide Media3 view (we use VLC now)
        binding.playerView.visibility = View.GONE
        binding.vlcVideo.visibility = View.VISIBLE

        binding.leftJoystick.setOnMoveListener { x, y ->
            val mag = hypot(x.toDouble(), y.toDouble()).toFloat()
            Log.d("ControlFragment", "leftJoystick x=$x y=$y |v|=$mag")
        }

        binding.rightJoystick.setOnMoveListener { x, y ->
            val mag = hypot(x.toDouble(), y.toDouble()).toFloat()
            Log.d("ControlFragment", "rightJoystick x=$x y=$y |v|=$mag")
        }

        binding.exitButton.setOnClickListener {
            runCatching {
                findNavController().popBackStack()
            }.getOrElse {
                requireActivity().finish()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        startVlc()
    }

    override fun onStop() {
        stopVlc()
        super.onStop()
    }

    override fun onDestroyView() {
        stopVlc()
        super.onDestroyView()
        _binding = null
    }

    private fun startVlc() {
        if (!isAdded) return
        if (vlcPlayer != null) return

        Log.d("ControlFragment", "Starting LibVLC RTSP: $rtspUrl")

        val context = requireContext().applicationContext

        val cacheNetwork = networkCachingMs()
        val cacheLive = liveCachingMs()
        val cacheRtsp = rtspCachingMs()
        val tcp = useRtspTcp()

        // LibVLC options
        val options = arrayListOf(
            "--network-caching=$cacheNetwork",
            "--live-caching=$cacheLive",
            "--rtsp-caching=$cacheRtsp",
            "--file-caching=0",
            "--udp-caching=0",
            "--clock-jitter=0",
            "--clock-synchro=0",
            "--drop-late-frames",
            "--skip-frames",
            "--avcodec-fast",
            "--no-audio"
        ).apply {
            if (tcp) {
                add("--rtsp-tcp")
            }
        }

        val lib = LibVLC(context, options)
        val mp = MediaPlayer(lib)

        mp.setEventListener { event ->
            when (event.type) {
                MediaPlayer.Event.Playing -> Log.d("ControlFragment", "VLC Playing")
                MediaPlayer.Event.Buffering -> Log.d("ControlFragment", "VLC Buffering ${event.buffering}")
                MediaPlayer.Event.EndReached -> Log.d("ControlFragment", "VLC EndReached")
                MediaPlayer.Event.EncounteredError -> Log.e("ControlFragment", "VLC EncounteredError")
            }
        }

        mp.attachViews(binding.vlcVideo, null, false, false)

        val media = Media(lib, Uri.parse(rtspUrl)).apply {
            // Force TCP at media level when requested; otherwise allow UDP for lower latency.
            if (tcp) {
                addOption(":rtsp-tcp")
            }
            addOption(":network-caching=$cacheNetwork")
            addOption(":live-caching=$cacheLive")
            addOption(":rtsp-caching=$cacheRtsp")
            addOption(":file-caching=0")
            addOption(":udp-caching=0")
            addOption(":clock-jitter=0")
            addOption(":clock-synchro=0")
            addOption(":drop-late-frames")
            addOption(":skip-frames")
            addOption(":avcodec-fast")
            addOption(":no-audio")
        }

        mp.media = media
        media.release()

        mp.play()

        libVlc = lib
        vlcPlayer = mp
    }

    private fun stopVlc() {
        val mp = vlcPlayer
        vlcPlayer = null

        runCatching { mp?.stop() }
        runCatching { mp?.detachViews() }
        runCatching { mp?.release() }

        libVlc?.release()
        libVlc = null

        mainHandler.removeCallbacksAndMessages(null)
    }
}