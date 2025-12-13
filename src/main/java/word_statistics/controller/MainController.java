package word_statistics.controller;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Border;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import word_statistics.controller.MainController.FileStatistics;
import word_statistics.core.FileSearchEngine;
import word_statistics.model.AppStatus;

public class MainController {
    @FXML
    private TableView<FileStatistics> statsTable;

    @FXML
    private TableColumn<FileStatistics, String> fileNameCol;

    @FXML
    private TableColumn<FileStatistics, Integer> wordCountCol;

    @FXML
    private TableColumn<FileStatistics, Integer> isCountCol;

    @FXML
    private TableColumn<FileStatistics, Integer> areCountCol;

    @FXML
    private TableColumn<FileStatistics, Integer> youCountCol;

    @FXML
    private TableColumn<FileStatistics, String> longestWordCol;

    @FXML
    private TableColumn<FileStatistics, String> shortestWordCol;

    @FXML
    private TableColumn<FileStatistics, String> statusCol;

    @FXML
    private Label statusLabel;
    @FXML
    private Label threadCountLabel;
    @FXML
    private Label filesProcessedLabel;
    @FXML
    private TextField directoryPathField;
    @FXML
    private CheckBox includeSubdirsCheck;
    @FXML
    private Button analyzeBtn;
    @FXML
    private Button browseBtn;
    @FXML
    private Button stopBtn;

    /*
     * ---------------------------------- Buttons Actoions Handlers
     * ---------------------------------
     */
    @FXML
    public void onIncludeSubdirsCheckToggle() {
        FileSearchEngine searchEngine = FileSearchEngine.getInstance();
        if (searchEngine.getStatus() == AppStatus.RUNNING) {
            includeSubdirsCheck.setDisable(true);
            return;
        }
        searchEngine.includeSubdirectoriesProperty()
                .set(includeSubdirsCheck.isSelected());
        includeSubdirsCheck.setDisable(false);
    }

    @FXML
    public void onBrowseButtonClick() {
        FileSearchEngine searchEngine = FileSearchEngine.getInstance();
        if (searchEngine.getStatus() == AppStatus.RUNNING) {
            return;
        }

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Directory");
        var selectedDirectory = directoryChooser.showDialog(directoryPathField.getScene().getWindow());
        if (selectedDirectory != null) {
            directoryPathField.setText(selectedDirectory.getAbsolutePath());
            searchEngine.currentDirectoryProperty().set(selectedDirectory.getAbsolutePath());
        }

    }

    @FXML
    public void onAnalyzeButtonClick() {
        FileSearchEngine searchEngine = FileSearchEngine.getInstance();

        if (searchEngine.getCurrentDirectory() == null ||
                searchEngine.getCurrentDirectory().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("No Directory Selected");
            alert.setContentText("Please select a directory to start analyze.");
            alert.showAndWait();
            return;
        }

        if (searchEngine.getStatus() == AppStatus.RUNNING) {
            analyzeBtn.setDisable(true);
            browseBtn.setDisable(true);
            includeSubdirsCheck.setDisable(true);
            stopBtn.setDisable(false);

            return;
        }
        searchEngine.setStatus(AppStatus.RUNNING);
        analyzeBtn.setDisable(true);
        browseBtn.setDisable(true);
        includeSubdirsCheck.setDisable(true);
        stopBtn.setDisable(false);


        // start the file search and analysis
        searchEngine.startEngine();
        // searchEngine.setTotalFiles(20);
        // AnimationTimer timer = new AnimationTimer() {
        //     private long lastUpdate = 0;
        //     private int filesProcessed = 0;
        //     private final int totalFiles = 20; // Simulate 20 files

        //     @Override
        //     public void handle(long now) {
        //         if (now - lastUpdate >= 500_000_000) { // Update every 0.5 seconds
        //             if (filesProcessed < totalFiles) {
        //                 filesProcessed++;
        //                 statsTable.getItems().add(new FileStatistics(
        //                         "File " + filesProcessed,
        //                         filesProcessed * 100,
        //                         filesProcessed * 10,
        //                         filesProcessed * 5,
        //                         filesProcessed * 3,
        //                         "LongestWord" + filesProcessed,
        //                         "Short" + filesProcessed,
        //                         "Completed"));
        //                 searchEngine.incrementFileProcessed();
        //                 lastUpdate = now;
        //                 searchEngine.incrementActiveThreads();
        //             } else {
        //                 this.stop();
                        searchEngine.setStatus(AppStatus.COMPLETED);
        //             }
        //         }
        //     }
        // };
        // timer.start();

    }

    private void makeTableResponsive() {
        // Set column widths as percentages of table width
        fileNameCol.prefWidthProperty().bind(
                statsTable.widthProperty().multiply(0.15) // 15% of table width
        );

        wordCountCol.prefWidthProperty().bind(
                statsTable.widthProperty().multiply(0.10) // 12%
        );

        isCountCol.prefWidthProperty().bind(
                statsTable.widthProperty().multiply(0.10) // 10%
        );

        areCountCol.prefWidthProperty().bind(
                statsTable.widthProperty().multiply(0.10) // 10%
        );

        youCountCol.prefWidthProperty().bind(
                statsTable.widthProperty().multiply(0.10) // 10%
        );

        longestWordCol.prefWidthProperty().bind(
                statsTable.widthProperty().multiply(0.165) // 16.5%
        );

        shortestWordCol.prefWidthProperty().bind(
                statsTable.widthProperty().multiply(0.135) // 13.5%
        );

        statusCol.prefWidthProperty().bind(
                statsTable.widthProperty().multiply(0.15) // 10%
        );
    }

    @FXML
    public void initialize() {
        FileSearchEngine searchEngine = FileSearchEngine.getInstance();
        statusLabel.textProperty().bind(searchEngine.statusProperty().asString());
        threadCountLabel.textProperty().bind(searchEngine.activeThreadsProperty().asString());
        filesProcessedLabel.textProperty().bind(
            searchEngine.fileProcessedProperty().asString().concat(" / ").concat(searchEngine.totalFilesProperty().asString())
        );
        includeSubdirsCheck.selectedProperty().bindBidirectional(searchEngine.includeSubdirectoriesProperty());
        System.out.println("Controller initialized");
        makeTableResponsive();

        // Configure column mappings - tells each column which getter to call
        fileNameCol.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        wordCountCol.setCellValueFactory(new PropertyValueFactory<>("wordCount"));
        isCountCol.setCellValueFactory(new PropertyValueFactory<>("isCount"));
        areCountCol.setCellValueFactory(new PropertyValueFactory<>("areCount"));
        youCountCol.setCellValueFactory(new PropertyValueFactory<>("youCount"));
        longestWordCol.setCellValueFactory(new PropertyValueFactory<>("longestWord"));
        shortestWordCol.setCellValueFactory(new PropertyValueFactory<>("shortestWord"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(column -> new TableCell<FileStatistics, String>() {
            private final HBox container = new HBox();
            private final Label badge = new Label();
            {
                // Configure badge
                badge.setAlignment(Pos.CENTER);
                badge.setMaxWidth(Double.MAX_VALUE);

                container.getStyleClass().add("table-cell");
                container.setBorder(Border.EMPTY);
                // Configure container
                container.getChildren().add(badge);
            }

            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);

                // Clear previous styling
                badge.getStyleClass().clear();

                if (empty || status == null) {
                    setGraphic(null);
                } else {
                    badge.setText(status);

                    // Add CSS class based on status value
                    switch (status.toLowerCase()) {
                        case "pending" -> badge.getStyleClass().add("status-badge-pending");
                        case "processing" -> badge.getStyleClass().add("status-badge-processing");
                        case "completed" -> badge.getStyleClass().add("status-badge-completed");
                    }
                    setGraphic(container);

                    // Add base badge class for common styling
                    if (!badge.getStyleClass().contains("status-badge")) {
                        badge.getStyleClass().add("status-badge");
                    }

                }
            }
        });

    }

    // Data class with getters that PropertyValueFactory will call
    // Data class with getters that PropertyValueFactory will call
    public static class FileStatistics {
        private final String fileName;
        private final int wordCount;
        private final int isCount;
        private final int areCount;
        private final int youCount;
        private final String longestWord;
        private final String shortestWord;
        private final String status;

        public FileStatistics(String fileName, int wordCount, int isCount, int areCount,
                int youCount, String longestWord, String shortestWord, String status) {
            this.fileName = fileName;
            this.wordCount = wordCount;
            this.isCount = isCount;
            this.areCount = areCount;
            this.youCount = youCount;
            this.longestWord = longestWord;
            this.shortestWord = shortestWord;
            this.status = status;
        }

        // These getters are called by PropertyValueFactory!
        public String getFileName() {
            return fileName;
        }

        public int getWordCount() {
            return wordCount;
        }

        public int getIsCount() {
            return isCount;
        }

        public int getAreCount() {
            return areCount;
        }

        public int getYouCount() {
            return youCount;
        }

        public String getLongestWord() {
            return longestWord;
        }

        public String getShortestWord() {
            return shortestWord;
        }

        public String getStatus() {
            return status;
        }
    }
}
