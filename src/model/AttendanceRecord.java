package model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class AttendanceRecord {
    private final StringProperty uid;
    private final StringProperty time;

    public AttendanceRecord(String uid, String time) {
        this.uid = new SimpleStringProperty(uid);
        this.time = new SimpleStringProperty(time);
    }

    public StringProperty uidProperty() {
        return uid;
    }

    public StringProperty timeProperty() {
        return time;
    }
}
