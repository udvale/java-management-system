package com.project.back_end.repo;

import com.project.back_end.models.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    // 1) Find by email (exact match)
    Doctor findByEmail(String email);

    // 2) Partial name match using LIKE + CONCAT
    @Query("""
           SELECT d
           FROM Doctor d
           WHERE d.name LIKE CONCAT('%', :name, '%')
           """)
    List<Doctor> findByNameLike(@Param("name") String name);

    // 3) Partial name (case-insensitive) + exact specialty (case-insensitive)
    @Query("""
           SELECT d
           FROM Doctor d
           WHERE LOWER(d.name) LIKE LOWER(CONCAT('%', :name, '%'))
             AND LOWER(d.specialty) = LOWER(:specialty)
           """)
    List<Doctor> findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(@Param("name") String name,
                                                                      @Param("specialty") String specialty);

    // 4) Find by specialty, ignoring case
    List<Doctor> findBySpecialtyIgnoreCase(String specialty);
}
