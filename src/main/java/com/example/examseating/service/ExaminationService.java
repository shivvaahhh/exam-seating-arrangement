package com.example.examseating.service;

import com.example.examseating.dto.ExaminationDto;
import com.example.examseating.entity.Department;
import com.example.examseating.entity.Examination;
import com.example.examseating.entity.ExaminationRegistration;
import com.example.examseating.entity.Student;
import com.example.examseating.entity.Subject;
import com.example.examseating.exception.ResourceNotFoundException;
import com.example.examseating.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExaminationService {

    private final ExaminationRepository examinationRepository;
    private final DepartmentRepository departmentRepository;
    private final SubjectRepository subjectRepository;
    private final ExaminationRegistrationRepository registrationRepository;
    private final SeatAllocationRepository allocationRepository;
    private final StudentRepository studentRepository;

    public ExaminationService(ExaminationRepository examinationRepository,
                              DepartmentRepository departmentRepository,
                              SubjectRepository subjectRepository,
                              ExaminationRegistrationRepository registrationRepository,
                              SeatAllocationRepository allocationRepository,
                              StudentRepository studentRepository) {
        this.examinationRepository = examinationRepository;
        this.departmentRepository = departmentRepository;
        this.subjectRepository = subjectRepository;
        this.registrationRepository = registrationRepository;
        this.allocationRepository = allocationRepository;
        this.studentRepository = studentRepository;
    }

    public List<ExaminationDto> findAll() {
        return examinationRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<Examination> findAllEntities() {
        return examinationRepository.findAll();
    }

    public List<ExaminationDto> findUpcoming() {
        return examinationRepository.findByExaminationDateGreaterThanEqualOrderByExaminationDateAscStartTimeAsc(LocalDate.now())
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<ExaminationDto> findWithFilters(Long deptId, Integer semester, String status) {
        String cleanStatus = (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status))
                ? status.trim() : null;
        return examinationRepository.findWithFilters(deptId, semester, cleanStatus).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public Examination findEntityById(Long id) {
        return examinationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Examination not found with ID: " + id));
    }

    public ExaminationDto findById(Long id) {
        return toDto(findEntityById(id));
    }

    @Transactional
    public ExaminationDto create(ExaminationDto dto) {
        validateExamTimes(dto);

        Department department = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + dto.getDepartmentId()));

        Subject subject = subjectRepository.findById(dto.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with ID: " + dto.getSubjectId()));

        String session = dto.getSession() != null ? dto.getSession() : "FORENOON";

        // Conflict check: Block scheduling two exams in the same date+session for the same department+semester
        List<Examination> conflicts = examinationRepository.findConflictingExams(
                dto.getExaminationDate(), session, dto.getDepartmentId(), dto.getSemester(), null
        );
        if (!conflicts.isEmpty()) {
            throw new IllegalArgumentException(
                    "Scheduling Conflict: Examination '" + conflicts.get(0).getExaminationName() +
                    "' is already scheduled on " + dto.getExaminationDate() + " (" + session + ") for " +
                    department.getDepartmentName() + " Semester " + dto.getSemester() + "."
            );
        }

        Examination exam = new Examination(
                dto.getExaminationName().trim(),
                subject,
                department,
                dto.getSemester(),
                dto.getExaminationDate(),
                dto.getStartTime(),
                dto.getEndTime(),
                dto.getAcademicYear().trim(),
                session
        );
        exam.setStatus("SCHEDULED");

        Examination saved = examinationRepository.save(exam);

        // Auto-register enrolled active students of that department & semester
        List<Student> eligibleStudents = studentRepository.findByDepartmentIdAndSemester(department.getId(), dto.getSemester())
                .stream()
                .filter(s -> "ACTIVE".equalsIgnoreCase(s.getStatus()))
                .collect(Collectors.toList());

        for (Student s : eligibleStudents) {
            registrationRepository.save(new ExaminationRegistration(saved, s));
        }

        return toDto(saved);
    }

    @Transactional
    public ExaminationDto update(Long id, ExaminationDto dto) {
        Examination exam = findEntityById(id);
        validateExamTimes(dto);

        Department department = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + dto.getDepartmentId()));

        Subject subject = subjectRepository.findById(dto.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with ID: " + dto.getSubjectId()));

        String session = dto.getSession() != null ? dto.getSession() : "FORENOON";

        // Conflict check excluding current exam
        List<Examination> conflicts = examinationRepository.findConflictingExams(
                dto.getExaminationDate(), session, dto.getDepartmentId(), dto.getSemester(), id
        );
        if (!conflicts.isEmpty()) {
            throw new IllegalArgumentException(
                    "Scheduling Conflict: Examination '" + conflicts.get(0).getExaminationName() +
                    "' is already scheduled on " + dto.getExaminationDate() + " (" + session + ") for " +
                    department.getDepartmentName() + " Semester " + dto.getSemester() + "."
            );
        }

        exam.setExaminationName(dto.getExaminationName().trim());
        exam.setDepartment(department);
        exam.setSubject(subject);
        exam.setSemester(dto.getSemester());
        exam.setExaminationDate(dto.getExaminationDate());
        exam.setStartTime(dto.getStartTime());
        exam.setEndTime(dto.getEndTime());
        exam.setAcademicYear(dto.getAcademicYear().trim());
        exam.setSession(session);
        if (dto.getStatus() != null) {
            exam.setStatus(dto.getStatus());
        }

        Examination updated = examinationRepository.save(exam);
        return toDto(updated);
    }

    @Transactional
    public void cancel(Long id) {
        Examination exam = findEntityById(id);
        exam.setStatus("CANCELLED");
        examinationRepository.save(exam);
    }

    @Transactional
    public void delete(Long id) {
        Examination exam = findEntityById(id);
        long allocCount = allocationRepository.countByExaminationId(id);
        if (allocCount > 0) {
            throw new IllegalStateException("Cannot delete examination '" + exam.getExaminationName() +
                    "' because it has " + allocCount + " seat allocations. Please clear allocations first.");
        }

        registrationRepository.deleteByExaminationId(id);
        examinationRepository.delete(exam);
    }

    private void validateExamTimes(ExaminationDto dto) {
        if (dto.getStartTime() == null || dto.getEndTime() == null) {
            throw new IllegalArgumentException("Start time and end time are required.");
        }
        if (!dto.getStartTime().isBefore(dto.getEndTime())) {
            throw new IllegalArgumentException("Examination start time (" + dto.getStartTime() +
                    ") must be strictly before end time (" + dto.getEndTime() + ").");
        }
    }

    public ExaminationDto toDto(Examination exam) {
        ExaminationDto dto = new ExaminationDto();
        dto.setId(exam.getId());
        dto.setExaminationName(exam.getExaminationName());
        dto.setSubjectId(exam.getSubject().getId());
        dto.setSubjectCode(exam.getSubject().getSubjectCode());
        dto.setSubjectName(exam.getSubject().getSubjectName());
        dto.setDepartmentId(exam.getDepartment().getId());
        dto.setDepartmentName(exam.getDepartment().getDepartmentName());
        dto.setSemester(exam.getSemester());
        dto.setExaminationDate(exam.getExaminationDate());
        dto.setStartTime(exam.getStartTime());
        dto.setEndTime(exam.getEndTime());
        dto.setAcademicYear(exam.getAcademicYear());
        dto.setSession(exam.getSession());
        dto.setStatus(exam.getStatus());

        long regCount = registrationRepository.countByExaminationId(exam.getId());
        if (regCount == 0) {
            // Auto-derived count from enrolled students in that department & semester
            regCount = studentRepository.countByDepartmentIdAndSemesterAndStatus(
                    exam.getDepartment().getId(), exam.getSemester(), "ACTIVE"
            );
        }

        dto.setEligibleStudentCount(regCount);
        dto.setAllocatedStudentCount(allocationRepository.countByExaminationId(exam.getId()));
        return dto;
    }
}
