package ui;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import nfc.SmartMifareReader;
import nfc.SmartMifareWriter;
// import db.DatabaseHelper;
// import model.AttendanceRecord;

public class MainUI extends Application {

    private Label statusLabel = new Label("Place NFC card to mark attendance...");
    // private TableView<AttendanceRecord> tableView = new TableView<>();

    @Override
    public void start(Stage stage) {
        // Buttons
        Button scanBtn = new Button("Scan NFC Card");
        scanBtn.setOnAction(e -> handleScan());

        Button refreshBtn = new Button("Refresh Records");
        // refreshBtn.setOnAction(e -> loadTable());

        // --- Writing Form Fields ---
        TextField fullNameField = new TextField();
        fullNameField.setPromptText("Full Name");

        TextField bsgUIDField = new TextField();
        bsgUIDField.setPromptText("BSGUID");

        TextField participationTypeField = new TextField();
        participationTypeField.setPromptText("Participation Type");

        TextField bsgDistrictField = new TextField();
        bsgDistrictField.setPromptText("BSG District");

        TextField emailField = new TextField();
        emailField.setPromptText("Email");

        TextField phoneField = new TextField();
        phoneField.setPromptText("Phone Number");

        TextField bsgStateField = new TextField();
        bsgStateField.setPromptText("BSG State");

        TextField memberTypeField = new TextField();
        memberTypeField.setPromptText("Member Type");

        TextField unitNameField = new TextField();
        unitNameField.setPromptText("Unit Name");

        TextField rankSectionField = new TextField();
        rankSectionField.setPromptText("Rank / Section");

        DatePicker dobPicker = new DatePicker();
        dobPicker.setPromptText("Date of Birth");

        TextField ageField = new TextField();
        ageField.setPromptText("Age");

        Button writeBtn = new Button("Write Data to Card");
        writeBtn.setOnAction(e -> {
            try {
                // Collect data from fields
                ObservableList<String> dataList = FXCollections.observableArrayList(
                        fullNameField.getText().trim(),
                        bsgUIDField.getText().trim(),
                        participationTypeField.getText().trim(),
                        bsgDistrictField.getText().trim(),
                        emailField.getText().trim(),
                        phoneField.getText().trim(),
                        bsgStateField.getText().trim(),
                        memberTypeField.getText().trim(),
                        unitNameField.getText().trim(),
                        rankSectionField.getText().trim(),
                        dobPicker.getValue() != null ? dobPicker.getValue().toString() : "",
                        ageField.getText().trim());

                // Write each field to next available block on the NFC card
                for (String data : dataList) {
                    if (!data.isEmpty()) {
                        SmartMifareWriter.writeNextAvailableBlock(data);
                    }
                }

                statusLabel.setText("✅ Data successfully written to card.");
                // Clear fields after writing
                fullNameField.clear();
                bsgUIDField.clear();
                participationTypeField.clear();
                bsgDistrictField.clear();
                emailField.clear();
                phoneField.clear();
                bsgStateField.clear();
                memberTypeField.clear();
                unitNameField.clear();
                rankSectionField.clear();
                dobPicker.setValue(null);
                ageField.clear();

            } catch (Exception ex) {
                statusLabel.setText("❌ Error writing: " + ex.getMessage());
            }
        });

        // Layout for writing form
        GridPane formGrid = new GridPane();
        formGrid.setPadding(new Insets(10));
        formGrid.setVgap(8);
        formGrid.setHgap(10);

        formGrid.addRow(0, new Label("Full Name:"), fullNameField);
        formGrid.addRow(1, new Label("BSGUID:"), bsgUIDField);
        formGrid.addRow(2, new Label("Participation Type:"), participationTypeField);
        formGrid.addRow(3, new Label("BSG District:"), bsgDistrictField);
        formGrid.addRow(4, new Label("Email:"), emailField);
        formGrid.addRow(5, new Label("Phone Number:"), phoneField);
        formGrid.addRow(6, new Label("BSG State:"), bsgStateField);
        formGrid.addRow(7, new Label("Member Type:"), memberTypeField);
        formGrid.addRow(8, new Label("Unit Name:"), unitNameField);
        formGrid.addRow(9, new Label("Rank / Section:"), rankSectionField);
        formGrid.addRow(10, new Label("Date of Birth:"), dobPicker);
        formGrid.addRow(11, new Label("Age:"), ageField);
        formGrid.add(writeBtn, 1, 12);

        // Table setup
        // TableColumn<AttendanceRecord, String> uidCol = new TableColumn<>("Card UID");
        // uidCol.setCellValueFactory(cell -> cell.getValue().uidProperty());

        // TableColumn<AttendanceRecord, String> timeCol = new TableColumn<>("Time");
        // timeCol.setCellValueFactory(cell -> cell.getValue().timeProperty());

        // tableView.getColumns().addAll(uidCol, timeCol);
        // tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        VBox root = new VBox(15, scanBtn, refreshBtn, statusLabel, formGrid);
        root.setPadding(new Insets(20));

        stage.setScene(new Scene(root, 600, 700));
        stage.setTitle("NFC Attendance System");
        stage.show();

        // // Initialize database and load table
        // DatabaseHelper.initDB();
        // loadTable();
    }

    private void handleScan() {
        try {
            String uid = SmartMifareReader.readUID();
            if (uid != null && !uid.isEmpty()) {
                // DatabaseHelper.insertAttendance(uid);
                statusLabel.setText("✅ Attendance marked for card: " + uid);
                // loadTable();
            } else {
                statusLabel.setText("❌ No card detected");
            }
        } catch (Exception ex) {
            statusLabel.setText("⚠️ Error: " + ex.getMessage());
        }
    }

    // private void loadTable() {
    // tableView.getItems().setAll(DatabaseHelper.getAllRecords());
    // }

}
