package ai.spatialreal.examples.tenclient

import ai.spatialreal.android.agora.AgoraAvatarSession
import ai.spatialreal.android.agora.AgoraAvatarSessionOptions
import ai.spatialreal.android.agora.AgoraConnection
import ai.spatialreal.android.agora.createSession
import ai.spatialreal.android.facade.SessionState
import ai.spatialreal.android.facade.SpatialReal
import ai.spatialreal.android.facade.SpatialRealError
import ai.spatialreal.android.facade.chat.MicPolicy
import android.content.Context
import android.widget.FrameLayout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.agora.rtc2.IRtcEngineEventHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

/**
 * Owns the Agora avatar session. The app joins your Agora channel, sends the user's microphone
 * to your agent, and renders the avatar that SpatialReal publishes into the channel. The app
 * sends no speech of its own to SpatialReal — your agent does.
 */
class AgoraController(private val context: Context) {
    /** The SDK draws into this view. The screen shows it through AndroidView. */
    val container = FrameLayout(context)

    // Compose state can be written from any thread; the SDK's callbacks run off the main thread
    var state by mutableStateOf<SessionState?>(null)
        private set
    var muted by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var channel by mutableStateOf<String?>(null)
        private set

    var busy by mutableStateOf(false)
        private set

    private var session: AgoraAvatarSession? = null
    private var removeStateListener: (() -> Unit)? = null
    private var removeErrorListener: (() -> Unit)? = null
    private val scope = MainScope()
    private val http = OkHttpClient()

    /**
     * Fetch the channel details and the two tokens from your server, then create the session:
     * the avatar is downloaded and shown, standing still. Nothing joins the channel yet.
     */
    suspend fun create() {
        if (session != null) return
        try {
            val cfg = fetchSession()
            channel = cfg.channel
            val sr = SpatialReal(context, appId = cfg.spatialrealAppId)
            val session = sr.createSession(
                AgoraAvatarSessionOptions(
                    avatarId = cfg.avatarId,
                    credential = cfg.credential,                                 // downloads the avatar
                    container = container,                                       // must be on screen and have a size
                    agora = AgoraConnection.Join(
                        appId = cfg.appId,
                        channel = cfg.channel,
                        token = cfg.token,
                        avatarUid = cfg.avatarUid,                               // the uid your agent publishes the avatar with
                        uid = cfg.uid,                                           // this app's uid
                    ),
                    mic = MicPolicy.REQUIRED,                                    // start() publishes the microphone
                )
            )
            removeStateListener = session.onState { change ->
                state = change.current
                // Every start() publishes the microphone again
                if (change.current != SessionState.LIVE) muted = false
            }
            removeErrorListener = session.onError { error -> message = describe(error) }
            // The SDK never renews the Agora token: see tokenRenewal below
            session.engine.addHandler(tokenRenewal)
            state = session.state
            this.session = session
        } catch (error: SpatialRealError) {
            message = describe(error)
        } catch (error: IOException) {
            message = "Token server: ${error.message}"
        }
    }

    /**
     * start() joins the channel and publishes the microphone (RECORD_AUDIO must be granted
     * first). The avatar speaks whenever your agent, in the same channel, replies.
     */
    suspend fun start() {
        if (busy) return
        busy = true
        message = null
        try {
            session?.start()
        } catch (error: SpatialRealError) {
            message = describe(error)
        } finally {
            busy = false
        }
    }

    /** The user refused the microphone: the agent can't hear them. */
    fun micDenied() {
        message = "Microphone permission denied. Allow it in the app's settings, then tap Start again."
    }

    /** Stop sending the microphone without leaving the channel. */
    fun toggleMute() {
        val session = session ?: return
        session.setMicrophoneEnabled(muted)
        muted = !muted
    }

    /** Leave the channel. The avatar stays on screen; start() joins again. */
    suspend fun end() {
        if (busy) return
        busy = true
        try {
            session?.end()
        } catch (error: SpatialRealError) {
            message = describe(error)
        } finally {
            busy = false
        }
    }

    /** The screen goes away: leave the channel and release everything. */
    suspend fun release() {
        scope.cancel()
        removeStateListener?.invoke()
        removeErrorListener?.invoke()
        removeStateListener = null
        removeErrorListener = null
        val previous = session
        session = null
        previous?.engine?.removeHandler(tokenRenewal)
        previous?.dispose()   // also destroys the Agora engine the SDK created
    }

    /** Agora asks for a fresh RTC token shortly before the current one expires. */
    private val tokenRenewal = object : IRtcEngineEventHandler() {
        override fun onTokenPrivilegeWillExpire(token: String?) {
            scope.launch {
                try {
                    val fresh = fetchRtcToken()
                    session?.engine?.renewToken(fresh)
                } catch (error: IOException) {
                    message = "Token renewal failed: ${error.message}"
                }
            }
        }
    }

    // ---- Your token server ----

    /** What `POST /api/session` returns: see ../web-client/server.js */
    private class SessionConfig(
        val appId: String,              // Agora App ID
        val channel: String,
        val token: String,              // Agora RTC token for `uid`
        val avatarUid: Int,
        val uid: Int,
        val credential: String,         // SpatialReal session token
        val spatialrealAppId: String,
        val avatarId: String,
    )

    private suspend fun fetchSession(): SessionConfig {
        val json = post("/api/session")
        try {
            return SessionConfig(
                appId = json.getString("appId"),
                channel = json.getString("channel"),
                token = json.getString("token"),
                // Agora uids are unsigned 32-bit: one above 2147483647 is a negative Int here
                avatarUid = json.getLong("avatarUid").toInt(),
                uid = json.getLong("uid").toInt(),
                credential = json.getString("credential"),
                spatialrealAppId = json.getString("spatialrealAppId"),
                avatarId = json.getString("avatarId"),
            )
        } catch (error: JSONException) {
            throw IOException("${error.message}. Is ../web-client/.env filled in?")
        }
    }

    /** `POST /api/agora-token`: a fresh RTC token for the same uid and channel */
    private suspend fun fetchRtcToken(): String = try {
        post("/api/agora-token").getString("token")
    } catch (error: JSONException) {
        throw IOException(error.message)
    }

    private suspend fun post(path: String): JSONObject = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("${Config.TOKEN_SERVER_URL}$path").post("".toRequestBody()).build()
        http.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                // server.js answers 502 with { error } when SpatialReal refuses the session token request
                val reason = runCatching { JSONObject(body).getString("error") }.getOrNull()
                throw IOException(reason ?: "replied ${response.code}. Is `npm run server` running in ../web-client, and is the URL reachable from this device?")
            }
            try {
                JSONObject(body)
            } catch (error: JSONException) {
                throw IOException("unexpected reply from $path")
            }
        }
    }

    private fun describe(error: SpatialRealError) = "[${error.code}] ${error.message}"
}
