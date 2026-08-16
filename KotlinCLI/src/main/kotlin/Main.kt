package org.example

import kotlinx.coroutines.*
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

// withContext(Dispatchers.IO) ensures this runs on a background thread optimized for I/O operations
private suspend fun downloadDocument(rawFileName: String, urlString: String) = withContext(Dispatchers.IO) {
    val folderPath = "./sources_unprocessed"

    try {
        val dir = Paths.get(folderPath)
        if (!Files.exists(dir)) {
            Files.createDirectories(dir)
        }

        val url = URL(urlString)

        // 1. Open an HTTP connection to inspect headers before downloading
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"

        // 2. Read the Content-Type header from the server (e.g., "application/json; charset=utf-8")
        val contentType = connection.contentType ?: ""

        // 3. Intelligently map the content type to a standard file extension
        val extension = when {
            contentType.contains("application/json") -> ".json"
            contentType.contains("text/html") -> ".html"
            contentType.contains("text/csv") -> ".csv"
            contentType.contains("application/xml") -> ".xml"
            contentType.contains("text/plain") -> ".txt"
            else -> "" // Fallback if the type is unknown or binary
        }

        // 4. Append the extension automatically if it isn't already there
        val finalFileName = if (rawFileName.endsWith(extension, ignoreCase = true) || extension.isEmpty()) {
            rawFileName
        } else {
            rawFileName + extension
        }

        val filePath = dir.resolve(finalFileName)
        println("⬇️ Downloading: $urlString")
        println("   Type Detected: ${contentType.ifEmpty { "Unknown" }}")
        println("   Destination: ${filePath.toAbsolutePath()}")

        // 5. Download the stream data directly from the connection
        connection.inputStream.use { input ->
            Files.copy(input, filePath, StandardCopyOption.REPLACE_EXISTING)
        }

        println("✅ Successfully saved: $finalFileName")

    } catch (ex: IOException) {
        System.err.println("❌ I/O Error downloading $urlString: ${ex.message}")
    } catch (ex: Exception) {
        System.err.println("❌ Unexpected error downloading $urlString: ${ex.message}")
    }
}

fun main(args: Array<String>) {
    println("▶️ CLI Started. Number of arguments received: ${args.size}")
    if (args.isNotEmpty()) {
        println("   Arguments: ${args.joinToString(", ")}")
    }

    if (args.isEmpty()) {
        println("⚠️ No arguments provided.")
        println("Usage: java -jar KotlinCLI-1.0-SNAPSHOT-all.jar \"https://rest.fnar.net/exchange/cxpc/ALO.AI1\"")
        return
    }

    // runBlocking keeps the main thread alive until all inner coroutines finish
    runBlocking {
        args.forEach { link ->
            // Validate the link before attempting to download
//            if (!link.startsWith("http")) {
//                System.err.println("⚠️ Skipping invalid link (must start with http/https): $link")
//                return@forEach // Acts like 'continue' in a loop
//            }

            val rawFileName = link.substringAfterLast("/")
            if (rawFileName.isBlank()) {
                System.err.println("⚠️ Could not extract filename from link: $link")
                return@forEach
            }

            // Launch a concurrent coroutine for every valid link
            launch {
                downloadDocument(rawFileName, link)
            }
        }
    }

    println("⏹️ All tasks completed. Exiting program.")
}