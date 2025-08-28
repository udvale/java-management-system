package com.project.back_end.repo;

import com.project.back_end.models.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // 1) Appointments for a doctor in a time window (fetch doctor/patient and optionally doctor availability)
    @Query("""
           SELECT DISTINCT a
           FROM Appointment a
           LEFT JOIN FETCH a.doctor d
           LEFT JOIN FETCH a.patient p
           LEFT JOIN FETCH d.availableTimes at   -- remove this line if you don't keep availability on Doctor
           WHERE d.id = :doctorId
             AND a.appointmentTime BETWEEN :start AND :end
           ORDER BY a.appointmentTime ASC
           """)
    List<Appointment> findByDoctorIdAndAppointmentTimeBetween(@Param("doctorId") Long doctorId,
                                                              @Param("start") LocalDateTime start,
                                                              @Param("end") LocalDateTime end);

    // 2) Appointments by doctor + patient name (case-insensitive) in time window
    @Query("""
           SELECT DISTINCT a
           FROM Appointment a
           LEFT JOIN FETCH a.doctor d
           LEFT JOIN FETCH a.patient p
           WHERE d.id = :doctorId
             AND LOWER(p.name) LIKE LOWER(CONCAT('%', :patientName, '%'))
             AND a.appointmentTime BETWEEN :start AND :end
           ORDER BY a.appointmentTime ASC
           """)
    List<Appointment> findByDoctorIdAndPatient_NameContainingIgnoreCaseAndAppointmentTimeBetween(
            @Param("doctorId") Long doctorId,
            @Param("patientName") String patientName,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // 3) Delete all appointments for a doctor
    @Modifying
    @Transactional
    @Query("DELETE FROM Appointment a WHERE a.doctor.id = :doctorId")
    void deleteAllByDoctorId(@Param("doctorId") Long doctorId);

    // 4) All appointments for a patient
    List<Appointment> findByPatient_Id(Long patientId);

    // 5) Appointments for a patient by status, ordered by time
    List<Appointment> findByPatient_IdAndStatusOrderByAppointmentTimeAsc(Long patientId, int status);

    // 6) Filter by (partial) doctor name + patient id
    @Query("""
           SELECT a
           FROM Appointment a
           JOIN a.doctor d
           JOIN a.patient p
           WHERE p.id = :patientId
             AND LOWER(d.name) LIKE LOWER(CONCAT('%', :doctorName, '%'))
           ORDER BY a.appointmentTime ASC
           """)
    List<Appointment> filterByDoctorNameAndPatientId(@Param("doctorName") String doctorName,
                                                     @Param("patientId") Long patientId);

    // 7) Filter by (partial) doctor name + patient id + status
    @Query("""
           SELECT a
           FROM Appointment a
           JOIN a.doctor d
           JOIN a.patient p
           WHERE p.id = :patientId
             AND a.status = :status
             AND LOWER(d.name) LIKE LOWER(CONCAT('%', :doctorName, '%'))
           ORDER BY a.appointmentTime ASC
           """)
    List<Appointment> filterByDoctorNameAndPatientIdAndStatus(@Param("doctorName") String doctorName,
                                                              @Param("patientId") Long patientId,
                                                              @Param("status") int status);

    // (Optional) 8) Update status by id
    @Modifying
    @Transactional
    @Query("UPDATE Appointment a SET a.status = :status WHERE a.id = :id")
    void updateStatus(@Param("status") int status, @Param("id") long id);
}
