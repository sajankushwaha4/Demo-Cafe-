package com.kushwahacafe.cafe.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 120)
    private String email;

    @Column(nullable = false, length = 15)
    private String phone;

    @Column(nullable = false, length = 20)
    private String date;

    @Column(name = "slot_in", nullable = false, length = 10)
    private String slotIn;

    @Column(name = "slot_out", nullable = false, length = 10)
    private String slotOut;

    @Column(nullable = false, length = 20)
    private String status = "Active"; // 'Active', 'Released'

    @Column(nullable = false)
    private Integer guests;

    @Column(name = "special_requests", columnDefinition = "TEXT")
    private String specialRequests;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private CafeLocation location;

    @Column(name = "hourly_rate", nullable = false)
    private Double hourlyRate = 0.0;

    @Column(name = "total_charge", nullable = false)
    private Double totalCharge = 0.0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Default constructor
    public Reservation() {}

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getSlotIn() {
        return slotIn;
    }

    public void setSlotIn(String slotIn) {
        this.slotIn = slotIn;
    }

    public String getSlotOut() {
        return slotOut;
    }

    public void setSlotOut(String slotOut) {
        this.slotOut = slotOut;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getGuests() {
        return guests;
    }

    public void setGuests(Integer guests) {
        this.guests = guests;
    }

    public String getSpecialRequests() {
        return specialRequests;
    }

    public void setSpecialRequests(String specialRequests) {
        this.specialRequests = specialRequests;
    }

    public CafeLocation getLocation() {
        return location;
    }

    public void setLocation(CafeLocation location) {
        this.location = location;
    }

    public Double getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(Double hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    public Double getTotalCharge() {
        return totalCharge;
    }

    public void setTotalCharge(Double totalCharge) {
        this.totalCharge = totalCharge;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
