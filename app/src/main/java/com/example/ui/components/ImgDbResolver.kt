package com.example.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ImgDbResolver {
    private val cache = mutableMapOf<String, String>()

    suspend fun resolveDirectUrl(url: String): String = withContext(Dispatchers.IO) {
        if (!url.contains("ibb.co") && !url.contains("ibb.co.com")) {
            return@withContext url
        }
        
        cache[url]?.let { return@withContext it }
        
        try {
            var currentUrl = url
            var redirects = 0
            while (redirects < 5) {
                val connection = URL(currentUrl).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 4000
                connection.readTimeout = 4000
                connection.instanceFollowRedirects = true
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                
                val status = connection.responseCode
                if (status == HttpURLConnection.HTTP_MOVED_TEMP || 
                    status == HttpURLConnection.HTTP_MOVED_PERM || 
                    status == 307 || 
                    status == 308) {
                    val location = connection.getHeaderField("Location")
                    if (location != null) {
                        currentUrl = if (location.startsWith("/")) {
                            val baseUri = URL(currentUrl)
                            "${baseUri.protocol}://${baseUri.host}$location"
                        } else {
                            location
                        }
                        redirects++
                        continue
                    }
                }
                
                if (status == 200) {
                    val pageContent = connection.inputStream.bufferedReader().use { it.readText() }
                    val match = """<meta property="og:image" content="([^"]+)"""".toRegex().find(pageContent)
                        ?: """<meta name="twitter:image" content="([^"]+)"""".toRegex().find(pageContent)
                        ?: """<link rel="image_src" href="([^"]+)"""".toRegex().find(pageContent)
                        ?: """<img\s+[^>]*src="([^"]+)"[^>]*id="image-viewer-container"""".toRegex().find(pageContent)
                        ?: """image\s*:\s*\{\s*url\s*:\s*"([^"]+)"""".toRegex().find(pageContent)
                        
                    if (match != null) {
                        var directUrl = match.groupValues[1]
                        if (directUrl.startsWith("//")) {
                            directUrl = "https:" + directUrl
                        }
                        cache[url] = directUrl
                        return@withContext directUrl
                    }
                }
                break
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext url
    }
}

@Composable
fun KhelaAsyncImage(
    model: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    error: Painter? = null
) {
    var resolvedUrl by remember(model) { mutableStateOf(model) }
    
    LaunchedEffect(model) {
        if (model.contains("ibb.co") || model.contains("ibb.co.com")) {
            resolvedUrl = ImgDbResolver.resolveDirectUrl(model)
        }
    }
    
    AsyncImage(
        model = resolvedUrl,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
        error = error
    )
}
