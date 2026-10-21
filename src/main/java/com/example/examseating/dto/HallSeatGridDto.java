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
    private String subjectCode;
    private String subjectName;
    private String examDate;
    private String examTime;
    private String examSession;

    // Adjacency Validation & Mode
    private String allocationMode = "SEQUENTIAL";
    private int horizontalAdjacentPairs = 0;
    private int verticalAdjacentPairs = 0;
    private int diagonalAdjacentPairs = 0;
    private int totalAdjacentPairs = 0;
    private String validationSummary = "Adjacent same-dept pairs: 0";
    private String validationWarning;

    // Grid: row by row, each row contains columns (for admin seating chart)
    private List<List<SeatCellDto>> grid = new ArrayList<>();

    // Structured row list (for invigilator chart)
    private List<RowDto> gridRows = new ArrayList<>();

    public HallSeatGridDto() {}

    public static class RowDto {
        private int rowNumber;
        private String rowLetter;
        private List<SeatCellDto> seats = new ArrayList<>();

        public RowDto() {}

        public RowDto(int rowNumber, String rowLetter) {
            this.rowNumber = rowNumber;
            this.rowLetter = rowLetter;
        }

        public int getRowNumber() { return rowNumber; }
        public void setRowNumber(int rowNumber) { this.rowNumber = rowNumber; }
        public String getRowLetter() { return rowLetter; }
        public void setRowLetter(String rowLetter) { this.rowLetter = rowLetter; }
        public List<SeatCellDto> getSeats() { return seats; }
        public void setSeats(List<SeatCellDto> seats) { this.seats = seats; }
    }

    public static class SeatCellDto {
        private int rowNumber;
        private int columnNumber;
        private String rowLetter; // A, B, C...
        private String seatNumber; // e.g. "A-01"
        private boolean isAvailableSeat = true; // whether within capacity
        private boolean isOccupied = false;
        private String status = "EMPTY"; // EMPTY, OCCUPIED, UNAVAILABLE
        private Long studentId;
        private String registerNumber;
        private String studentName;
        private String departmentCode;
        private Integer semester;
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
        public int getRow() { return rowNumber; }
        public void setRow(int row) { this.rowNumber = row; }

        public int getColumnNumber() { return columnNumber; }
        public void setColumnNumber(int columnNumber) { this.columnNumber = columnNumber; }
        public int getColumn() { return columnNumber; }
        public void setColumn(int column) { this.columnNumber = column; }

        public String getRowLetter() { return rowLetter; }
        public void setRowLetter(String rowLetter) { this.rowLetter = rowLetter; }
        public String getSeatNumber() { return seatNumber; }
        public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }

        public boolean isAvailableSeat() { return isAvailableSeat; }
        public void setAvailableSeat(boolean availableSeat) {
            isAvailableSeat = availableSeat;
            if (!availableSeat) this.status = "UNAVAILABLE";
        }

        public boolean isOccupied() { return isOccupied; }
        public void setOccupied(boolean occupied) {
            isOccupied = occupied;
            if (occupied) this.status = "OCCUPIED";
        }

        public String getStatus() { return status; }
        public void setStatus(String status) {
            this.status = status;
            if ("OCCUPIED".equalsIgnoreCase(status)) {
                this.isOccupied = true;
                this.isAvailableSeat = true;
            } else if ("UNAVAILABLE".equalsIgnoreCase(status)) {
                this.isOccupied = false;
                this.isAvailableSeat = false;
            } else {
                this.isOccupied = false;
                this.isAvailableSeat = true;
            }
        }

        public Long getStudentId() { return studentId; }
        public void setStudentId(Long studentId) { this.studentId = studentId; }
        public String getRegisterNumber() { return registerNumber; }
        public void setRegisterNumber(String registerNumber) { this.registerNumber = registerNumber; }
        public String getStudentName() { return studentName; }
        public void setStudentName(String studentName) { this.studentName = studentName; }
        public String getDepartmentCode() { return departmentCode; }
        public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }
        public Integer getSemester() { return semester; }
        public void setSemester(Integer semester) { this.semester = semester; }
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
    public int getRows() { return numberOfRows; }
    public void setRows(int rows) { this.numberOfRows = rows; }

    public int getNumberOfColumns() { return numberOfColumns; }
    public void setNumberOfColumns(int numberOfColumns) { this.numberOfColumns = numberOfColumns; }
    public int getColumns() { return numberOfColumns; }
    public void setColumns(int columns) { this.numberOfColumns = columns; }

    public int getTotalCapacity() { return totalCapacity; }
    public void setTotalCapacity(int totalCapacity) { this.totalCapacity = totalCapacity; }

    public int getAllocatedSeatsCount() { return allocatedSeatsCount; }
    public void setAllocatedSeatsCount(int allocatedSeatsCount) { this.allocatedSeatsCount = allocatedSeatsCount; }
    public int getAllocatedCount() { return allocatedSeatsCount; }
    public void setAllocatedCount(int allocatedCount) { this.allocatedSeatsCount = allocatedCount; }

    public int getEmptySeatsCount() { return emptySeatsCount; }
    public void setEmptySeatsCount(int emptySeatsCount) { this.emptySeatsCount = emptySeatsCount; }

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

    public String getExamSession() { return examSession; }
    public void setExamSession(String examSession) { this.examSession = examSession; }

    public List<List<SeatCellDto>> getGrid() { return grid; }
    public void setGrid(List<List<SeatCellDto>> grid) { this.grid = grid; }

    public List<RowDto> getGridRows() { return gridRows; }
    public void setGridRows(List<RowDto> gridRows) { this.gridRows = gridRows; }

    public String getAllocationMode() { return allocationMode; }
    public void setAllocationMode(String allocationMode) { this.allocationMode = allocationMode; }

    public int getHorizontalAdjacentPairs() { return horizontalAdjacentPairs; }
    public void setHorizontalAdjacentPairs(int horizontalAdjacentPairs) { this.horizontalAdjacentPairs = horizontalAdjacentPairs; }

    public int getVerticalAdjacentPairs() { return verticalAdjacentPairs; }
    public void setVerticalAdjacentPairs(int verticalAdjacentPairs) { this.verticalAdjacentPairs = verticalAdjacentPairs; }

    public int getDiagonalAdjacentPairs() { return diagonalAdjacentPairs; }
    public void setDiagonalAdjacentPairs(int diagonalAdjacentPairs) { this.diagonalAdjacentPairs = diagonalAdjacentPairs; }

    public int getTotalAdjacentPairs() { return totalAdjacentPairs; }
    public void setTotalAdjacentPairs(int totalAdjacentPairs) { this.totalAdjacentPairs = totalAdjacentPairs; }

    public String getValidationSummary() { return validationSummary; }
    public void setValidationSummary(String validationSummary) { this.validationSummary = validationSummary; }

    public String getValidationWarning() { return validationWarning; }
    public void setValidationWarning(String validationWarning) { this.validationWarning = validationWarning; }
}
