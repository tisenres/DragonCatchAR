package com.tisenres.dragoncatch

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import kotlinx.coroutines.delay
import kotlin.random.Random
import android.os.CountDownTimer
import androidx.compose.runtime.*
import com.tisenres.dragoFncatch.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch

@Composable
fun GameScreen(navController: NavController) {
    var score by remember { mutableIntStateOf(0) }
    var timeLeft by remember { mutableIntStateOf(30) } // 30-second timer
    var gameOver by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // Timer using CountDownTimer
    LaunchedEffect(Unit) {
        object : CountDownTimer(30000, 1000) { // 30 seconds, tick every 1 second
            override fun onTick(millisUntilFinished: Long) {
                timeLeft = (millisUntilFinished / 1000).toInt()
            }

            override fun onFinish() {
                gameOver = true
            }
        }.start()
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // AR View
        if (!gameOver) {
            AndroidView(
                factory = { context ->
                    val arSceneView = ArSceneView(context)
                    arSceneView.setupSession(context)
                    coroutineScope.launch(Dispatchers.IO) {
                        spawnCreatures(arSceneView) { score += it }
                    }
                    arSceneView
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Score and Timer Display
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Text(text = "Score: $score | Time: $timeLeft")
        }

        // Game Over Screen
        if (gameOver) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Game Over! Final Score: $score",
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

fun ArSceneView.setupSession(context: Context) {
    val session = Session(context)
    val config = Config(session).apply { planeFindingMode = Config.PlaneFindingMode.HORIZONTAL }
    session.configure(config)
    this.setupSession(session)
}

suspend fun spawnCreatures(
    arSceneView: ArSceneView,
    onCaught: (Int) -> Unit
) {
    val context = arSceneView.context
    val random = Random.Default

    while (true) {
        // Load the model
        val renderable = ModelRenderable.builder()
            .setSource(context, R.raw.oiiaioooooia_fin)
            .build()
            .await()

        // Create a node and place it randomly
        val node = com.google.ar.sceneform.Node().apply {
            setParent(arSceneView.scene)
            localPosition = Vector3(
                random.nextFloat() * 2 - 1, // X-axis
                random.nextFloat() * 0.5f + 0.5f, // Y-axis
                random.nextFloat() * -3 - 1 // Z-axis
            )
            this.renderable = renderable
        }

        // Add tap interaction
        node.setOnTapListener { _, _ ->
            arSceneView.scene.removeChild(node)
            onCaught(random.nextInt(10, 20)) // Reward random points
        }

        // Keep creature for a short duration
        delay(3000)
        arSceneView.scene.removeChild(node)
    }
}