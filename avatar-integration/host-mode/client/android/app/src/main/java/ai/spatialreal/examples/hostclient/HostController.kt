package ai.spatialreal.examples.hostclient

import ai.spatialreal.android.facade.HostAvatarSession
import ai.spatialreal.android.facade.HostAvatarSessionOptions
import ai.spatialreal.android.facade.SessionAudioFormat
import ai.spatialreal.android.facade.SessionState
import ai.spatialreal.android.facade.SpatialReal
import ai.spatialreal.android.facade.SpatialRealError
import android.content.Context
import android.util.Base64
import android.widget.FrameLayout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONException
import org.json.JSONObject

/**
 * Owns the Host mode session and the connection to your server.
 * Follows "Host mode client" (Android tab) in the docs.
 */
class HostController(private val context: Context) {
    /** The SDK draws into this view. The screen shows it through AndroidView. */
    val container = FrameLayout(context)

    // Compose state can be written from any thread; the SDK's and OkHttp's callbacks run off the main thread
    var state by mutableStateOf<SessionState?>(null)
        private set
    var serverConnected by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val inbox = Channel<String>(Channel.UNLIMITED)
    private var host: HostAvatarSession? = null
    private var ws: WebSocket? = null

    /**
     * Create a Host mode session: the token is used only to download the avatar.
     * The SDK opens no connection of its own; everything arrives through your server.
     */
    suspend fun create() {
        if (host != null) return
        try {
            val sr = SpatialReal(context, appId = Config.APP_ID)
            val session = sr.createSession(
                HostAvatarSessionOptions(
                    avatarId = Config.AVATAR_ID,
                    credential = Config.SESSION_TOKEN,
                    container = container,
                    audioFormat = SessionAudioFormat.pcm16(16000), // the rate of the audio your server forwards
                )
            )
            session.onState { change -> state = change.current }
            session.onError { error -> message = describe(error) }
            state = session.state
            host = session
            connect(session)
        } catch (error: SpatialRealError) {
            message = describe(error)
        }
    }

    /** start() opens no connection in Host mode; it gets playback ready. */
    suspend fun start() {
        message = null
        try {
            host?.start()
        } catch (error: SpatialRealError) {
            message = describe(error)
        }
    }

    /** Ask the server for speech. */
    fun speak(voice: String) {
        ws?.send(JSONObject().put("type", "speak").put("voice", voice).toString())
    }

    /** Stop playback here and tell the server, so it stops forwarding the old utterance. */
    fun stop() {
        host?.interrupt()
        ws?.send("""{"type":"interrupt"}""")
    }

    /** The screen goes away: close the connection and release everything. */
    suspend fun release() {
        ws?.close(1000, null)
        scope.cancel()
        host?.dispose()
        host = null
    }

    private fun connect(session: HostAvatarSession) {
        val request = Request.Builder().url(Config.HOST_SERVER_URL).build()
        ws = OkHttpClient().newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                serverConnected = true
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                inbox.trySend(text)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                serverConnected = false
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                serverConnected = false
                message = "Can't reach the server: ${t.message}"
            }
        })

        // Hand over every message, one at a time and in the order it arrived:
        // audio to yieldAudioData, motion to yieldFramesData
        scope.launch {
            for (text in inbox) {
                try {
                    val msg = JSONObject(text)
                    when (msg.getString("type")) {
                        "audio" -> session.yieldAudioData(
                            Base64.decode(msg.getString("data"), Base64.DEFAULT),
                            end = msg.optBoolean("end"),
                        )
                        "motion" -> {
                            val data = msg.getJSONArray("data")
                            session.yieldFramesData(List(data.length()) { Base64.decode(data.getString(it), Base64.DEFAULT) })
                        }
                    }
                } catch (error: SpatialRealError) {
                    message = describe(error)
                } catch (error: JSONException) {
                    // Not a message from the example server: skip it
                }
            }
        }
    }

    private fun describe(error: SpatialRealError) = "[${error.code}] ${error.message}"
}
