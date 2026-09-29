package ai.spatialreal.examples.sdkmode

import ai.spatialreal.android.facade.SessionState
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
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
    private lateinit var controller: AvatarController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        controller = AvatarController(this)
        setContent { MaterialTheme { AvatarScreen(controller) } }
    }

    override fun onDestroy() {
        super.onDestroy()
        // lifecycleScope is already cancelled here, so release the session in its own scope
        MainScope().launch { controller.release() }
    }
}

@Composable
fun AvatarScreen(controller: AvatarController) {
    val scope = rememberCoroutineScope()
    val live = controller.state == SessionState.LIVE

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
                .aspectRatio(4f / 5f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFE4E4E7)),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { scope.launch { controller.start() } },
                enabled = controller.state == SessionState.IDLE,
            ) { Text("Start") }
            OutlinedButton(onClick = { scope.launch { controller.end() } }, enabled = live) { Text("End") }
            OutlinedButton(onClick = { controller.stop() }, enabled = live) { Text("Stop") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { scope.launch { controller.speak("female") } }, enabled = live) {
                Text("Speak (female voice)")
            }
            OutlinedButton(onClick = { scope.launch { controller.speak("male") } }, enabled = live) {
                Text("Speak (male voice)")
            }
        }

        Text("Session: ${controller.state?.name?.lowercase() ?: "loading"}", color = Color.Gray)
        controller.message?.let { Text(it, color = Color(0xFFB91C1C)) }
    }
}

/** Resumes once the view has been laid out, at once if it already has been */
private suspend fun View.awaitLayout() = suspendCancellableCoroutine { cont ->
    doOnLayout { if (cont.isActive) cont.resume(Unit) }
}
