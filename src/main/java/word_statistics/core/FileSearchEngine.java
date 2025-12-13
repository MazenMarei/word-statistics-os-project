package word_statistics.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Alert;
import word_statistics.model.AppStatus;

public class FileSearchEngine {

    private StringProperty currentDirectory;
    private IntegerProperty activeThreads;
    private IntegerProperty fileProcessed;
    private IntegerProperty totalFiles;
    private ObjectProperty<AppStatus> status;
    private BooleanProperty includeSubdirectories;

    // Internal atomic counters for thread-safe updates
    private AtomicInteger activeThreadsAtomic;
    private AtomicInteger fileProcessedAtomic;
    private AtomicInteger totalFilesAtomic;

    // applying singleton pattern
    private static FileSearchEngine instance = null;

    public static synchronized FileSearchEngine getInstance() {
        if (instance == null) {
            instance = new FileSearchEngine();
        }
        return instance;
    }

    private FileSearchEngine() {
        if (instance != null) {
            return;
        }
        this.currentDirectory = new SimpleStringProperty("");
        this.activeThreads = new SimpleIntegerProperty(0);
        this.fileProcessed = new SimpleIntegerProperty(0);
        this.totalFiles = new SimpleIntegerProperty(0);
        this.status = new SimpleObjectProperty<>(AppStatus.Ready);
        this.includeSubdirectories = new SimpleBooleanProperty(false);

        this.activeThreadsAtomic = new AtomicInteger(0);
        this.fileProcessedAtomic = new AtomicInteger(0);
        this.totalFilesAtomic = new AtomicInteger(0);
    }

    public void startEngine() {
        FileSearchEngine searchEngine = FileSearchEngine.getInstance();

        try {
            List<Path> files = FileSearch.searchTextFiles(searchEngine.getCurrentDirectory(),
                    searchEngine.getIncludeSubdirectories());
            searchEngine.setTotalFiles(files.size());
            if (files.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("No Files Found");
                alert.setContentText("No .txt files were found in the selected directory.");
                alert.showAndWait();
            } else {
                // Proceed with processing files
            }
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }

    }

    // Property Getters
    public ObjectProperty<AppStatus> statusProperty() {
        return status;
    }

    public BooleanProperty includeSubdirectoriesProperty() {
        return includeSubdirectories;
    }

    public IntegerProperty activeThreadsProperty() {
        return activeThreads;
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

    // normal Getters
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

    // setters
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
            totalFiles.set(value);
        }
    }

    public void setStatus(AppStatus value) {
        if (value != null && value != status.get()) {
            status.set(value);
        }
    }

    public void incrementFileProcessed() {
        int newValue = fileProcessedAtomic.incrementAndGet();
        fileProcessed.set(newValue);
    }

    public void incrementActiveThreads() {
        if (activeThreadsAtomic.get() >= Runtime.getRuntime().availableProcessors()) {
            System.err.println("Warning: Active threads exceed available processors");
        }

        int newValue = activeThreadsAtomic.incrementAndGet();
        activeThreads.set(newValue);
    }

    public void decrementActiveThreads() {
        int newValue = activeThreadsAtomic.decrementAndGet();
        if (newValue < 0) {
            activeThreadsAtomic.set(0);
            newValue = 0;
        }
        activeThreads.set(newValue);
    }

    public void reset() {
        activeThreadsAtomic.set(0);
        fileProcessedAtomic.set(0);
        totalFilesAtomic.set(0);

        activeThreads.set(0);
        fileProcessed.set(0);
        totalFiles.set(0);
        status.set(AppStatus.Ready);
        currentDirectory.set("");
    }

}
