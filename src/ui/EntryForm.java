package ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class EntryForm {

    /**
     * Create the entry form UI.
     * 
     * @param onSave callback invoked with a Map<String,String> of field -> value
     *               when Save is pressed and validation passes.
     * @return Parent node (you can add to scene, or use in your setContent(...)
     *         method)
     */
    public static Parent create(Consumer<Map<String, String>> onSave) {
        // Root container
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #ffffff, #f0f4f8);");

        // Header
        Label title = new Label("📝 Entry Form");
        title.setFont(Font.font("System", FontWeight.BOLD, 22));
        HBox header = new HBox(title);
        header.setPadding(new Insets(0, 0, 12, 0));
        header.setAlignment(Pos.CENTER_LEFT);

        // Create a Grid for labels & fields (2 columns)
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(10);
        grid.setPadding(new Insets(6, 6, 6, 6));

        // Helper to add label+field to grid
        final int[] row = { 0 };
        java.util.function.BiConsumer<String, Node> addRow = (labelText, node) -> {
            Label lbl = new Label(labelText);
            lbl.setMinWidth(150);
            lbl.setWrapText(true);
            lbl.setStyle("-fx-font-weight: 600; -fx-text-fill: #2c3e50;");
            GridPane.setConstraints(lbl, 0, row[0]);
            GridPane.setConstraints(node, 1, row[0]);
            grid.getChildren().addAll(lbl, node);
            row[0]++;
        };

        // Common style for all text inputs
        String textFieldStyle = "-fx-background-color: white;" +
                "-fx-border-color: #e0e0e0;" +
                "-fx-border-radius: 4;" +
                "-fx-background-radius: 4;" +
                "-fx-padding: 8;" +
                "-fx-prompt-text-fill: #9e9e9e;" +
                "-fx-font-size: 13;";

        String textFieldHoverStyle = textFieldStyle +
                "-fx-border-color: #2196f3;" +
                "-fx-effect: dropshadow(gaussian, rgba(33,150,243,0.1), 8, 0, 0, 0);";

        // Fields (use TextField, DatePicker, ComboBox, TextArea as appropriate)
        TextField m_uid = new TextField();
        m_uid.setPromptText("Auto or enter UID");
        m_uid.setStyle(textFieldStyle);
        m_uid.setOnMouseEntered(e -> m_uid.setStyle(textFieldHoverStyle));
        m_uid.setOnMouseExited(e -> m_uid.setStyle(textFieldStyle));

        TextField m_name = new TextField();
        m_name.setPromptText("Full name");
        m_name.setStyle(textFieldStyle);
        m_name.setOnMouseEntered(e -> m_name.setStyle(textFieldHoverStyle));
        m_name.setOnMouseExited(e -> m_name.setStyle(textFieldStyle));

        TextField m_mobile = new TextField();
        m_mobile.setPromptText("10-digit mobile");
        m_mobile.setStyle(textFieldStyle);
        m_mobile.setOnMouseEntered(e -> m_mobile.setStyle(textFieldHoverStyle));
        m_mobile.setOnMouseExited(e -> m_mobile.setStyle(textFieldStyle));

        TextField m_aadhar = new TextField();
        m_aadhar.setPromptText("Aadhar number");
        m_aadhar.setStyle(textFieldStyle);
        m_aadhar.setOnMouseEntered(e -> m_aadhar.setStyle(textFieldHoverStyle));
        m_aadhar.setOnMouseExited(e -> m_aadhar.setStyle(textFieldStyle));

        DatePicker m_dob = new DatePicker();
        m_dob.setPromptText("Date of birth");
        m_dob.setStyle(textFieldStyle);
        m_dob.getEditor().setStyle(textFieldStyle);

        ComboBox<String> m_bloodgroup = new ComboBox<>();
        m_bloodgroup.getItems().addAll("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-");
        m_bloodgroup.setEditable(false);
        m_bloodgroup.setPromptText("Select");
        m_bloodgroup.setStyle(textFieldStyle + "-fx-background-color: white;");
        m_bloodgroup
                .setOnMouseEntered(e -> m_bloodgroup.setStyle(textFieldHoverStyle + "-fx-background-color: white;"));
        m_bloodgroup.setOnMouseExited(e -> m_bloodgroup.setStyle(textFieldStyle + "-fx-background-color: white;"));

        TextField m_father = new TextField();
        m_father.setStyle(textFieldStyle);
        m_father.setOnMouseEntered(e -> m_father.setStyle(textFieldHoverStyle));
        m_father.setOnMouseExited(e -> m_father.setStyle(textFieldStyle));

        TextField m_mother = new TextField();
        m_mother.setStyle(textFieldStyle);
        m_mother.setOnMouseEntered(e -> m_mother.setStyle(textFieldHoverStyle));
        m_mother.setOnMouseExited(e -> m_mother.setStyle(textFieldStyle));

        TextField m_email = new TextField();
        m_email.setPromptText("example@domain.com");
        m_email.setStyle(textFieldStyle);
        m_email.setOnMouseEntered(e -> m_email.setStyle(textFieldHoverStyle));
        m_email.setOnMouseExited(e -> m_email.setStyle(textFieldStyle));

        TextArea m_address = new TextArea();
        m_address.setPrefRowCount(3);
        m_address.setWrapText(true);
        m_address.setStyle(textFieldStyle);
        m_address.setOnMouseEntered(e -> m_address.setStyle(textFieldHoverStyle));
        m_address.setOnMouseExited(e -> m_address.setStyle(textFieldStyle));

        TextField m_state = new TextField();
        m_state.setStyle(textFieldStyle);
        m_state.setOnMouseEntered(e -> m_state.setStyle(textFieldHoverStyle));
        m_state.setOnMouseExited(e -> m_state.setStyle(textFieldStyle));

        TextField m_pincode = new TextField();
        m_pincode.setStyle(textFieldStyle);
        m_pincode.setOnMouseEntered(e -> m_pincode.setStyle(textFieldHoverStyle));
        m_pincode.setOnMouseExited(e -> m_pincode.setStyle(textFieldStyle));

        TextField m_district = new TextField();
        m_district.setStyle(textFieldStyle);
        m_district.setOnMouseEntered(e -> m_district.setStyle(textFieldHoverStyle));
        m_district.setOnMouseExited(e -> m_district.setStyle(textFieldStyle));

        TextField m_category = new TextField();
        m_category.setStyle(textFieldStyle);
        m_category.setOnMouseEntered(e -> m_category.setStyle(textFieldHoverStyle));
        m_category.setOnMouseExited(e -> m_category.setStyle(textFieldStyle));

        TextField m_rank = new TextField();
        m_rank.setStyle(textFieldStyle);
        m_rank.setOnMouseEntered(e -> m_rank.setStyle(textFieldHoverStyle));
        m_rank.setOnMouseExited(e -> m_rank.setStyle(textFieldStyle));

        TextField m_econtactperson = new TextField();
        m_econtactperson.setStyle(textFieldStyle);
        m_econtactperson.setOnMouseEntered(e -> m_econtactperson.setStyle(textFieldHoverStyle));
        m_econtactperson.setOnMouseExited(e -> m_econtactperson.setStyle(textFieldStyle));

        TextField m_econtactmobile = new TextField();
        m_econtactmobile.setStyle(textFieldStyle);
        m_econtactmobile.setOnMouseEntered(e -> m_econtactmobile.setStyle(textFieldHoverStyle));
        m_econtactmobile.setOnMouseExited(e -> m_econtactmobile.setStyle(textFieldStyle));

        TextField m_erelation = new TextField();
        m_erelation.setStyle(textFieldStyle);
        m_erelation.setOnMouseEntered(e -> m_erelation.setStyle(textFieldHoverStyle));
        m_erelation.setOnMouseExited(e -> m_erelation.setStyle(textFieldStyle));

        TextField m_bsguid = new TextField();
        m_bsguid.setStyle(textFieldStyle);
        m_bsguid.setOnMouseEntered(e -> m_bsguid.setStyle(textFieldHoverStyle));
        m_bsguid.setOnMouseExited(e -> m_bsguid.setStyle(textFieldStyle));

        TextField m_age = new TextField();
        m_age.setPromptText("Numeric age");
        m_age.setStyle(textFieldStyle);
        m_age.setOnMouseEntered(e -> m_age.setStyle(textFieldHoverStyle));
        m_age.setOnMouseExited(e -> m_age.setStyle(textFieldStyle));

        // Add rows
        addRow.accept("UID:", m_uid);
        addRow.accept("Name:", m_name);
        addRow.accept("Mobile:", m_mobile);
        addRow.accept("Aadhar:", m_aadhar);
        addRow.accept("DOB:", m_dob);
        addRow.accept("Blood Group:", m_bloodgroup);
        addRow.accept("Father's Name:", m_father);
        addRow.accept("Mother's Name:", m_mother);
        addRow.accept("Email:", m_email);
        addRow.accept("Address:", m_address);
        addRow.accept("State:", m_state);
        addRow.accept("Pincode:", m_pincode);
        addRow.accept("District:", m_district);
        addRow.accept("Category:", m_category);
        addRow.accept("Rank:", m_rank);
        addRow.accept("Emergency Contact Person:", m_econtactperson);
        addRow.accept("Emergency Contact Mobile:", m_econtactmobile);
        addRow.accept("Emergency Relation:", m_erelation);
        addRow.accept("BS GUID:", m_bsguid);
        addRow.accept("Age:", m_age);

        // Put grid inside a scrollable pane for small windows
        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setStyle(
                "-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");
        scroll.setPrefViewportHeight(520);

        // Action buttons
        String btnBaseStyle = "-fx-background-radius: 6; -fx-padding: 10 20 10 20; -fx-font-weight: 600; -fx-font-size: 13;";

        Button saveBtn = new Button("Save");
        saveBtn.setDefaultButton(true);
        saveBtn.setStyle(btnBaseStyle +
                "-fx-background-color: #2196f3; -fx-text-fill: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 1);");
        saveBtn.setOnMouseEntered(e -> saveBtn.setStyle(btnBaseStyle +
                "-fx-background-color: #1976d2; -fx-text-fill: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 6, 0, 0, 2);"));
        saveBtn.setOnMouseExited(e -> saveBtn.setStyle(btnBaseStyle +
                "-fx-background-color: #2196f3; -fx-text-fill: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 1);"));

        Button clearBtn = new Button("Clear");
        clearBtn.setStyle(btnBaseStyle +
                "-fx-background-color: #f5f5f5; -fx-text-fill: #424242; -fx-border-color: #e0e0e0; -fx-border-radius: 6;");
        clearBtn.setOnMouseEntered(e -> clearBtn.setStyle(btnBaseStyle +
                "-fx-background-color: #eeeeee; -fx-text-fill: #424242; -fx-border-color: #e0e0e0; -fx-border-radius: 6;"));
        clearBtn.setOnMouseExited(e -> clearBtn.setStyle(btnBaseStyle +
                "-fx-background-color: #f5f5f5; -fx-text-fill: #424242; -fx-border-color: #e0e0e0; -fx-border-radius: 6;"));

        HBox actions = new HBox(10, saveBtn, clearBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setPadding(new Insets(12, 0, 0, 0));

        // Validation label
        Label validation = new Label();
        validation.setStyle("-fx-text-fill: #b00020; -fx-font-size: 12;");
        validation.setWrapText(true);

        VBox centerBox = new VBox(header, scroll, validation, actions);
        centerBox.setSpacing(10);
        centerBox.setPadding(new Insets(6));
        root.setCenter(centerBox);

        // Save action: validate and build a map of values
        saveBtn.setOnAction(evt -> {
            validation.setText("");
            String error = validateFields(m_name, m_mobile, m_email, m_pincode, m_age, m_econtactmobile);
            if (!error.isEmpty()) {
                validation.setText(error);
                return;
            }

            Map<String, String> data = new LinkedHashMap<>();
            data.put("m_uid", m_uid.getText());
            data.put("m_name", m_name.getText());
            data.put("m_mobile", m_mobile.getText());
            data.put("m_aadhar", m_aadhar.getText());
            LocalDate dobVal = m_dob.getValue();
            data.put("m_dob", dobVal == null ? "" : dobVal.toString());
            data.put("m_bloodgroup", m_bloodgroup.getValue() == null ? "" : m_bloodgroup.getValue());
            data.put("m_father", m_father.getText());
            data.put("m_mother", m_mother.getText());
            data.put("m_email", m_email.getText());
            data.put("m_address", m_address.getText());
            data.put("m_state", m_state.getText());
            data.put("m_pincode", m_pincode.getText());
            data.put("m_district", m_district.getText());
            data.put("m_category", m_category.getText());
            data.put("m_rank", m_rank.getText());
            data.put("m_econtactperson", m_econtactperson.getText());
            data.put("m_econtactmobile", m_econtactmobile.getText());
            data.put("m_erelation", m_erelation.getText());
            data.put("m_bsguid", m_bsguid.getText());
            data.put("m_age", m_age.getText());

            // Callback for parent to handle the data (DB insert, etc.)
            if (onSave != null)
                onSave.accept(data);

            // Optionally show success and clear
            Alert ok = new Alert(Alert.AlertType.INFORMATION, "Saved successfully.", ButtonType.OK);
            ok.setHeaderText(null);
            ok.showAndWait();
        });

        clearBtn.setOnAction(evt -> {
            m_uid.clear();
            m_name.clear();
            m_mobile.clear();
            m_aadhar.clear();
            m_dob.setValue(null);
            m_bloodgroup.setValue(null);
            m_father.clear();
            m_mother.clear();
            m_email.clear();
            m_address.clear();
            m_state.clear();
            m_pincode.clear();
            m_district.clear();
            m_category.clear();
            m_rank.clear();
            m_econtactperson.clear();
            m_econtactmobile.clear();
            m_erelation.clear();
            m_bsguid.clear();
            m_age.clear();
            validation.setText("");
        });

        // A little responsive width handling for the grid fields
        grid.getColumnConstraints().addAll(
                new ColumnConstraints(160, 160, 260), // labels
                new ColumnConstraints(300, 500, Double.MAX_VALUE) // fields grow
        );

        return root;
    }

    // Basic validation example, returns error message (empty if ok)
    private static String validateFields(TextField name, TextField mobile, TextField email,
            TextField pincode, TextField age, TextField econtactMobile) {
        StringBuilder err = new StringBuilder();
        if (name.getText() == null || name.getText().trim().length() < 2) {
            err.append("Please enter a valid name.\n");
        }
        if (mobile.getText() == null || !mobile.getText().matches("\\d{10}")) {
            err.append("Mobile must be 10 digits.\n");
        }
        if (email.getText() != null && !email.getText().trim().isEmpty() &&
                !email.getText().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            err.append("Email doesn't look valid.\n");
        }
        if (pincode.getText() != null && !pincode.getText().trim().isEmpty() &&
                !pincode.getText().matches("\\d{4,6}")) {
            err.append("Pincode should be 4-6 digits.\n");
        }
        if (age.getText() != null && !age.getText().trim().isEmpty() &&
                !age.getText().matches("\\d{1,3}")) {
            err.append("Age must be numeric.\n");
        }
        if (econtactMobile.getText() != null && !econtactMobile.getText().trim().isEmpty() &&
                !econtactMobile.getText().matches("\\d{10}")) {
            err.append("Emergency contact mobile must be 10 digits.\n");
        }
        return err.toString();
    }
}
