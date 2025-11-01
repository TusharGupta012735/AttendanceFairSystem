package ui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.util.Duration;
import nfc.SmartMifareReader;
import nfc.SmartMifareWriter;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.paint.Color;

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
        infoBtn.setStyle(btnStyle);

        // --- Hover Animations ---
        for (Button btn : new Button[] { attendanceBtn, entryFormBtn, autoUploadBtn, reportBtn, infoBtn }) {
            btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
            btn.setOnMouseExited(e -> btn.setStyle(btnStyle));
        }

        // --- Navbar Layout ---
        HBox navBar = new HBox(15, attendanceBtn, entryFormBtn, autoUploadBtn, reportBtn, infoBtn);
        navBar.setPadding(new Insets(10));
        navBar.setStyle("-fx-background-color: #34495E; -fx-alignment: center;");

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
        attendanceBtn.setOnAction(e -> {
            // show waiting message
            Label status = new Label("📡 Waiting for card...");
            status.setStyle("-fx-font-size: 16px; -fx-text-fill: #2980B9;");
            setContent(status);

            // read UID in a background thread
            new Thread(() -> {
                String uid = SmartMifareReader.readUID(); // <-- using your provided method
                System.out.println("Read UID: " + uid);
                // update UI on the JavaFX Application Thread
                javafx.application.Platform.runLater(() -> {
                    if (uid != null && !uid.isEmpty()) {
                        Label success = new Label("✅ Attendance marked for uuid : " + uid);
                        success.setStyle("-fx-font-size: 16px; -fx-text-fill: #27AE60;");
                        setContent(success);
                        System.out.println("Attendance marked for UID: " + uid);
                    } else {
                        Label err = new Label("❌ Error: Failed to read card UID.");
                        err.setStyle("-fx-font-size: 16px; -fx-text-fill: #E74C3C;");
                        setContent(err);
                        System.err.println("Error: Failed to read card UID.");
                    }
                });
            }).start();
        });

        entryFormBtn.setOnAction(e -> {
            Parent form = EntryForm.create(formData -> {

                new Thread(() -> {
                    try {
                        String textToWrite = formData.values().stream()
                                .filter(v -> v != null && !v.trim().isEmpty())
                                .collect(Collectors.joining(" "));

                        SmartMifareWriter.WriteResult result = SmartMifareWriter.writeText(textToWrite);
                        Platform.runLater(() -> {
                            Label success = new Label("✅ Wrote to card UID: " + result.uid +
                                    " blocks=" + result.blocks + " — saved locally.");
                            success.setStyle("-fx-font-size:16px; -fx-text-fill: #27AE60;");
                            setContent(success);
                        });

                    } catch (Exception ex) {
                        ex.printStackTrace();
                        Platform.runLater(() -> {
                            Label err = new Label("❌ Write failed: " + ex.getMessage());
                            err.setStyle("-fx-font-size:16px; -fx-text-fill: #E74C3C;");
                            setContent(err);
                        });
                    }
                }, "writer-thread").start();
            });

            setContent(form);
        });

        infoBtn.setOnAction(e -> {
            // show immediate "waiting" UI
            Label waitLbl = new Label("📡 Present card to read info...");
            waitLbl.setStyle("-fx-font-size:16px; -fx-text-fill:#2980B9;");
            setContent(new StackPane(waitLbl));

            new Thread(() -> {
                // blocking call off the UI thread (10s timeout)
                SmartMifareReader.ReadResult rr = SmartMifareReader.readUIDWithData(10_000);

                Platform.runLater(() -> {
                    if (rr == null || rr.uid == null) {
                        Label err = new Label("❌ No card read (timed out or error).");
                        err.setStyle("-fx-font-size:16px; -fx-text-fill:#E74C3C;");
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
                    grid.setVgap(8);
                    grid.setHgap(12);
                    grid.setPadding(new Insets(12));

                    int row = 0;
                    for (Map.Entry<String, String> entry : map.entrySet()) {
                        Label k = new Label(entry.getKey() + ":");
                        k.setStyle("-fx-font-weight:600;");
                        Label v = new Label(entry.getValue());
                        v.setWrapText(true);
                        grid.add(k, 0, row);
                        grid.add(v, 1, row);
                        row++;
                    }

                    VBox container = new VBox(8);
                    container.setPadding(new Insets(8));
                    Label header = new Label("Card Info");
                    header.setStyle("-fx-font-size:18px; -fx-font-weight:600;");
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
        newText.setStyle("-fx-font-size: 18px; -fx-fill: #2C3E50;");
        setContent(newText);
    }

}
