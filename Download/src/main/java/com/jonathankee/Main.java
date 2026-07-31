package com.jonathankee;

import com.jonathankee.schema.Tuple;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.jonathankee.database.Database.executeQueryJdbcResult;

public class Main {
    private static void downloadSeveralDocumentsHardcode() {
        // multipleDocuments();
        var t = new Tuple("document1.html", "https://www.examtopics.com/discussions/oracle/view/79888-exam-1z0-071-topic-1-question-1-discussion/");
        var t2 = new Tuple("document2.html", "https://www.examtopics.com/discussions/oracle/view/79530-exam-1z0-071-topic-1-question-2-discussion/");
        List<Tuple> list = new ArrayList<>();
        list.add(t);
        list.add(t2);
        for (int i = 0; i < list.size(); i++) {
            downloadDocument(list.get(i).getFileName(), list.get(i).getUrl());
        }
    }

    private static void downloadDocument(String fileName, String urlString) {
        // Define destination folder and filename
        String folderPath = "./src/main/resources/tmp"; // Relative or absolute path (e.g., "C:/my_folder")
        try {
            // 1. Ensure the destination directory exists
            Path dir = Paths.get(folderPath);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            // 2. Resolve full file path
            Path filePath = dir.resolve(fileName);

            // 3. Open streams using try-with-resources (auto-closes reader & writer)
            URL url = new URL(urlString);
            try (Reader reader = new InputStreamReader(new BufferedInputStream(url.openStream()));
                 Writer writer = Files.newBufferedWriter(filePath)) {

                char[] buffer = new char[8192]; // Use a buffer array for drastically better performance
                int length;
                while ((length = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, length);
                }
            }

            System.out.println("File saved successfully to: " + filePath.toAbsolutePath());

        } catch (IOException ex) {
            System.err.println("Error downloading content: " + ex.getMessage());
        }
    }

    private static void downloadSeveralDocumentsDatabase() throws SQLException, InterruptedException {
        List<Tuple> list = executeQueryJdbcResult("SELECT number, link FROM questionslink;", 1, 2);
        for (int i = 0; i < list.size(); i++) {
            downloadDocument(list.get(i).getFileName(), list.get(i).getUrl());
            Thread.sleep(650);
        }
    }

    private static void downloadSeveralDocumentsDatabaseMultiThread() throws SQLException, InterruptedException {
        List<Tuple> list = executeQueryJdbcResult("SELECT number, link FROM questionslink;", 1, 2);

        int cpuCount = Runtime.getRuntime().availableProcessors();
        try (ExecutorService executor = Executors.newFixedThreadPool(cpuCount)) {
            for (int i = 0; i < list.size(); i++) {
                int finalI = i;
                // Submits work that doesn’t need to block the main flow (No need to return any result)
                executor.submit(() -> {
                    downloadDocument(list.get(finalI).getFileName(), list.get(finalI).getUrl());
                    try {
                        Thread.sleep(650);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
    }

    public static void main(String[] args) throws SQLException, InterruptedException {
        Instant startInstant = Instant.now();
        downloadSeveralDocumentsDatabase();              // 435 seconds  (Single Threaded)
        downloadSeveralDocumentsDatabaseMultiThread();   // 44 seconds   (Multi Threaded)    (435/44) = 9x speed up

        //singleDocument("document8.html");
        Instant endInstant = Instant.now();
        Duration duration = Duration.between(startInstant, endInstant);
        System.out.println("Execution time: " + duration.toMillis() + " ms");
        System.out.println("Formatted: " + duration.toSeconds() + " seconds");
    }
}