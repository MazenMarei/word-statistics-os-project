package word_statistics.model;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;

public class DirectoryModel {

    public IntegerProperty totalWordsProperty;
    public IntegerProperty isCountProperty;
    public IntegerProperty areCountProperty;
    public IntegerProperty youCountProperty;

    public AtomicInteger totalWords;
    public AtomicInteger isCount;
    public AtomicInteger areCount;
    public AtomicInteger youCount;

    public AtomicReference<String> longestWord;
    public AtomicReference<String> shortestWord;

    private String directoryPath;

    public DirectoryModel(String directoryPath) {

        if (directoryPath == null || directoryPath.isEmpty()) {
            throw new IllegalArgumentException("Directory path cannot be null or empty");
        }
        File dir = new File(directoryPath);
        if (!dir.exists() || !dir.isDirectory()) {
            throw new IllegalArgumentException("Invalid directory path: " + directoryPath);
        }

        this.directoryPath = directoryPath;

        this.totalWords = new AtomicInteger(0);
        this.isCount = new AtomicInteger(0);
        this.areCount = new AtomicInteger(0);
        this.youCount = new AtomicInteger(0);
        this.longestWord = new AtomicReference<>("N/A");
        this.shortestWord = new AtomicReference<>("N/A");
    }

    public String getDirectoryPath() {
        return directoryPath;
    }

    public void updateFromFile(FileModel fileModel, Runnable uiCallback) {

        totalWords.addAndGet(fileModel.getWordCount());
        isCount.addAndGet(fileModel.getIsCount());
        areCount.addAndGet(fileModel.getAreCount());
        youCount.addAndGet(fileModel.getYouCount());

        String fileLongest = fileModel.getLongestWord();
        if (!fileLongest.equals("N/A") && !fileLongest.equals("ERROR") && !fileLongest.equals("-")) {
            String currentLongest = longestWord.get();
            if (currentLongest.equals("N/A") || fileLongest.length() > currentLongest.length()) {
                longestWord.set(fileLongest);
            }
        }

        String fileShortest = fileModel.getShortestWord();
        if (!fileShortest.equals("N/A") && !fileShortest.equals("ERROR") && !fileShortest.equals("-")) {
            String currentShortest = shortestWord.get();
            if (currentShortest.equals("N/A") || fileShortest.length() < currentShortest.length()) {
                shortestWord.set(fileShortest);
            }
        }

        if (uiCallback != null) {
            Platform.runLater(uiCallback);
        }

    }
}