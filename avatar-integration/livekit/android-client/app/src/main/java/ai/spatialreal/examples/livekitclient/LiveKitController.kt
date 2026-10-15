package ai.spatialreal.examples.livekitclient

import ai.spatialreal.android.facade.SessionState
import ai.spatialreal.android.facade.SpatialReal
import ai.spatialreal.android.facade.SpatialRealError
import ai.spatialreal.android.facade.chat.MicPolicy
import ai.spatialreal.android.livekit.LiveKitAvatarSession
import ai.spatialreal.android.livekit.LiveKitAvatarSessionOptions
import ai.spatialreal.android.livekit.LiveKitConnection
import ai.spatialreal.android.livekit.createSession
import android.content.Context
import android.widget.FrameLayout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

/**
 * Owns the LiveKit avatar session. The app joins your LiveKit room, sends the user's microphone
 * to your agent, and renders the avatar that SpatialReal publishes into the room. The app sends
 * no speech of its own to SpatialReal — your agent does.
 */
class LiveKitController(private val context: Context) {
    /** The SDK draws into this view. The screen shows it through AndroidView. */
    val container = FrameLayout(context)

    // Compose state can be written from any thread; the SDK's callbacks run off the main thread
    var state by mutableStateOf<SessionState?>(null)
        private set
    var muted by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set

    private var session: LiveKitAvatarSession? = null

    /**
     * Fetch the two tokens from your server, then create the session: the avatar is downloaded
     * and shown, standing still. Nothing joins the room yet.
     */
    suspend fun create() {
        if (session != null) return
        try {
            val tokens = fetchTokens()
            val sr = SpatialReal(context, appId = Config.APP_ID)
            val session = sr.createSession(
                LiveKitAvatarSessionOptions(
                    avatarId = Config.AVATAR_ID,
                    credential = tokens.sessionToken,                            // downloads the avatar
                    container = container,                                       // must be on screen and have a size
                    livekit = LiveKitConnection(tokens.url, tokens.roomToken),
                    mic = MicPolicy.REQUIRED,                                    // start() turns the microphone on
                )
            )
            session.onState { change -> state = change.current }
            session.onError { error -> message = describe(error) }
            state = session.state
            this.session = session
        } catch (error: SpatialRealError) {
            message = describe(error)
        } catch (error: IOException) {
            message = "Token server: ${error.message}"
        }
    }

    /**
     * start() joins the room and turns on the microphone (RECORD_AUDIO must be granted first).
     * Your agent joins the new room and greets you; the avatar speaks its replies.
     */
    suspend fun start() {
        message = null
        try {
            session?.start()
        } catch (error: SpatialRealError) {
            message = describe(error)
        }
    }

    /** The user refused the microphone: the agent can't hear them. */
    fun micDenied() {
        message = "Microphone permission denied. Allow it in the app's settings, then tap Start again."
    }

    /** Stop sending the microphone without leaving the room. */
    suspend fun toggleMute() {
        val session = session ?: return
        try {
            session.setMicrophoneEnabled(muted)
            muted = !muted
        } catch (error: Exception) {
            message = error.message
        }
    }

    /** Leave the room. The avatar stays on screen; start() joins again. */
    suspend fun leave() {
        try {
            session?.end()
        } catch (error: SpatialRealError) {
            message = describe(error)
        }
        muted = false
    }

    /** The screen goes away: leave the room and release everything. */
    suspend fun release() {
        session?.dispose()
        session = null
    }

    // ---- Your token server ----

    /** What `POST /api/tokens` returns: see ../web-client/server.js */
    private class Tokens(val url: String, val roomToken: String, val sessionToken: String)

    private suspend fun fetchTokens(): Tokens = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("${Config.TOKEN_SERVER_URL}/api/tokens").post("".toRequestBody()).build()
        OkHttpClient().newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("replied ${response.code}. Is `npm run server` running in ../web-client, and is the URL reachable from this device?")
            }
            val json = JSONObject(response.body!!.string())
            Tokens(json.getString("url"), json.getString("roomToken"), json.getString("sessionToken"))
        }
    }

    private fun describe(error: SpatialRealError) = "[${error.code}] ${error.message}"
}
