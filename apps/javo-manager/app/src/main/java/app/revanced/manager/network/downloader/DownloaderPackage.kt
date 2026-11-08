package app.revanced.manager.network.downloader

data class DownloaderPackage(
    val downloaders: List<LoadedDownloader>,
    val classLoader: ClassLoader,
    val packageName: String,
    val name: String,
    val version: String
)