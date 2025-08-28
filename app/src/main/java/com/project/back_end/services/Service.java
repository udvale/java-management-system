package com.project.back_end.services;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Admin; 
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient; 

import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;

import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Central service for auth/validation and cross-entity coordination.
 */
@org.springframework.stereotype.Service // <-- fully-qualify to avoid name clash with this class
public class Service {

    private final TokenService tokenService;
    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final DoctorService doctorService;
    private final PatientService patientService;

    public Service(TokenService tokenService,
                   AdminRepository adminRepository,
                   DoctorRepository doctorRepository,
                   PatientRepository patientRepository,
                   DoctorService doctorService,
                   PatientService patientService) {
        this.tokenService = tokenService;
        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.doctorService = doctorService;
        this.patientService = patientService;
    }

    /* ====================== validateToken ====================== */
    public ResponseEntity<Map<String, String>> validateToken(String token, String user) {
        try {
            boolean ok = tokenService.validateToken(token, user);
            if (!ok) return resp(401, "Invalid or expired token");
            return ResponseEntity.ok(Map.of("message", "Token valid"));
        } catch (Exception e) {
            return resp(500, "Internal error");
        }
    }

    /* ====================== validateAdmin ====================== */
    public ResponseEntity<Map<String, String>> validateAdmin(Admin receivedAdmin) {
        try {
            if (receivedAdmin == null || receivedAdmin.getUsername() == null || receivedAdmin.getPassword() == null) {
                return resp(400, "Missing credentials");
            }

            Admin admin = adminRepository.findByUsername(receivedAdmin.getUsername());
            if (admin == null || !Objects.equals(admin.getPassword(), receivedAdmin.getPassword())) {
                return resp(401, "Invalid username or password");
            }

            String token = tokenService.generateToken(admin.getUsername());
            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "role", "ADMIN",
                    "username", admin.getUsername()
            ));
        } catch (Exception e) {
            return resp(500, "Internal error");
        }
    }

    /* ====================== filterDoctor ====================== */
    public Map<String, Object> filterDoctor(String name, String specialty, String time) {
        // time expected "AM"/"PM" or null
        boolean hasName = name != null && !name.isBlank();
        boolean hasSpec = specialty != null && !specialty.isBlank();
        boolean hasTime = time != null && !time.isBlank();

        if (hasName && hasSpec && hasTime) {
            return doctorService.filterDoctorsByNameSpecilityandTime(name.trim(), specialty.trim(), time.trim());
        }
        if (hasName && hasSpec) {
            return doctorService.filterDoctorByNameAndSpecility(name.trim(), specialty.trim());
        }
        if (hasName && hasTime) {
            return doctorService.filterDoctorByNameAndTime(name.trim(), time.trim());
        }
        if (hasSpec && hasTime) {
            return doctorService.filterDoctorByTimeAndSpecility(specialty.trim(), time.trim());
        }
        if (hasName) {
            return doctorService.findDoctorByName(name.trim());
        }
        if (hasSpec) {
            return doctorService.filterDoctorBySpecility(specialty.trim());
        }
        if (hasTime) {
            return doctorService.filterDoctorsByTime(time.trim());
        }
        // no filters: return all
        return Map.of("doctors", doctorService.getDoctors());
    }

    /* ====================== validateAppointment ====================== */
    // Returns: 1 if appointment time valid, 0 if time unavailable, -1 if doctor missing
    public int validateAppointment(Appointment appointment) {
        try {
            if (appointment == null || appointment.getDoctor() == null ||
                appointment.getDoctor().getId() == null || appointment.getAppointmentTime() == null) {
                return 0;
            }

            Long doctorId = appointment.getDoctor().getId();
            Optional<Doctor> docOpt = doctorRepository.findById(doctorId);
            if (docOpt.isEmpty()) return -1;

            LocalDate date = appointment.getAppointmentTime().toLocalDate();
            List<String> free = doctorService.getDoctorAvailability(doctorId, date);

            // compare HH:mm
            String target = appointment.getAppointmentTime().toLocalTime()
                    .format(DateTimeFormatter.ofPattern("HH:mm"));

            return free.stream().anyMatch(s -> normalizeHHmm(s).equals(target)) ? 1 : 0;

        } catch (Exception e) {
            return 0;
        }
    }

    /* ====================== validatePatient ====================== */
    // true if patient does NOT exist yet (ok to register), false if already exists
    public boolean validatePatient(Patient patient) {
        if (patient == null) return false;
        String email = patient.getEmail();
        String phone = patient.getPhone();
        var existing = patientRepository.findByEmailOrPhone(email, phone);
        return existing == null; // true means no duplicate found
    }

    /* ====================== validatePatientLogin ====================== */
    public ResponseEntity<Map<String, String>> validatePatientLogin(Login login) {
        try {
            if (login == null || login.getIdentifier() == null || login.getPassword() == null) {
                return resp(400, "Missing credentials");
            }
            Patient p = patientRepository.findByEmail(login.getIdentifier());
            if (p == null || !Objects.equals(p.getPassword(), login.getPassword())) {
                return resp(401, "Invalid email or password");
            }
            String token = tokenService.generateToken(p.getEmail());
            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "role", "PATIENT",
                    "name", p.getName()
            ));
        } catch (Exception e) {
            return resp(500, "Internal error");
        }
    }

    /* ====================== filterPatient ====================== */
    // Delegates to PatientService based on provided filters
    public ResponseEntity<Map<String, Object>> filterPatient(String condition, String name, String token) {
        try {
            if (token == null || token.isBlank()) {
                return ResponseEntity.status(400).body(Map.of("error", "Token required"));
            }
            // identify patient
            String email = tokenService.extractIdentifier(token); 
            if (email == null || email.isBlank()) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
            }
            Patient patient = patientRepository.findByEmail(email);
            if (patient == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Patient not found"));
            }

            boolean hasCond = condition != null && !condition.isBlank();
            boolean hasName = name != null && !name.isBlank();

            if (hasCond && hasName) {
                return patientService.filterByDoctorAndCondition(condition.trim(), name.trim(), patient.getId());
            } else if (hasCond) {
                return patientService.filterByCondition(condition.trim(), patient.getId());
            } else if (hasName) {
                return patientService.filterByDoctor(name.trim(), patient.getId());
            } else {
                // no filters: return all appointments for this patient
                return patientService.getPatientAppointment(patient.getId(), token);
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal error"));
        }
    }

    /* ====================== helpers ====================== */

    private ResponseEntity<Map<String, String>> resp(int status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }

    // Normalize inputs like "9:00", "09:00", "09:00 AM" → "HH:mm"
    private String normalizeHHmm(String raw) {
        if (raw == null) return "";
        String s = raw.trim().toUpperCase(Locale.ROOT);
        if (s.matches("\\d{2}:\\d{2}")) return s;          // "09:00"
        if (s.matches("\\d{1}:\\d{2}")) return "0" + s;    // "9:00" -> "09:00"
        if (s.matches("\\d{1,2}:\\d{2}\\s?(AM|PM)")) {     // "09:00 AM"
            String[] parts = s.split("\\s+");
            String hhmm = parts[0];
            boolean pm = s.endsWith("PM");
            String[] hm = hhmm.split(":");
            int h = Integer.parseInt(hm[0]);
            int m = Integer.parseInt(hm[1]);
            if (pm && h < 12) h += 12;
            if (!pm && h == 12) h = 0;
            return String.format("%02d:%02d", h, m);
        }
        return s; // fallback
    }
}
