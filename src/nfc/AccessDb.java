package nfc; // or nfc — use whichever package you want

import java.sql.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class AccessDb {

    // Change to your .accdb path
    private static final String DB_FILE_PATH = "C:/data/attendance.accdb";
    // UCanAccess connection string
    private static final String CONN_URL = "jdbc:ucanaccess://" + DB_FILE_PATH;

    // Ensure ucanaccess and its deps are on the classpath
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(CONN_URL);
    }

    /**
     * Insert an attendee row. Returns generated Id (AUTOINCREMENT) or -1 on
     * failure.
     * Map fields should match the keys you're sending from the form:
     * FullName, BSGUID, ParticipationType, bsgDistrict, Email, phoneNumber,
     * bsgState, memberTyp, unitNam, rank_or_section, dataOfBirth, age
     *
     * Also accepts cardUid (nullable) to store the NFC UID.
     */
    public static long insertAttendee(Map<String, String> data, String cardUid) throws SQLException {
        String sql = "INSERT INTO Attendees (FullName, BSGUID, ParticipationType, BSGDistrict, Email, PhoneNumber," +
                " BSGState, MemberType, UnitName, RankOrSection, DateOfBirth, Age, CardUID, CreatedAt) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = getConnection();
                PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, trimToNull(data.get("FullName")));
            ps.setString(2, trimToNull(data.get("BSGUID")));
            ps.setString(3, trimToNull(data.get("ParticipationType")));
            ps.setString(4, trimToNull(data.get("bsgDistrict")));
            ps.setString(5, trimToNull(data.get("Email")));
            ps.setString(6, trimToNull(data.get("phoneNumber")));
            ps.setString(7, trimToNull(data.get("bsgState")));
            ps.setString(8, trimToNull(data.get("memberTyp")));
            ps.setString(9, trimToNull(data.get("unitNam")));
            ps.setString(10, trimToNull(data.get("rank_or_section")));
            // DateOfBirth: store as date if provided and parseable; else null
            String dob = trimToNull(data.get("dataOfBirth"));
            if (dob != null && !dob.isEmpty()) {
                // Accepts ISO yyyy-MM-dd (your DatePicker uses that)
                ps.setDate(11, Date.valueOf(dob));
            } else {
                ps.setNull(11, Types.DATE);
            }
            ps.setString(12, trimToNull(data.get("age")));
            ps.setString(13, trimToNull(cardUid));

            // CreatedAt as current timestamp
            Timestamp now = Timestamp.from(Instant.now());
            ps.setTimestamp(14, now);

            int affected = ps.executeUpdate();
            if (affected == 0)
                return -1;

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next())
                    return keys.getLong(1);
            }
            return -1;
        }
    }

    private static String trimToNull(String s) {
        if (s == null)
            return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
