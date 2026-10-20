package com.example.examseating.service;

import com.example.examseating.dto.SubjectDto;
import com.example.examseating.entity.Department;
import com.example.examseating.entity.Subject;
import com.example.examseating.exception.DuplicateResourceException;
import com.example.examseating.exception.ResourceNotFoundException;
import com.example.examseating.repository.DepartmentRepository;
import com.example.examseating.repository.ExaminationRepository;
import com.example.examseating.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final DepartmentRepository departmentRepository;
    private final ExaminationRepository examinationRepository;

    public SubjectService(SubjectRepository subjectRepository,
                          DepartmentRepository departmentRepository,
                          ExaminationRepository examinationRepository) {
        this.subjectRepository = subjectRepository;
        this.departmentRepository = departmentRepository;
        this.examinationRepository = examinationRepository;
    }

    public List<SubjectDto> findAll() {
        return subjectRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<Subject> findAllEntities() {
        return subjectRepository.findAll();
    }

    public Subject findEntityById(Long id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with ID: " + id));
    }

    public SubjectDto findById(Long id) {
        Subject subject = findEntityById(id);
        return toDto(subject);
    }

    public List<SubjectDto> findByDepartmentAndSemester(Long departmentId, Integer semester) {
        if (departmentId != null && semester != null) {
            return subjectRepository.findByDepartmentIdAndSemester(departmentId, semester).stream()
                    .map(this::toDto).collect(Collectors.toList());
        } else if (departmentId != null) {
            return subjectRepository.findByDepartmentId(departmentId).stream()
                    .map(this::toDto).collect(Collectors.toList());
        }
        return findAll();
    }

    @Transactional
    public SubjectDto create(SubjectDto dto) {
        String code = dto.getSubjectCode().trim().toUpperCase();
        if (subjectRepository.existsBySubjectCode(code)) {
            throw new DuplicateResourceException("Subject with code '" + code + "' already exists.");
        }

        Department department = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + dto.getDepartmentId()));

        Subject subject = new Subject(
                code,
                dto.getSubjectName().trim(),
                department,
                dto.getSemester(),
                dto.getCredits() != null ? dto.getCredits() : 3
        );

        Subject saved = subjectRepository.save(subject);
        return toDto(saved);
    }

    @Transactional
    public SubjectDto update(Long id, SubjectDto dto) {
        Subject subject = findEntityById(id);
        String code = dto.getSubjectCode().trim().toUpperCase();

        if (subjectRepository.existsBySubjectCodeAndIdNot(code, id)) {
            throw new DuplicateResourceException("Subject with code '" + code + "' already exists for another subject.");
        }

        Department department = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + dto.getDepartmentId()));

        subject.setSubjectCode(code);
        subject.setSubjectName(dto.getSubjectName().trim());
        subject.setDepartment(department);
        subject.setSemester(dto.getSemester());
        subject.setCredits(dto.getCredits() != null ? dto.getCredits() : 3);

        Subject updated = subjectRepository.save(subject);
        return toDto(updated);
    }

    @Transactional
    public void delete(Long id) {
        Subject subject = findEntityById(id);

        boolean inUse = examinationRepository.findAll().stream()
                .anyMatch(exam -> exam.getSubject().getId().equals(id));

        if (inUse) {
            throw new IllegalStateException("Cannot delete subject '" + subject.getSubjectName() +
                    "' because examinations are scheduled for this subject.");
        }

        subjectRepository.delete(subject);
    }

    public List<SubjectDto> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll();
        }
        return subjectRepository.findBySubjectNameContainingIgnoreCaseOrSubjectCodeContainingIgnoreCase(keyword.trim(), keyword.trim())
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public SubjectDto toDto(Subject subject) {
        SubjectDto dto = new SubjectDto();
        dto.setId(subject.getId());
        dto.setSubjectCode(subject.getSubjectCode());
        dto.setSubjectName(subject.getSubjectName());
        dto.setDepartmentId(subject.getDepartment().getId());
        dto.setDepartmentName(subject.getDepartment().getDepartmentName());
        dto.setSemester(subject.getSemester());
        dto.setCredits(subject.getCredits());
        return dto;
    }
}
