package com.project.back_end.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "name cannot be null")
    @Size(min = 3, max = 100, message = "name must be between 3 and 100 characters")
    private String name;

    @NotNull(message = "specialty cannot be null")
    @Size(min = 3, max = 50, message = "specialty must be between 3 and 50 characters")
    private String specialty;

    @NotNull(message = "email cannot be null")
    @Email(message = "email must be a valid address")
    private String email;

    @NotNull(message = "password cannot be null")
    @Size(min = 6, message = "password must be at least 6 characters")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @NotNull(message = "phone cannot be null")
    @Pattern(regexp = "\\d{10}", message = "Phone number must be 10 digits")
    private String phone;

    @ElementCollection
    @CollectionTable(name = "doctor_available_times", joinColumns = @JoinColumn(name = "doctor_id"))
    @Column(name = "time_slot")
    private List<String> availableTimes = new ArrayList<>();

    // ----- Constructors -----
    public Doctor() {}

    public Doctor(String name, String specialty, String email, String password, String phone) {
        this.name = name;
        this.specialty = specialty;
        this.email = email;
        this.password = password;
        this.phone = phone;
    }

    // ----- Getters & Setters -----
    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getSpecialty() { return specialty; }

    public void setSpecialty(String specialty) { this.specialty = specialty; }

    public String getEmail() { return email; }

    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }

    public void setPassword(String password) { this.password = password; }

    public String getPhone() { return phone; }

    public void setPhone(String phone) { this.phone = phone; }

    public List<String> getAvailableTimes() { return availableTimes; }

    public void setAvailableTimes(List<String> availableTimes) { this.availableTimes = availableTimes; }
}
