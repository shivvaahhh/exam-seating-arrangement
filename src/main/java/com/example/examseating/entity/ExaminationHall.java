package com.example.examseating.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "examination_halls")
public class ExaminationHall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hall_name", nullable = false, unique = true, length = 100)
    private String hallName;

    @Column(name = "building_name", nullable = false, length = 100)
    private String buildingName;

    @Column(length = 30)
    private String floor;

    @Column(name = "room_number", nullable = false, length = 30)
    private String roomNumber;

    @Column(name = "total_capacity", nullable = false)
    private Integer totalCapacity;

    @Column(name = "number_of_rows", nullable = false)
    private Integer numberOfRows;

    @Column(name = "number_of_columns", nullable = false)
    private Integer numberOfColumns;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE"; // "ACTIVE" or "INACTIVE"

    @Column(length = 500)
    private String description;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public ExaminationHall() {}

    public ExaminationHall(String hallName, String buildingName, String floor, String roomNumber,
                           Integer totalCapacity, Integer numberOfRows, Integer numberOfColumns) {
        this.hallName = hallName;
        this.buildingName = buildingName;
        this.floor = floor;
        this.roomNumber = roomNumber;
        this.totalCapacity = totalCapacity;
        this.numberOfRows = numberOfRows;
        this.numberOfColumns = numberOfColumns;
        this.status = "ACTIVE";
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Usable capacity cannot exceed rows * columns
    public int getMaxPhysicalSeats() {
        if (numberOfRows != null && numberOfColumns != null) {
            return numberOfRows * numberOfColumns;
        }
        return totalCapacity != null ? totalCapacity : 0;
    }

    public int getEffectiveCapacity() {
        int physical = getMaxPhysicalSeats();
        if (totalCapacity != null && totalCapacity > 0) {
            return Math.min(totalCapacity, physical);
        }
        return physical;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getHallName() {
        return hallName;
    }

    public void setHallName(String hallName) {
        this.hallName = hallName;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    public String getFloor() {
        return floor;
    }

    public void setFloor(String floor) {
        this.floor = floor;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public Integer getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(Integer totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public Integer getNumberOfRows() {
        return numberOfRows;
    }

    public void setNumberOfRows(Integer numberOfRows) {
        this.numberOfRows = numberOfRows;
    }

    public Integer getNumberOfColumns() {
        return numberOfColumns;
    }

    public void setNumberOfColumns(Integer numberOfColumns) {
        this.numberOfColumns = numberOfColumns;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
