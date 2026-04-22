module com.app {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.connectcolor to javafx.fxml;
    exports com.connectcolor;
}
