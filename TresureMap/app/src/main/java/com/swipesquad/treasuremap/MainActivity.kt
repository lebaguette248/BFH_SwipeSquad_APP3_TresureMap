package com.swipesquad.treasuremap

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.swipesquad.treasuremap.ui.OSMTreasureMap
import com.swipesquad.treasuremap.viewmodel.TreasureMapViewModel
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreasureMapApp(viewModel: TreasureMapViewModel) {
    val treasureMarkers by viewModel.treasureMarkers.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Treasure Map") },
                actions = {
                    // Button to send coordinates to Logbook
                    IconButton(onClick = {
                        val intent = Intent().apply {
                            action = "ch.bfh.logbook.ACTION_LOG"
                            putExtra("coordinates", viewModel.getFormattedCoordinatesForLogbook())
                        }
                        // Start the activity or show chooser if multiple apps can handle it
                        if (intent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(Intent.createChooser(intent, "Send to Logbook"))
                        } else {
                            Toast.makeText(
                                context,
                                "Logbook app not found. Please install it first.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
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
        Box(modifier = Modifier
            .padding(padding)
            .fillMaxSize()) {

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
}