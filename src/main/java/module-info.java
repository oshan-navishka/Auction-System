module lk.ijse.realtimeactionprojects {
    requires javafx.controls;
    requires javafx.fxml;

    opens lk.ijse.realtimeactionprojects.controller to javafx.fxml;
    opens lk.ijse.realtimeactionprojects to javafx.graphics, javafx.fxml;

    exports lk.ijse.realtimeactionprojects;
    exports lk.ijse.realtimeactionprojects.controller;
    exports lk.ijse.realtimeactionprojects.model;
    exports lk.ijse.realtimeactionprojects.util;
}