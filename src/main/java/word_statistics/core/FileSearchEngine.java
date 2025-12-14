package word_statistics.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

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
import word_statistics.model.DirectoryModel;
import word_statistics.model.FileModel;
import word_statistics.model.FileStatus;

public class FileSearchEngine {

    private final StringProperty currentDirectory;
    private final IntegerProperty activeThreads;
    private final IntegerProperty availableThreads;
    private final IntegerProperty fileProcessed;
    private final IntegerProperty totalFiles;
    private final ObjectProperty<AppStatus> status;
    private final BooleanProperty includeSubdirectories;

    private final Semaphore threadLimiter;
    private final AtomicInteger activeThreadsAtomic;
    private final AtomicInteger fileProcessedAtomic;
    private final AtomicInteger totalFilesAtomic;

    private MainController controller;
    private DirectoryModel directoryModel;
    private final int numThreads;
    private volatile boolean shouldStop = false;

    private static FileSearchEngine instance = null;

    public static synchronized FileSearchEngine getInstance() {
        if (instance == null) {
            instance = new FileSearchEngine();
        }
        return instance;
    }

    private FileSearchEngine() {
        this.currentDirectory = new SimpleStringProperty("");
        this.activeThreads = new SimpleIntegerProperty(0);
        this.availableThreads = new SimpleIntegerProperty(0);
        this.fileProcessed = new SimpleIntegerProperty(0);
        this.totalFiles = new SimpleIntegerProperty(0);
        this.status = new SimpleObjectProperty<>(AppStatus.Ready);
        this.includeSubdirectories = new SimpleBooleanProperty(false);

        this.activeThreadsAtomic = new AtomicInteger(0);
        this.fileProcessedAtomic = new AtomicInteger(0);
        this.totalFilesAtomic = new AtomicInteger(0);

        this.numThreads = Runtime.getRuntime().availableProcessors();
        this.threadLimiter = new Semaphore(numThreads);
        this.availableThreads.set(numThreads);
    }

    public void setController(MainController controller) {
        this.controller = controller;
    }

    public MainController getController() {
        return controller;
    }

    public DirectoryModel getDirectoryModel() {
        return directoryModel;
    }

    public void startEngine() {
        try {
            List<Path> files = FileSearch.searchTextFiles(
                    getCurrentDirectory(),
                    getIncludeSubdirectories());

            setTotalFiles(files.size());

            if (files.isEmpty()) {
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

            directoryModel = new DirectoryModel(getCurrentDirectory());
            shouldStop = false;

            if (controller != null) {
                Platform.runLater(() -> {
                    controller.clearResults();
                    for (Path filePath : files) {
                        FileModel fileModel = new FileModel(
                                filePath.getFileName().toString(),
                                filePath.toString(),
                                false);
                        fileModel.setStatus(FileStatus.PENDING);
                        controller.addFileStatistics(fileModel);
                    }
                });
            }

            for (int i = 0; i < files.size(); i++) {
                final int fileIndex = i;
                final Path filePath = files.get(i);
                FileAnalyzer analyzer = new FileAnalyzer(filePath, this, fileIndex, threadLimiter);
                Thread workerThread = new Thread(analyzer);
                workerThread.setName("FileAnalyzer-" + filePath.getFileName());
                workerThread.setDaemon(true);
                workerThread.start();
            }

        } catch (IOException e) {
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

    public void updateDirectoryStatisticsFromFile(FileModel fileModel) {
        if (directoryModel == null)
            return;

        directoryModel.updateFromFile(fileModel, () -> {
            if (controller != null) {
                controller.updateDirectoryStatistics(
                        directoryModel.totalWords.get(),
                        directoryModel.isCount.get(),
                        directoryModel.areCount.get(),
                        directoryModel.youCount.get(),
                        directoryModel.longestWord.get(),
                        directoryModel.shortestWord.get());
            }
        });
    }

    public void stopEngine() {
        setStatus(AppStatus.STOPPED);
        shouldStop = true;
    }

    public boolean isStopped() {
        return shouldStop;
    }

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

    public void setFileProcessed(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("File processed cannot be negative");
        }

        int oldValue = fileProcessedAtomic.getAndSet(value);

        if (oldValue != value) {
            Platform.runLater(() -> fileProcessed.set(value));
        }
    }

    public void setStatus(AppStatus value) {
        if (value != null && value != status.get()) {
            Platform.runLater(() -> status.set(value));
        }
    }

    public void incrementFileProcessed() {
        int newValue = fileProcessedAtomic.incrementAndGet();
        Platform.runLater(() -> fileProcessed.set(newValue));
    }

    public void incrementActiveThreads() {
        int newValue = activeThreadsAtomic.incrementAndGet();
        Platform.runLater(() -> {
            activeThreads.set(newValue);
            availableThreads.set(numThreads - newValue);
        });
    }

    public void decrementActiveThreads() {
        int newValue = activeThreadsAtomic.decrementAndGet();
        if (newValue < 0) {
            activeThreadsAtomic.set(0);
            newValue = 0;
        }
        final int finalValue = newValue;
        Platform.runLater(() -> {
            activeThreads.set(finalValue);
            availableThreads.set(numThreads - finalValue);
        });
    }
}
