package word_statistics.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Alert;
import word_statistics.controller.MainController;
import word_statistics.model.AppStatus;
import word_statistics.model.FileModel;
import word_statistics.model.FileStatus;

public class FileSearchEngine {

    private StringProperty currentDirectory;
    private IntegerProperty activeThreads;
    private IntegerProperty availableThreads;
    private IntegerProperty fileProcessed;
    private IntegerProperty totalFiles;
    private ObjectProperty<AppStatus> status;
    private BooleanProperty includeSubdirectories;

    // Synchronization tools for thread management
    private final Semaphore threadLimiter; // Limits number of concurrent threads
    private final ReentrantLock statsLock; // Lock for updating directory statistics
    private final Object monitorLock; // Monitor lock for thread coordination

    // Internal atomic counters for thread-safe updates from worker threads
    private AtomicInteger activeThreadsAtomic;
    private AtomicInteger fileProcessedAtomic;
    private AtomicInteger totalFilesAtomic;

    // Reference to controller for real-time UI updates
    private MainController controller;

    // Track directory-level statistics (protected by statsLock)
    private String longestWordInDirectory = "";
    private String shortestWordInDirectory = "";
    private int totalWordsInDirectory = 0;
    private int totalIsInDirectory = 0;
    private int totalAreInDirectory = 0;
    private int totalYouInDirectory = 0;

    // Number of worker threads (based on CPU cores)
    private final int numThreads;

    // Flag to stop processing
    private volatile boolean shouldStop = false;

    // Singleton pattern
    private static FileSearchEngine instance = null;

    public static synchronized FileSearchEngine getInstance() {
        if (instance == null) {
            instance = new FileSearchEngine();
        }
        return instance;
    }

    private FileSearchEngine() {
        // Initialize JavaFX properties for UI binding
        this.currentDirectory = new SimpleStringProperty("");
        this.activeThreads = new SimpleIntegerProperty(0);
        this.availableThreads = new SimpleIntegerProperty(0);
        this.fileProcessed = new SimpleIntegerProperty(0);
        this.totalFiles = new SimpleIntegerProperty(0);
        this.status = new SimpleObjectProperty<>(AppStatus.Ready);
        this.includeSubdirectories = new SimpleBooleanProperty(false);

        // Initialize atomic counters for thread-safe operations
        this.activeThreadsAtomic = new AtomicInteger(0);
        this.fileProcessedAtomic = new AtomicInteger(0);
        this.totalFilesAtomic = new AtomicInteger(0);

        // Initialize synchronization tools based on CPU cores
        this.numThreads = Runtime.getRuntime().availableProcessors();
        this.threadLimiter = new Semaphore(numThreads); // Limit concurrent threads
        this.statsLock = new ReentrantLock(); // Lock for statistics updates
        this.monitorLock = new Object(); // Monitor for thread coordination

        // Initialize available threads to total threads
        this.availableThreads.set(numThreads);

        System.out.println("FileSearchEngine initialized with " + numThreads + " thread limit");
    }

    /**
     * Set reference to MainController for UI updates
     */
    public void setController(MainController controller) {
        this.controller = controller;
    }

    /**
     * Get reference to MainController
     */
    public MainController getController() {
        return controller;
    }

    /**
     * Main engine method - discovers files and processes them with manual threads
     */
    public void startEngine() {
        try {
            // Discover all .txt files in directory (and subdirectories if enabled)
            List<Path> files = FileSearch.searchTextFiles(
                    getCurrentDirectory(),
                    getIncludeSubdirectories());

            setTotalFiles(files.size());

            if (files.isEmpty()) {
                // No files found - show message and stop
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("No Files Found");
                    alert.setHeaderText(null);
                    alert.setContentText("No .txt files were found in the selected directory.");
                    alert.showAndWait();
                    setStatus(AppStatus.Ready);
                });
                return;
            }

            // Reset directory statistics using ReentrantLock
            statsLock.lock();
            try {
                longestWordInDirectory = "";
                shortestWordInDirectory = "";
                totalWordsInDirectory = 0;
                shouldStop = false;
            } finally {
                statsLock.unlock();
            }

            // Clear previous results in UI
            if (controller != null) {
                Platform.runLater(() -> {
                    controller.clearResults();
                    // Add all files to table with PENDING status first
                    for (Path filePath : files) {
                        FileModel fileModel = new FileModel(
                                filePath.getFileName().toString(),
                                filePath.toString(),
                                false // Skip validation for UI display
                        );
                        fileModel.setStatus(FileStatus.PENDING);
                        controller.addFileStatistics(fileModel);
                    }
                });
            }

            // Create and start worker threads manually for each file
            for (int i = 0; i < files.size(); i++) {
                final int fileIndex = i;
                final Path filePath = files.get(i);
                // Create a worker thread for this file
                Thread workerThread = new Thread(() -> processFile(filePath, fileIndex));
                workerThread.setName("FileAnalyzer-" + filePath.getFileName());
                workerThread.setDaemon(true);
                workerThread.start();
            }

        } catch (IOException e) {
            // Handle file search errors
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("File Search Error");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
                setStatus(AppStatus.Ready);
            });
        }
    }

    /**
     * Process a single file using Semaphore for thread limiting
     */
    private void processFile(Path filePath, int fileIndex) {
        try {
            // Acquire semaphore permit (blocks if max threads reached)
            threadLimiter.acquire();

            // Check if we should stop
            synchronized (monitorLock) {
                if (shouldStop) {
                    threadLimiter.release();
                    return;
                }
            }

            try {
                // Increment active threads counter
                incrementActiveThreads();

                // Analyze the file (FileAnalyzer handles all updates internally)
                FileAnalyzer analyzer = new FileAnalyzer(filePath, this, fileIndex);
                analyzer.run();

                // Check if all files are processed using monitor lock
                synchronized (monitorLock) {
                    if (getFileProcessed() >= getTotalFiles()) {
                        Platform.runLater(() -> {
                            setStatus(AppStatus.COMPLETED);
                            if (controller != null) {
                                controller.enableControls();
                            }
                        });
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // Decrement active threads counter
                decrementActiveThreads();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            // Release semaphore permit
            threadLimiter.release();
        }
    }

    /**
     * Update directory-level statistics using ReentrantLock for thread safety
     */
    public void updateDirectoryStatisticsFromFile(FileModel fileModel) {
        // Use ReentrantLock to protect shared directory statistics
        statsLock.lock();
        try {
            // Update total words
            totalWordsInDirectory += fileModel.getWordCount();

            // Update is/are/you counts
            totalIsInDirectory += fileModel.getIsCount();
            totalAreInDirectory += fileModel.getAreCount();
            totalYouInDirectory += fileModel.getYouCount();

            // Update longest word
            String filelongest = fileModel.getLongestWord();
            if (!filelongest.equals("N/A") && !filelongest.equals("ERROR")) {
                if (longestWordInDirectory.isEmpty() ||
                        filelongest.length() > longestWordInDirectory.length()) {
                    longestWordInDirectory = filelongest;
                }
            }

            // Update shortest word
            String fileShortest = fileModel.getShortestWord();
            if (!fileShortest.equals("N/A") && !fileShortest.equals("ERROR")) {
                if (shortestWordInDirectory.isEmpty() ||
                        fileShortest.length() < shortestWordInDirectory.length()) {
                    shortestWordInDirectory = fileShortest;
                }
            }

            // Update UI stat cards in real-time
            Platform.runLater(() -> {
                if (controller != null) {
                    controller.updateDirectoryStatistics(
                            totalWordsInDirectory,
                            totalIsInDirectory,
                            totalAreInDirectory,
                            totalYouInDirectory,
                            longestWordInDirectory,
                            shortestWordInDirectory);
                }
            });
        } finally {
            // Always release lock in finally block
            statsLock.unlock();
        }
    }

    /**
     * Stop processing - sets flag using monitor lock
     */
    public void stopEngine() {
        setStatus(AppStatus.STOPPED);

        // Set stop flag using synchronized block
        synchronized (monitorLock) {
            shouldStop = true;
            monitorLock.notifyAll(); // Wake up any waiting threads
        }
    }

    /**
     * Check if engine has been stopped
     */
    public boolean isStopped() {
        synchronized (monitorLock) {
            return shouldStop;
        }
    }

    // ================================ Property Getters (for UI binding)
    // ================================

    public ObjectProperty<AppStatus> statusProperty() {
        return status;
    }

    public BooleanProperty includeSubdirectoriesProperty() {
        return includeSubdirectories;
    }

    public IntegerProperty activeThreadsProperty() {
        return activeThreads;
    }

    public IntegerProperty availableThreadsProperty() {
        return availableThreads;
    }

    public IntegerProperty fileProcessedProperty() {
        return fileProcessed;
    }

    public IntegerProperty totalFilesProperty() {
        return totalFiles;
    }

    public StringProperty currentDirectoryProperty() {
        return currentDirectory;
    }

    // ================================ Regular Getters
    // ================================

    public String getCurrentDirectory() {
        return currentDirectory.get();
    }

    public int getActiveThreads() {
        return activeThreadsAtomic.get();
    }

    public int getFileProcessed() {
        return fileProcessedAtomic.get();
    }

    public int getTotalFiles() {
        return totalFilesAtomic.get();
    }

    public AppStatus getStatus() {
        return status.get();
    }

    public Boolean getIncludeSubdirectories() {
        return includeSubdirectories.get();
    }

    // ================================ Setters ================================

    public void setCurrentDirectory(String value) {
        File dir = new File(value);
        if (!dir.exists() || !dir.isDirectory()) {
            throw new IllegalArgumentException("Invalid directory path: " + value);
        }
        currentDirectory.set(value);
    }

    public void setTotalFiles(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Total files cannot be negative");
        }

        int oldValue = totalFilesAtomic.getAndSet(value);

        if (oldValue != value) {
            Platform.runLater(() -> totalFiles.set(value));
        }
    }

    public void setStatus(AppStatus value) {
        if (value != null && value != status.get()) {
            Platform.runLater(() -> status.set(value));
        }
    }

    // ================================ Thread-safe Counter Updates
    // ================================

    /**
     * thread-safe using AtomicInteger)
     */
    public void incrementFileProcessed() {
        int newValue = fileProcessedAtomic.incrementAndGet();
        // Update JavaFX property on FX thread
        Platform.runLater(() -> fileProcessed.set(newValue));
    }

    /**
     * Increment active thread count (thread-safe using AtomicInteger)
     */
    public void incrementActiveThreads() {
        int newValue = activeThreadsAtomic.incrementAndGet();

        // Update JavaFX property on FX thread
        Platform.runLater(() -> {
            activeThreads.set(newValue);
            availableThreads.set(numThreads - newValue);
        });
    }

    /**
     * Decrement active thread count (thread-safe using AtomicInteger)
     */
    public void decrementActiveThreads() {
        int newValue = activeThreadsAtomic.decrementAndGet();
        if (newValue < 0) {
            activeThreadsAtomic.set(0);
            newValue = 0;
        }

        // Update JavaFX property on FX thread
        final int finalValue = newValue;
        Platform.runLater(() -> {
            activeThreads.set(finalValue);
            availableThreads.set(numThreads - finalValue);
        });
    }
}
