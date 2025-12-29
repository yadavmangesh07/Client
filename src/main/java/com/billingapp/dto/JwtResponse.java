package com.billingapp.dto; // (Check your package name)

public class JwtResponse {
    private String token;
    @SuppressWarnings("unused")
    private String type = "Bearer";
    private String username; // Add this
    private String role;     // Add this

    public JwtResponse(String accessToken, String username, String role) {
        this.token = accessToken;
        this.username = username;
        this.role = role;
    }

    // Getters and Setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}