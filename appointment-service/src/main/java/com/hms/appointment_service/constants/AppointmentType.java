package com.hms.appointment_service.constants;

public enum AppointmentType {

    CONSULTATION("Khám tư vấn"),
    FOLLOW_UP("Tái khám"),
    DIAGNOSIS("Chuẩn đoán"),
    TREATMENT("Điều trị"),
    VACCINATION("Tiêm ngừa");

    private final String label;

    AppointmentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
