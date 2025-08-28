package com.project.back_end.controllers;

import com.project.back_end.models.Prescription;
import com.project.back_end.services.PrescriptionService;
import com.project.back_end.services.Service;
import com.project.back_end.services.AppointmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("${api.path}" + "prescription")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final Service service;
    private final AppointmentService appointmentService;

    public PrescriptionController(PrescriptionService prescriptionService,
                                  Service service,
                                  AppointmentService appointmentService) {
        this.prescriptionService = prescriptionService;
        this.service = service;
        this.appointmentService = appointmentService;
    }

    /* ================================
       1) Save Prescription (Doctor only)
       ================================ */
    @PostMapping("/{token}")
    public ResponseEntity<Map<String, String>> savePrescription(
            @PathVariable String token,
            @RequestBody Prescription prescription) {

        // Validate doctor token
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "doctor");
        if (validation.getStatusCode().isError()) {
            return validation;
        }

        // Save prescription
        ResponseEntity<Map<String, String>> result = prescriptionService.savePrescription(prescription);

        // If prescription is saved, update appointment status (optional: set status = 1 for completed)
        if (result.getStatusCode().is2xxSuccessful() && prescription.getAppointmentId() != null) {
            appointmentService.changeStatus(prescription.getAppointmentId(), 1);
        }

        return result;
    }

    /* ================================
       2) Get Prescription by Appointment ID (Doctor only)
       ================================ */
    @GetMapping("/{appointmentId}/{token}")
    public ResponseEntity<Map<String, Object>> getPrescription(
            @PathVariable Long appointmentId,
            @PathVariable String token) {

        // Validate doctor token
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "doctor");
        if (validation.getStatusCode().isError()) {
            return ResponseEntity.status(validation.getStatusCode())
                    .body(Map.of("error", "Unauthorized or invalid token"));
        }

        return prescriptionService.getPrescription(appointmentId);
    }
}
