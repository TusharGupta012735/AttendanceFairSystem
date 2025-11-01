// package ui;

// import javafx.application.Platform;
// import javafx.geometry.Insets;
// import javafx.geometry.Pos;
// import javafx.scene.control.Label;
// import javafx.scene.layout.*;
// import javafx.scene.paint.Color;
// import javafx.scene.text.Font;
// import javafx.scene.text.FontWeight;
// import nfc.SmartMifareReader;

// import java.util.Map;

// public class AttendanceSection extends VBox {

//     private final Label statusLabel = new Label("Checking for NFC reader...");
//     private volatile boolean running = true;

//     public AttendanceSection() {
//         setSpacing(20);
//         setPadding(new Insets(40));
//         setAlignment(Pos.CENTER);
//         setStyle("-fx-background-color: #ECF0F1; -fx-border-radius: 10; -fx-background-radius: 10;");

//         statusLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
//         statusLabel.setTextFill(Color.web("#2C3E50"));

//         getChildren().add(statusLabel);

//         // Start background NFC polling
//         Thread nfcThread = new Thread(this::pollNFC);
//         nfcThread.setDaemon(true);
//         nfcThread.start();
//     }

//     private void pollNFC() {
//         while (running) {
//             try {
//                 Map<String, String> result = SmartMifareReader.readCardData();

//                 if (result.containsKey("error")) {
//                     String err = result.get("error");
//                     if (err.contains("reader")) {
//                         updateMessage("⚠️ Please connect a reader device (ACR1552U).");
//                     } else if (err.contains("known key") || err.contains("Authentication")) {
//                         updateMessage("❌ Unable to read card data. Try again.");
//                     } else {
//                         updateMessage("⚠️ " + err);
//                     }
//                     Thread.sleep(2000);
//                 } else if (result.containsKey("uid")) {
//                     String uid = result.get("uid");
//                     updateMessage("✅ Attendance marked for UUID: " + uid);

//                     // Optional small delay before next polling
//                     Thread.sleep(2500);
//                     updateMessage("Please tap your card (Mifare 1K)");
//                 } else {
//                     updateMessage("Please tap your card (Mifare 1K)");
//                     Thread.sleep(1000);
//                 }

//             } catch (Exception e) {
//                 updateMessage("⚠️ " + e.getMessage());
//                 try {
//                     Thread.sleep(2000);
//                 } catch (InterruptedException ignored) {
//                 }
//             }
//         }
//     }

//     private void updateMessage(String message) {
//         Platform.runLater(() -> statusLabel.setText(message));
//     }

//     public void stop() {
//         running = false;
//     }
// }
