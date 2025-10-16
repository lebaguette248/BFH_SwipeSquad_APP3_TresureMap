package com.swipesquad.treasuremap

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.swipesquad.treasuremap.data.TreasureMarker
import com.swipesquad.treasuremap.ui.OSMTreasureMap
import com.swipesquad.treasuremap.viewmodel.TreasureMapViewModel
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.config.Configuration
import java.io.File

class MainActivity : ComponentActivity() {

    private val viewModel: TreasureMapViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize OSMDroid
        Configuration.getInstance().load(
            applicationContext,
            getSharedPreferences("osmdroid", MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = "com.swipesquad.treasuremap"
        Configuration.getInstance().osmdroidTileCache = File(cacheDir, "osmdroid")

        setContent {
            MaterialTheme {
                TreasureMapApp(viewModel)
            }
        }
    }
}

fun sendLogbookIntent(context: Context, value: String) {
    val intent = Intent("ch.apprun.intent.LOG").apply {
        putExtra("ch.apprun.logmessage", value)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Log.e("Logger", "LogBook application is not installed on this device.")
    }
}

fun createJsonObject(value: JSONArray): String {
    val log = JSONObject()
    log.put("task", "Schatzkarte")
    log.put("points", value)

    return log.toString()
}

fun createCordsObject(value: List<TreasureMarker>): JSONArray {
    val jsonArray = JSONArray()

    for (marker in value) {
        val latMicro = (marker.latitude * 1_000_000).toInt()
        val lonMicro = (marker.longitude * 1_000_000).toInt()

        val jsonObject = JSONObject()
            .put("lat", latMicro)
            .put("lon", lonMicro)

        jsonArray.put(jsonObject)

    }

    return jsonArray
}

@Composable
fun InputPopup(
    title: String = "Enter value",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    initialText: JSONArray
) {
    var text by remember { mutableStateOf(createJsonObject(initialText)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            TextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Type something...") }
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(text)
                    onDismiss()
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreasureMapApp(viewModel: TreasureMapViewModel) {
    val treasureMarkers by viewModel.treasureMarkers.collectAsState()
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Treasure Map") },
                actions = {
                    // Button to send coordinates to Logbook
                    IconButton(onClick = {
                        showDialog = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send to Logbook"
                        )
                    }

                    // Button to clear all markers
                    IconButton(onClick = { viewModel.clearAllMarkers() }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear All Markers"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {

            OSMTreasureMap(
                treasureMarkers = treasureMarkers,
                onAddMarker = { lat, lng -> viewModel.addMarker(lat, lng) },
                onRemoveMarker = { lat, lng -> viewModel.removeMarker(lat, lng) }
            )

            // Markers counter
            Card(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Flags: ${treasureMarkers.size}",
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
    if (showDialog) {
        InputPopup(
            title = "Is this value correct?",
            onDismiss = { showDialog = false },
            onConfirm = { input ->
                sendLogbookIntent(context, input)
            },
            initialText = createCordsObject(treasureMarkers)
        )
    }
}