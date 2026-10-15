package ai.spatialreal.examples.livekitclient

import ai.spatialreal.android.facade.SessionState
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class MainActivity : ComponentActivity() {
    private lateinit var controller: LiveKitController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        controller = LiveKitController(this)
        setContent { MaterialTheme { LiveKitScreen(controller) } }
    }

    override fun onDestroy() {
        super.onDestroy()
        // lifecycleScope is already cancelled here, so release the session in its own scope
        MainScope().launch { controller.release() }
    }
}

@Composable
fun LiveKitScreen(controller: LiveKitController) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val live = controller.state == SessionState.LIVE

    // Create the session once the container is on screen and has a size
    LaunchedEffect(Unit) {
        controller.container.awaitLayout()
        controller.create()
    }

    // start() turns the microphone on, so ask for RECORD_AUDIO first
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) scope.launch { controller.start() } else controller.micDenied()
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
                onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        scope.launch { controller.start() }
                    } else {
                        micPermission.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                enabled = controller.state == SessionState.IDLE,
            ) { Text("Start") }
            OutlinedButton(onClick = { scope.launch { controller.toggleMute() } }, enabled = live) {
                Text(if (controller.muted) "Unmute" else "Mute")
            }
            OutlinedButton(onClick = { scope.launch { controller.leave() } }, enabled = live) { Text("Leave") }
        }

        Text("Session: ${controller.state?.name?.lowercase() ?: "loading"}", color = Color.Gray)
        controller.message?.let { Text(it, color = Color(0xFFB91C1C)) }
    }
}

/** Resumes once the view has been laid out, at once if it already has been */
private suspend fun View.awaitLayout() = suspendCancellableCoroutine { cont ->
    doOnLayout { if (cont.isActive) cont.resume(Unit) }
}
