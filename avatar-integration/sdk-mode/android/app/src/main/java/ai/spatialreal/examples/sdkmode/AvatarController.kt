package ai.spatialreal.examples.sdkmode

import ai.spatialreal.android.facade.AvatarSession
import ai.spatialreal.android.facade.AvatarSessionOptions
import ai.spatialreal.android.facade.SessionAudioFormat
import ai.spatialreal.android.facade.SessionState
import ai.spatialreal.android.facade.SpatialReal
import ai.spatialreal.android.facade.SpatialRealError
import android.content.Context
import android.widget.FrameLayout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Owns the avatar session. Each method is one step of "SDK mode on Android" in the docs.
 * The SDK's calls that wait are suspend functions; the screen calls them from a coroutine.
 */
class AvatarController(private val context: Context) {
    /** The SDK draws into this view. The screen shows it through AndroidView. */
    val container = FrameLayout(context)

    // Compose state can be written from any thread; the SDK's callbacks run off the main thread
    var state by mutableStateOf<SessionState?>(null)
        private set
    var message by mutableStateOf<String?>(null)
        private set

    private var avatar: AvatarSession? = null

    /**
     * Create the session: it checks the token, loads the avatar and shows it.
     * It doesn't connect yet, so nothing is billed. The container must be laid out first.
     */
    suspend fun create() {
        if (avatar != null) return
        try {
            val sr = SpatialReal(context, appId = Config.APP_ID)
            val session = sr.createSession(
                AvatarSessionOptions(
                    avatarId = Config.AVATAR_ID,
                    credential = Config.SESSION_TOKEN,
                    container = container,
                    audioFormat = SessionAudioFormat.pcm16(16000), // the rate of the audio you will send
                )
            )
            session.onState { change -> state = change.current }
            session.onError { error -> message = describe(error) }
            state = session.state
            avatar = session
        } catch (error: SpatialRealError) {
            message = describe(error)
        }
    }

    /** Returns once the session is live. From here on, the session is billed. */
    suspend fun start() {
        message = null
        try {
            avatar?.start()
        } catch (error: SpatialRealError) {
            message = describe(error)
        }
    }

    /** Send the whole clip in one call. `end = true` marks the end of the utterance. */
    suspend fun speak(clip: String) {
        try {
            avatar?.send(AudioClip.pcm16(context, clip), end = true)
        } catch (error: SpatialRealError) {
            message = describe(error)
        }
    }

    /** The user talks over the avatar: stop at once. */
    fun stop() {
        avatar?.interrupt()
    }

    /** Close the session but keep the avatar on screen; start() opens it again. */
    suspend fun end() {
        avatar?.end()
    }

    /** The screen goes away: release everything. */
    suspend fun release() {
        avatar?.dispose()
        avatar = null
    }

    private fun describe(error: SpatialRealError) = "[${error.code}] ${error.message}"
}
