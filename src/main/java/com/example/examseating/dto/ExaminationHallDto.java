package com.example.examseating.dto;

import jakarta.validation.constraints.*;

public class ExaminationHallDto {

    private Long id;

    @NotBlank(message = "Hall name is required")
    @Size(max = 100, message = "Hall name must not exceed 100 characters")
    private String hallName;

    @NotBlank(message = "Building name is required")
    @Size(max = 100, message = "Building name must not exceed 100 characters")
    private String buildingName;

    @Size(max = 30, message = "Floor must not exceed 30 characters")
    private String floor;

    @NotBlank(message = "Room number is required")
    @Size(max = 30, message = "Room number must not exceed 30 characters")
    private String roomNumber;

    @NotNull(message = "Total capacity is required")
    @Min(value = 1, message = "Total capacity must be at least 1")
    @Max(value = 1000, message = "Total capacity must not exceed 1000")
    private Integer totalCapacity;

    @NotNull(message = "Number of rows is required")
    @Min(value = 1, message = "Number of rows must be at least 1")
    @Max(value = 50, message = "Number of rows must not exceed 50")
    private Integer numberOfRows;

    @NotNull(message = "Number of columns is required")
    @Min(value = 1, message = "Number of columns must be at least 1")
    @Max(value = 50, message = "Number of columns must not exceed 50")
    private Integer numberOfColumns;

    private String status = "ACTIVE";

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    public ExaminationHallDto() {}

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

    public boolean hasCapacityMismatch() {
        return totalCapacity != null && totalCapacity > getMaxPhysicalSeats();
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
}
