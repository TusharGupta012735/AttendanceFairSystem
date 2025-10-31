package ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import nfc.SmartMifareReader;
import db.DatabaseHelper;
import model.AttendanceRecord;

import java.time.LocalDateTime;

public class MainUI extends Application {

    private Label statusLabel = new Label("Place NFC card to mark attendance...");
    private TableView<AttendanceRecord> tableView = new TableView<>();

    @Override
    public void start(Stage stage) {
        // Buttons
        Button scanBtn = new Button("Scan NFC Card");
        scanBtn.setOnAction(e -> handleScan());

        Button refreshBtn = new Button("Refresh Records");
        refreshBtn.setOnAction(e -> loadTable());

        // Table setup
        TableColumn<AttendanceRecord, String> uidCol = new TableColumn<>("Card UID");
        uidCol.setCellValueFactory(cell -> cell.getValue().uidProperty());

        TableColumn<AttendanceRecord, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(cell -> cell.getValue().timeProperty());

        tableView.getColumns().addAll(uidCol, timeCol);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        VBox root = new VBox(10, scanBtn, refreshBtn, statusLabel, tableView);
        root.setStyle("-fx-padding: 20; -fx-alignment: center;");

        stage.setScene(new Scene(root, 500, 400));
        stage.setTitle("NFC Attendance System");
        stage.show();

        DatabaseHelper.initDB();
        loadTable();
    }

    private void handleScan() {
        try {
            String uid = SmartMifareReader.readUID(); // your NFC logic
            if (uid != null && !uid.isEmpty()) {
                DatabaseHelper.insertAttendance(uid);
                statusLabel.setText("✅ Attendance marked for card: " + uid);
                loadTable();
            } else {
                statusLabel.setText("❌ No card detected");
            }
        } catch (Exception ex) {
            statusLabel.setText("⚠️ Error: " + ex.getMessage());
        }
    }

    private void loadTable() {
        tableView.getItems().setAll(DatabaseHelper.getAllRecords());
    }
}
