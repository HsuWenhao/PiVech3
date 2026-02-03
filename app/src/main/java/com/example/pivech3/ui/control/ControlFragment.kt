package com.example.pivech3.ui.control

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.pivech3.databinding.FragmentControlBinding
import com.example.pivech3.prefs.AppPreferences
import kotlin.math.hypot
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnection.IceGatheringState
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpTransceiver
import org.webrtc.RtpTransceiver.RtpTransceiverDirection
import org.webrtc.RtpTransceiver.RtpTransceiverInit
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.VideoTrack
import androidx.core.net.toUri

class ControlFragment : Fragment() {

    private var _binding: FragmentControlBinding? = null
    private val binding get() = _binding!!

    private val mainHandler = Handler(Looper.getMainLooper())
    private val http = OkHttpClient()

    private var eglBase: EglBase? = null
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null

    private var remoteVideoTrack: VideoTrack? = null

    // Public stream URL (browser player). Read from Settings.
    private val streamUrl: String by lazy {
        val ctx = requireContext().applicationContext
        AppPreferences.migrateRtspToWebRtcIfNeeded(ctx)
        AppPreferences.getWebRtcUrl(ctx).trim().ifEmpty { AppPreferences.DEFAULT_WEBRTC_URL }
    }

    private var sessionUrl: String? = null

    private val pendingIceCandidates = mutableListOf<IceCandidate>()

    private var isStarted = false

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

        // Keep overlays above system bars (gesture nav, cutouts)
        ViewCompat.setOnApplyWindowInsetsListener(binding.controlRoot) { _, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Keep video full-bleed. Only pad overlay margins via root padding.
            binding.controlRoot.setPadding(sys.left, sys.top, sys.right, sys.bottom)
            insets
        }

        // Init renderer early (view lifecycle) so we can attach the remote track as soon as it arrives.
        // NOTE: stream-webrtc-android exposes the same org.webrtc APIs.
        val egl = eglBase ?: EglBase.create().also { eglBase = it }

        binding.webrtcView.keepScreenOn = true
        binding.webrtcView.setEnableHardwareScaler(true)
        binding.webrtcView.setMirror(false)
        binding.webrtcView.init(egl.eglBaseContext, null)
        binding.webrtcView.setScalingType(org.webrtc.RendererCommon.ScalingType.SCALE_ASPECT_FILL)

        // Left joystick (independent)
        binding.leftJoystick.setOnMoveListener { x, y ->
            // TODO: hook into your control channel. For now we just log magnitude.
            val mag = hypot(x.toDouble(), y.toDouble()).toFloat()
            Log.d("ControlFragment", "leftJoystick x=$x y=$y |v|=$mag")
        }

        // Right joystick (independent)
        binding.rightJoystick.setOnMoveListener { x, y ->
            val mag = hypot(x.toDouble(), y.toDouble()).toFloat()
            Log.d("ControlFragment", "rightJoystick x=$x y=$y |v|=$mag")
        }

        binding.exitButton.setOnClickListener {
            // Prefer navigation back; fallback to finishing the activity.
            runCatching {
                findNavController().popBackStack()
            }.getOrElse {
                requireActivity().finish()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        isStarted = true
        startWebRtc()
    }

    override fun onStop() {
        super.onStop()
        isStarted = false
        mainHandler.removeCallbacksAndMessages(null)
        stopWebRtc(releaseRenderer = false)
    }

    override fun onDestroyView() {
        mainHandler.removeCallbacksAndMessages(null)
        stopWebRtc(releaseRenderer = true)
        super.onDestroyView()
        _binding = null
    }

    private fun startWebRtc() {
        initWebRtcOnce()
        connect()
    }

    private fun initWebRtcOnce() {
        if (peerConnectionFactory != null) return

        val ctx = requireContext().applicationContext
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(ctx)
                .setEnableInternalTracer(false)
                .createInitializationOptions()
        )

        val egl = eglBase ?: EglBase.create().also { eglBase = it }
        val eglContext = egl.eglBaseContext

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglContext))
            .createPeerConnectionFactory()

        // Renderer init moved to onViewCreated() to match view lifecycle.
    }

    private fun parseBaseAndPath(): Pair<String, String> {
        val uri = streamUrl.toUri()
        val base = "${uri.scheme}://${uri.host}${if (uri.port != -1) ":${uri.port}" else ""}"
        val path = uri.pathSegments.firstOrNull().orEmpty()
        return base to path
    }

    private fun connect() {
        if (!isAdded || !isStarted) return

        val factory = peerConnectionFactory ?: return

        val rtcConfig = PeerConnection.RTCConfiguration(
            listOf(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer())
        ).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }

        peerConnection = factory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                synchronized(pendingIceCandidates) {
                    pendingIceCandidates.add(candidate)
                }
            }

            override fun onIceGatheringChange(newState: IceGatheringState?) {
                if (newState == IceGatheringState.COMPLETE) {
                    val url = sessionUrl ?: return
                    val batch = synchronized(pendingIceCandidates) {
                        val copy = pendingIceCandidates.toList()
                        pendingIceCandidates.clear()
                        copy
                    }
                    if (batch.isNotEmpty()) {
                        postIceCandidatesBatch(url, batch)
                    }
                }
            }

            override fun onAddTrack(
                receiver: org.webrtc.RtpReceiver?,
                mediaStreams: Array<out org.webrtc.MediaStream>?
            ) {
                val track = receiver?.track()
                if (track is VideoTrack) {
                    // WebRTC callbacks are not guaranteed to run on the main thread.
                    mainHandler.post {
                        if (!isAdded || !isStarted) return@post
                        val renderer = _binding?.webrtcView ?: return@post

                        // Replace existing track sink if renegotiation happens.
                        remoteVideoTrack?.removeSink(renderer)
                        remoteVideoTrack = track
                        track.addSink(renderer)
                    }
                }
            }

            override fun onTrack(transceiver: RtpTransceiver?) {}
            override fun onSignalingChange(newState: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {}
            override fun onIceConnectionReceivingChange(p0: Boolean) {}
            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
            override fun onRemoveStream(p0: org.webrtc.MediaStream?) {}
            override fun onAddStream(p0: org.webrtc.MediaStream?) {}
            override fun onDataChannel(p0: org.webrtc.DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {}
        })

        val pc = peerConnection ?: return

        pc.addTransceiver(
            org.webrtc.MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO,
            RtpTransceiverInit(RtpTransceiverDirection.RECV_ONLY)
        )
        pc.addTransceiver(
            org.webrtc.MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO,
            RtpTransceiverInit(RtpTransceiverDirection.RECV_ONLY)
        )

        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }

        pc.createOffer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription) {
                pc.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() {
                        postOffer(desc)
                    }

                    override fun onSetFailure(error: String?) {
                        Log.w("ControlFragment", "setLocalDescription failed: $error")
                    }

                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onCreateFailure(p0: String?) {}
                }, desc)
            }

            override fun onCreateFailure(error: String?) {
                Log.w("ControlFragment", "createOffer failed: $error")
            }

            override fun onSetSuccess() {}
            override fun onSetFailure(error: String?) {}
        }, constraints)
    }

    private fun postOffer(offer: SessionDescription) {
        val (base, path) = parseBaseAndPath()
        val url = "$base/$path/whep"

        val req = Request.Builder()
            .url(url)
            .post(offer.description.toRequestBody("application/sdp".toMediaType()))
            .build()

        http.newCall(req).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                Log.w("ControlFragment", "offer POST failed: ${e.message}")
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    if (!it.isSuccessful) {
                        Log.w("ControlFragment", "offer POST http ${it.code}")
                        return
                    }

                    val answerSdp = it.body?.string().orEmpty()
                    val location = it.header("Location")

                    sessionUrl = if (!location.isNullOrBlank()) {
                        if (location.startsWith("http")) location else "$base$location"
                    } else {
                        null
                    }

                    setRemoteAnswer(answerSdp)
                }
            }
        })
    }

    private fun setRemoteAnswer(answerSdp: String) {
        val pc = peerConnection ?: return
        val answer = SessionDescription(SessionDescription.Type.ANSWER, answerSdp)

        pc.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                mainHandler.post {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "WebRTC 已连接", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onSetFailure(error: String?) {
                Log.w("ControlFragment", "setRemoteDescription failed: $error")
            }

            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onCreateFailure(p0: String?) {}
        }, answer)
    }

    private fun postIceCandidatesBatch(sessionUrl: String, candidates: List<IceCandidate>) {
        val frag = buildString {
            for (c in candidates) {
                append("a=candidate:")
                append(c.sdp.removePrefix("candidate:"))
                append("\r\n")
            }
        }

        val req = Request.Builder()
            .url(sessionUrl)
            .patch(frag.toRequestBody("application/trickle-ice-sdpfrag".toMediaType()))
            .build()

        http.newCall(req).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                Log.d("ControlFragment", "ICE batch PATCH failed: ${e.message}")
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.close()
            }
        })
    }

    private fun stopWebRtc(releaseRenderer: Boolean) {
        sessionUrl = null
        synchronized(pendingIceCandidates) {
            pendingIceCandidates.clear()
        }

        val renderer = _binding?.webrtcView

        // Detach sinks first to avoid frames being delivered into a released surface.
        try {
            remoteVideoTrack?.let { track ->
                if (renderer != null) track.removeSink(renderer)
            }
        } catch (_: Throwable) {
        }
        remoteVideoTrack = null

        try {
            renderer?.clearImage()
        } catch (_: Throwable) {
        }

        peerConnection?.close()
        peerConnection?.dispose()
        peerConnection = null

        if (releaseRenderer) {
            try {
                renderer?.release()
            } catch (_: Throwable) {
            }

            eglBase?.release()
            eglBase = null
        }

        peerConnectionFactory?.dispose()
        peerConnectionFactory = null
    }
}