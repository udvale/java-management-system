package com.project.back_end.DTO;

public class Login {

    // Fields
    private String identifier; // email (Doctor/Patient) or username (Admin)
    private String password;

    // Default constructor (needed for deserialization with @RequestBody)
    public Login() {}

    // All-args constructor (optional, for convenience)
    public Login(String identifier, String password) {
        this.identifier = identifier;
        this.password = password;
    }

    // Getters and Setters
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
