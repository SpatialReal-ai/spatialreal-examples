package ai.spatialreal.examples.pipecatclient

import ai.pipecat.client.PipecatClient
import ai.pipecat.client.PipecatClientOptions
import ai.pipecat.client.PipecatEventCallbacks
import ai.pipecat.client.daily.DailyTransport
import ai.pipecat.client.result.RTVIException
import ai.pipecat.client.types.APIRequest
import ai.pipecat.client.types.BotReadyData
import ai.pipecat.client.types.Value
import ai.spatialreal.android.facade.SessionState
import ai.spatialreal.android.facade.SpatialReal
import ai.spatialreal.android.facade.SpatialRealError
import ai.spatialreal.android.pipecat.PipecatAvatarSession
import ai.spatialreal.android.pipecat.PipecatAvatarSessionOptions
import ai.spatialreal.android.pipecat.PipecatVoiceRoute
import ai.spatialreal.android.pipecat.createSession
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
 * Owns the Pipecat avatar session and Pipecat's client. The app joins your bot's Daily room,
 * sends the user's microphone to the bot, and renders the avatar that speaks the bot's replies.
 * The app sends no speech to SpatialReal and opens no connection of its own to it: the bot's
 * voice and the animation both arrive through the call.
 */
class PipecatController(private val context: Context) {
    /** The SDK draws into this view. The screen shows it through AndroidView. */
    val container = FrameLayout(context)

    // Compose state can be written from any thread; the SDK's and Pipecat's callbacks run off the main thread
    var state by mutableStateOf<SessionState?>(null)
        private set
    var voiceRoute by mutableStateOf(PipecatVoiceRoute.CALL)
        private set
    var botReady by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var busy by mutableStateOf(false)
        private set

    /** The avatar session and Pipecat's client share this transport, for the life of the screen. */
    private var transport: DailyTransport? = null
    private var avatar: PipecatAvatarSession? = null
    private var client: PipecatClient<DailyTransport, *>? = null
    private var removeListeners: List<() -> Unit> = emptyList()

    /**
     * Fetch a session token from your server, then create the session: the avatar is downloaded
     * and shown, standing still. Nothing connects yet.
     */
    suspend fun create() {
        if (avatar != null) return
        try {
            val sessionToken = fetchSessionToken()
            val sr = SpatialReal(context, appId = Config.APP_ID)
            val transport = DailyTransport(context)
            val avatar = sr.createSession(
                PipecatAvatarSessionOptions(
                    avatarId = Config.AVATAR_ID,   // the avatar your bot's processor uses
                    credential = sessionToken,     // downloads the avatar
                    container = container,         // must be on screen and have a size
                    transport = transport,
                )
            )
            removeListeners = listOf(
                avatar.onState { change -> state = change.current },
                avatar.onError { error -> message = describe(error) },
                // CALL: Daily plays the bot's track. AVATAR: the avatar plays the bot's voice
                avatar.onVoiceRouteChange { route -> voiceRoute = route },
            )
            state = avatar.state
            voiceRoute = avatar.voiceRoute
            this.transport = transport
            this.avatar = avatar
        } catch (error: SpatialRealError) {
            message = describe(error)
        } catch (error: IOException) {
            message = "Token server: ${error.message}"
        }
    }

    /**
     * Start the avatar, then ask Pipecat's development runner for a bot in a new Daily room and
     * join it (RECORD_AUDIO must be granted first). The bot greets you; from its first pause on,
     * the avatar speaks its replies.
     */
    suspend fun start() {
        if (busy) return
        busy = true
        message = null
        try {
            val avatar = avatar ?: return
            val transport = transport ?: return
            avatar.start()   // no network: it only starts the avatar's player

            // A new client for every conversation, on the same transport
            val client = PipecatClient(
                transport,
                PipecatClientOptions(callbacks = callbacks(avatar, transport), enableMic = true, enableCam = false),
            )
            this.client = client
            client.startBotAndConnect(
                APIRequest(
                    endpoint = "${Config.BOT_SERVER_URL}/start",
                    requestData = Value.Object("createDailyRoom" to Value.Bool(true)),
                )
            ).await()
        } catch (error: SpatialRealError) {
            message = describe(error)
        } catch (error: RTVIException) {
            message = "Could not reach the bot at ${Config.BOT_SERVER_URL}: is `uv run bot.py -t daily --host 0.0.0.0` running? (${error.message})"
            leaveCall()
        } finally {
            busy = false
        }
    }

    /** The user refused the microphone: the bot can't hear them. */
    fun micDenied() {
        message = "Microphone permission denied. Allow it in the app's settings, then tap Start again."
    }

    /** Leave the call and keep the avatar on screen; Start begins a new conversation. */
    suspend fun leave() {
        if (busy) return
        busy = true
        try {
            leaveCall()
        } finally {
            busy = false
        }
    }

    /** The screen goes away: leave the call and release everything. */
    suspend fun release() {
        leaveCall()
        client?.release()
        client = null
        removeListeners.forEach { it() }
        removeListeners = emptyList()
        val previous = avatar
        avatar = null
        transport = null
        previous?.dispose()
    }

    private suspend fun leaveCall() {
        botReady = false
        // End the avatar first, while the call can still switch the bot's audio track back on
        try {
            if (avatar?.state == SessionState.LIVE) avatar?.end()
        } catch (error: SpatialRealError) {
            message = describe(error)
        }
        client?.disconnect()?.awaitNoThrow()
    }

    // ---- Pipecat's callbacks ----

    private fun callbacks(avatar: PipecatAvatarSession, transport: DailyTransport) = object : PipecatEventCallbacks() {
        /** Lets the session ask your bot's processor to send the bot's voice with the animation */
        override fun onBotReady(data: BotReadyData) {
            avatar.handleBotReady()
            botReady = true
            // Daily picks the earpiece; use the loudspeaker, as for a video call
            transport.callClient?.setAudioDevice("SPEAKERPHONE")
        }

        /**
         * Every RTVI server message goes to the session: it takes the avatar's and returns false
         * for anything else, so your own handling can sit behind it.
         */
        override fun onServerMessage(data: Value) {
            if (avatar.handleServerMessage(data)) return
        }

        override fun onBackendError(message: String) {
            this@PipecatController.message = "Bot: $message"
        }
    }

    // ---- Your token server ----

    /** What `POST /api/session-token` returns: see ../web-client/server.js */
    private suspend fun fetchSessionToken(): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("${Config.TOKEN_SERVER_URL}/api/session-token").post("".toRequestBody()).build()
        OkHttpClient().newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("replied ${response.code}. Is `npm run server` running in ../web-client, and is the URL reachable from this device?")
            }
            JSONObject(response.body!!.string()).getString("sessionToken")
        }
    }

    private fun describe(error: SpatialRealError) = "[${error.code}] ${error.message}"
}
