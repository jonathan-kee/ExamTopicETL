package org.example

import kotlinx.coroutines.*
import java.io.IOException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

// 1. Mark as suspend and explicitly define the IO dispatcher here
private suspend fun downloadDocument(fileName: String, urlString: String) = withContext(Dispatchers.IO) {
    val folderPath = "./sources_unprocessed"

    try {
        val dir = Paths.get(folderPath)
        if (!Files.exists(dir)) {
            Files.createDirectories(dir)
        }

        val filePath = dir.resolve(fileName)
        val url = URL(urlString)

        // 2. Use byte streams and Files.copy to prevent binary file corruption
        url.openStream().use { input ->
            Files.copy(input, filePath, StandardCopyOption.REPLACE_EXISTING)
        }

        println("File saved successfully to: ${filePath.toAbsolutePath()}")
    } catch (ex: IOException) {
        System.err.println("Error downloading content from $urlString: ${ex.message}")
    }
}

fun main(args: Array<String>) = runBlocking {
    if (args.isNotEmpty()) {
        // 3. Map arguments directly into a list of Pairs
        val downloads = args.map { link ->
            // Use startsWith instead of contains for safety
            require(link.startsWith("https")) { "Invalid link (must be https): $link" }

            val fileName = link.substringAfterLast("/")
            fileName to link // Idiomatic Pair creation
        }

        // 4. Launch child coroutines. runBlocking automatically waits for all of them.
        downloads.forEach { (fileName, link) ->
            launch {
                downloadDocument(fileName, link)
            }
        }
    } else {
        println("Usage: java -jar /KotlinCLI-1.0-SNAPSHOT-all.jar \"https://rest.fnar.net/exchange/cxpc/ALO.AI1\"")
    }
}