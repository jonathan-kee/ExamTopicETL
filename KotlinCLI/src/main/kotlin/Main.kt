package org.example

import kotlinx.coroutines.*
import java.io.IOException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

// withContext(Dispatchers.IO) ensures this runs on a background thread optimized for I/O operations
private suspend fun downloadDocument(fileName: String, urlString: String) = withContext(Dispatchers.IO) {
    val folderPath = "./sources_unprocessed"

    try {
        val dir = Paths.get(folderPath)
        if (!Files.exists(dir)) {
            Files.createDirectories(dir)
        }

        val filePath = dir.resolve(fileName)
        println("⬇️ Downloading: $urlString")
        println("   Destination: ${filePath.toAbsolutePath()}")

        val url = URL(urlString)

        // Use raw byte streams to prevent corrupting binary data
        url.openStream().use { input ->
            Files.copy(input, filePath, StandardCopyOption.REPLACE_EXISTING)
        }

        println("✅ Successfully saved: $fileName")

    } catch (ex: IOException) {
        System.err.println("❌ I/O Error downloading $urlString: ${ex.message}")
    } catch (ex: Exception) {
        System.err.println("❌ Unexpected error downloading $urlString: ${ex.message}")
    }
}

fun main(args: Array<String>) {
    // 1. Immediately log that the program started to ensure the JVM is actually running it
    println("▶️ CLI Started. Number of arguments received: ${args.size}")
    if (args.isNotEmpty()) {
        println("   Arguments: ${args.joinToString(", ")}")
    }

    if (args.isEmpty()) {
        println("⚠️ No arguments provided.")
        println("Usage: java -jar KotlinCLI-1.0-SNAPSHOT-all.jar \"https://rest.fnar.net/exchange/cxpc/ALO.AI1\"")
        return
    }

    // 2. runBlocking keeps the main thread alive until all inner coroutines finish
    runBlocking {
        args.forEach { link ->
            // Validate the link before attempting to download
            if (!link.startsWith("http")) {
                System.err.println("⚠️ Skipping invalid link (must start with http/https): $link")
                return@forEach // Acts like 'continue' in a standard for-loop
            }

            val fileName = link.substringAfterLast("/")
            if (fileName.isBlank()) {
                System.err.println("⚠️ Could not extract filename from link: $link")
                return@forEach
            }

            // 3. Launch a concurrent coroutine for every valid link
            launch {
                downloadDocument(fileName, link)
            }
        }
    }

    // 4. This will only print once every single download has finished or failed
    println("⏹️ All tasks completed. Exiting program.")
}