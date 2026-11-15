package ar.com.santuchotv

import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

private const val PLAYLIST_URL = "https://iptv-org.github.io/iptv/categories/sports.m3u"
private val SouthAmerica = setOf("AR", "BO", "BR", "CL", "CO", "EC", "PY", "PE", "UY", "VE")
private val Red = Color(0xFFF00000)
private val Black = Color(0xFF080808)
private val Panel = Color(0xFF171717)

data class TvChannel(val name: String, val url: String, val country: String, val group: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SantuchoApp() }
    }
}

@Composable
fun SantuchoApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("santucho", 0) }
    var channels by remember { mutableStateOf<List<TvChannel>>(emptyList()) }
    var selected by remember { mutableStateOf<TvChannel?>(null) }
    var category by remember { mutableStateOf("Sudamérica") }
    var query by remember { mutableStateOf("") }
    var favorites by remember { mutableStateOf(prefs.getStringSet("favorites", emptySet()) ?: emptySet()) }
    var loading by remember { mutableStateOf(true) }
    var refreshCounter by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(refreshCounter) {
        loading = true
        try {
            channels = withContext(Dispatchers.IO) { loadM3u(PLAYLIST_URL) }
            loading = false
            selected = channels.firstOrNull { SouthAmerica.contains(it.country.uppercase()) }
                ?: channels.firstOrNull()
        } catch (e: Exception) {
            loading = false
            error = "No se pudo cargar la lista. Revisá tu conexión e intentá actualizar."
        }
    }

    val southAmerica = channels.filter { SouthAmerica.contains(it.country.uppercase()) }
    val visible = when (category) {
        "Sudamérica" -> southAmerica
        "Favoritos" -> channels.filter { favorites.contains(it.url) }
        "Todos" -> channels
        else -> channels
    }.filter { query.isBlank() || it.name.contains(query, true) }

    Row(Modifier.fillMaxSize().background(Black)) {
        Column(
            Modifier.width(220.dp).fillMaxHeight().background(Panel).padding(12.dp)
        ) {
            Text("SANTUCHO TV", color = Red, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.height(16.dp))
            listOf("Sudamérica", "Todos", "Favoritos").forEach { cat ->
                Text(
                    cat,
                    color = if (category == cat) Red else Color.White,
                    fontWeight = if (category == cat) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { category = cat }
                        .padding(vertical = 10.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { refreshCounter++ },
                colors = ButtonDefaults.buttonColors(containerColor = Red)
            ) { Text("Actualizar lista") }
        }

        Column(
            Modifier.width(300.dp).fillMaxHeight().background(Color(0xFF101010)).padding(12.dp)
        ) {
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 16.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Panel, RoundedCornerShape(8.dp))
                    .padding(10.dp)
            )
            Spacer(Modifier.height(10.dp))
            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Red)
                }
            } else if (error != null) {
                Text(error ?: "", color = Color.White)
            } else {
                LazyColumn {
                    items(visible) { channel ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { selected = channel }
                                .focusable()
                                .background(if (selected == channel) Panel else Color.Transparent)
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(channel.name, color = Color.White, modifier = Modifier.weight(1f))
                            Icon(
                                imageVector = if (favorites.contains(channel.url)) Icons.Filled.Star else Icons.Filled.StarBorder,
                                contentDescription = null,
                                tint = Red,
                                modifier = Modifier.clickable {
                                    favorites = if (favorites.contains(channel.url))
                                        favorites - channel.url
                                    else
                                        favorites + channel.url
                                    prefs.edit().putStringSet("favorites", favorites).apply()
                                }
                            )
                        }
                    }
                }
            }
        }

        Box(Modifier.weight(1f).fillMaxHeight().background(Color.Black)) {
            selected?.let { channel ->
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            player = ExoPlayer.Builder(ctx).build().also {
                                it.setMediaItem(MediaItem.fromUri(channel.url))
                                it.prepare()
                                it.playWhenReady = true
                            }
                        }
                    },
                    update = { view ->
                        val current = view.player
                        if (current?.currentMediaItem?.localConfiguration?.uri.toString() != channel.url) {
                            current?.release()
                            view.player = ExoPlayer.Builder(view.context).build().also {
                                it.setMediaItem(MediaItem.fromUri(channel.url))
                                it.prepare()
                                it.playWhenReady = true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

private suspend fun loadM3u(url: String): List<TvChannel> {
    val text = URL(url).readText()
    val lines = text.lines()
    val result = mutableListOf<TvChannel>()
    var name = ""
    var country = ""
    var group = ""
    for (line in lines) {
        when {
            line.startsWith("#EXTINF") -> {
                name = line.substringAfterLast(",").trim()
                country = Regex("tvg-country=\"([^\"]*)\"").find(line)?.groupValues?.get(1) ?: ""
                group = Regex("group-title=\"([^\"]*)\"").find(line)?.groupValues?.get(1) ?: ""
            }
            line.isNotBlank() && !line.startsWith("#") -> {
                result.add(TvChannel(name = name, url = line.trim(), country = country, group = group))
            }
        }
    }
    return result
}
