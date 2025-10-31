package db;

import model.AttendanceRecord;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper {
    private static final String DB_URL = "jdbc:sqlite:attendance.db";

    public static void initDB() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            Statement stmt = conn.createStatement();
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

    public static void insertAttendance(String uid) {
        String sql = "INSERT INTO attendance(uid, time) VALUES(?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uid);
            ps.setString(2, LocalDateTime.now().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<AttendanceRecord> getAllRecords() {
        List<AttendanceRecord> list = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT uid, time FROM attendance ORDER BY id DESC")) {

            while (rs.next()) {
                list.add(new AttendanceRecord(rs.getString("uid"), rs.getString("time")));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
