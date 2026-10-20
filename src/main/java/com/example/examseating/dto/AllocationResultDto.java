package com.example.examseating.dto;

import java.util.ArrayList;
import java.util.List;

public class AllocationResultDto {

    private Long examinationId;
    private String examinationName;
    private String subjectCode;
    private String subjectName;
    private String examDate;
    private String examTime;

    private int totalEligibleStudents;
    private int totalAllocatedStudents;
    private int totalHallsUsed;
    private int totalAvailableSeats;
    private int unallocatedStudents;

    private String allocationStatus;
    private boolean success;
    private String message;

    private List<HallSummaryDto> hallSummaries = new ArrayList<>();

    public AllocationResultDto() {}

    public static class HallSummaryDto {
        private Long hallId;
        private String hallName;
        private String buildingName;
        private String roomNumber;
        private int capacity;
        private int allocatedCount;
        private int remainingSeats;

        public HallSummaryDto() {}

        public HallSummaryDto(Long hallId, String hallName, String buildingName, String roomNumber,
                              int capacity, int allocatedCount) {
            this.hallId = hallId;
            this.hallName = hallName;
            this.buildingName = buildingName;
            this.roomNumber = roomNumber;
            this.capacity = capacity;
            this.allocatedCount = allocatedCount;
            this.remainingSeats = Math.max(0, capacity - allocatedCount);
        }

        public Long getHallId() { return hallId; }
        public void setHallId(Long hallId) { this.hallId = hallId; }
        public String getHallName() { return hallName; }
        public void setHallName(String hallName) { this.hallName = hallName; }
        public String getBuildingName() { return buildingName; }
        public void setBuildingName(String buildingName) { this.buildingName = buildingName; }
        public String getRoomNumber() { return roomNumber; }
        public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
        public int getCapacity() { return capacity; }
        public void setCapacity(int capacity) { this.capacity = capacity; }
        public int getAllocatedCount() { return allocatedCount; }
        public void setAllocatedCount(int allocatedCount) { this.allocatedCount = allocatedCount; }
        public int getRemainingSeats() { return remainingSeats; }
        public void setRemainingSeats(int remainingSeats) { this.remainingSeats = remainingSeats; }
    }

    // Getters and Setters
    public Long getExaminationId() { return examinationId; }
    public void setExaminationId(Long examinationId) { this.examinationId = examinationId; }
    public String getExaminationName() { return examinationName; }
    public void setExaminationName(String examinationName) { this.examinationName = examinationName; }
    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
    public String getExamDate() { return examDate; }
    public void setExamDate(String examDate) { this.examDate = examDate; }
    public String getExamTime() { return examTime; }
    public void setExamTime(String examTime) { this.examTime = examTime; }
    public int getTotalEligibleStudents() { return totalEligibleStudents; }
    public void setTotalEligibleStudents(int totalEligibleStudents) { this.totalEligibleStudents = totalEligibleStudents; }
    public int getTotalAllocatedStudents() { return totalAllocatedStudents; }
    public void setTotalAllocatedStudents(int totalAllocatedStudents) { this.totalAllocatedStudents = totalAllocatedStudents; }
    public int getTotalHallsUsed() { return totalHallsUsed; }
    public void setTotalHallsUsed(int totalHallsUsed) { this.totalHallsUsed = totalHallsUsed; }
    public int getTotalAvailableSeats() { return totalAvailableSeats; }
    public void setTotalAvailableSeats(int totalAvailableSeats) { this.totalAvailableSeats = totalAvailableSeats; }
    public int getUnallocatedStudents() { return unallocatedStudents; }
    public void setUnallocatedStudents(int unallocatedStudents) { this.unallocatedStudents = unallocatedStudents; }
    public String getAllocationStatus() { return allocationStatus; }
    public void setAllocationStatus(String allocationStatus) { this.allocationStatus = allocationStatus; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public List<HallSummaryDto> getHallSummaries() { return hallSummaries; }
    public void setHallSummaries(List<HallSummaryDto> hallSummaries) { this.hallSummaries = hallSummaries; }
}
