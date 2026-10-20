package com.example.examseating.service;

import com.example.examseating.dto.DepartmentDto;
import com.example.examseating.entity.Department;
import com.example.examseating.exception.DuplicateResourceException;
import com.example.examseating.exception.ResourceNotFoundException;
import com.example.examseating.repository.DepartmentRepository;
import com.example.examseating.repository.StudentRepository;
import com.example.examseating.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;

    public DepartmentService(DepartmentRepository departmentRepository,
                             StudentRepository studentRepository,
                             SubjectRepository subjectRepository) {
        this.departmentRepository = departmentRepository;
        this.studentRepository = studentRepository;
        this.subjectRepository = subjectRepository;
    }

    public List<DepartmentDto> findAll() {
        return departmentRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<Department> findAllEntities() {
        return departmentRepository.findAll();
    }

    public Department findEntityById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + id));
    }

    public DepartmentDto findById(Long id) {
        Department dept = findEntityById(id);
        return toDto(dept);
    }

    @Transactional
    public DepartmentDto create(DepartmentDto dto) {
        if (departmentRepository.existsByDepartmentCode(dto.getDepartmentCode().trim().toUpperCase())) {
            throw new DuplicateResourceException("Department with code '" + dto.getDepartmentCode() + "' already exists.");
        }

        Department department = new Department(
                dto.getDepartmentName().trim(),
                dto.getDepartmentCode().trim().toUpperCase(),
                dto.getDescription() != null ? dto.getDescription().trim() : null
        );

        Department saved = departmentRepository.save(department);
        return toDto(saved);
    }

    @Transactional
    public DepartmentDto update(Long id, DepartmentDto dto) {
        Department department = findEntityById(id);

        String newCode = dto.getDepartmentCode().trim().toUpperCase();
        if (departmentRepository.existsByDepartmentCodeAndIdNot(newCode, id)) {
            throw new DuplicateResourceException("Department with code '" + newCode + "' already exists for another department.");
        }

        department.setDepartmentName(dto.getDepartmentName().trim());
        department.setDepartmentCode(newCode);
        department.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);

        Department updated = departmentRepository.save(department);
        return toDto(updated);
    }

    @Transactional
    public void delete(Long id) {
        Department department = findEntityById(id);

        long studentCount = studentRepository.findByDepartmentId(id).size();
        if (studentCount > 0) {
            throw new IllegalStateException("Cannot delete department '" + department.getDepartmentName() +
                    "' because it currently has " + studentCount + " enrolled student(s).");
        }

        long subjectCount = subjectRepository.findByDepartmentId(id).size();
        if (subjectCount > 0) {
            throw new IllegalStateException("Cannot delete department '" + department.getDepartmentName() +
                    "' because it has " + subjectCount + " associated subject(s).");
        }

        departmentRepository.delete(department);
    }

    public List<DepartmentDto> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll();
        }
        return departmentRepository.findByDepartmentNameContainingIgnoreCaseOrDepartmentCodeContainingIgnoreCase(keyword.trim(), keyword.trim())
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private DepartmentDto toDto(Department dept) {
        DepartmentDto dto = new DepartmentDto(dept.getId(), dept.getDepartmentName(), dept.getDepartmentCode(), dept.getDescription());
        dto.setStudentCount(studentRepository.findByDepartmentId(dept.getId()).size());
        return dto;
    }
}
