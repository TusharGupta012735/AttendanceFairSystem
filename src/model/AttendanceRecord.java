package model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Model class representing a single attendance record
 * Uses JavaFX properties for automatic UI updates when data changes
 * Maps to a row in the attendance database table
 */
public class AttendanceRecord {
    /** The NFC card's UID as a JavaFX property */
    private final StringProperty uid;
    /** Timestamp of attendance as a JavaFX property */
    private final StringProperty time;

    /**
     * Creates a new attendance record
     * 
     * @param uid  The NFC card's unique identifier
     * @param time The timestamp of attendance
     */
    public AttendanceRecord(String uid, String time) {
        this.uid = new SimpleStringProperty(uid);
        this.time = new SimpleStringProperty(time);
    }

    /**
     * @return JavaFX property containing the card's UID
     *         Used by TableView for binding
     */
    public StringProperty uidProperty() {
        return uid;
    }

    /**
     * @return JavaFX property containing the attendance timestamp
     *         Used by TableView for binding
     */
    public StringProperty timeProperty() {
        return time;
    }
}
