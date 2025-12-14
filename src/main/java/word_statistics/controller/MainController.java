package word_statistics.controller;

import java.io.IOException;

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
import word_statistics.core.FileSearch;
import word_statistics.core.FileSearchEngine;
import word_statistics.model.AppStatus;
import word_statistics.model.FileModel;
import word_statistics.model.FileStatus;

public class MainController {

    @FXML
    private TableView<FileModel> statsTable;

    @FXML
    private TableColumn<FileModel, String> fileNameCol;

    @FXML
    private TableColumn<FileModel, Integer> wordCountCol;

    @FXML
    private TableColumn<FileModel, Integer> isCountCol;

    @FXML
    private TableColumn<FileModel, Integer> areCountCol;

    @FXML
    private TableColumn<FileModel, Integer> youCountCol;

    @FXML
    private TableColumn<FileModel, String> longestWordCol;

    @FXML
    private TableColumn<FileModel, String> shortestWordCol;

    @FXML
    private TableColumn<FileModel, FileStatus> statusCol;

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

    // Labels for directory-level statistics
    @FXML
    private Label totalWordsLabel;

    @FXML
    private Label totalIsLabel;

    @FXML
    private Label totalAreLabel;

    @FXML
    private Label totalYouLabel;

    @FXML
    private Label ShortestLabel;

    @FXML
    private Label LongestLabel;

    @FXML
    public void initialize() {
        FileSearchEngine searchEngine = FileSearchEngine.getInstance();

        // Bind status labels to FileSearchEngine properties for real-time updates
        statusLabel
                .textProperty()
                .bind(searchEngine.statusProperty().asString());
        threadCountLabel
                .textProperty()
                .bind(
                        searchEngine
                                .activeThreadsProperty()
                                .asString()
                                .concat(" / ")
                                .concat(searchEngine.availableThreadsProperty().asString()));
        filesProcessedLabel
                .textProperty()
                .bind(
                        searchEngine
                                .fileProcessedProperty()
                                .asString()
                                .concat(" / ")
                                .concat(searchEngine.totalFilesProperty().asString()));

        // Bind checkbox to search engine property (bidirectional)
        includeSubdirsCheck
                .selectedProperty()
                .bindBidirectional(searchEngine.includeSubdirectoriesProperty());

        System.out.println("Controller initialized");
        makeTableResponsive();

        // Configure column mappings - tells each column which getter to call
        fileNameCol.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        wordCountCol.setCellValueFactory(
                new PropertyValueFactory<>("wordCount"));
        isCountCol.setCellValueFactory(new PropertyValueFactory<>("isCount"));
        areCountCol.setCellValueFactory(new PropertyValueFactory<>("areCount"));
        youCountCol.setCellValueFactory(new PropertyValueFactory<>("youCount"));
        longestWordCol.setCellValueFactory(
                new PropertyValueFactory<>("longestWord"));
        shortestWordCol.setCellValueFactory(
                new PropertyValueFactory<>("shortestWord"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Custom cell factory for status column - displays colored badges
        statusCol.setCellFactory(column -> new TableCell<FileModel, FileStatus>() {
            private final HBox container = new HBox();
            private final Label badge = new Label();

            {
                // Configure badge styling
                badge.setAlignment(Pos.CENTER);
                badge.setMaxWidth(Double.MAX_VALUE);
                container.getStyleClass().add("table-cell");
                container.setBorder(Border.EMPTY);
                container.getChildren().add(badge);
            }

            @Override
            protected void updateItem(FileStatus status, boolean empty) {
                super.updateItem(status, empty);
                badge.getStyleClass().clear();

                if (empty || status == null) {
                    setGraphic(null);
                } else {
                    badge.setText(status.toString());

                    // Add CSS class based on status value
                    switch (status) {
                        case PENDING -> badge
                                .getStyleClass()
                                .add("status-badge-pending");
                        case PROCESSING -> badge
                                .getStyleClass()
                                .add("status-badge-processing");
                        case COMPLETED -> badge
                                .getStyleClass()
                                .add("status-badge-completed");
                        case STOPPED -> badge
                                .getStyleClass()
                                .add("status-badge-stopped");
                        case ERROR -> badge
                                .getStyleClass()
                                .add("status-badge-error");
                    }

                    // Add base badge class for common styling
                    if (!badge.getStyleClass().contains("status-badge")) {
                        badge.getStyleClass().add("status-badge");
                    }

                    setGraphic(container);
                }
            }
        });
    }

    // Button Action Handlers
    @FXML
    public void onIncludeSubdirsCheckToggle() {
        FileSearchEngine searchEngine = FileSearchEngine.getInstance();
        if (searchEngine.getStatus() == AppStatus.RUNNING) {
            includeSubdirsCheck.setDisable(true);
            return;
        }
        searchEngine
                .includeSubdirectoriesProperty()
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
        directoryChooser.setTitle("Select Directory to Analyze");

        var selectedDirectory = directoryChooser.showDialog(
                directoryPathField.getScene().getWindow());
        if (selectedDirectory != null) {
            directoryPathField.setText(selectedDirectory.getAbsolutePath());
            searchEngine
                    .currentDirectoryProperty()
                    .set(selectedDirectory.getAbsolutePath());
            try {
                searchEngine.setTotalFiles(
                        FileSearch.searchTextFiles(selectedDirectory.toString(), includeSubdirsCheck.isSelected())
                                .size());
            } catch (IOException e) {
            }
        }

    }

    @FXML
    public void onStopButtonClick() {
        FileSearchEngine searchEngine = FileSearchEngine.getInstance();
        searchEngine.stopEngine();

        analyzeBtn.setDisable(false);
        browseBtn.setDisable(false);
        includeSubdirsCheck.setDisable(false);
        stopBtn.setDisable(true);
    }

    @FXML
    public void onAnalyzeButtonClick() {
        FileSearchEngine searchEngine = FileSearchEngine.getInstance();

        if (searchEngine.getCurrentDirectory() == null ||
                searchEngine.getCurrentDirectory().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("No Directory Selected");
            alert.setContentText(
                    "Please select a directory to start analysis.");
            alert.showAndWait();
            return;
        }

        if (searchEngine.getStatus() == AppStatus.RUNNING) {
            return;
        }

        searchEngine.setController(this);

        searchEngine.setStatus(AppStatus.RUNNING);
        analyzeBtn.setDisable(true);
        browseBtn.setDisable(true);
        includeSubdirsCheck.setDisable(true);
        stopBtn.setDisable(false);

        searchEngine.setTotalFiles(0);
        searchEngine.setFileProcessed(0);

        // Start the file search and analysis engine
        searchEngine.startEngine();
    }

    public void addFileStatistics(FileModel fileModel) {
        statsTable.getItems().add(fileModel);
    }

    public void updateFileStatistics(int index, FileModel fileModel) {
        if (index >= 0 && index < statsTable.getItems().size()) {
            statsTable.getItems().set(index, fileModel);
        }
    }

    public void clearResults() {
        statsTable.getItems().clear();
        totalWordsLabel.setText("0");
        totalIsLabel.setText("0");
        totalAreLabel.setText("0");
        totalYouLabel.setText("0");
        ShortestLabel.setText("N/A");
        LongestLabel.setText("N/A");
    }

    public void updateDirectoryStatistics(
            int totalWords,
            int totalIs,
            int totalAre,
            int totalYou,
            String longest,
            String shortest) {
        totalWordsLabel.setText(String.valueOf(totalWords));
        totalIsLabel.setText(String.valueOf(totalIs));
        totalAreLabel.setText(String.valueOf(totalAre));
        totalYouLabel.setText(String.valueOf(totalYou));
        LongestLabel.setText(longest.isEmpty() ? "N/A" : longest);
        ShortestLabel.setText(shortest.isEmpty() ? "N/A" : shortest);
    }

    public void enableControls() {
        analyzeBtn.setDisable(false);
        browseBtn.setDisable(false);
        includeSubdirsCheck.setDisable(false);
        stopBtn.setDisable(true);
    }

    private void makeTableResponsive() {
        fileNameCol
                .prefWidthProperty()
                .bind(statsTable.widthProperty().multiply(0.15));
        wordCountCol
                .prefWidthProperty()
                .bind(statsTable.widthProperty().multiply(0.08));
        isCountCol
                .prefWidthProperty()
                .bind(statsTable.widthProperty().multiply(0.05));
        areCountCol
                .prefWidthProperty()
                .bind(statsTable.widthProperty().multiply(0.07));
        youCountCol
                .prefWidthProperty()
                .bind(statsTable.widthProperty().multiply(0.07));
        longestWordCol
                .prefWidthProperty()
                .bind(statsTable.widthProperty().multiply(0.35));
        shortestWordCol
                .prefWidthProperty()
                .bind(statsTable.widthProperty().multiply(0.1));
        statusCol
                .prefWidthProperty()
                .bind(statsTable.widthProperty().multiply(0.125));
    }
}
