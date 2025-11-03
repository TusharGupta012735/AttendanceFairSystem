package db;

import java.sql.*;
import java.time.Instant;
import java.util.Map;

/**
 * AccessDb utility — targets the ParticipantsWrite table and matches the
 * EntryForm field order.
 *
 * Field order (EntryForm) -> DB columns:
 * FullName -> FullName (TEXT)
 * BSGUID -> BSGUID (TEXT)
 * ParticipationType -> ParticipationType (TEXT)
 * bsgDistrict -> BSGDistrict (TEXT)
 * Email -> Email (TEXT)
 * phoneNumber -> PhoneNumber (TEXT)
 * bsgState -> BSGState (TEXT)
 * memberTyp -> MemberType (TEXT)
 * unitNam -> UnitName (TEXT)
 * rank_or_section -> RankOrSection (TEXT)
 * dataOfBirth -> DateOfBirth (DATETIME)
 * age -> Age (TEXT)
 *
 * Additional columns:
 * CardUID -> CardUID (TEXT)
 * CreatedAt -> CreatedAt (DATETIME)
 *
 * CLI:
 * java -cp "out;lib/*" db.AccessDb list
 * java -cp "out;lib/*" db.AccessDb describe ParticipantsWrite
 * java -cp "out;lib/*" db.AccessDb create-participants
 * java -cp "out;lib/*" db.AccessDb test
 */
public class AccessDb {

    private static final String DB_FILE_PATH = "C:/Users/kamal/Documents/Database1.accdb";
    private static final String CONN_URL = "jdbc:ucanaccess://" + DB_FILE_PATH;

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("net.ucanaccess.jdbc.UcanaccessDriver");
        } catch (ClassNotFoundException e) {
            throw new SQLException(
                    "UCanAccess driver not found on runtime classpath. Put ucanaccess.jar and dependencies in lib/ and run with -cp \"out;lib/*\"",
                    e);
        }
        return DriverManager.getConnection(CONN_URL);
    }

    /**
     * Normalize incoming form value:
     * - null -> null
     * - trim whitespace
     * - remove trailing commas that EntryForm may have appended
     */
    private static String normalize(String s) {
        if (s == null)
            return null;
        String t = s.trim();
        // remove all trailing commas and whitespace
        while (t.endsWith(",")) {
            t = t.substring(0, t.length() - 1).trim();
        }
        return t.isEmpty() ? null : t;
    }

    /**
     * Insert a row into ParticipantsWrite. Returns generated Id (AUTOINCREMENT) or
     * -1 on failure.
     *
     * Expects map keys as produced by EntryForm:
     * FullName, BSGUID, ParticipationType, bsgDistrict, Email, phoneNumber,
     * bsgState, memberTyp, unitNam, rank_or_section, dataOfBirth, age
     *
     * cardUid may be null.
     */
    public static long insertAttendee(Map<String, String> data, String cardUid) throws SQLException {
        String sql = "INSERT INTO [ParticipantsWrite] "
                + "([FullName],[BSGUID],[ParticipationType],[BSGDistrict],[Email],[PhoneNumber],"
                + "[BSGState],[MemberType],[UnitName],[RankOrSection],[DateOfBirth],[Age],[CardUID],[CreatedAt]) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = getConnection();
                PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, normalize(data.get("FullName")));
            ps.setString(2, normalize(data.get("BSGUID")));
            ps.setString(3, normalize(data.get("ParticipationType")));
            ps.setString(4, normalize(data.get("bsgDistrict")));
            ps.setString(5, normalize(data.get("Email")));
            ps.setString(6, normalize(data.get("phoneNumber")));
            ps.setString(7, normalize(data.get("bsgState")));
            ps.setString(8, normalize(data.get("memberTyp")));
            ps.setString(9, normalize(data.get("unitNam")));
            ps.setString(10, normalize(data.get("rank_or_section")));

            // DateOfBirth -> Date
            String dob = normalize(data.get("dataOfBirth"));
            if (dob != null) {
                try {
                    ps.setDate(11, Date.valueOf(dob)); // expects yyyy-MM-dd
                } catch (IllegalArgumentException ex) {
                    ps.setNull(11, Types.DATE);
                }
            } else {
                ps.setNull(11, Types.DATE);
            }

            ps.setString(12, normalize(data.get("age")));
            ps.setString(13, normalize(cardUid));

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

    /**
     * Update CardUID for a participant row id.
     */
    public static boolean updateCardUid(long id, String cardUid) throws SQLException {
        String upd = "UPDATE [ParticipantsWrite] SET [CardUID] = ? WHERE [Id] = ?";
        try (Connection c = getConnection();
                PreparedStatement ps = c.prepareStatement(upd)) {
            ps.setString(1, normalize(cardUid));
            ps.setLong(2, id);
            int n = ps.executeUpdate();
            return n > 0;
        }
    }

    // ------------------------ CLI helpers ------------------------

    public static void main(String[] args) {
        String cmd = args.length > 0 ? args[0].toLowerCase() : "help";
        try {
            switch (cmd) {
                case "list":
                    listTables();
                    break;
                case "describe":
                    if (args.length < 2) {
                        System.out.println("Usage: describe <TableName>");
                    } else {
                        describeTable(args[1]);
                    }
                    break;
                case "create-participants":
                    createParticipantsWriteIfMissing();
                    break;
                case "test":
                    testConnection();
                    break;
                default:
                    System.out.println("AccessDb helper");
                    System.out.println("Usage:");
                    System.out.println("  java -cp \"out;lib/*\" db.AccessDb list");
                    System.out.println("  java -cp \"out;lib/*\" db.AccessDb describe <TableName>");
                    System.out.println("  java -cp \"out;lib/*\" db.AccessDb create-participants");
                    System.out.println("  java -cp \"out;lib/*\" db.AccessDb test");
                    break;
            }
        } catch (Exception ex) {
            System.out.println("Error: " + ex.getMessage());
            ex.printStackTrace(System.out);
        }
    }

    public static void listTables() {
        try (Connection c = getConnection()) {
            DatabaseMetaData md = c.getMetaData();
            try (ResultSet rs = md.getTables(null, null, "%", new String[] { "TABLE", "VIEW" })) {
                System.out.println("Tables/Views found in " + DB_FILE_PATH + ":");
                boolean any = false;
                while (rs.next()) {
                    String name = rs.getString("TABLE_NAME");
                    String type = rs.getString("TABLE_TYPE");
                    System.out.println(" - " + name + "  (" + type + ")");
                    any = true;
                }
                if (!any)
                    System.out.println("  (no tables/views found)");
            }
        } catch (SQLException ex) {
            System.out.println("Failed to list tables: " + ex.getMessage());
            ex.printStackTrace(System.out);
        }
    }

    public static void describeTable(String tableName) {
        try (Connection c = getConnection()) {
            DatabaseMetaData md = c.getMetaData();
            boolean any = false;
            try (ResultSet rs = md.getColumns(null, null, tableName, "%")) {
                while (rs.next()) {
                    if (!any)
                        System.out.println("Columns for table " + tableName + ":");
                    String col = rs.getString("COLUMN_NAME");
                    String type = rs.getString("TYPE_NAME");
                    int size = rs.getInt("COLUMN_SIZE");
                    System.out.printf(" - %s : %s(%d)%n", col, type, size);
                    any = true;
                }
            }
            if (!any) {
                try (ResultSet rs2 = md.getColumns(null, null, tableName.toUpperCase(), "%")) {
                    while (rs2.next()) {
                        if (!any)
                            System.out.println("Columns for table " + tableName.toUpperCase() + ":");
                        String col = rs2.getString("COLUMN_NAME");
                        String type = rs2.getString("TYPE_NAME");
                        int size = rs2.getInt("COLUMN_SIZE");
                        System.out.printf(" - %s : %s(%d)%n", col, type, size);
                        any = true;
                    }
                }
            }
            if (!any) {
                System.out.println("  (no columns found or table does not exist: " + tableName + ")");
            }
        } catch (SQLException ex) {
            System.out.println("Failed to describe table: " + ex.getMessage());
            ex.printStackTrace(System.out);
        }
    }

    /**
     * Create ParticipantsWrite table if it does not exist.
     */
    public static void createParticipantsWriteIfMissing() {
        String target = "ParticipantsWrite";
        try (Connection c = getConnection()) {
            DatabaseMetaData md = c.getMetaData();
            boolean exists = false;
            try (ResultSet rs = md.getTables(null, null, target, new String[] { "TABLE" })) {
                if (rs.next())
                    exists = true;
            }
            if (!exists) {
                try (ResultSet rs2 = md.getTables(null, null, target.toUpperCase(), new String[] { "TABLE" })) {
                    if (rs2.next())
                        exists = true;
                }
            }

            if (exists) {
                System.out.println("Table '" + target + "' already exists - no action taken.");
                describeTable(target);
                return;
            }

            System.out.println("Creating table '" + target + "'...");

            String createSql = "CREATE TABLE [ParticipantsWrite] ("
                    + "[Id] AUTOINCREMENT PRIMARY KEY, "
                    + "[FullName] TEXT(255), "
                    + "[BSGUID] TEXT(255), "
                    + "[ParticipationType] TEXT(100), "
                    + "[BSGDistrict] TEXT(100), "
                    + "[Email] TEXT(255), "
                    + "[PhoneNumber] TEXT(50), "
                    + "[BSGState] TEXT(100), "
                    + "[MemberType] TEXT(100), "
                    + "[UnitName] TEXT(255), "
                    + "[RankOrSection] TEXT(100), "
                    + "[DateOfBirth] DATETIME, "
                    + "[Age] TEXT(10), "
                    + "[CardUID] TEXT(255), "
                    + "[CreatedAt] DATETIME"
                    + ")";

            try (Statement st = c.createStatement()) {
                st.executeUpdate(createSql);
                System.out.println("Table created successfully.");
                describeTable(target);
            } catch (SQLException ex) {
                System.out.println("Failed to create table: " + ex.getMessage());
                ex.printStackTrace(System.out);
            }
        } catch (SQLException ex) {
            System.out.println("DB error: " + ex.getMessage());
            ex.printStackTrace(System.out);
        }
    }

    private static void testConnection() {
        System.out.println("Attempting to connect to the Access database...");
        try (Connection c = getConnection()) {
            System.out.println("Connection successful.");
            try (Statement st = c.createStatement()) {
                try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM [ParticipantsWrite]")) {
                    if (rs.next()) {
                        long count = rs.getLong(1);
                        System.out.println("ParticipantsWrite table row count: " + count);
                    } else {
                        System.out.println("ParticipantsWrite table query returned no rows.");
                    }
                }
            } catch (SQLException qex) {
                System.out.println(
                        "Could not query ParticipantsWrite table. Maybe the table doesn't exist or column names differ.");
                System.out.println("SQLException: " + qex.getMessage());
            }
        } catch (SQLException ex) {
            System.out.println("Failed to connect: " + ex.getMessage());
            Throwable root = ex.getCause();
            if (root != null)
                System.out.println("Cause: " + root.getMessage());
            ex.printStackTrace(System.out);
            System.out.println();
            System.out.println("Checklist:");
            System.out.println("- Are the UCanAccess jars present in lib/? (ucanaccess, jackcess, hsqldb, commons-*)");
            System.out.println("- Did you run with the jars on the runtime classpath? Example:");
            System.out.println(
                    "    java --module-path \"C:\\path\\to\\javafx-sdk\\lib\" --add-modules javafx.controls,javafx.fxml -cp \"out;lib/*\" ui.MainUI");
            System.out.println("  or for CLI test without JavaFX modules:");
            System.out.println("    java -cp \"out;lib/*\" db.AccessDb test");
        }
    }
}