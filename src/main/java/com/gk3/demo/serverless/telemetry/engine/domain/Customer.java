package com.gk3.demo.serverless.telemetry.engine.domain;

public class Customer {
    private final String id;
    private String name;
    private String email;
    private final boolean premiumSupportActive; // Business rule (e.g. priority for alerts)

    public Customer(String id, String name, String email, boolean premiumSupportActive) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.premiumSupportActive = premiumSupportActive;
    }

    // Customer-specific business logic
    public void updateContactDetails(String newName, String newEmail) {
        if (newEmail == null || !newEmail.contains("@")) {
            throw new IllegalArgumentException("Invalid email address format.");
        }
        this.name = newName;
        this.email = newEmail;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public boolean isPremiumSupportActive() { return premiumSupportActive; }
}
