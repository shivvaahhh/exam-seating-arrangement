package com.example.examseating.service;

import com.example.examseating.dto.StudentDto;
import com.example.examseating.entity.Department;
import com.example.examseating.entity.Student;
import com.example.examseating.entity.User;
import com.example.examseating.exception.DuplicateResourceException;
import com.example.examseating.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ExaminationRegistrationRepository registrationRepository;

    @Mock
    private SeatAllocationRepository allocationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private StudentService studentService;

    private Department department;
    private StudentDto studentDto;

    @BeforeEach
    void setUp() {
        department = new Department("Computer Science", "CSE", "CS Dept");
        department.setId(1L);

        studentDto = new StudentDto();
        studentDto.setRegisterNumber("2024CSE001");
        studentDto.setFullName("Aditi Sharma");
        studentDto.setEmail("aditi@college.edu");
        studentDto.setDepartmentId(1L);
        studentDto.setSemester(6);
        studentDto.setAcademicYear("2026-2027");
        studentDto.setPassword("secret123");
    }

    @Test
    @DisplayName("Should create student and associated login account successfully")
    void testCreateStudentSuccess() {
        when(studentRepository.existsByRegisterNumber("2024CSE001")).thenReturn(false);
        when(studentRepository.existsByEmail("aditi@college.edu")).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(userRepository.existsByUsername("2024cse001")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed_pass");

        User savedUser = new User("2024cse001", "hashed_pass", "aditi@college.edu", "ROLE_STUDENT");
        when(userRepository.save(any())).thenReturn(savedUser);

        when(studentRepository.save(any())).thenAnswer(inv -> {
            Student s = inv.getArgument(0);
            s.setId(10L);
            return s;
        });

        StudentDto result = studentService.create(studentDto);
        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("2024CSE001", result.getRegisterNumber());
        assertEquals("Aditi Sharma", result.getFullName());
        verify(studentRepository, times(1)).save(any());
        verify(userRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException on duplicate register number")
    void testDuplicateRegisterNumberThrowsException() {
        when(studentRepository.existsByRegisterNumber("2024CSE001")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> studentService.create(studentDto));
        verify(studentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException on duplicate email")
    void testDuplicateEmailThrowsException() {
        when(studentRepository.existsByRegisterNumber("2024CSE001")).thenReturn(false);
        when(studentRepository.existsByEmail("aditi@college.edu")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> studentService.create(studentDto));
        verify(studentRepository, never()).save(any());
    }
}
