package com.project.back_end.services;

import com.project.back_end.DTO.AppointmentDTO;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final TokenService tokenService; // assumed to exist in your project

    public AppointmentService(AppointmentRepository appointmentRepository,
                              PatientRepository patientRepository,
                              DoctorRepository doctorRepository,
                              TokenService tokenService) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.tokenService = tokenService;
    }

    /* =========================================
       1) Book Appointment
       - Returns 1 if saved, 0 on any error
       ========================================= */
    @Transactional
    public int bookAppointment(Appointment appointment) {
        try {
            // Basic sanity checks (optional but helpful)
            if (appointment == null || appointment.getDoctor() == null || appointment.getPatient() == null
                    || appointment.getAppointmentTime() == null) {
                return 0;
            }

            // Ensure doctor and patient exist (avoid detached entities)
            Optional<Doctor> dOpt = doctorRepository.findById(appointment.getDoctor().getId());
            Optional<Patient> pOpt = patientRepository.findById(appointment.getPatient().getId());
            if (dOpt.isEmpty() || pOpt.isEmpty()) return 0;

            // (Optional) prevent double-booking at the exact time (1-hour assumed)
            if (!isSlotFree(dOpt.get().getId(), appointment.getAppointmentTime())) {
                return 0;
            }

            appointmentRepository.save(appointment);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    /* =========================================
       2) Update Appointment
       - Validates existence + slot + ownership (patient)
       - Returns ResponseEntity with status message
       ========================================= */
    @Transactional
    public ResponseEntity<Map<String, String>> updateAppointment(Appointment updated) {
        if (updated == null || updated.getId() == null) {
            return response(400, "Invalid appointment payload");
        }

        Optional<Appointment> existingOpt = appointmentRepository.findById(updated.getId());
        if (existingOpt.isEmpty()) {
            return response(404, "Appointment not found");
        }

        Appointment existing = existingOpt.get();

        // Validate change (doctor, time etc.). If you have a separate validator, call it here.
        String validationError = validateAppointmentUpdate(existing, updated);
        if (validationError != null) {
            return response(400, validationError);
        }

        // Persist updates (update only allowed fields)
        if (updated.getDoctor() != null && updated.getDoctor().getId() != null) {
            Optional<Doctor> dOpt = doctorRepository.findById(updated.getDoctor().getId());
            if (dOpt.isEmpty()) return response(400, "Invalid doctor");
            existing.setDoctor(dOpt.get());
        }

        if (updated.getAppointmentTime() != null) {
            // ensure new slot is free for that doctor (ignore if same exact slot)
            Long docId = existing.getDoctor().getId();
            LocalDateTime newTime = updated.getAppointmentTime();
            if (!sameSlot(existing.getAppointmentTime(), newTime) && !isSlotFree(docId, newTime)) {
                return response(409, "Selected time is not available");
            }
            existing.setAppointmentTime(newTime);
        }

        if (updated.getStatus() != existing.getStatus()) {
            existing.setStatus(updated.getStatus());
        }

        appointmentRepository.save(existing);
        return response(200, "Appointment updated successfully");
    }

    /* =========================================
       3) Cancel Appointment
       - Only the owning patient can cancel
       - Verifies token belongs to the patient in question
       ========================================= */
    @Transactional
    public ResponseEntity<Map<String, String>> cancelAppointment(long id, String token) {
        Optional<Appointment> apptOpt = appointmentRepository.findById(id);
        if (apptOpt.isEmpty()) {
            return response(404, "Appointment not found");
        }

        Appointment appt = apptOpt.get();

        // AuthZ: patient-only cancellation (adjust per your roles/logic as needed)
        try {
            String identifier = tokenService.extractIdentifier(token);
				Patient requester = patientRepository.findByEmail(identifier);
				if (requester == null) {
					return response(403, "Only patients can cancel their appointments");
				}
				if (appt.getPatient() == null || !Objects.equals(appt.getPatient().getId(), requester.getId())) {
					return response(403, "You can only cancel your own appointments");
				}
        } catch (Exception e) {
            return response(401, "Invalid token");
        }

        appointmentRepository.delete(appt);
        return response(200, "Appointment canceled successfully");
    }

    /* =========================================
       4) Get Appointments for a Doctor by Date
       - token → doctor identity
       - optional patient-name filter
       - returns Map with "appointments": List<AppointmentDTO>
       ========================================= */
    @Transactional(readOnly = true)
    public Map<String, Object> getAppointment(String pname, LocalDate date, String token) {
        Long doctorId;
        try {
            String identifier = tokenService.extractIdentifier(token);
				Doctor doctor = doctorRepository.findByEmail(identifier);
				if (doctor == null) {
					return Map.of("error", "Only doctors can view this endpoint");
				}
				doctorId = doctor.getId();
        } catch (Exception e) {
            return Map.of("error", "Invalid token");
        }

        if (date == null) {
            return Map.of("error", "Date is required");
        }

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay().minusNanos(1);

        List<Appointment> results;
        if (pname != null && !pname.isBlank()) {
            results = appointmentRepository
                    .findByDoctorIdAndPatient_NameContainingIgnoreCaseAndAppointmentTimeBetween(
                            doctorId, pname.trim(), start, end);
        } else {
            results = appointmentRepository
                    .findByDoctorIdAndAppointmentTimeBetween(doctorId, start, end);
        }

        List<AppointmentDTO> dtos = results.stream()
                .map(this::toDTO)
                .sorted(Comparator.comparing(AppointmentDTO::getAppointmentTime)) // stable sort
                .collect(Collectors.toList());

        return Map.of("appointments", dtos);
    }

    /* =========================================
       5) Change Status (not in the prompt list, but in your earlier notes)
       ========================================= */
    @Transactional
    public ResponseEntity<Map<String, String>> changeStatus(long id, int status) {
        Optional<Appointment> apptOpt = appointmentRepository.findById(id);
        if (apptOpt.isEmpty()) {
            return response(404, "Appointment not found");
        }
        appointmentRepository.updateStatus(status, id);
        return response(200, "Status updated");
    }

    /* ======= Helpers ======= */

    // Map entity → DTO
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

        return new AppointmentDTO(id, doctorId, doctorName, patientId, patientName,
                patientEmail, patientPhone, patientAddress, time, status);
    }

    // Validate update rules (doctor change, time availability, etc.)
    private String validateAppointmentUpdate(Appointment existing, Appointment updated) {
        // If doctor changed, ensure new doctor exists
        if (updated.getDoctor() != null && updated.getDoctor().getId() != null) {
            if (!Objects.equals(updated.getDoctor().getId(), existing.getDoctor().getId())) {
                if (doctorRepository.findById(updated.getDoctor().getId()).isEmpty()) {
                    return "Invalid doctor";
                }
            }
        }

        // If time changed, ensure slot is available
        if (updated.getAppointmentTime() != null &&
            !sameSlot(existing.getAppointmentTime(), updated.getAppointmentTime())) {

            Long targetDoctorId = (updated.getDoctor() != null && updated.getDoctor().getId() != null)
                    ? updated.getDoctor().getId()
                    : existing.getDoctor().getId();

            if (!isSlotFree(targetDoctorId, updated.getAppointmentTime())) {
                return "Selected time is not available";
            }
        }

        return null; // OK
    }

    private boolean sameSlot(LocalDateTime a, LocalDateTime b) {
        return Objects.equals(a, b);
    }

    // Checks if there’s no other appointment for the doctor within the hour starting at 'when'
    private boolean isSlotFree(Long doctorId, LocalDateTime when) {
        if (doctorId == null || when == null) return false;
        LocalDateTime start = when;
        LocalDateTime end = when.plusHours(1).minusNanos(1);
        List<Appointment> taken = appointmentRepository
                .findByDoctorIdAndAppointmentTimeBetween(doctorId, start, end);
        return taken.isEmpty();
    }

    private ResponseEntity<Map<String, String>> response(int statusCode, String message) {
        return ResponseEntity.status(statusCode).body(Map.of("message", message));
    }
}
