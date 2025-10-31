package db;

import model.AttendanceRecord;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DatabaseHelper: Manages SQLite database operations for attendance tracking
 * Uses SQLite with a simple schema:
 * Table: attendance
 * Columns:
 * - id: INTEGER PRIMARY KEY AUTOINCREMENT
 * - uid: TEXT (stores NFC card UID)
 * - time: TEXT (stores timestamp)
 */
public class DatabaseHelper {
    // SQLite database URL pointing to attendance.db in the current directory
    private static final String DB_URL = "jdbc:sqlite:attendance.db";

    /**
     * Initializes the database by creating the attendance table if it doesn't exist
     * Schema:
     * - id: Auto-incrementing primary key
     * - uid: Card UID from NFC scan
     * - time: Timestamp of attendance
     */
    public static void initDB() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            Statement stmt = conn.createStatement();
            // Create table if it doesn't exist
            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS attendance (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            uid TEXT,
                            time TEXT
                        )
                    """);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Records a new attendance entry in the database
     * 
     * @param uid The UID of the NFC card that was scanned
     *            Uses PreparedStatement to safely handle SQL injection prevention
     *            Records current timestamp automatically
     */
    public static void insertAttendance(String uid) {
        // SQL with parameterized query for safety
        String sql = "INSERT INTO attendance(uid, time) VALUES(?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
                PreparedStatement ps = conn.prepareStatement(sql)) {
            // Bind parameters: uid and current timestamp
            ps.setString(1, uid);
            ps.setString(2, LocalDateTime.now().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves all attendance records from the database
     * 
     * @return List of AttendanceRecord objects, ordered by most recent first
     *         Returns empty list if database error occurs
     */
    public static List<AttendanceRecord> getAllRecords() {
        List<AttendanceRecord> list = new ArrayList<>();
        // Query to get all records, newest first
        try (Connection conn = DriverManager.getConnection(DB_URL);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT uid, time FROM attendance ORDER BY id DESC")) {

            while (rs.next()) {
                // Convert each row to an AttendanceRecord object
                list.add(new AttendanceRecord(rs.getString("uid"), rs.getString("time")));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
