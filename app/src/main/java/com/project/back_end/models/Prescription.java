package com.project.back_end.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Document(collection = "prescriptions")
public class Prescription {

    @Id
    private String id;  // MongoDB ObjectId as String

    @NotNull(message = "patientName cannot be null")
    @Size(min = 3, max = 100, message = "patientName must be between 3 and 100 characters")
    private String patientName;

    @NotNull(message = "appointmentId cannot be null")
    private Long appointmentId;

    @NotNull(message = "medication cannot be null")
    @Size(min = 3, max = 100, message = "medication must be between 3 and 100 characters")
    private String medication;

    @NotNull(message = "dosage cannot be null")
    @Size(min = 3, max = 20, message = "dosage must be between 3 and 20 characters")
    private String dosage;

    @Size(max = 200, message = "doctorNotes must be at most 200 characters")
    private String doctorNotes;

    // ----- Constructors -----
    public Prescription() {}

    public Prescription(
            String patientName,
            Long appointmentId,
            String medication,
            String dosage,
            String doctorNotes
    ) {
        this.patientName = patientName;
        this.appointmentId = appointmentId;
        this.medication = medication;
        this.dosage = dosage;
        this.doctorNotes = doctorNotes;
    }

    // ----- Getters & Setters -----
    public String getId() { return id; }

    public void setId(String id) { this.id = id; }

    public String getPatientName() { return patientName; }

    public void setPatientName(String patientName) { this.patientName = patientName; }

    public Long getAppointmentId() { return appointmentId; }

    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }

    public String getMedication() { return medication; }

    public void setMedication(String medication) { this.medication = medication; }

    public String getDosage() { return dosage; }

    public void setDosage(String dosage) { this.dosage = dosage; }

    public String getDoctorNotes() { return doctorNotes; }

    public void setDoctorNotes(String doctorNotes) { this.doctorNotes = doctorNotes; }
}
