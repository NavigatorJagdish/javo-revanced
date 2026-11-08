package app.revanced.manager.video

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.format.Formatter
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.R
import app.revanced.manager.ui.theme.ReVancedManagerTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.launch

class VideoDownloadActivity : ComponentActivity() {
    private val model by viewModels<VideoDownloadModel>()
    private val videoPicker = registerForActivityResult(ActivityResultContracts.CreateDocument("video/mp4")) { model.finishPick(it) }
    private val audioPicker = registerForActivityResult(ActivityResultContracts.CreateDocument("audio/mp4")) { model.finishPick(it) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            model.link = intent.takeIf { it.action == Intent.ACTION_SEND }
                ?.getCharSequenceExtra(Intent.EXTRA_TEXT)?.take(8192)?.toString().orEmpty()
        }
        setContent {
            ReVancedManagerTheme(isSystemInDarkTheme(), false, false) {
                Surface(Modifier.fillMaxSize()) {
                    val progress by model.progress.collectAsState()
                    Column(
                        Modifier.systemBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(stringResource(R.string.javo_save_mp4), style = MaterialTheme.typography.headlineSmall)
                        Text(stringResource(R.string.javo_media_hint))
                        OutlinedTextField(
                            value = model.link, onValueChange = { model.link = it.take(8192) },
                            label = { Text(stringResource(R.string.javo_media_link)) },
                            enabled = !model.busy, modifier = Modifier.fillMaxWidth()
                        )
                        VideoDownload.Format.entries.forEach { format ->
                            Button(
                                onClick = {
                                    model.start(format) { filename ->
                                        when (format) {
                                            VideoDownload.Format.VIDEO -> videoPicker.launch(filename)
                                            VideoDownload.Format.AUDIO -> audioPicker.launch(filename)
                                        }
                                    }
                                },
                                enabled = !model.busy && model.link.isNotBlank(),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(if (format == VideoDownload.Format.VIDEO) R.string.javo_media_video else R.string.javo_media_audio))
                            }
                        }
                        if (model.busy) {
                            val transfer = progress
                            if (transfer?.second != null) {
                                val total = transfer.second ?: 1L
                                LinearProgressIndicator(progress = { (transfer.first.toDouble() / total).toFloat().coerceIn(0f, 1f) })
                            } else LinearProgressIndicator()
                            if (transfer != null) Text(Formatter.formatFileSize(this@VideoDownloadActivity, transfer.first))
                            TextButton(onClick = model::cancel) { Text(stringResource(android.R.string.cancel)) }
                        }
                        model.message?.let { Text(stringResource(it)) }
                    }
                }
            }
        }
    }
}

internal class VideoDownloadModel(private val app: Application) : AndroidViewModel(app) {
    var link by mutableStateOf("")
    var message by mutableStateOf<Int?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    val progress = MutableStateFlow<Pair<Long, Long?>?>(null)
    private var pending: VideoDownload.FileSource? = null
    private var operation: Job? = null

    fun start(format: VideoDownload.Format, choose: (String) -> Unit) {
        if (busy) return
        val requested = link
        busy = true
        message = R.string.javo_media_preparing
        progress.value = null
        operation = viewModelScope.launch {
            try {
                val source = VideoDownload.resolve(requested, format)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    save(source, null)
                } else {
                    pending = source
                    message = R.string.javo_media_choose_downloads
                    choose(VideoDownload.filename(source.title, format))
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IllegalArgumentException) {
                pending = null
                message = R.string.javo_media_invalid
            } catch (_: Exception) {
                pending = null
                message = R.string.javo_media_failed
            } finally {
                busy = pending != null
            }
        }
    }

    fun finishPick(uri: Uri?) {
        val source = pending ?: return
        pending = null
        if (uri == null) {
            busy = false
            message = null
            return
        }
        operation = viewModelScope.launch {
            try {
                save(source, uri)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                message = R.string.javo_media_failed
            } finally {
                busy = false
            }
        }
    }

    fun cancel() {
        pending = null
        operation?.cancel()
        message = null
        if (operation?.isCompleted != false) busy = false
    }

    private suspend fun save(source: VideoDownload.FileSource, uri: Uri?) {
        message = R.string.javo_media_saving
        VideoDownload.save(app, source, uri) { downloaded, total -> progress.value = downloaded to total }
        message = if (uri == null) R.string.javo_media_saved else R.string.javo_media_saved_location
    }
}
