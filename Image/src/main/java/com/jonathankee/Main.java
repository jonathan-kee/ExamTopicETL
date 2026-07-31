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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.jonathankee.database.Database.executeQueryJdbcResultImage;

public class Main {
    private static void downloadImages(String urlString) {
        // Define destination folder and filename
        String folderPath = "./src/main/resources/images"; // Relative or absolute path (e.g., "C:/my_folder")
        try {
            // 1. Ensure the destination directory exists
            Path dir = Paths.get(folderPath);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            // Finds the last '/' and takes everything after it
            String fileName = urlString.substring(urlString.lastIndexOf('/') + 1);

            System.out.println(fileName); // Output: image100.png

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

    private static void downloadSeveralImagesDatabase() throws SQLException, InterruptedException {
        List<Tuple> list = executeQueryJdbcResultImage("SELECT url FROM all_images_url;", 1);
        for (int i = 0; i < list.size(); i++) {
            // Tuple fileName is null
            downloadImages(list.get(i).getUrl());
            Thread.sleep(650);
        }
    }

    private static void downloadSeveralImagesDatabaseMultiThread() throws SQLException, InterruptedException {
        List<Tuple> list = executeQueryJdbcResultImage("SELECT url FROM all_images_url;", 1);

        int cpuCount = Runtime.getRuntime().availableProcessors();
        try (ExecutorService executor = Executors.newFixedThreadPool(cpuCount)) {
            for (int i = 0; i < list.size(); i++) {
                int finalI = i;
                // Submits work that doesn’t need to block the main flow (No need to return any result)
                executor.submit(() -> {
                    // Tuple fileName is null
                    downloadImages(list.get(finalI).getUrl());
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
        // downloadSeveralImagesDatabase(); // 196 seconds (Single Threaded)
        downloadSeveralImagesDatabaseMultiThread(); // 21 seconds (Multi Threaded)    (196/21) = 9x speed up
        Instant endInstant = Instant.now();
        Duration duration = Duration.between(startInstant, endInstant);
        System.out.println("Execution time: " + duration.toMillis() + " ms");
        System.out.println("Formatted: " + duration.toSeconds() + " seconds");
    }
}