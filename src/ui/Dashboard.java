package ui;

import java.util.Map;

import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.util.Duration;
import nfc.SmartMifareReader;
import javafx.scene.Node;
import javafx.scene.paint.Color;

public class Dashboard extends BorderPane {

    private final StackPane contentArea = new StackPane();

    public Dashboard() {
        // --- Buttons ---
        Button attendanceBtn = new Button("Attendance");
        Button entryFormBtn = new Button("Entry Form");
        Button autoUploadBtn = new Button("Auto Upload");
        Button reportBtn = new Button("Report");

        // --- Common Button Style ---
        String btnStyle = """
                    -fx-background-color: #2ECC71;
                    -fx-text-fill: white;
                    -fx-font-weight: bold;
                    -fx-font-size: 14px;
                    -fx-background-radius: 10;
                    -fx-padding: 10 16;
                    -fx-cursor: hand;
                """;

        String hoverStyle = """
                    -fx-background-color: #27AE60;
                    -fx-text-fill: white;
                    -fx-font-weight: bold;
                    -fx-font-size: 14px;
                    -fx-background-radius: 10;
                    -fx-padding: 10 16;
                    -fx-cursor: hand;
                """;

        attendanceBtn.setStyle(btnStyle);
        entryFormBtn.setStyle(btnStyle);
        autoUploadBtn.setStyle(btnStyle);
        reportBtn.setStyle(btnStyle);

        // --- Hover Animations ---
        for (Button btn : new Button[] { attendanceBtn, entryFormBtn, autoUploadBtn, reportBtn }) {
            btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
            btn.setOnMouseExited(e -> btn.setStyle(btnStyle));
        }

        // --- Navbar Layout ---
        HBox navBar = new HBox(15, attendanceBtn, entryFormBtn, autoUploadBtn, reportBtn);
        navBar.setPadding(new Insets(10));
        navBar.setStyle("-fx-background-color: #34495E; -fx-alignment: center;");

        // --- Make Buttons Expand Evenly ---
        for (Button btn : new Button[] { attendanceBtn, entryFormBtn, autoUploadBtn, reportBtn }) {
            HBox.setHgrow(btn, Priority.ALWAYS);
            btn.setMaxWidth(Double.MAX_VALUE);
        }

        // --- Default Content ---
        contentArea.setPadding(new Insets(20));
        setContent("Welcome to Attendance System");

        // --- Add Components to Layout ---
        setTop(navBar);
        setCenter(contentArea);

        // --- Button Actions (with animation) ---
        attendanceBtn.setOnAction(e -> {
            Label status = new Label("📡 Waiting for card...");
            status.setStyle("-fx-font-size: 16px; -fx-text-fill: #2980B9;");
            setContent(status);

            new Thread(() -> {
                Map<String, String> data = SmartMifareReader.readCardData();

                javafx.application.Platform.runLater(() -> {
                    if ("ok".equals(data.get("status"))) {
                        Label uidLabel = new Label("Card UID: " + data.get("uid"));
                        uidLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: green;");
                        setContent(uidLabel);
                        System.out.println("Scanned UID: " + data.get("uid"));
                    } else {
                        Label err = new Label("Error: " + data.get("error"));
                        err.setStyle("-fx-text-fill: red;");
                        setContent(err);
                    }
                });
            }).start();
        });

        entryFormBtn.setOnAction(e -> setContent("📝 Entry Form Page"));
        autoUploadBtn.setOnAction(e -> setContent("⏫ Auto Upload Page"));
        reportBtn.setOnAction(e -> setContent("📊 Report Page"));
    }

    // --- Helper Method for Page Switching with Animation ---
    private void setContent(Node node) {
        contentArea.getChildren().setAll(node);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(400), node);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    private void setContent(String text) {
        Text newText = new Text(text);
        newText.setStyle("-fx-font-size: 18px; -fx-fill: #2C3E50;");
        setContent(newText);
    }

}
