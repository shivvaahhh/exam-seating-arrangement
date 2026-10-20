package com.example.examseating.service;

import com.example.examseating.entity.Examination;
import com.example.examseating.entity.ExaminationRegistration;
import com.example.examseating.entity.Student;
import com.example.examseating.exception.DuplicateResourceException;
import com.example.examseating.exception.ResourceNotFoundException;
import com.example.examseating.repository.ExaminationRegistrationRepository;
import com.example.examseating.repository.ExaminationRepository;
import com.example.examseating.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ExaminationRegistrationService {

    private final ExaminationRegistrationRepository registrationRepository;
    private final ExaminationRepository examinationRepository;
    private final StudentRepository studentRepository;

    public ExaminationRegistrationService(ExaminationRegistrationRepository registrationRepository,
                                         ExaminationRepository examinationRepository,
                                         StudentRepository studentRepository) {
        this.registrationRepository = registrationRepository;
        this.examinationRepository = examinationRepository;
        this.studentRepository = studentRepository;
    }

    public List<ExaminationRegistration> getRegistrationsForExam(Long examId) {
        return registrationRepository.findByExaminationIdOrderByStudent_RegisterNumberAsc(examId);
    }

    public long countEligibleStudents(Long examId) {
        return registrationRepository.countByExaminationId(examId);
    }

    @Transactional
    public int registerStudentsByDepartmentAndSemester(Long examId, Long departmentId, Integer semester) {
        Examination exam = examinationRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Examination not found with ID: " + examId));

        List<Student> students;
        if (departmentId != null && semester != null) {
            students = studentRepository.findByDepartmentIdAndSemester(departmentId, semester);
        } else if (departmentId != null) {
            students = studentRepository.findByDepartmentId(departmentId);
        } else if (semester != null) {
            students = studentRepository.findBySemester(semester);
        } else {
            students = studentRepository.findByDepartmentIdAndSemester(exam.getDepartment().getId(), exam.getSemester());
        }

        int registeredCount = 0;
        for (Student student : students) {
            if (!"ACTIVE".equalsIgnoreCase(student.getStatus())) {
                continue;
            }
            if (!registrationRepository.existsByExaminationIdAndStudentId(examId, student.getId())) {
                ExaminationRegistration reg = new ExaminationRegistration(exam, student);
                registrationRepository.save(reg);
                registeredCount++;
            }
        }
        return registeredCount;
    }

    @Transactional
    public ExaminationRegistration registerStudent(Long examId, Long studentId) {
        Examination exam = examinationRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Examination not found with ID: " + examId));

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + studentId));

        if (registrationRepository.existsByExaminationIdAndStudentId(examId, studentId)) {
            throw new DuplicateResourceException("Student " + student.getRegisterNumber() + " is already registered for this examination.");
        }

        ExaminationRegistration reg = new ExaminationRegistration(exam, student);
        return registrationRepository.save(reg);
    }

    @Transactional
    public void removeRegistration(Long registrationId) {
        ExaminationRegistration reg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found with ID: " + registrationId));

        if (reg.isAllocated()) {
            throw new IllegalStateException("Cannot remove student " + reg.getStudent().getRegisterNumber() +
                    " because seat allocation has already been finalized. Clear or regenerate allocation first.");
        }

        registrationRepository.delete(reg);
    }

    @Transactional
    public void removeStudentFromExam(Long examId, Long studentId) {
        ExaminationRegistration reg = registrationRepository.findByExaminationIdAndStudentId(examId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student is not registered for this examination."));

        if (reg.isAllocated()) {
            throw new IllegalStateException("Cannot remove student because seat allocation has already been generated.");
        }

        registrationRepository.delete(reg);
    }
}
