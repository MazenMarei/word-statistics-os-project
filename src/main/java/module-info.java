module word_statistics {
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.base;

    // Export packages
    exports word_statistics;
    exports word_statistics.controller;
    exports word_statistics.model;

    // open controllers to allow FXML loading
    opens word_statistics.controller to javafx.fxml;
    opens word_statistics.model to javafx.base;
}
