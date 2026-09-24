package com.college.library.dto;

public class LoginResponse {

    private boolean success;
    private String token;
    private String name;
    private String email;
    private String role;
    private String message;

    public LoginResponse() {
    }

    public LoginResponse(boolean success, String token, String name, String email, String role, String message) {
        this.success = success;
        this.token = token;
        this.name = name;
        this.email = email;
        this.role = role;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
