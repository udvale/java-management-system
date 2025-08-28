package com.project.back_end.services;

import com.project.back_end.DTO.AppointmentDTO;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.PatientRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final TokenService tokenService;

    public PatientService(PatientRepository patientRepository,
                          AppointmentRepository appointmentRepository,
                          TokenService tokenService) {
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.tokenService = tokenService;
    }

    // 3) Create patient: 1 on success, 0 on failure
    @Transactional
    public int createPatient(Patient patient) {
        try {
            if (patient == null) return 0;
            patientRepository.save(patient);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    // 4) Get a patient's appointments (authorized by token email → patient id must match)
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getPatientAppointment(Long id, String token) {
        try {
            if (id == null || token == null || token.isBlank()) {
                return error(400, "Invalid request");
            }

            // extract email from token and load patient
            String email = tokenService.extractIdentifier(token);  // assumed method
            Patient authPatient = patientRepository.findByEmail(email);
            if (authPatient == null) return error(401, "Unauthorized");
            if (!Objects.equals(authPatient.getId(), id)) return error(403, "Forbidden");

            List<Appointment> appts = appointmentRepository.findByPatient_Id(id);
            List<AppointmentDTO> dtos = appts.stream()
                    .map(this::toDTO)
                    .sorted(Comparator.comparing(AppointmentDTO::getAppointmentTime))
                    .collect(Collectors.toList());

            return ok(Map.of("appointments", dtos));
        } catch (Exception e) {
            return error(500, "Internal error");
        }
    }

    // 5) Filter by condition (past/future) for a patient
    // condition: "past" -> status=1, "future" -> status=0
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> filterByCondition(String condition, Long id) {
        try {
            if (id == null || condition == null) return error(400, "Invalid request");

            Integer status = mapConditionToStatus(condition);
            if (status == null) return error(400, "Condition must be 'past' or 'future'");

            List<Appointment> appts =
                    appointmentRepository.findByPatient_IdAndStatusOrderByAppointmentTimeAsc(id, status);

            List<AppointmentDTO> dtos = appts.stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList());

            return ok(Map.of("appointments", dtos));
        } catch (Exception e) {
            return error(500, "Internal error");
        }
    }

    // 6) Filter by doctor name for a patient
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> filterByDoctor(String name, Long patientId) {
        try {
            if (patientId == null || name == null) return error(400, "Invalid request");

            List<Appointment> appts =
                    appointmentRepository.filterByDoctorNameAndPatientId(name.trim(), patientId);

            List<AppointmentDTO> dtos = appts.stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList());

            return ok(Map.of("appointments", dtos));
        } catch (Exception e) {
            return error(500, "Internal error");
        }
    }

    // 7) Filter by doctor name + condition (past/future) for a patient
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> filterByDoctorAndCondition(String condition, String name, long patientId) {
        try {
            Integer status = mapConditionToStatus(condition);
            if (status == null || name == null) return error(400, "Invalid request");

            List<Appointment> appts =
                    appointmentRepository.filterByDoctorNameAndPatientIdAndStatus(name.trim(), patientId, status);

            List<AppointmentDTO> dtos = appts.stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList());

            return ok(Map.of("appointments", dtos));
        } catch (Exception e) {
            return error(500, "Internal error");
        }
    }

    // 8) Get patient details from token
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getPatientDetails(String token) {
        try {
            if (token == null || token.isBlank()) return error(400, "Token required");

            String email = tokenService.extractIdentifier(token);  
            if (email == null || email.isBlank()) return error(401, "Invalid token");

            Patient patient = patientRepository.findByEmail(email);
            if (patient == null) return error(404, "Patient not found");

            Map<String, Object> body = new HashMap<>();
            body.put("id", patient.getId());
            body.put("name", patient.getName());
            body.put("email", patient.getEmail());
            body.put("phone", patient.getPhone());
            body.put("address", patient.getAddress());

            return ok(body);
        } catch (Exception e) {
            return error(500, "Internal error");
        }
    }

    /* ================= Helpers ================= */

    private AppointmentDTO toDTO(Appointment a) {
        Long id = a.getId();
        Long doctorId = (a.getDoctor() != null) ? a.getDoctor().getId() : null;
        String doctorName = (a.getDoctor() != null) ? a.getDoctor().getName() : null;
        Long patientId = (a.getPatient() != null) ? a.getPatient().getId() : null;
        String patientName = (a.getPatient() != null) ? a.getPatient().getName() : null;
        String patientEmail = (a.getPatient() != null) ? a.getPatient().getEmail() : null;
        String patientPhone = (a.getPatient() != null) ? a.getPatient().getPhone() : null;
        String patientAddress = (a.getPatient() != null) ? a.getPatient().getAddress() : null;
        LocalDateTime time = a.getAppointmentTime();
        int status = a.getStatus();

        return new AppointmentDTO(
                id, doctorId, doctorName, patientId, patientName,
                patientEmail, patientPhone, patientAddress, time, status
        );
    }

    private Integer mapConditionToStatus(String condition) {
        if (condition == null) return null;
        String c = condition.trim().toLowerCase(Locale.ROOT);
        if (c.equals("future")) return 0;
        if (c.equals("past")) return 1;
        return null;
    }

    private ResponseEntity<Map<String, Object>> ok(Map<String, Object> body) {
        return ResponseEntity.ok(body);
    }

    private ResponseEntity<Map<String, Object>> error(int status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}
