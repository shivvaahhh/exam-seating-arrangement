package com.example.examseating.service;

import com.example.examseating.dto.DepartmentDto;
import com.example.examseating.entity.Department;
import com.example.examseating.entity.Student;
import com.example.examseating.exception.DuplicateResourceException;
import com.example.examseating.repository.DepartmentRepository;
import com.example.examseating.repository.StudentRepository;
import com.example.examseating.repository.SubjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @InjectMocks
    private DepartmentService departmentService;

    private Department department;

    @BeforeEach
    void setUp() {
        department = new Department("Computer Science", "CSE", "CS Dept");
        department.setId(1L);
    }

    @Test
    @DisplayName("Should create department when code is unique")
    void testCreateDepartmentSuccess() {
        DepartmentDto dto = new DepartmentDto(null, "Information Technology", "IT", "IT Dept");

        when(departmentRepository.existsByDepartmentCode("IT")).thenReturn(false);
        when(departmentRepository.save(any())).thenAnswer(inv -> {
            Department d = inv.getArgument(0);
            d.setId(2L);
            return d;
        });

        DepartmentDto created = departmentService.create(dto);
        assertNotNull(created);
        assertEquals(2L, created.getId());
        assertEquals("IT", created.getDepartmentCode());
        verify(departmentRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when department code already exists")
    void testCreateDepartmentDuplicateCode() {
        DepartmentDto dto = new DepartmentDto(null, "Computer Science", "CSE", "Duplicate");
        when(departmentRepository.existsByDepartmentCode("CSE")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> departmentService.create(dto));
        verify(departmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should prevent deletion of department with active students")
    void testPreventDeleteDepartmentWithStudents() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(studentRepository.findByDepartmentId(1L)).thenReturn(Collections.singletonList(new Student()));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> departmentService.delete(1L));
        assertTrue(ex.getMessage().contains("currently has 1 enrolled student"));
        verify(departmentRepository, never()).delete(any());
    }
}
