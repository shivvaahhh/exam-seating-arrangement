package com.example.examseating.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "examination_registrations",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_exam_student_registration", columnNames = {"examination_id", "student_id"})
    }
)
public class ExaminationRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "examination_id", nullable = false)
    private Examination examination;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "is_allocated", nullable = false)
    private boolean isAllocated = false;

    @Column(name = "registered_at", updatable = false)
    private LocalDateTime registeredAt;

    public ExaminationRegistration() {}

    public ExaminationRegistration(Examination examination, Student student) {
        this.examination = examination;
        this.student = student;
        this.isAllocated = false;
    }

    @PrePersist
    protected void onCreate() {
        this.registeredAt = LocalDateTime.now();
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

    public boolean isAllocated() {
        return isAllocated;
    }

    public void setAllocated(boolean allocated) {
        isAllocated = allocated;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }
}
