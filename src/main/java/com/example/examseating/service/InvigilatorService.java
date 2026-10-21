package com.example.examseating.service;

import com.example.examseating.dto.InvigilatorDto;
import com.example.examseating.entity.*;
import com.example.examseating.exception.DuplicateResourceException;
import com.example.examseating.exception.ResourceNotFoundException;
import com.example.examseating.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class InvigilatorService {

    private final InvigilatorRepository invigilatorRepository;
    private final InvigilatorAssignmentRepository assignmentRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final ExaminationRepository examinationRepository;
    private final ExaminationHallRepository hallRepository;
    private final PasswordEncoder passwordEncoder;

    public InvigilatorService(InvigilatorRepository invigilatorRepository,
                              InvigilatorAssignmentRepository assignmentRepository,
                              DepartmentRepository departmentRepository,
                              UserRepository userRepository,
                              ExaminationRepository examinationRepository,
                              ExaminationHallRepository hallRepository,
                              PasswordEncoder passwordEncoder) {
        this.invigilatorRepository = invigilatorRepository;
        this.assignmentRepository = assignmentRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.examinationRepository = examinationRepository;
        this.hallRepository = hallRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<InvigilatorDto> findAll() {
        return invigilatorRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<InvigilatorDto> findWithFilters(Long deptId, String keyword) {
        String cleanKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        return invigilatorRepository.findWithFilters(deptId, cleanKeyword).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public Invigilator findEntityById(Long id) {
        return invigilatorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invigilator not found with ID: " + id));
    }

    public InvigilatorDto findById(Long id) {
        return toDto(findEntityById(id));
    }

    public Optional<Invigilator> findByUsername(String username) {
        return invigilatorRepository.findByUser_Username(username.trim().toLowerCase());
    }

    @Transactional
    public InvigilatorDto create(InvigilatorDto dto) {
        String employeeId = dto.getEmployeeId().trim().toUpperCase();
        if (invigilatorRepository.existsByEmployeeId(employeeId)) {
            throw new DuplicateResourceException("Invigilator with Employee ID '" + employeeId + "' already exists.");
        }

        String email = dto.getEmail().trim().toLowerCase();
        if (invigilatorRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Invigilator with Email '" + email + "' already exists.");
        }

        String username = email;
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            username = employeeId.toLowerCase();
            if (userRepository.existsByUsernameIgnoreCase(username)) {
                throw new DuplicateResourceException("User account with username '" + username + "' already exists.");
            }
        }

        Department department = null;
        if (dto.getDepartmentId() != null) {
            department = departmentRepository.findById(dto.getDepartmentId()).orElse(null);
        }

        String generatedPass = generateRandomPassword();
        User user = new User(username, passwordEncoder.encode(generatedPass), email, "ROLE_INVIGILATOR", false);
        user = userRepository.save(user);

        Invigilator invigilator = new Invigilator(
                employeeId,
                dto.getName().trim(),
                email,
                dto.getContact() != null ? dto.getContact().trim() : null,
                department
        );
        invigilator.setStatus(dto.getStatus() != null ? dto.getStatus() : "ACTIVE");
        invigilator.setUser(user);

        Invigilator saved = invigilatorRepository.save(invigilator);
        InvigilatorDto resultDto = toDto(saved);
        resultDto.setGeneratedPassword(generatedPass);
        return resultDto;
    }

    @Transactional
    public InvigilatorDto update(Long id, InvigilatorDto dto) {
        Invigilator invigilator = findEntityById(id);

        String employeeId = dto.getEmployeeId().trim().toUpperCase();
        if (invigilatorRepository.existsByEmployeeIdAndIdNot(employeeId, id)) {
            throw new DuplicateResourceException("Employee ID '" + employeeId + "' is already in use by another invigilator.");
        }

        String email = dto.getEmail().trim().toLowerCase();
        if (invigilatorRepository.existsByEmailAndIdNot(email, id)) {
            throw new DuplicateResourceException("Email '" + email + "' is already in use by another invigilator.");
        }

        Department department = null;
        if (dto.getDepartmentId() != null) {
            department = departmentRepository.findById(dto.getDepartmentId()).orElse(null);
        }

        invigilator.setEmployeeId(employeeId);
        invigilator.setName(dto.getName().trim());
        invigilator.setEmail(email);
        invigilator.setContact(dto.getContact() != null ? dto.getContact().trim() : null);
        invigilator.setDepartment(department);
        invigilator.setStatus(dto.getStatus() != null ? dto.getStatus() : "ACTIVE");

        if (invigilator.getUser() != null) {
            invigilator.getUser().setEmail(email);
        }

        Invigilator updated = invigilatorRepository.save(invigilator);
        return toDto(updated);
    }

    @Transactional
    public void delete(Long id) {
        Invigilator invigilator = findEntityById(id);

        long assignmentCount = assignmentRepository.countByInvigilatorId(id);
        if (assignmentCount > 0) {
            throw new IllegalStateException("Cannot delete invigilator '" + invigilator.getName() +
                    "' because they have " + assignmentCount + " active examination duties. Remove assignments first.");
        }

        invigilatorRepository.delete(invigilator);
    }

    @Transactional
    public String resetPassword(Long id) {
        Invigilator inv = findEntityById(id);
        if (inv.getUser() == null) {
            throw new IllegalStateException("No user account associated with invigilator " + inv.getName());
        }

        String newPassword = generateRandomPassword();
        inv.getUser().setPassword(passwordEncoder.encode(newPassword));
        inv.getUser().setMustChangePassword(false);
        userRepository.save(inv.getUser());
        return newPassword;
    }

    @Transactional
    public boolean toggleActive(Long id) {
        Invigilator inv = findEntityById(id);
        boolean newStatus = "ACTIVE".equalsIgnoreCase(inv.getStatus()) ? false : true;
        inv.setStatus(newStatus ? "ACTIVE" : "INACTIVE");
        if (inv.getUser() != null) {
            inv.getUser().setActive(newStatus);
            userRepository.save(inv.getUser());
        }
        invigilatorRepository.save(inv);
        return newStatus;
    }

    // ========== Duty Assignment & Conflict Validation ==========

    public List<InvigilatorAssignment> getAssignmentsForExam(Long examId) {
        return assignmentRepository.findByExaminationId(examId);
    }

    public List<InvigilatorAssignment> getAssignmentsForExamAndHall(Long examId, Long hallId) {
        return assignmentRepository.findByExaminationIdAndExaminationHallId(examId, hallId);
    }

    public List<InvigilatorAssignment> getUpcomingDutiesForInvigilator(Long invigilatorId) {
        return assignmentRepository.findUpcomingByInvigilatorId(invigilatorId);
    }

    @Transactional
    public InvigilatorAssignment assignInvigilatorToHall(Long examId, Long hallId, Long invigilatorId) {
        Examination exam = examinationRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Examination not found with ID: " + examId));

        ExaminationHall hall = hallRepository.findById(hallId)
                .orElseThrow(() -> new ResourceNotFoundException("Examination Hall not found with ID: " + hallId));

        Invigilator invigilator = findEntityById(invigilatorId);

        if (!"ACTIVE".equalsIgnoreCase(invigilator.getStatus())) {
            throw new IllegalArgumentException("Cannot assign inactive invigilator '" + invigilator.getName() + "'.");
        }

        // Validate double-booking: check all duties on the same date and session across ALL exams!
        List<InvigilatorAssignment> sameSessionDuties = assignmentRepository.findByInvigilatorAndDateAndSession(
                invigilatorId, exam.getExaminationDate(), exam.getSession()
        );

        for (InvigilatorAssignment duty : sameSessionDuties) {
            if (!duty.getExamination().getId().equals(examId) || !duty.getExaminationHall().getId().equals(hallId)) {
                throw new IllegalArgumentException(
                        "Clash detected! Invigilator '" + invigilator.getName() +
                        "' is already assigned to " + duty.getExaminationHall().getHallName() +
                        " (" + duty.getExaminationHall().getRoomNumber() + ") for exam '" +
                        duty.getExamination().getExaminationName() + "' on " +
                        exam.getExaminationDate() + " (" + exam.getSession() + ")."
                );
            }
        }

        // Check if already assigned to this exact exam & hall
        if (assignmentRepository.existsByExaminationIdAndInvigilatorId(examId, invigilatorId)) {
            throw new IllegalArgumentException("Invigilator '" + invigilator.getName() +
                    "' is already assigned to an examination hall for this exam.");
        }

        InvigilatorAssignment assignment = new InvigilatorAssignment(exam, hall, invigilator);
        return assignmentRepository.save(assignment);
    }

    @Transactional
    public void removeAssignment(Long assignmentId) {
        assignmentRepository.deleteById(assignmentId);
    }

    private String generateRandomPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public InvigilatorDto toDto(Invigilator inv) {
        InvigilatorDto dto = new InvigilatorDto();
        dto.setId(inv.getId());
        dto.setEmployeeId(inv.getEmployeeId());
        dto.setName(inv.getName());
        dto.setEmail(inv.getEmail());
        dto.setContact(inv.getContact());
        if (inv.getDepartment() != null) {
            dto.setDepartmentId(inv.getDepartment().getId());
            dto.setDepartmentName(inv.getDepartment().getDepartmentName());
        }
        dto.setStatus(inv.getStatus());
        if (inv.getUser() != null) {
            dto.setUserId(inv.getUser().getId());
            dto.setActiveUser(inv.getUser().isActive());
        }
        return dto;
    }
}
