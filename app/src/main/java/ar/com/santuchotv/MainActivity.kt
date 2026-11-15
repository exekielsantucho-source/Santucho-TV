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

private const val PLAYLIST_URL = "https://iptv-org.github.io/iptv/index.m3u"
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
    var category by remember { mutableStateOf("Argentina") }
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
            selected = channels.firstOrNull { it.country.equals("AR", true) }
                ?: channels.firstOrNull()
        } catch (e: Exception) {
            loading = false
            error = "No se pudo cargar la lista. Revisá tu conexión e intentá actualizar."
        }
    }

    val argentina = channels.filter { it.country.equals("AR", true) }
    val visible = when (category) {
