package com.example.examseating.service;

import com.example.examseating.dto.StudentDto;
import com.example.examseating.entity.Department;
import com.example.examseating.entity.Student;
import com.example.examseating.entity.User;
import com.example.examseating.exception.DuplicateResourceException;
import com.example.examseating.exception.ResourceNotFoundException;
import com.example.examseating.repository.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final ExaminationRegistrationRepository registrationRepository;
    private final SeatAllocationRepository allocationRepository;
    private final PasswordEncoder passwordEncoder;

    public StudentService(StudentRepository studentRepository,
                          DepartmentRepository departmentRepository,
                          UserRepository userRepository,
                          ExaminationRegistrationRepository registrationRepository,
                          SeatAllocationRepository allocationRepository,
                          PasswordEncoder passwordEncoder) {
        this.studentRepository = studentRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.registrationRepository = registrationRepository;
        this.allocationRepository = allocationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<StudentDto> findAll() {
        return studentRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<Student> findAllEntities() {
        return studentRepository.findAll();
    }

    public Student findEntityById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + id));
    }

    public StudentDto findById(Long id) {
        return toDto(findEntityById(id));
    }

    public Optional<Student> findByRegisterNumber(String registerNumber) {
        return studentRepository.findByRegisterNumber(registerNumber);
    }

    public Optional<Student> findByUsername(String username) {
        return studentRepository.findByUser_Username(username);
    }

    public List<StudentDto> findWithFilters(Long deptId, Integer semester, String keyword) {
        String cleanKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        return studentRepository.findWithFilters(deptId, semester, cleanKeyword).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public StudentDto create(StudentDto dto) {
        String regNo = dto.getRegisterNumber().trim().toUpperCase();
        if (studentRepository.existsByRegisterNumber(regNo)) {
            throw new DuplicateResourceException("Student with Register Number '" + regNo + "' already exists.");
        }

        String email = dto.getEmail().trim().toLowerCase();
        if (studentRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Student with Email '" + email + "' already exists.");
        }

        Department department = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + dto.getDepartmentId()));

        // Create associated User for login (username = lowercase regNo)
        String username = regNo.toLowerCase();
        if (userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("User account with username '" + username + "' already exists.");
        }

        String rawPassword = (dto.getPassword() != null && !dto.getPassword().trim().isEmpty())
                ? dto.getPassword().trim()
                : "password123";

        User user = new User(username, passwordEncoder.encode(rawPassword), email, "ROLE_STUDENT");
        user = userRepository.save(user);

        Student student = new Student();
        student.setRegisterNumber(regNo);
        student.setFullName(dto.getFullName().trim());
        student.setEmail(email);
        student.setPhoneNumber(dto.getPhoneNumber() != null ? dto.getPhoneNumber().trim() : null);
        student.setDepartment(department);
        student.setSemester(dto.getSemester());
        student.setAcademicYear(dto.getAcademicYear().trim());
        student.setStatus(dto.getStatus() != null ? dto.getStatus() : "ACTIVE");
        student.setUser(user);

        Student saved = studentRepository.save(student);
        return toDto(saved);
    }

    @Transactional
    public StudentDto update(Long id, StudentDto dto) {
        Student student = findEntityById(id);
        String regNo = dto.getRegisterNumber().trim().toUpperCase();

        if (studentRepository.existsByRegisterNumberAndIdNot(regNo, id)) {
            throw new DuplicateResourceException("Register Number '" + regNo + "' is already in use by another student.");
        }

        String email = dto.getEmail().trim().toLowerCase();
        if (studentRepository.existsByEmailAndIdNot(email, id)) {
            throw new DuplicateResourceException("Email '" + email + "' is already in use by another student.");
        }

        Department department = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + dto.getDepartmentId()));

        student.setRegisterNumber(regNo);
        student.setFullName(dto.getFullName().trim());
        student.setEmail(email);
        student.setPhoneNumber(dto.getPhoneNumber() != null ? dto.getPhoneNumber().trim() : null);
        student.setDepartment(department);
        student.setSemester(dto.getSemester());
        student.setAcademicYear(dto.getAcademicYear().trim());
        student.setStatus(dto.getStatus() != null ? dto.getStatus() : "ACTIVE");

        // Also update linked User email / password if specified
        if (student.getUser() != null) {
            student.getUser().setEmail(email);
            if (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) {
                student.getUser().setPassword(passwordEncoder.encode(dto.getPassword().trim()));
            }
        }

        Student updated = studentRepository.save(student);
        return toDto(updated);
    }

    @Transactional
    public void delete(Long id) {
        Student student = findEntityById(id);

        long allocCount = allocationRepository.findByStudentId(id).size();
        if (allocCount > 0) {
            throw new IllegalStateException("Cannot delete student '" + student.getFullName() +
                    "' (" + student.getRegisterNumber() + ") because " + allocCount +
                    " active seat allocation(s) exist for this student. Remove allocations first.");
        }

        long regCount = registrationRepository.findByStudentId(id).size();
        if (regCount > 0) {
            throw new IllegalStateException("Cannot delete student '" + student.getFullName() +
                    "' (" + student.getRegisterNumber() + ") because the student is registered for " +
                    regCount + " examination(s). Remove registrations first.");
        }

        studentRepository.delete(student);
    }

    @Transactional
    public int importFromCsv(MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("The uploaded CSV file is empty.");
        }

        int importedCount = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                     .builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreHeaderCase(true)
                     .setTrim(true)
                     .build())) {

            for (CSVRecord record : csvParser) {
                // Expected columns: RegisterNumber, FullName, Email, PhoneNumber, DepartmentCode, Semester, AcademicYear
                String regNo = record.get("RegisterNumber").trim().toUpperCase();
                String fullName = record.get("FullName").trim();
                String email = record.get("Email").trim().toLowerCase();
                String phone = record.isSet("PhoneNumber") ? record.get("PhoneNumber").trim() : "";
                String deptCode = record.get("DepartmentCode").trim().toUpperCase();
                int semester = Integer.parseInt(record.get("Semester").trim());
                String academicYear = record.get("AcademicYear").trim();

                if (studentRepository.existsByRegisterNumber(regNo) || studentRepository.existsByEmail(email)) {
                    continue; // Skip duplicates during batch import
                }

                Department dept = departmentRepository.findByDepartmentCode(deptCode)
                        .orElse(null);
                if (dept == null) {
                    continue; // Skip if department not found
                }

                StudentDto dto = new StudentDto();
                dto.setRegisterNumber(regNo);
                dto.setFullName(fullName);
                dto.setEmail(email);
                dto.setPhoneNumber(phone);
                dto.setDepartmentId(dept.getId());
                dto.setSemester(semester);
                dto.setAcademicYear(academicYear);
                dto.setStatus("ACTIVE");
                dto.setPassword("password123");

                create(dto);
                importedCount++;
            }
        }
        return importedCount;
    }

    public StudentDto toDto(Student student) {
        StudentDto dto = new StudentDto();
        dto.setId(student.getId());
        dto.setRegisterNumber(student.getRegisterNumber());
        dto.setFullName(student.getFullName());
        dto.setEmail(student.getEmail());
        dto.setPhoneNumber(student.getPhoneNumber());
        dto.setDepartmentId(student.getDepartment().getId());
        dto.setDepartmentName(student.getDepartment().getDepartmentName());
        dto.setSemester(student.getSemester());
        dto.setAcademicYear(student.getAcademicYear());
        dto.setStatus(student.getStatus());
        return dto;
    }
}
