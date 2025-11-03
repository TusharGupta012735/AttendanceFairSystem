package ui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import db.AccessDb; // the helper class above
import nfc.SmartMifareWriter;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.util.Duration;
import nfc.SmartMifareReader;
import javafx.scene.Node;

public class Dashboard extends BorderPane {

    private final StackPane contentArea = new StackPane();

    public Dashboard() {
        // --- Buttons ---
        Button attendanceBtn = new Button("Attendance");
        Button entryFormBtn = new Button("Entry Form");
        Button infoBtn = new Button("Information");
        Button autoUploadBtn = new Button("Auto Upload");
        Button reportBtn = new Button("Report");

        // --- Common Button Style ---
        String btnStyle = """
                    -fx-background-color: #1976d2;
                    -fx-text-fill: white;
                    -fx-font-weight: bold;
                    -fx-font-size: 15px;
                    -fx-background-radius: 8;
                    -fx-padding: 12 20;
                    -fx-cursor: hand;
                    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 1);
                """;

        String hoverStyle = """
                    -fx-background-color: #2196f3;
                    -fx-text-fill: white;
                    -fx-font-weight: bold;
                    -fx-font-size: 15px;
                    -fx-background-radius: 8;
                    -fx-padding: 12 20;
                    -fx-cursor: hand;
                    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 6, 0, 0, 2);
                """;

        attendanceBtn.setStyle(btnStyle);
        entryFormBtn.setStyle(btnStyle);
        autoUploadBtn.setStyle(btnStyle);
        reportBtn.setStyle(btnStyle);
        infoBtn.setStyle(btnStyle);

        // --- Hover Animations ---
        for (Button btn : new Button[] { attendanceBtn, entryFormBtn, autoUploadBtn, reportBtn, infoBtn }) {
            btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
            btn.setOnMouseExited(e -> btn.setStyle(btnStyle));
        }

        // --- Navbar Layout ---
        HBox navBar = new HBox(20, attendanceBtn, entryFormBtn, autoUploadBtn, reportBtn, infoBtn);
        navBar.setPadding(new Insets(15, 20, 15, 20));
        navBar.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #1565c0, #0d47a1); -fx-alignment: center; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 2);");

        // --- Make Buttons Expand Evenly ---
        for (Button btn : new Button[] { attendanceBtn, entryFormBtn, autoUploadBtn, reportBtn, infoBtn }) {
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
        attendanceBtn.setOnAction(e -> setContent(AttendancePage.create()));

        entryFormBtn.setOnAction(e -> {
            Parent form = EntryForm.create((formData, done) -> {
                // Run on background thread
                new Thread(() -> {
                    long dbId = -1;
                    try {
                        System.out.println("\n[DEBUG] Starting form submission process...");
                        System.out.println("[DEBUG] Form data: " + formData);

                        // 1) Insert to DB first
                        try {
                            System.out.println("[DEBUG] Attempting database insert...");
                            dbId = AccessDb.insertAttendee(formData, null); // no card UID
                            System.out.println("[DEBUG] Database insert successful, id=" + dbId);
                        } catch (Exception dbEx) {
                            final String msg = "DB insert failed: " + dbEx.getMessage();
                            System.err.println("[ERROR] " + msg);
                            Platform.runLater(() -> {
                                Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
                                alert.setHeaderText(null);
                                alert.showAndWait();
                            });
                            return;
                        }

                        // 2) Build CSV / text to write to NFC
                        System.out.println("[DEBUG] Building CSV data for card...");
                        String textToWrite = formData.get("__CSV__");
                        if (textToWrite == null) {
                            // fallback to joining all values (preserve field order used by EntryForm)
                            textToWrite = formData.values().stream()
                                    .map(v -> v == null ? "" : v.trim())
                                    .collect(Collectors.joining(","));
                        }
                        System.out.println("[DEBUG] Data to write to card: " + textToWrite);

                        // 3) Write to NFC (this may block while waiting for a card)
                        try {
                            System.out.println("[DEBUG] Attempting to write to NFC card...");
                            SmartMifareWriter.WriteResult result = SmartMifareWriter.writeText(textToWrite);
                            if (result != null) {
                                System.out.println("[DEBUG] Card write finished. Card UID: " + result.uid);
                            } else {
                                System.out.println("[WARN] Card write returned null result (no UID).");
                            }
                        } catch (Exception nfcEx) {
                            // NFC failure is non-fatal — DB row is already saved
                            final String nfcMsg = nfcEx.getMessage() == null ? nfcEx.toString() : nfcEx.getMessage();
                            System.err.println("[WARN] NFC write failed: " + nfcMsg);
                            // notify UI but keep DB insert as successful
                            Platform.runLater(() -> {
                                Alert warn = new Alert(Alert.AlertType.WARNING,
                                        "Saved to database, but writing to NFC card failed: " + nfcMsg,
                                        ButtonType.OK);
                                warn.setHeaderText(null);
                                warn.showAndWait();
                            });
                            // return here would skip the final success dialog — we continue so user sees DB
                            // success
                        }

                        // 4) Notify success on UI thread
                        final String okMsg = "Saved successfully to database.";
                        Platform.runLater(() -> {
                            Alert ok = new Alert(Alert.AlertType.INFORMATION, okMsg, ButtonType.OK);
                            ok.setHeaderText(null);
                            ok.showAndWait();
                        });

                    } catch (Exception ex) {
                        // Unexpected failure
                        final String err = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                        System.out.println("[ERROR] Operation failed: " + err);
                        ex.printStackTrace(); // Print full stack trace for debugging
                        Platform.runLater(() -> {
                            Alert a = new Alert(Alert.AlertType.ERROR, "Operation failed: " + err, ButtonType.OK);
                            a.setHeaderText(null);
                            a.showAndWait();
                        });
                    } finally {
                        // ALWAYS call done to re-enable buttons
                        if (done != null)
                            done.run();
                    }
                }, "writer-db-thread").start();
            });

            setContent(form);
        });

        infoBtn.setOnAction(e -> {
            // show immediate "waiting" UI
            Label waitLbl = new Label("📡 Present card to read info...");
            waitLbl.setStyle("""
                        -fx-font-size: 24px;
                        -fx-font-weight: bold;
                        -fx-text-fill: #1565C0;
                        -fx-background-color: #E3F2FD;
                        -fx-padding: 20 30;
                        -fx-background-radius: 10;
                        -fx-border-color: #1565C0;
                        -fx-border-radius: 10;
                        -fx-border-width: 2;
                        -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);
                    """);
            setContent(new StackPane(waitLbl));

            new Thread(() -> {
                // blocking call off the UI thread (10s timeout)
                SmartMifareReader.ReadResult rr = SmartMifareReader.readUIDWithData(10_000);

                Platform.runLater(() -> {
                    if (rr == null || rr.uid == null) {
                        Label err = new Label("❌ No card read (timed out or error).");
                        err.setStyle("""
                                    -fx-font-size: 24px;
                                    -fx-font-weight: bold;
                                    -fx-text-fill: #C62828;
                                    -fx-background-color: #FFEBEE;
                                    -fx-padding: 20 30;
                                    -fx-background-radius: 10;
                                    -fx-border-color: #C62828;
                                    -fx-border-radius: 10;
                                    -fx-border-width: 2;
                                    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);
                                """);
                        setContent(new StackPane(err));
                        return;
                    }

                    // Build a simple Key / Value grid
                    Map<String, String> map = new LinkedHashMap<>();
                    map.put("UID", rr.uid);

                    String data = (rr.data == null) ? "" : rr.data.trim();
                    if (!data.isEmpty()) {
                        // If data looks structured (contains '|' or ';' or ',' or ':'), try to parse
                        // into key:value pairs
                        boolean parsed = false;
                        String[] items;
                        if (data.contains("|")) {
                            items = data.split("\\|");
                        } else if (data.contains(";")) {
                            items = data.split(";");
                        } else if (data.contains(",")) {
                            items = data.split(",");
                        } else {
                            items = new String[] { data };
                        }

                        // try split each item into key:value if possible
                        List<String> leftovers = new ArrayList<>();
                        for (String it : items) {
                            String s = it.trim();
                            if (s.contains(":")) {
                                String[] kv = s.split(":", 2);
                                map.put(kv[0].trim(), kv[1].trim());
                                parsed = true;
                            } else if (s.contains("=")) {
                                String[] kv = s.split("=", 2);
                                map.put(kv[0].trim(), kv[1].trim());
                                parsed = true;
                            } else {
                                leftovers.add(s);
                            }
                        }
                        if (!parsed) {
                            // show whole readable data as one row
                            map.put("Readable data", data);
                        } else if (!leftovers.isEmpty()) {
                            map.put("Other data", String.join(" | ", leftovers));
                        }
                    } else {
                        map.put("Readable data", "(none)");
                    }

                    // Create GridPane (Field | Value)
                    GridPane grid = new GridPane();
                    grid.setVgap(12);
                    grid.setHgap(20);
                    grid.setPadding(new Insets(20));
                    grid.setStyle("""
                                -fx-background-color: white;
                                -fx-background-radius: 12;
                                -fx-border-radius: 12;
                                -fx-border-color: #E0E0E0;
                                -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 2);
                            """);

                    int row = 0;
                    for (Map.Entry<String, String> entry : map.entrySet()) {
                        Label k = new Label(entry.getKey() + ":");
                        k.setStyle("""
                                    -fx-font-weight: bold;
                                    -fx-font-size: 16px;
                                    -fx-text-fill: #1976D2;
                                """);
                        Label v = new Label(entry.getValue());
                        v.setStyle("""
                                    -fx-font-size: 16px;
                                    -fx-text-fill: #212121;
                                """);
                        v.setWrapText(true);
                        grid.add(k, 0, row);
                        grid.add(v, 1, row);
                        row++;
                    }

                    VBox container = new VBox(16);
                    container.setPadding(new Insets(20));
                    container.setStyle("-fx-background-color: #F5F5F5;");
                    Label header = new Label("Card Information");
                    header.setStyle("""
                                -fx-font-size: 28px;
                                -fx-font-weight: bold;
                                -fx-text-fill: #1565C0;
                            """);
                    container.getChildren().addAll(header, grid);

                    setContent(container); // your method that places the UI Node
                });
            }, "info-read-thread").start();
        });

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
        newText.setStyle("-fx-font-size: 20px; -fx-fill: #212121; -fx-font-weight: 600;");
        setContent(newText);
    }

}
