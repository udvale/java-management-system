package com.project.back_end.controllers;

import com.project.back_end.models.Appointment;
import com.project.back_end.services.AppointmentService;
import com.project.back_end.services.Service;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final Service service;

    public AppointmentController(AppointmentService appointmentService, Service service) {
        this.appointmentService = appointmentService;
        this.service = service;
    }

    /* ===============================
       1) Get Appointments (Doctor only)
       =============================== */
    @GetMapping("/{date}/{patientName}/{token}")
    public ResponseEntity<Map<String, Object>> getAppointments(
            @PathVariable String date,
            @PathVariable String patientName,
            @PathVariable String token) {

        // Validate doctor token
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "doctor");
        if (validation.getStatusCode().isError()) {
            return ResponseEntity.status(validation.getStatusCode()).body(Map.of("error", "Unauthorized"));
        }

        try {
            LocalDate parsedDate = LocalDate.parse(date);
            Map<String, Object> result = appointmentService.getAppointment(patientName, parsedDate, token);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid date format, expected yyyy-MM-dd"));
        }
    }

    /* ===============================
       2) Book Appointment (Patient only)
       =============================== */
    @PostMapping("/{token}")
    public ResponseEntity<Map<String, String>> bookAppointment(
            @PathVariable String token,
            @RequestBody Appointment appointment) {

        // Validate patient token
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "patient");
        if (validation.getStatusCode().isError()) {
            return validation;
        }

        // Validate appointment
        int validationResult = service.validateAppointment(appointment);
        if (validationResult == -1) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid doctor ID"));
        } else if (validationResult == 0) {
            return ResponseEntity.status(409).body(Map.of("message", "Selected slot is not available"));
        }

        int booked = appointmentService.bookAppointment(appointment);
        if (booked == 1) {
            return ResponseEntity.status(201).body(Map.of("message", "Appointment booked successfully"));
        } else {
            return ResponseEntity.status(500).body(Map.of("message", "Failed to book appointment"));
        }
    }

    /* ===============================
       3) Update Appointment (Patient only)
       =============================== */
    @PutMapping("/{token}")
    public ResponseEntity<Map<String, String>> updateAppointment(
            @PathVariable String token,
            @RequestBody Appointment appointment) {

        // Validate patient token
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "patient");
        if (validation.getStatusCode().isError()) {
            return validation;
        }

        return appointmentService.updateAppointment(appointment);
    }

    /* ===============================
       4) Cancel Appointment (Patient only)
       =============================== */
    @DeleteMapping("/{id}/{token}")
    public ResponseEntity<Map<String, String>> cancelAppointment(
            @PathVariable long id,
            @PathVariable String token) {

        // Validate patient token
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "patient");
        if (validation.getStatusCode().isError()) {
            return validation;
        }

        return appointmentService.cancelAppointment(id, token);
    }
}
