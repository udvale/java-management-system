package com.project.back_end.services;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final TokenService tokenService;

    public DoctorService(DoctorRepository doctorRepository,
                         AppointmentRepository appointmentRepository,
                         TokenService tokenService) {
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.tokenService = tokenService;
    }

    /* ============================================================
       getDoctorAvailability
       - returns doctor's available time slots (String) for a date,
         excluding already-booked appointment times
       ============================================================ */
    @Transactional(readOnly = true)
    public List<String> getDoctorAvailability(Long doctorId, LocalDate date) {
        if (doctorId == null || date == null) return Collections.emptyList();

        Optional<Doctor> docOpt = doctorRepository.findById(doctorId);
        if (docOpt.isEmpty()) return Collections.emptyList();

        // canonical available slots configured on the doctor (e.g., ["09:00","09:30","10:00",...])
        List<String> allSlots = Optional.ofNullable(docOpt.get().getAvailableTimes())
                .orElseGet(ArrayList::new);

        if (allSlots.isEmpty()) return allSlots;

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay().minusNanos(1);

        // appointments already booked that day
        List<Appointment> appts = appointmentRepository
                .findByDoctorIdAndAppointmentTimeBetween(doctorId, start, end);

        // normalize booked times to "HH:mm"
        DateTimeFormatter HHmm = DateTimeFormatter.ofPattern("HH:mm");
        Set<String> booked = appts.stream()
                .map(a -> a.getAppointmentTime() != null ? a.getAppointmentTime().format(HHmm) : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return allSlots.stream()
                .filter(s -> !booked.contains(normalizeSlot(s)))
                .sorted(this::compareHHmm)
                .collect(Collectors.toList());
    }

    /* ============================================================
       saveDoctor
       - 1 success, -1 conflict(email exists), 0 error
       ============================================================ */
    @Transactional
    public int saveDoctor(Doctor doctor) {
        try {
            if (doctor == null || doctor.getEmail() == null) return 0;
            if (doctorRepository.findByEmail(doctor.getEmail()) != null) return -1;
            doctorRepository.save(doctor);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    /* ============================================================
       updateDoctor
       - 1 success, -1 not found, 0 error
       ============================================================ */
    @Transactional
    public int updateDoctor(Doctor doctor) {
        try {
            if (doctor == null || doctor.getId() == null) return 0;
            if (!doctorRepository.existsById(doctor.getId())) return -1;
            doctorRepository.save(doctor);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    /* ============================================================
       getDoctors
       ============================================================ */
    @Transactional(readOnly = true)
    public List<Doctor> getDoctors() {
        return doctorRepository.findAll();
    }

    /* ============================================================
       deleteDoctor
       - 1 success, -1 not found, 0 error
       ============================================================ */
    @Transactional
    public int deleteDoctor(long id) {
        try {
            if (!doctorRepository.existsById(id)) return -1;
            // remove all appointments for this doctor first
            appointmentRepository.deleteAllByDoctorId(id);
            doctorRepository.deleteById(id);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    /* ============================================================
       validateDoctor
       - Login.identifier is email for doctors
       - returns { token, name, role } or 401 with error
       ============================================================ */
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, String>> validateDoctor(Login login) {
        if (login == null || login.getIdentifier() == null || login.getPassword() == null) {
            return ResponseEntity.status(400).body(Map.of("error", "Missing credentials"));
        }
        Doctor doc = doctorRepository.findByEmail(login.getIdentifier());
        if (doc == null || !Objects.equals(doc.getPassword(), login.getPassword())) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
        }

        String token = tokenService.generateToken(doc.getEmail()); // assumed method
        return ResponseEntity.ok(Map.of(
                "token", token,
                "name", doc.getName(),
                "role", "DOCTOR"
        ));
    }

    /* ============================================================
       findDoctorByName
       - uses partial name matching
       ============================================================ */
    @Transactional(readOnly = true)
    public Map<String, Object> findDoctorByName(String name) {
        List<Doctor> doctors = doctorRepository.findByNameLike(name == null ? "" : name);
        return Map.of("doctors", doctors);
    }

    /* ============================================================
       filterDoctorsByNameSpecilityandTime (AM/PM)
       ============================================================ */
    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorsByNameSpecilityandTime(String name, String specialty, String amOrPm) {
        List<Doctor> base = doctorRepository
                .findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(
                        name == null ? "" : name,
                        specialty == null ? "" : specialty
                );
        return Map.of("doctors", filterDoctorByTime(base, amOrPm));
    }

    /* ============================================================
       filterDoctorByNameAndTime
       ============================================================ */
    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorByNameAndTime(String name, String amOrPm) {
        List<Doctor> base = doctorRepository.findByNameLike(name == null ? "" : name);
        return Map.of("doctors", filterDoctorByTime(base, amOrPm));
    }

    /* ============================================================
       filterDoctorByNameAndSpecility
       ============================================================ */
    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorByNameAndSpecility(String name, String specilty) {
        List<Doctor> base = doctorRepository
                .findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(
                        name == null ? "" : name,
                        specilty == null ? "" : specilty
                );
        return Map.of("doctors", base);
    }

    /* ============================================================
       filterDoctorByTimeAndSpecility
       ============================================================ */
    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorByTimeAndSpecility(String specilty, String amOrPm) {
        List<Doctor> base = doctorRepository.findBySpecialtyIgnoreCase(specilty == null ? "" : specilty);
        return Map.of("doctors", filterDoctorByTime(base, amOrPm));
    }

    /* ============================================================
       filterDoctorBySpecility
       ============================================================ */
    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorBySpecility(String specilty) {
        List<Doctor> base = doctorRepository.findBySpecialtyIgnoreCase(specilty == null ? "" : specilty);
        return Map.of("doctors", base);
    }

    /* ============================================================
       filterDoctorsByTime (AM/PM across all doctors)
       ============================================================ */
    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorsByTime(String amOrPm) {
        List<Doctor> base = doctorRepository.findAll();
        return Map.of("doctors", filterDoctorByTime(base, amOrPm));
    }

    /* ============================================================
       Private helper: filterDoctorByTime
       - Doctors kept if ANY availableTimes entry falls in the requested half-day
       - availableTimes expected like "HH:mm" or "hh:mm AM/PM"
       ============================================================ */
    private List<Doctor> filterDoctorByTime(List<Doctor> doctors, String amOrPm) {
        if (amOrPm == null || amOrPm.isBlank()) return doctors;
        boolean wantAM = amOrPm.equalsIgnoreCase("AM");

        return doctors.stream()
                .filter(d -> {
                    List<String> slots = Optional.ofNullable(d.getAvailableTimes()).orElseGet(ArrayList::new);
                    if (slots.isEmpty()) return false;
                    return slots.stream().anyMatch(s -> isAM(normalizeSlot(s)) == wantAM);
                })
                .collect(Collectors.toList());
    }

    /* =================== utilities =================== */

    // normalize "9:00", "09:00", "09:00 AM" → "HH:mm" when possible; otherwise uppercase text
    private String normalizeSlot(String raw) {
        if (raw == null) return "";
        String s = raw.trim().toUpperCase(Locale.ROOT);

        // Already "HH:mm"
        if (s.matches("\\d{2}:\\d{2}")) return s;

        // "H:mm" -> pad
        if (s.matches("\\d{1}:\\d{2}")) return "0" + s;

        // "HH:MM AM/PM"
        if (s.matches("\\d{1,2}:\\d{2}\\s?(AM|PM)")) {
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

        // Fallback
        return s;
    }

    private boolean isAM(String normalizedHHmmOrText) {
        // If it's HH:mm, decide by hour < 12
        if (normalizedHHmmOrText.matches("\\d{2}:\\d{2}")) {
            int hour = Integer.parseInt(normalizedHHmmOrText.substring(0, 2));
            return hour < 12;
        }
        // Otherwise use AM/PM tokens
        if (normalizedHHmmOrText.contains("AM")) return true;
        if (normalizedHHmmOrText.contains("PM")) return false;
        // Unknown format → treat as not matching either
        return false;
    }

    private int compareHHmm(String a, String b) {
        String na = normalizeSlot(a);
        String nb = normalizeSlot(b);
        if (na.matches("\\d{2}:\\d{2}") && nb.matches("\\d{2}:\\d{2}")) {
            return na.compareTo(nb);
        }
        return a.compareToIgnoreCase(b);
    }
}
