package com.afilishop.app.ui.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import io.agora.base.internal.SurfaceViewRenderer
import io.agora.base.internal.video.EglBaseFactory
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.video.VideoCanvas

@Composable
fun AgoraLiveView(
    appId: String,
    token: String?,
    channel: String,
    uid: Int,
    isBroadcaster: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val eglBase = remember { EglBaseFactory.create() }
    val localRenderer = remember { SurfaceViewRenderer(context) }
    val remoteRenderer = remember { SurfaceViewRenderer(context) }
    val engineRef = remember { arrayOfNulls<RtcEngine>(1) }
    val eventHandler = remember {
        object : IRtcEngineEventHandler() {
            override fun onUserJoined(remoteUid: Int, elapsed: Int) {
                remoteRenderer.post { engineRef[0]?.setupRemoteVideo(VideoCanvas(remoteRenderer, VideoCanvas.RENDER_MODE_HIDDEN, remoteUid)) }
            }
        }
    }
    val engine = remember(appId) { RtcEngine.create(context, appId, eventHandler).also { engineRef[0] = it } }
    val container = remember { FrameLayout(context) }
    DisposableEffect(appId, token, channel, uid, isBroadcaster) {
        localRenderer.init(eglBase.eglBaseContext, null)
        remoteRenderer.init(eglBase.eglBaseContext, null)
        container.removeAllViews()
        container.addView(remoteRenderer, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        if (isBroadcaster) {
            val localParams = FrameLayout.LayoutParams(320, 420).apply { leftMargin = 24; topMargin = 24 }
            container.addView(localRenderer, localParams)
        }
        engine.enableVideo()
        if (isBroadcaster) engine.setupLocalVideo(VideoCanvas(localRenderer, VideoCanvas.RENDER_MODE_HIDDEN, uid))
        val options = ChannelMediaOptions().apply {
            channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING
            clientRoleType = if (isBroadcaster) Constants.CLIENT_ROLE_BROADCASTER else Constants.CLIENT_ROLE_AUDIENCE
            publishCameraTrack = isBroadcaster
            publishMicrophoneTrack = isBroadcaster
            autoSubscribeVideo = true
            autoSubscribeAudio = true
        }
        engine.joinChannel(token, channel, uid, options)
        onDispose {
            runCatching { engine.leaveChannel() }
            localRenderer.release()
            remoteRenderer.release()
            eglBase.release()
            RtcEngine.destroy()
            engineRef[0] = null
        }
    }
    AndroidView(factory = { container }, modifier = modifier)
}
