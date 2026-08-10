package com.jonathankee

import com.jonathankee.database.Database.executeQueryJdbcResultImage
import com.jonathankee.schema.Tuple
import kotlinx.coroutines.*
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.sql.SQLException
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Executors

object Main {
    private fun downloadImages(urlString: String) {
        // Define destination folder and filename
        val folderPath = "./src/main/resources/images" // Relative or absolute path (e.g., "C:/my_folder")
        try {
            // 1. Ensure the destination directory exists
            val dir = Paths.get(folderPath)
            if (!Files.exists(dir)) {
                Files.createDirectories(dir)
            }

            // Finds the last '/' and takes everything after it
            val fileName = urlString.substring(urlString.lastIndexOf('/') + 1)

            println(fileName) // Output: image100.png

            // 2. Resolve full file path
            val filePath = dir.resolve(fileName)

            // 3. Open streams using try-with-resources (auto-closes reader & writer)
            val url = URL(urlString)
            BufferedInputStream(url.openStream()).use { `in` ->
                BufferedOutputStream(Files.newOutputStream(filePath)).use { out ->
                    val buffer = ByteArray(8192) // Use byte[], NOT char[]
                    var length: Int
                    while ((`in`.read(buffer).also { length = it }) != -1) {
                        out.write(buffer, 0, length)
                    }
                }
            }
            println("File saved successfully to: " + filePath.toAbsolutePath())
        } catch (ex: IOException) {
            System.err.println("Error downloading content: " + ex.message)
        }
    }

    @Throws(SQLException::class, InterruptedException::class)
    private fun downloadSeveralImagesDatabase() {
        val list: List<Tuple> = executeQueryJdbcResultImage(
            "SELECT url FROM scrape.\"stg_viewAllImagesUrl\";",
            1
        )
        for (i in list.indices) {
            // Tuple fileName is null
            downloadImages(list[i].getUrl())
            Thread.sleep(650)
        }
    }

    @Throws(SQLException::class, InterruptedException::class)
    private fun downloadSeveralImagesDatabaseMultiThread() {
        val list: List<com.jonathankee.schema.Tuple> = com.jonathankee.database.Database.executeQueryJdbcResultImage(
            "SELECT url FROM scrape.\"stg_viewAllImagesUrl\";",
            1
        )

        val cpuCount = Runtime.getRuntime().availableProcessors()
        Executors.newFixedThreadPool(cpuCount).use { executor ->
            for (i in list.indices) {
                val finalI = i
                // Submits work that doesn’t need to block the main flow (No need to return any result)
                executor.submit {
                    // Tuple fileName is null
                    downloadImages(list[finalI].getUrl())
                    try {
                        Thread.sleep(650)
                    } catch (e: InterruptedException) {
                        throw RuntimeException(e)
                    }
                }
            }
        }
    }

    @Throws(SQLException::class)
    suspend fun downloadSeveralImagesDatabaseCoroutine() = coroutineScope {
        // 1. BLOCKING I/O: Fetch URLs from Database
        val list = withContext(Dispatchers.IO) {
            com.jonathankee.database.Database.executeQueryJdbcResultImage(
                "SELECT url FROM scrape.\"stg_viewAllImagesUrl\";",
                1
            )
        }

        if (list.isEmpty()) return@coroutineScope

        // 2. NETWORK I/O: Launch all image downloads concurrently on Dispatchers.IO
        list.map { tuple ->
            launch(Dispatchers.IO) {
                downloadImages(tuple.getUrl())
                delay(650) // Non-blocking delay (frees thread during pause)
            }
        }.joinAll() // Wait for all downloads to finish before returning
    }

    @Throws(SQLException::class, InterruptedException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val startInstant = Instant.now()
        // downloadSeveralImagesDatabase(); // 196 seconds (Single Threaded)
        // downloadSeveralImagesDatabaseMultiThread() // 21 seconds (Multi Threaded)    (196/21) = 9x speed up

        runBlocking {
            downloadSeveralImagesDatabaseCoroutine() // 2 seconds (Multi Threaded / Coroutine) (196/2) = 98x speed up
        }

        val endInstant = Instant.now()
        val duration = Duration.between(startInstant, endInstant)
        println("Execution time: " + duration.toMillis() + " ms")
        println("Formatted: " + duration.toSeconds() + " seconds")
    }
}