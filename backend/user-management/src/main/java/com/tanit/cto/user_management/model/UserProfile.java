package com.tanit.cto.user_management.model;

public class UserProfile {
    private String email;
    private String fullName;
    private String gender;
    private boolean enabled;

    public UserProfile(String email, String fullName, String gender, boolean enabled) {
        this.email = email;
        this.fullName = fullName;
        this.gender = gender;
        this.enabled = enabled;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getGender() {
        return gender;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
