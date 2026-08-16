package org.example
import kotlinx.coroutines.*
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.util.ArrayList

private fun downloadDocument(fileName: String, urlString: String) {
    // Define destination folder and filename
    // JAR is at ingestion folder
    val folderPath = "./sources_unprocessed" // Relative or absolute path (e.g., "C:/my_folder")
    try {
        // 1. Ensure the destination directory exists
        val dir = Paths.get(folderPath)
        if (!Files.exists(dir)) {
            Files.createDirectories(dir)
        }

        // 2. Resolve full file path
        val filePath = dir.resolve(fileName)

        // 3. Open streams using try-with-resources (auto-closes reader & writer)
        val url = URL(urlString)
        InputStreamReader(BufferedInputStream(url.openStream())).use { reader ->
            Files.newBufferedWriter(filePath).use { writer ->
                val buffer = CharArray(8192) // Use a buffer array for drastically better performance
                var length: Int
                while ((reader.read(buffer).also { length = it }) != -1) {
                    writer.write(buffer, 0, length)
                }
            }
        }
        println("File saved successfully to: " + filePath.toAbsolutePath())
    } catch (ex: IOException) {
        System.err.println("Error downloading content: " + ex.message)
    }
}

fun main(args: Array<String>) {
    if (args.size > 0) {
        // fileName and Link
        val list: MutableList<(Pair<String,String>)> = ArrayList()

        for (link in args) {
            if(!link.contains("https")) throw Exception("Invalid link: $link")

            // 1. Get the last part of the URL ("ALO.AI1")
            val lastSegment = link.substringAfterLast("/")

            list.add(Pair(lastSegment, link))
        }

        runBlocking(Dispatchers.IO) {
            // Launch a concurrent coroutine for every document download
            list.map { pair ->
                launch {
                    // pair.first = fileName
                    // pair.second = Link
                    downloadDocument(pair.first, pair.second)
                }
            }.joinAll() // Wait for all parallel downloads to complete
        }
    } else {
        println("Usage: java -jar /KotlinCLI-1.0-SNAPSHOT-all.jar \"https://rest.fnar.net/exchange/cxpc/ALO.AI1\"")
    }
}