package com.example.examseating.dto;

import java.util.ArrayList;
import java.util.List;

public class HallSeatGridDto {

    private Long hallId;
    private String hallName;
    private String buildingName;
    private String floor;
    private String roomNumber;
    private int numberOfRows;
    private int numberOfColumns;
    private int totalCapacity;
    private int allocatedSeatsCount;
    private int emptySeatsCount;

    private Long examinationId;
    private String examinationName;
    private String subjectName;
    private String examDate;
    private String examTime;

    // Grid: row by row, each row contains columns
    private List<List<SeatCellDto>> grid = new ArrayList<>();

    public HallSeatGridDto() {}

    public static class SeatCellDto {
        private int rowNumber;
        private int columnNumber;
        private String rowLetter; // A, B, C...
        private String seatNumber; // e.g. "A-01"
        private boolean isAvailableSeat = true; // whether within capacity
        private boolean isOccupied = false;
        private Long studentId;
        private String registerNumber;
        private String studentName;
        private String departmentCode;
        private String attendanceStatus;

        public SeatCellDto() {}

        public SeatCellDto(int rowNumber, int columnNumber, String rowLetter, String seatNumber) {
            this.rowNumber = rowNumber;
            this.columnNumber = columnNumber;
            this.rowLetter = rowLetter;
            this.seatNumber = seatNumber;
        }

        public int getRowNumber() { return rowNumber; }
        public void setRowNumber(int rowNumber) { this.rowNumber = rowNumber; }
        public int getColumnNumber() { return columnNumber; }
        public void setColumnNumber(int columnNumber) { this.columnNumber = columnNumber; }
        public String getRowLetter() { return rowLetter; }
        public void setRowLetter(String rowLetter) { this.rowLetter = rowLetter; }
        public String getSeatNumber() { return seatNumber; }
        public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }
        public boolean isAvailableSeat() { return isAvailableSeat; }
        public void setAvailableSeat(boolean availableSeat) { isAvailableSeat = availableSeat; }
        public boolean isOccupied() { return isOccupied; }
        public void setOccupied(boolean occupied) { isOccupied = occupied; }
        public Long getStudentId() { return studentId; }
        public void setStudentId(Long studentId) { this.studentId = studentId; }
        public String getRegisterNumber() { return registerNumber; }
        public void setRegisterNumber(String registerNumber) { this.registerNumber = registerNumber; }
        public String getStudentName() { return studentName; }
        public void setStudentName(String studentName) { this.studentName = studentName; }
        public String getDepartmentCode() { return departmentCode; }
        public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }
        public String getAttendanceStatus() { return attendanceStatus; }
        public void setAttendanceStatus(String attendanceStatus) { this.attendanceStatus = attendanceStatus; }
    }

    // Getters and Setters
    public Long getHallId() { return hallId; }
    public void setHallId(Long hallId) { this.hallId = hallId; }
    public String getHallName() { return hallName; }
    public void setHallName(String hallName) { this.hallName = hallName; }
    public String getBuildingName() { return buildingName; }
    public void setBuildingName(String buildingName) { this.buildingName = buildingName; }
    public String getFloor() { return floor; }
    public void setFloor(String floor) { this.floor = floor; }
    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public int getNumberOfRows() { return numberOfRows; }
    public void setNumberOfRows(int numberOfRows) { this.numberOfRows = numberOfRows; }
    public int getNumberOfColumns() { return numberOfColumns; }
    public void setNumberOfColumns(int numberOfColumns) { this.numberOfColumns = numberOfColumns; }
    public int getTotalCapacity() { return totalCapacity; }
    public void setTotalCapacity(int totalCapacity) { this.totalCapacity = totalCapacity; }
    public int getAllocatedSeatsCount() { return allocatedSeatsCount; }
    public void setAllocatedSeatsCount(int allocatedSeatsCount) { this.allocatedSeatsCount = allocatedSeatsCount; }
    public int getEmptySeatsCount() { return emptySeatsCount; }
    public void setEmptySeatsCount(int emptySeatsCount) { this.emptySeatsCount = emptySeatsCount; }
    public Long getExaminationId() { return examinationId; }
    public void setExaminationId(Long examinationId) { this.examinationId = examinationId; }
    public String getExaminationName() { return examinationName; }
    public void setExaminationName(String examinationName) { this.examinationName = examinationName; }
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
    public String getExamDate() { return examDate; }
    public void setExamDate(String examDate) { this.examDate = examDate; }
    public String getExamTime() { return examTime; }
    public void setExamTime(String examTime) { this.examTime = examTime; }
    public List<List<SeatCellDto>> getGrid() { return grid; }
    public void setGrid(List<List<SeatCellDto>> grid) { this.grid = grid; }
}
