package com.example.examseating.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "invigilator_assignments",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_exam_invigilator", columnNames = {"examination_id", "invigilator_id"}),
        @UniqueConstraint(name = "uk_exam_hall_invigilator", columnNames = {"examination_id", "examination_hall_id", "invigilator_id"})
    }
)
public class InvigilatorAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assignment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "examination_id", nullable = false)
    private Examination examination;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "examination_hall_id", nullable = false)
    private ExaminationHall examinationHall;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "invigilator_id", nullable = false)
    private Invigilator invigilator;

    @Column(name = "assigned_at", updatable = false)
    private LocalDateTime assignedAt;

    public InvigilatorAssignment() {}

    public InvigilatorAssignment(Examination examination, ExaminationHall examinationHall, Invigilator invigilator) {
        this.examination = examination;
        this.examinationHall = examinationHall;
        this.invigilator = invigilator;
    }

    @PrePersist
    protected void onCreate() {
        this.assignedAt = LocalDateTime.now();
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

    public ExaminationHall getExaminationHall() {
        return examinationHall;
    }

    public void setExaminationHall(ExaminationHall examinationHall) {
        this.examinationHall = examinationHall;
    }

    public Invigilator getInvigilator() {
        return invigilator;
    }

    public void setInvigilator(Invigilator invigilator) {
        this.invigilator = invigilator;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }
}
