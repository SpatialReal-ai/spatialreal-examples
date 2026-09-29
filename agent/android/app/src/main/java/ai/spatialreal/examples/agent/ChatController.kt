package ai.spatialreal.examples.agent

import ai.spatialreal.android.facade.ChatSessionOptions
import ai.spatialreal.android.facade.SessionState
import ai.spatialreal.android.facade.SpatialReal
import ai.spatialreal.android.facade.SpatialRealError
import ai.spatialreal.android.facade.chat.ChatSession
import ai.spatialreal.android.facade.chat.TranscriptEvent
import ai.spatialreal.android.facade.chat.TranscriptRole
import android.content.Context
import android.widget.FrameLayout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Owns the conversation with the agent. Follows "Your own app" (Android tab) in the docs.
 * The SDK's calls that wait are suspend functions; the screen calls them from a coroutine.
 */
class ChatController(private val context: Context) {
    data class Line(val id: String, val role: TranscriptRole, val text: String)

    /** The SDK draws the agent's avatar into this view. The screen shows it through AndroidView. */
    val container = FrameLayout(context)

    // Compose state can be written from any thread; the SDK's callbacks run off the main thread
    var state by mutableStateOf<SessionState?>(null)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    val lines = mutableStateListOf<Line>()

    private var chat: ChatSession? = null

    /**
     * Create the session: it checks the token and shows the agent's avatar.
     * It doesn't connect yet, so nothing is billed. The container must be laid out first.
     */
    suspend fun create() {
        if (chat != null) return
        try {
            val sr = SpatialReal(context, appId = Config.APP_ID)
            val session = sr.createSession(
                ChatSessionOptions(
                    agentId = Config.AGENT_ID,
                    credential = Config.SESSION_TOKEN,
                    container = container, // null for voice only
                )
            )
            session.onTranscript { event -> show(event) }
            session.onState { change -> state = change.current }
            session.onError { error -> report(error) }
            state = session.state
            chat = session
        } catch (error: SpatialRealError) {
            message = describe(error)
        }
    }

    /** Connects to the agent and opens the microphone. Ask for RECORD_AUDIO before calling it. */
    suspend fun start() {
        message = null
        lines.clear()
        try {
            chat?.start()
        } catch (error: SpatialRealError) {
            message = describe(error)
        }
    }

    fun micRefused() {
        message = "The agent needs the microphone. Allow it in Settings, then tap Start again."
    }

    /** Finish the conversation but keep the session: start() opens a new one. */
    suspend fun end() {
        chat?.end()
    }

    /** The screen goes away: release everything. */
    suspend fun release() {
        chat?.dispose()
        chat = null
    }

    /** text is the whole utterance so far: replace this turn's line, don't append to it. */
    private fun show(event: TranscriptEvent) {
        val id = "${event.turnId}:${event.role}"
        val i = lines.indexOfFirst { it.id == id }
        if (i >= 0) lines[i] = lines[i].copy(text = event.text) else lines.add(Line(id, event.role, event.text))
    }

    /**
     * session-ended: the agent, a time limit or your credits ended the conversation.
     * Don't reconnect on your own; let the user start again.
     */
    private fun report(error: SpatialRealError) {
        message = if (error is SpatialRealError.SessionEnded) {
            "The conversation ended (${error.reason})."
        } else {
            describe(error)
        }
    }

    private fun describe(error: SpatialRealError) = "[${error.code}] ${error.message}"
}
