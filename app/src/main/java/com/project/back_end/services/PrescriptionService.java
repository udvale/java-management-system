package com.project.back_end.services;

import com.project.back_end.models.Prescription;
import com.project.back_end.repo.PrescriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PrescriptionService {

    private static final Logger log = LoggerFactory.getLogger(PrescriptionService.class);

    private final PrescriptionRepository prescriptionRepository;

    // 2) Constructor injection
    public PrescriptionService(PrescriptionRepository prescriptionRepository) {
        this.prescriptionRepository = prescriptionRepository;
    }

    /**
     * 1) Save prescription
     * - Prevent duplicates for the same appointmentId
     * - 201 on success
     * - 400 if already exists
     * - 500 on error
     */
    public ResponseEntity<Map<String, String>> savePrescription(Prescription prescription) {
        Map<String, String> body = new HashMap<>();
        try {
            if (prescription == null || prescription.getAppointmentId() == null) {
                body.put("error", "Invalid prescription payload");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
            }

            List<Prescription> existing = prescriptionRepository.findByAppointmentId(prescription.getAppointmentId());
            if (existing != null && !existing.isEmpty()) {
                body.put("error", "Prescription already exists for this appointment");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
            }

            prescriptionRepository.save(prescription);
            body.put("message", "Prescription saved");
            return ResponseEntity.status(HttpStatus.CREATED).body(body);

        } catch (Exception e) {
            log.error("Failed to save prescription", e);
            body.put("error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

    /**
     * 2) Get prescription by appointmentId
     * - 200 with prescription(s) if found
     * - 404 if none found
     * - 500 on error
     */
    public ResponseEntity<Map<String, Object>> getPrescription(Long appointmentId) {
        Map<String, Object> body = new HashMap<>();
        try {
            if (appointmentId == null) {
                body.put("error", "appointmentId is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
            }

            List<Prescription> list = prescriptionRepository.findByAppointmentId(appointmentId);
            if (list == null || list.isEmpty()) {
                body.put("error", "No prescription found for this appointment");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
            }

            // If you expect exactly one per appointment, you could return list.get(0)
            body.put("prescriptions", list);
            return ResponseEntity.ok(body);

        } catch (Exception e) {
            log.error("Failed to fetch prescription for appointmentId={}", appointmentId, e);
            body.put("error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }
}
