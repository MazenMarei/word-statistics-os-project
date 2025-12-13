package word_statistics.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javafx.application.Platform;
import word_statistics.model.FileModel;
import word_statistics.model.FileStatus;

/**
 * Analyzes a single text file and extracts word statistics
 * Implements Runnable for manual thread execution
 */
public class FileAnalyzer implements Runnable {
    private final Path filePath;
    private final FileSearchEngine searchEngine;
    private final int fileIndex;

    public FileAnalyzer(Path filePath, FileSearchEngine searchEngine, int fileIndex) {
        this.filePath = filePath;
        this.searchEngine = searchEngine;
        this.fileIndex = fileIndex;
    }

    @Override
    public void run() {
        try {
            // Check if stop was requested before starting
            if (searchEngine.isStopped()) {
                handleStoppedFile();
                return;
            }

            // Add small delay for real-time visual effect (100-300ms)
            try {
                Thread.sleep(1000 + (long) (Math.random() * 5000));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                handleStoppedFile();
                return;
            }

            // Check again after sleep
            if (searchEngine.isStopped()) {
                handleStoppedFile();
                return;
            }

            // Read entire file content
            String content = Files.readString(filePath);

            // Split content into words (using whitespace and punctuation as delimiters)
            String[] words = content.split("[\\s\\p{Punct}]+");

            // Initialize statistics
            int wordCount = 0;
            int isCount = 0;
            int areCount = 0;
            int youCount = 0;
            String longestWord = "";
            String shortestWord = "";

            // Process each word
            for (String word : words) {
                // Check if stop was requested during processing
                if (searchEngine.isStopped()) {
                    handleStoppedFile();
                    return;
                }

                // Skip empty strings
                if (word.isEmpty()) {
                    continue;
                }

                wordCount++;

                // Count specific words (case-insensitive)
                String lowerWord = word.toLowerCase();
                if (lowerWord.equals("is")) {
                    isCount++;
                } else if (lowerWord.equals("are")) {
                    areCount++;
                } else if (lowerWord.equals("you")) {
                    youCount++;
                }

                // Track longest word
                if (longestWord.isEmpty() || word.length() > longestWord.length()) {
                    longestWord = word;
                }

                // Track shortest word
                if (shortestWord.isEmpty() || word.length() < shortestWord.length()) {
                    shortestWord = word;
                }
            }

            // Create FileModel for this file
            FileModel fileModel = new FileModel(
                    filePath.getFileName().toString(),
                    filePath.toString());

            // Check if stopped before updating
            if (searchEngine.isStopped()) {
                handleStoppedFile();
                return;
            }

            fileModel.setLongestWord(longestWord.isEmpty() ? "N/A" : longestWord);
            fileModel.setShortestWord(shortestWord.isEmpty() ? "N/A" : shortestWord);
            fileModel.setWordCount(wordCount);
            fileModel.setIsCount(isCount);
            fileModel.setAreCount(areCount);
            fileModel.setYouCount(youCount);
            fileModel.setStatus(FileStatus.COMPLETED);

            // Update UI with file statistics (thread-safe using Platform.runLater)
            Platform.runLater(() -> {
                if (searchEngine.getController() != null) {
                    searchEngine.getController().updateFileStatistics(fileIndex, fileModel);
                }
            });

            // Update directory statistics
            searchEngine.updateDirectoryStatisticsFromFile(fileModel);

            // Update processed file count (thread-safe)
            searchEngine.incrementFileProcessed();

        } catch (IOException e) {
            // Handle file reading errors
            System.err.println("Error reading file: " + filePath + " - " + e.getMessage());

            // Create and update error FileModel
            try {
                FileModel errorModel = new FileModel(
                        filePath.getFileName().toString(),
                        filePath.toString(),
                        false);
                errorModel.setLongestWord("ERROR");
                errorModel.setShortestWord("ERROR");
                errorModel.setStatus(FileStatus.ERROR);

                // Update UI with error
                Platform.runLater(() -> {
                    if (searchEngine.getController() != null) {
                        searchEngine.getController().updateFileStatistics(fileIndex, errorModel);
                    }
                });

                searchEngine.incrementFileProcessed();
            } catch (Exception ex) {
                System.err.println("Cannot create error FileModel: " + ex.getMessage());
            }
        }
    }

    /**
     * Handle file that was stopped before completion
     */
    private void handleStoppedFile() {
        try {
            FileModel stoppedModel = new FileModel(
                    filePath.getFileName().toString(),
                    filePath.toString(),
                    false);
            stoppedModel.setStatus(FileStatus.STOPPED);
            stoppedModel.setLongestWord("-");
            stoppedModel.setShortestWord("-");

            // Update UI
            Platform.runLater(() -> {
                if (searchEngine.getController() != null) {
                    searchEngine.getController().updateFileStatistics(fileIndex, stoppedModel);
                }
            });

            // Increment file count so completion check works
            searchEngine.incrementFileProcessed();
        } catch (Exception ex) {
            System.err.println("Cannot create stopped FileModel: " + ex.getMessage());
            // Still increment to avoid hanging
            searchEngine.incrementFileProcessed();
        }
    }
}
