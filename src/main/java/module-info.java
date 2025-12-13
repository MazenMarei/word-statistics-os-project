module word_statistics {
    requires javafx.controls;
    requires javafx.fxml;

    // Export packages
    exports word_statistics;
    exports word_statistics.controller;
    // exports word_statistics.model;
    // exports word_statistics.service;

    // open controllers to allow FXML loading
    opens word_statistics.controller to javafx.fxml;
}
