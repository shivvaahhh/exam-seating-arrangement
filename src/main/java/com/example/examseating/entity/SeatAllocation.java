package com.example.examseating.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "seat_allocations",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_exam_student_allocation", columnNames = {"examination_id", "student_id"}),
        @UniqueConstraint(name = "uk_exam_hall_seat", columnNames = {"examination_id", "examination_hall_id", "seat_number"}),
        @UniqueConstraint(name = "uk_exam_hall_pos", columnNames = {"examination_id", "examination_hall_id", "row_number", "column_number"})
    }
)
public class SeatAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "examination_id", nullable = false)
    private Examination examination;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "examination_hall_id", nullable = false)
    private ExaminationHall examinationHall;

    @Column(name = "seat_number", nullable = false, length = 30)
    private String seatNumber;

    @Column(name = "row_number", nullable = false)
    private Integer rowNumber;

    @Column(name = "column_number", nullable = false)
    private Integer columnNumber;

    @Column(name = "attendance_status", nullable = false, length = 20)
    private String attendanceStatus = "PENDING"; // "PENDING", "PRESENT", "ABSENT"

    @Column(name = "allocated_at", updatable = false)
    private LocalDateTime allocatedAt;

    public SeatAllocation() {}

    public SeatAllocation(Examination examination, Student student, ExaminationHall examinationHall,
                          String seatNumber, Integer rowNumber, Integer columnNumber) {
        this.examination = examination;
        this.student = student;
        this.examinationHall = examinationHall;
        this.seatNumber = seatNumber;
        this.rowNumber = rowNumber;
        this.columnNumber = columnNumber;
        this.attendanceStatus = "PENDING";
    }

    @PrePersist
    protected void onCreate() {
        this.allocatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Examination getExamination() {
        return examination;
    }

    public void setExamination(Examination examination) {
        this.examination = examination;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public ExaminationHall getExaminationHall() {
        return examinationHall;
    }

    public void setExaminationHall(ExaminationHall examinationHall) {
        this.examinationHall = examinationHall;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public Integer getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(Integer rowNumber) {
        this.rowNumber = rowNumber;
    }

    public Integer getColumnNumber() {
        return columnNumber;
    }

    public void setColumnNumber(Integer columnNumber) {
        this.columnNumber = columnNumber;
    }

    public String getAttendanceStatus() {
        return attendanceStatus;
    }

    public void setAttendanceStatus(String attendanceStatus) {
        this.attendanceStatus = attendanceStatus;
    }

    public LocalDateTime getAllocatedAt() {
        return allocatedAt;
    }
}
