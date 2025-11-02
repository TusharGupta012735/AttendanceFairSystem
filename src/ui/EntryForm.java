package ui;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Window;
import nfc.SmartMifareReader;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class EntryForm {

    public static Parent create(Consumer<Map<String, String>> onSave) {

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #ffffff, #f4f6f8);");

        Label title = new Label("📝 Entry Form");
        title.setFont(Font.font("System", FontWeight.BOLD, 24));
        title.setStyle("-fx-text-fill: #1565c0;");
        HBox header = new HBox(title);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 12, 0));

        // Base styles (will be updated dynamically)
        String baseStyleCore = "-fx-background-color: white; -fx-border-color: #bdbdbd; -fx-border-radius: 6; " +
                "-fx-background-radius: 6; -fx-padding: 8 10; -fx-text-fill: #212121;";
        String labelBaseStyleCore = "-fx-font-weight:600; -fx-text-fill:#212121;";

        // Create input controls
        TextField fullName = new TextField();
        fullName.setPromptText("Full name");
        TextField bsguid = new TextField();
        bsguid.setPromptText("BS GUID");
        ComboBox<String> participationType = new ComboBox<>();
        participationType.getItems().addAll("guide", "scout", "ranger");
        participationType.setPromptText("Select");
        TextField bsgDistrict = new TextField();
        bsgDistrict.setPromptText("District");
        TextField email = new TextField();
        email.setPromptText("example@domain.com");
        TextField phoneNumber = new TextField();
        phoneNumber.setPromptText("Phone number");
        TextField bsgState = new TextField();
        bsgState.setPromptText("State");
        TextField memberTyp = new TextField();
        memberTyp.setPromptText("Member type");
        TextField unitNam = new TextField();
        unitNam.setPromptText("Unit name");
        ComboBox<String> rank_or_section = new ComboBox<>();
        rank_or_section.getItems().addAll("guide", "scout", "ranger");
        rank_or_section.setPromptText("Select");
        DatePicker dataOfBirth = new DatePicker();
        dataOfBirth.setPromptText("Date of birth");
        TextField age = new TextField();
        age.setPromptText("Age");

        Control[] controls = new Control[] {
                fullName, bsguid, participationType, bsgDistrict,
                email, phoneNumber, bsgState, memberTyp,
                unitNam, rank_or_section, dataOfBirth, age
        };

        for (Control c : controls) {
            c.setStyle(baseStyleCore);
        }

        // Left and right grids
        GridPane left = new GridPane();
        left.setHgap(10);
        left.setVgap(14);
        GridPane right = new GridPane();
        right.setHgap(10);
        right.setVgap(14);

        addField(left, 0, "FullName", fullName);
        addField(left, 1, "BSGUID", bsguid);
        addField(left, 2, "ParticipationType", participationType);
        addField(left, 3, "bsgDistrict", bsgDistrict);
        addField(left, 4, "unitNam", unitNam);
        addField(left, 5, "dataOfBirth", dataOfBirth);

        addField(right, 0, "Email", email);
        addField(right, 1, "phoneNumber", phoneNumber);
        addField(right, 2, "bsgState", bsgState);
        addField(right, 3, "memberTyp", memberTyp);
        addField(right, 4, "rank_or_section", rank_or_section);
        addField(right, 5, "age", age);

        HBox columns = new HBox(60, left, right);
        columns.setAlignment(Pos.CENTER);
        columns.setPadding(new Insets(10));

        // Bind input widths dynamically
        DoubleBinding fieldWidthBinding = columns.widthProperty().divide(2.3);
        for (Control c : controls) {
            ((Region) c).prefWidthProperty().bind(fieldWidthBinding);
        }

        // Dynamic font size (safe version)
        DoubleBinding fontSizeBinding = Bindings.createDoubleBinding(() -> {
            double w = columns.getWidth();
            double fs = w / 70.0;
            if (fs < 12)
                fs = 12;
            if (fs > 18)
                fs = 18;
            return fs;
        }, columns.widthProperty());

        fontSizeBinding.addListener((obs, oldV, newV) -> {
            double fs = newV.doubleValue();
            left.getChildren().stream()
                    .filter(n -> n instanceof Label)
                    .forEach(n -> n.setStyle(labelBaseStyleCore + "; -fx-font-size: " + (fs - 1) + "px;"));
            right.getChildren().stream()
                    .filter(n -> n instanceof Label)
                    .forEach(n -> n.setStyle(labelBaseStyleCore + "; -fx-font-size: " + (fs - 1) + "px;"));
            for (Control c : controls) {
                c.setStyle(baseStyleCore + " -fx-font-size: " + fs + "px;");
                if (c instanceof DatePicker) {
                    DatePicker dp = (DatePicker) c;
                    if (dp.getEditor() != null)
                        dp.getEditor().setStyle("-fx-font-size:" + fs + "px;");
                }
            }
            title.setFont(Font.font("System", FontWeight.BOLD, Math.max(16, fs + 4)));
        });

        // Buttons
        Button saveBtn = new Button("Save");
        Button clearBtn = new Button("Clear");
        saveBtn.setStyle(
                "-fx-background-color:#2196f3;-fx-text-fill:white;-fx-font-weight:600;-fx-padding:8 20;-fx-background-radius:6;");
        clearBtn.setStyle(
                "-fx-background-color:#f5f5f5;-fx-text-fill:#424242;-fx-font-weight:600;-fx-padding:8 20;-fx-background-radius:6;-fx-border-color:#e0e0e0;");

        HBox buttons = new HBox(10, saveBtn, clearBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(10, 0, 0, 0));

        Label validation = new Label();
        validation.setStyle("-fx-text-fill:#b00020;-fx-font-size:12;");
        validation.setWrapText(true);

        VBox center = new VBox(header, columns, validation, buttons);
        center.setSpacing(10);
        center.setPadding(new Insets(10));
        root.setCenter(center);

        // Save action
        saveBtn.setOnAction(e -> {
            validation.setText("");
            if (fullName.getText().trim().isEmpty() || phoneNumber.getText().trim().isEmpty()) {
                validation.setText("Please fill required fields.");
                return;
            }

            Map<String, String> data = new LinkedHashMap<>();
            data.put("FullName", fullName.getText() + ",");
            data.put("BSGUID", bsguid.getText() + ",");
            data.put("ParticipationType",
                    participationType.getValue() == null ? "" : participationType.getValue() + ",");
            data.put("bsgDistrict", bsgDistrict.getText() + ",");
            data.put("Email", email.getText() + ",");
            data.put("phoneNumber", phoneNumber.getText() + ",");
            data.put("bsgState", bsgState.getText() + ",");
            data.put("memberTyp", memberTyp.getText() + ",");
            data.put("unitNam", unitNam.getText() + ",");
            data.put("rank_or_section", rank_or_section.getValue() == null ? "" : rank_or_section.getValue() + ",");
            LocalDate dobVal = dataOfBirth.getValue();
            data.put("dataOfBirth", dobVal == null ? "" : dobVal.toString() + ",");
            data.put("age", age.getText() + ",");

            if (onSave != null)
                onSave.accept(data);

            Alert ok = new Alert(Alert.AlertType.INFORMATION, "Saved successfully.", ButtonType.OK);
            ok.setHeaderText(null);
            ok.showAndWait();
        });

        // Clear action
        clearBtn.setOnAction(evt -> {
            fullName.clear();
            bsguid.clear();
            participationType.setValue(null);
            bsgDistrict.clear();
            email.clear();
            phoneNumber.clear();
            bsgState.clear();
            memberTyp.clear();
            unitNam.clear();
            rank_or_section.setValue(null);
            dataOfBirth.setValue(null);
            age.clear();
            validation.setText("");
        });

        // Force evaluate font binding once
        fontSizeBinding.getValue();

        // Start NFC auto-fill (false = don't overwrite existing fields; 1000ms poll)
        startNfcAutoFill(root,
                fullName, bsguid, participationType,
                bsgDistrict, email, phoneNumber,
                bsgState, memberTyp, unitNam,
                rank_or_section, dataOfBirth, age,
                false, // overwriteAlways
                1000);

        return root;
    }

    private static void addField(GridPane grid, int row, String labelText, Control field) {
        Label lbl = new Label(labelText + ":");
        lbl.setStyle("-fx-font-weight:600;-fx-text-fill:#212121;");
        GridPane.setConstraints(lbl, 0, row);
        GridPane.setConstraints(field, 1, row);
        grid.getChildren().addAll(lbl, field);
    }

    // ---------------- NFC auto-fill helpers ----------------
    private static ScheduledExecutorService startNfcAutoFill(
            Parent root,
            TextField fullName, TextField bsguid, ComboBox<String> participationType,
            TextField bsgDistrict, TextField email, TextField phoneNumber,
            TextField bsgState, TextField memberTyp, TextField unitNam,
            ComboBox<String> rank_or_section, DatePicker dataOfBirth, TextField age,
            boolean overwriteAlways, long pollIntervalMs) {

        ScheduledExecutorService svc = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "nfc-poller");
            t.setDaemon(true);
            return t;
        });

        AtomicReference<String> lastUid = new AtomicReference<>("");

        Runnable task = () -> {
            try {
                SmartMifareReader.ReadResult rr = SmartMifareReader.readUIDWithData(3000);
                if (rr == null || rr.uid == null || rr.uid.isEmpty())
                    return;

                String uid = rr.uid;
                String data = rr.data == null ? "" : rr.data.trim();

                // debounce: ignore if same UID processed recently
                if (uid.equals(lastUid.get()))
                    return;
                lastUid.set(uid);

                if (data.isEmpty())
                    return;

                // split preserving empty segments
                String[] parts = Arrays.stream(data.split(",", -1))
                        .map(String::trim)
                        .toArray(String[]::new);

                Platform.runLater(() -> {
                    try {
                        int i = 0;
                        setFieldFromCsv(fullName, parts, i++, overwriteAlways);
                        setFieldFromCsv(bsguid, parts, i++, overwriteAlways);
                        setComboFromCsv(participationType, parts, i++, overwriteAlways);
                        setFieldFromCsv(bsgDistrict, parts, i++, overwriteAlways);
                        setFieldFromCsv(email, parts, i++, overwriteAlways);
                        setFieldFromCsv(phoneNumber, parts, i++, overwriteAlways);
                        setFieldFromCsv(bsgState, parts, i++, overwriteAlways);
                        setFieldFromCsv(memberTyp, parts, i++, overwriteAlways);
                        setFieldFromCsv(unitNam, parts, i++, overwriteAlways);
                        setComboFromCsv(rank_or_section, parts, i++, overwriteAlways);

                        if (parts.length > i) {
                            String dobStr = parts[i++].trim();
                            if (!dobStr.isEmpty()) {
                                try {
                                    LocalDate d = LocalDate.parse(dobStr);
                                    if (overwriteAlways || dataOfBirth.getValue() == null)
                                        dataOfBirth.setValue(d);
                                } catch (Exception ex) {
                                    // ignore parse error
                                }
                            }
                        }

                        if (parts.length > i) {
                            setFieldFromCsv(age, parts, i++, overwriteAlways);
                        }

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });

            } catch (Exception ex) {
                // keep polling even if one read fails
                ex.printStackTrace();
            }
        };

        svc.scheduleWithFixedDelay(task, 0, Math.max(200, pollIntervalMs), TimeUnit.MILLISECONDS);

        // shutdown when window closes
        root.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                Window w = newScene.getWindow();
                if (w != null) {
                    w.setOnHidden(evt -> svc.shutdownNow());
                } else {
                    newScene.windowProperty().addListener((o, oldW, newW) -> {
                        if (newW != null)
                            newW.setOnHidden(e -> svc.shutdownNow());
                    });
                }
            }
        });

        return svc;
    }

    private static void setFieldFromCsv(TextField tf, String[] parts, int idx, boolean overwriteAlways) {
        if (idx < 0 || idx >= parts.length)
            return;
        String v = parts[idx];
        if (v == null)
            return;
        if (overwriteAlways || tf.getText() == null || tf.getText().isEmpty()) {
            tf.setText(v);
        }
    }

    private static void setComboFromCsv(ComboBox<String> cb, String[] parts, int idx, boolean overwriteAlways) {
        if (idx < 0 || idx >= parts.length)
            return;
        String v = parts[idx];
        if (v == null || v.isEmpty())
            return;
        if (cb.getItems().contains(v)) {
            if (overwriteAlways || cb.getValue() == null)
                cb.setValue(v);
        } else {
            // if you want to accept arbitrary values into the combo box, uncomment:
            // cb.setValue(v);
        }
    }
}