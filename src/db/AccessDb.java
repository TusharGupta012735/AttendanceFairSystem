package db;

import java.sql.*;
import java.time.Instant;
import java.util.*;

/**
 * AccessDb utility — targets the ParticipantsWrite table and matches the
 * EntryForm field order.
 *
 * Works safely when some columns (CardUID, CreatedAt) are missing by
 * discovering
 * actual table columns and building the INSERT accordingly.
 *
 * CLI:
 * javac -cp "lib/*;src" -d out src\db\AccessDb.java
 * java -cp "out;lib/*" db.AccessDb list
 * java -cp "out;lib/*" db.AccessDb describe ParticipantsWrite
 * java -cp "out;lib/*" db.AccessDb create-participants
 * java -cp "out;lib/*" db.AccessDb test
 */
public class AccessDb {

    private static final String DB_FILE_PATH = "C:/Users/kamal/Documents/bsd.accdb";
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

    /** Trim and strip any trailing commas the UI might add. */
    private static String normalize(String s) {
        if (s == null)
            return null;
        String t = s.trim();
        while (t.endsWith(","))
            t = t.substring(0, t.length() - 1).trim();
        return t.isEmpty() ? null : t;
    }

    /**
     * Insert a row into ParticipantsWrite. Returns generated Id (AUTOINCREMENT) or
     * -1 on failure.
     *
     * The method only inserts columns that actually exist in the table so it won't
     * fail
     * if CardUID or CreatedAt are missing.
     *
     * Expects map keys from EntryForm:
     * FullName, BSGUID, ParticipationType, bsgDistrict, Email, phoneNumber,
     * bsgState, memberTyp, unitNam, rank_or_section, dataOfBirth, age
     */
    public static long insertAttendee(Map<String, String> data, String cardUid) throws SQLException {
        List<String> expected = Arrays.asList(
                "FullName", "BSGUID", "ParticipationType", "BSGDistrict",
                "Email", "PhoneNumber", "BSGState", "MemberType",
                "UnitName", "RankOrSection", "DateOfBirth", "Age",
                "CardUID", "CreatedAt");

        try (Connection c = getConnection()) {
            // discover actual columns present (uppercase for comparison)
            Set<String> actual = new HashSet<>();
            DatabaseMetaData md = c.getMetaData();
            try (ResultSet rs = md.getColumns(null, null, "ParticipantsWrite", "%")) {
                while (rs.next()) {
                    String col = rs.getString("COLUMN_NAME");
                    if (col != null)
                        actual.add(col.toUpperCase());
                }
            }

            List<String> cols = new ArrayList<>();
            List<Object> vals = new ArrayList<>();

            for (String col : expected) {
                if (!actual.contains(col.toUpperCase()))
                    continue;

                switch (col) {
                    case "DateOfBirth": {
                        String dob = normalize(data.get("dataOfBirth"));
                        if (dob == null)
                            vals.add(null);
                        else {
                            try {
                                vals.add(java.sql.Date.valueOf(dob)); // explicitly java.sql.Date
                            } catch (IllegalArgumentException ex) {
                                vals.add(null);
                            }
                        }
                        break;
                    }
                    case "CreatedAt":
                        vals.add(java.sql.Timestamp.from(Instant.now()));
                        break;
                    case "CardUID":
                        vals.add(normalize(cardUid));
                        break;
                    default: {
                        String mapKey;
                        switch (col) {
                            case "FullName":
                                mapKey = "FullName";
                                break;
                            case "BSGUID":
                                mapKey = "BSGUID";
                                break;
                            case "ParticipationType":
                                mapKey = "ParticipationType";
                                break;
                            case "BSGDistrict":
                                mapKey = "bsgDistrict";
                                break;
                            case "Email":
                                mapKey = "Email";
                                break;
                            case "PhoneNumber":
                                mapKey = "phoneNumber";
                                break;
                            case "BSGState":
                                mapKey = "bsgState";
                                break;
                            case "MemberType":
                                mapKey = "memberTyp";
                                break;
                            case "UnitName":
                                mapKey = "unitNam";
                                break;
                            case "RankOrSection":
                                mapKey = "rank_or_section";
                                break;
                            case "Age":
                                mapKey = "age";
                                break;
                            default:
                                mapKey = col;
                        }
                        vals.add(normalize(data.get(mapKey)));
                    }
                }
                cols.add("[" + col + "]");
            }

            if (cols.isEmpty())
                throw new SQLException("No insertable columns found in ParticipantsWrite.");

            String placeholders = String.join(",", Collections.nCopies(cols.size(), "?"));
            String sql = "INSERT INTO [ParticipantsWrite] (" + String.join(",", cols) + ") VALUES (" + placeholders
                    + ")";

            try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                for (int i = 0; i < vals.size(); i++) {
                    Object v = vals.get(i);
                    int idx = i + 1;
                    String colName = cols.get(i).replace("[", "").replace("]", "");
                    if (v == null) {
                        if ("DateOfBirth".equalsIgnoreCase(colName))
                            ps.setNull(idx, Types.DATE);
                        else if ("CreatedAt".equalsIgnoreCase(colName))
                            ps.setNull(idx, Types.TIMESTAMP);
                        else
                            ps.setNull(idx, Types.VARCHAR);
                    } else if (v instanceof java.sql.Date) {
                        ps.setDate(idx, (java.sql.Date) v);
                    } else if (v instanceof java.sql.Timestamp) {
                        ps.setTimestamp(idx, (java.sql.Timestamp) v);
                    } else {
                        ps.setString(idx, v.toString());
                    }
                }

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
    }

    /**
     * Update CardUID if the column exists. If not, return false without throwing.
     */
    public static boolean updateCardUid(long id, String cardUid) throws SQLException {
        try (Connection c = getConnection()) {
            DatabaseMetaData md = c.getMetaData();
            boolean hasCol = false;
            try (ResultSet rs = md.getColumns(null, null, "ParticipantsWrite", "CardUID")) {
                if (rs.next())
                    hasCol = true;
            }
            if (!hasCol)
                return false;

            String upd = "UPDATE [ParticipantsWrite] SET [CardUID] = ? WHERE [Id] = ?";
            try (PreparedStatement ps = c.prepareStatement(upd)) {
                ps.setString(1, normalize(cardUid));
                ps.setLong(2, id);
                return ps.executeUpdate() > 0;
            }
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
                    if (args.length < 2)
                        System.out.println("Usage: describe <TableName>");
                    else
                        describeTable(args[1]);
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
                    System.out.println(" - " + rs.getString("TABLE_NAME") + "  (" + rs.getString("TABLE_TYPE") + ")");
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
                    System.out.printf(" - %s : %s(%d)%n",
                            rs.getString("COLUMN_NAME"),
                            rs.getString("TYPE_NAME"),
                            rs.getInt("COLUMN_SIZE"));
                    any = true;
                }
            }
            if (!any) {
                try (ResultSet rs2 = md.getColumns(null, null, tableName.toUpperCase(), "%")) {
                    while (rs2.next()) {
                        if (!any)
                            System.out.println("Columns for table " + tableName.toUpperCase() + ":");
                        System.out.printf(" - %s : %s(%d)%n",
                                rs2.getString("COLUMN_NAME"),
                                rs2.getString("TYPE_NAME"),
                                rs2.getInt("COLUMN_SIZE"));
                        any = true;
                    }
                }
            }
            if (!any)
                System.out.println("  (no columns found or table does not exist: " + tableName + ")");
        } catch (SQLException ex) {
            System.out.println("Failed to describe table: " + ex.getMessage());
            ex.printStackTrace(System.out);
        }
    }

    /** Create ParticipantsWrite if missing (without CardUID). */
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
            try (Statement st = c.createStatement();
                    ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM [ParticipantsWrite]")) {
                if (rs.next()) {
                    System.out.println("ParticipantsWrite table row count: " + rs.getLong(1));
                } else {
                    System.out.println("ParticipantsWrite table query returned no rows.");
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
            System.out.println("    java -cp \"out;lib/*\" db.AccessDb test");
        }
    }
}
