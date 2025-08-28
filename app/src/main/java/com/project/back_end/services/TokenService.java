package com.project.back_end.services;

import com.project.back_end.models.Admin;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

@Component
public class TokenService {

    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    @Value("${jwt.secret}")
    private String secret;

    public TokenService(AdminRepository adminRepository,
                        DoctorRepository doctorRepository,
                        PatientRepository patientRepository) {
        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
    }

    /* ====================== generateToken ====================== */
    public String generateToken(String identifier) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000L); // 7 days

        return Jwts.builder()
                .subject(identifier)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    /* ====================== extractIdentifier ====================== */
    public String extractIdentifier(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    /* ====================== validateToken ====================== */
    public boolean validateToken(String token, String userType) {
        try {
            String identifier = extractIdentifier(token);
            if (identifier == null) return false;

            return switch (userType.toLowerCase()) {
                case "admin" -> {
                    Admin admin = adminRepository.findByUsername(identifier);
                    yield admin != null;
                }
                // in TokenService.validateToken(...)
               case "doctor" -> {
                  com.project.back_end.models.Doctor doctor = doctorRepository.findByEmail(identifier);
                  yield doctor != null;
               }

                case "patient" -> {
                    Patient patient = patientRepository.findByEmail(identifier);
                    yield patient != null;
                }
                default -> false;
            };
        } catch (Exception e) {
            return false;
        }
    }

    /* ====================== getSigningKey ====================== */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
