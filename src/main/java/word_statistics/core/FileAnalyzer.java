package word_statistics.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Semaphore;

import javafx.application.Platform;
import word_statistics.model.FileModel;
import word_statistics.model.FileStatus;

public class FileAnalyzer implements Runnable {
    private final Path filePath;
    private final FileSearchEngine searchEngine;
    private final int fileIndex;
    private final Semaphore threadLimiter;

    public FileAnalyzer(Path filePath, FileSearchEngine searchEngine, int fileIndex, Semaphore threadLimiter) {
        this.filePath = filePath;
        this.searchEngine = searchEngine;
        this.fileIndex = fileIndex;
        this.threadLimiter = threadLimiter;
    }

    @Override
    public void run() {
        try {
            threadLimiter.acquire();

            if (searchEngine.isStopped()) {
                threadLimiter.release();
                return;
            }

            try {
                searchEngine.incrementActiveThreads();
                analyzeFile();
                System.err.println(
                        "Finished Thread (" + Thread.currentThread().getName() + ") analyzing file: " + filePath);

                if (searchEngine.getFileProcessed() >= searchEngine.getTotalFiles()) {
                    Platform.runLater(() -> {
                        searchEngine.setStatus(word_statistics.model.AppStatus.COMPLETED);
                        if (searchEngine.getController() != null) {
                            searchEngine.getController().enableControls();
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                searchEngine.decrementActiveThreads();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            threadLimiter.release();
        }
    }

    private void analyzeFile() {
        try {
            if (searchEngine.isStopped()) {
                handleStoppedFile();
                return;
            }
            System.err.println("Started Thread (" + Thread.currentThread().getName() + ") analyzing file: " + filePath);
            try {
                Thread.sleep(1000 + (long) (Math.random() * 3000));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                handleStoppedFile();
                return;
            }

            if (searchEngine.isStopped()) {
                handleStoppedFile();
                return;
            }

            String content = Files.readString(filePath);

            String[] words = content.split("[\\s\\p{Punct}]+");

            int wordCount = 0;
            int isCount = 0;
            int areCount = 0;
            int youCount = 0;
            String longestWord = "";
            String shortestWord = "";

            for (String word : words) {
                if (searchEngine.isStopped()) {
                    handleStoppedFile();
                    return;
                }

                if (word.isEmpty()) {
                    continue;
                }

                wordCount++;

                String lowerWord = word.toLowerCase();
                switch (lowerWord) {
                    case "is" -> isCount++;
                    case "are" -> areCount++;
                    case "you" -> youCount++;
                    default -> {
                    }
                }

                if (longestWord.isEmpty() || word.length() > longestWord.length()) {
                    longestWord = word;
                }

                if (shortestWord.isEmpty() || word.length() < shortestWord.length()) {
                    shortestWord = word;
                }
            }

            FileModel fileModel = new FileModel(
                    filePath.getFileName().toString(),
                    filePath.toString());

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

            Platform.runLater(() -> {
                if (searchEngine.getController() != null) {
                    searchEngine.getController().updateFileStatistics(fileIndex, fileModel);
                }
            });

            searchEngine.updateDirectoryStatisticsFromFile(fileModel);

            searchEngine.incrementFileProcessed();

        } catch (IOException e) {
            System.err.println("Error reading file: " + filePath + " - " + e.getMessage());

            try {
                FileModel errorModel = new FileModel(
                        filePath.getFileName().toString(),
                        filePath.toString(),
                        false);
                errorModel.setLongestWord("ERROR");
                errorModel.setShortestWord("ERROR");
                errorModel.setStatus(FileStatus.ERROR);

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

    private void handleStoppedFile() {
        try {
            FileModel stoppedModel = new FileModel(
                    filePath.getFileName().toString(),
                    filePath.toString(),
                    false);
            stoppedModel.setStatus(FileStatus.STOPPED);
            stoppedModel.setLongestWord("-");
            stoppedModel.setShortestWord("-");

            Platform.runLater(() -> {
                if (searchEngine.getController() != null) {
                    searchEngine.getController().updateFileStatistics(fileIndex, stoppedModel);
                }
            });

            searchEngine.incrementFileProcessed();
        } catch (Exception ex) {
            System.err.println("Cannot create stopped FileModel: " + ex.getMessage());
            searchEngine.incrementFileProcessed();
        }
    }
}
