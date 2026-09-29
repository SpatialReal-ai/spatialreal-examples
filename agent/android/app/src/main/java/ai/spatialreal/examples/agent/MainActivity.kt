package ai.spatialreal.examples.agent

import ai.spatialreal.android.facade.SessionState
import ai.spatialreal.android.facade.chat.TranscriptRole
import android.Manifest
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.doOnLayout
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class MainActivity : ComponentActivity() {
    private lateinit var controller: ChatController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        controller = ChatController(this)
        setContent { MaterialTheme { ChatScreen(controller) } }
    }

    override fun onDestroy() {
        super.onDestroy()
        // lifecycleScope is already cancelled here, so release the session in its own scope
        MainScope().launch { controller.release() }
    }
}

@Composable
fun ChatScreen(controller: ChatController) {
    val scope = rememberCoroutineScope()

    // The SDK can't show the permission prompt itself: ask first, then start
    val askForMic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) scope.launch { controller.start() } else controller.micRefused()
    }

    // Create the session once the container is on screen and has a size
    LaunchedEffect(Unit) {
        controller.container.awaitLayout()
        controller.create()
    }

    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AndroidView(
            factory = { controller.container },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .aspectRatio(4f / 5f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFE4E4E7)),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = { askForMic.launch(Manifest.permission.RECORD_AUDIO) },
                enabled = controller.state == SessionState.IDLE,
            ) { Text("Start") }
            OutlinedButton(
                onClick = { scope.launch { controller.end() } },
                enabled = controller.state == SessionState.LIVE,
            ) { Text("End") }
            Spacer(Modifier.weight(1f))
            Text(controller.state?.name?.lowercase() ?: "loading", color = Color.Gray)
        }

        controller.message?.let { Text(it, color = Color(0xFFB91C1C)) }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(controller.lines, key = { it.id }) { line ->
                val user = line.role == TranscriptRole.USER
                Box(Modifier.fillMaxWidth(), contentAlignment = if (user) Alignment.CenterEnd else Alignment.CenterStart) {
                    Text(
                        line.text,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (user) Color(0xFFDBEAFE) else Color(0xFFF4F4F5))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

/** Resumes once the view has been laid out, at once if it already has been */
private suspend fun View.awaitLayout() = suspendCancellableCoroutine { cont ->
    doOnLayout { if (cont.isActive) cont.resume(Unit) }
}
