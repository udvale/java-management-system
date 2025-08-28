package com.project.back_end.controllers;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Patient;
import com.project.back_end.services.PatientService;
import com.project.back_end.services.Service;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/patient")
public class PatientController {

    private final PatientService patientService;
    private final Service service;

    public PatientController(PatientService patientService, Service service) {
        this.patientService = patientService;
        this.service = service;
    }

    /* ================================
       1) Get Patient Details
       ================================ */
    @GetMapping("/{token}")
    public ResponseEntity<Map<String, Object>> getPatient(@PathVariable String token) {
        // Validate token for role patient
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "patient");
        if (validation.getStatusCode().isError()) {
            return ResponseEntity.status(validation.getStatusCode())
                    .body(Map.of("error", "Unauthorized or invalid token"));
        }

        return patientService.getPatientDetails(token);
    }

    /* ================================
       2) Create a New Patient
       ================================ */
    @PostMapping
    public ResponseEntity<Map<String, String>> createPatient(@RequestBody Patient patient) {
        // Check for duplicates
        if (!service.validatePatient(patient)) {
            return ResponseEntity.status(409)
                    .body(Map.of("message", "Patient with email id or phone no already exist"));
        }

        int result = patientService.createPatient(patient);
        if (result == 1) {
            return ResponseEntity.status(201).body(Map.of("message", "Signup successful"));
        } else {
            return ResponseEntity.status(500).body(Map.of("message", "Internal server error"));
        }
    }

    /* ================================
       3) Patient Login
       ================================ */
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Login login) {
        return service.validatePatientLogin(login);
    }

    /* ================================
       4) Get Patient Appointments
       ================================ */
    @GetMapping("/{id}/{token}")
    public ResponseEntity<Map<String, Object>> getPatientAppointment(
            @PathVariable Long id,
            @PathVariable String token) {

        // Validate token
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "patient");
        if (validation.getStatusCode().isError()) {
            return ResponseEntity.status(validation.getStatusCode())
                    .body(Map.of("error", "Unauthorized or invalid token"));
        }

        return patientService.getPatientAppointment(id, token);
    }

    /* ================================
       5) Filter Patient Appointments
       ================================ */
    @GetMapping("/filter/{condition}/{name}/{token}")
    public ResponseEntity<Map<String, Object>> filterPatientAppointment(
            @PathVariable String condition,
            @PathVariable String name,
            @PathVariable String token) {

        // Validate token
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "patient");
        if (validation.getStatusCode().isError()) {
            return ResponseEntity.status(validation.getStatusCode())
                    .body(Map.of("error", "Unauthorized or invalid token"));
        }

        return service.filterPatient(condition, name, token);
    }
}
