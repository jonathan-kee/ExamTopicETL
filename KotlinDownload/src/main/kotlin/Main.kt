package com.jonathankee

import com.jonathankee.database.Database
import com.jonathankee.schema.Tuple
import kotlinx.coroutines.*
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.sql.SQLException
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Executors

object Main {
    private fun downloadSeveralDocumentsHardcode() {
        // multipleDocuments();
        val t = Tuple(
            "document1.html",
            "https://www.examtopics.com/discussions/oracle/view/79888-exam-1z0-071-topic-1-question-1-discussion/"
        )
        val t2 = Tuple(
            "document2.html",
            "https://www.examtopics.com/discussions/oracle/view/79530-exam-1z0-071-topic-1-question-2-discussion/"
        )
        val list: MutableList<Tuple> = ArrayList()
        list.add(t)
        list.add(t2)
        for (i in list.indices) {
            downloadDocument(list[i].fileName, list[i].url)
        }
    }

    private fun downloadDocument(fileName: String, urlString: String) {
        // Define destination folder and filename
        val folderPath = "./src/main/resources/tmp" // Relative or absolute path (e.g., "C:/my_folder")
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

    @Throws(SQLException::class, InterruptedException::class)
    private fun downloadSeveralDocumentsDatabase(exam: String) {
        val sql = "SELECT number, link FROM scrape.\"questionslink\" WHERE exam = '$exam';"
        val list = Database.executeQueryJdbcResult(sql, 1, 2)
        for (i in list.indices) {
            downloadDocument(list[i].fileName, list[i].url)
            Thread.sleep(650)
        }
    }

    @Throws(SQLException::class, InterruptedException::class)
    private fun downloadSeveralDocumentsDatabaseMultiThreadJava(exam: String) {
        val sql = "SELECT number, link FROM scrape.\"questionslink\" WHERE exam = '$exam';"
        val list = Database.executeQueryJdbcResult(sql, 1, 2)
        val cpuCount = Runtime.getRuntime().availableProcessors()
        Executors.newFixedThreadPool(cpuCount).use { executor ->
            for (i in list.indices) {
                val finalI = i
                // Submits work that doesn’t need to block the main flow (No need to return any result)
                executor.submit {
                    downloadDocument(list[finalI].fileName, list[finalI].url)
                    try {
                        Thread.sleep(650)
                    } catch (e: InterruptedException) {
                        throw RuntimeException(e)
                    }
                }
            }
        }
    }

    @Throws(SQLException::class, InterruptedException::class)
    private fun downloadSeveralDocumentsDatabaseMultiThreadCoroutine(exam: String) {
        val sql = "SELECT number, link FROM scrape.\"questionslink\" WHERE exam = '$exam';"
        val list = Database.executeQueryJdbcResult(sql, 1, 2)

        runBlocking(Dispatchers.IO) {
            // Launch a concurrent coroutine for every document download
            list.map { item ->
                launch {
                    downloadDocument(item.fileName, item.url)
                }
            }.joinAll() // Wait for all parallel downloads to complete
        }
    }

    @Throws(SQLException::class, InterruptedException::class)
    @JvmStatic
    fun main(args: Array<String> = arrayOf("1z0-071")) {
        if (args.size > 0) {
            println("Argument received: " + args[0])
            val exam = args[0]
            val startInstant = Instant.now()
            // downloadSeveralDocumentsDatabase();              // 435 seconds  (Single Threaded)
            // downloadSeveralDocumentsDatabaseMultiThreadJava(exam) // 44 seconds   (Multi Threaded)    (435/44) = 9x speed up
            downloadSeveralDocumentsDatabase(exam)
            //singleDocument("document8.html");
            val endInstant = Instant.now()
            val duration = Duration.between(startInstant, endInstant)
            println("Execution time: " + duration.toMillis() + " ms")
            println("Formatted: " + duration.toSeconds() + " seconds")
        } else {
            println("Usage: java -jar /Users/jonathankee/examTopicScraper/static_page/Download/build/libs/Download-all.jar \"1z0-071\"")
        }
    }

    fun test(){
        val exam = "1z0-071"
        val startInstant = Instant.now()
        // downloadSeveralDocumentsDatabase();                   // 435 seconds  (Single Threaded)
        // downloadSeveralDocumentsDatabaseMultiThreadJava(exam) // 44 seconds   (Multi Threaded)    (435/44) = 9x speed up
        downloadSeveralDocumentsDatabaseMultiThreadCoroutine(exam)  // 5 seconds (Multi Threaded Coroutine)  435/44) = 87x speed up
        //singleDocument("document8.html");
        val endInstant = Instant.now()
        val duration = Duration.between(startInstant, endInstant)
        println("Execution time: " + duration.toMillis() + " ms")
        println("Formatted: " + duration.toSeconds() + " seconds")
    }
}