package com.example.examseating.service;

import com.example.examseating.dto.AllocationRequestDto;
import com.example.examseating.dto.AllocationResultDto;
import com.example.examseating.entity.*;
import com.example.examseating.exception.AllocationException;
import com.example.examseating.exception.InsufficientCapacityException;
import com.example.examseating.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SeatAllocationEngineTest {

    @Mock
    private ExaminationRepository examinationRepository;

    @Mock
    private ExaminationHallRepository hallRepository;

    @Mock
    private ExaminationRegistrationRepository registrationRepository;

    @Mock
    private SeatAllocationRepository allocationRepository;

    @InjectMocks
    private SeatAllocationService allocationService;

    private Department department;
    private Subject subject;
    private Examination examination;
    private ExaminationHall hallA;
    private ExaminationHall hallB;
    private List<Student> students;
    private List<ExaminationRegistration> registrations;

    @BeforeEach
    void setUp() {
        department = new Department("Computer Science", "CSE", "CS Dept");
        department.setId(1L);

        subject = new Subject("CS601", "Database Systems", department, 6, 4);
        subject.setId(1L);

        examination = new Examination(
                "Database Systems Final",
                subject,
                department,
                6,
                LocalDate.now().plusDays(2),
                LocalTime.of(9, 30),
                LocalTime.of(12, 30),
                "2026-2027",
                "FORENOON"
        );
        examination.setId(10L);
        examination.setStatus("SCHEDULED");

        // Hall A: 5 rows * 6 cols = 30 capacity
        hallA = new ExaminationHall("Hall A", "Block 1", "Ground", "101", 30, 5, 6);
        hallA.setId(101L);
        hallA.setStatus("ACTIVE");

        // Hall B: 4 rows * 5 cols = 20 capacity
        hallB = new ExaminationHall("Hall B", "Block 1", "1st Floor", "201", 20, 4, 5);
        hallB.setId(102L);
        hallB.setStatus("ACTIVE");

        // Setup 15 sample students
        students = new ArrayList<>();
        registrations = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            Student s = new Student();
            s.setId((long) i);
            s.setRegisterNumber(String.format("2024CSE%03d", i));
            s.setFullName("Student " + i);
            s.setDepartment(department);
            s.setSemester(6);
            s.setStatus("ACTIVE");
            students.add(s);

            ExaminationRegistration reg = new ExaminationRegistration(examination, s);
            reg.setId((long) (100 + i));
            registrations.add(reg);
        }
    }

    @Test
    @DisplayName("Should successfully allocate seats when capacity is sufficient")
    void testSuccessfulSeatAllocation() {
        when(examinationRepository.findById(10L)).thenReturn(Optional.of(examination));
        when(allocationRepository.existsByExaminationId(10L)).thenReturn(false);
        when(registrationRepository.findByExaminationId(10L)).thenReturn(registrations);
        when(hallRepository.findAllById(any())).thenReturn(Collections.singletonList(hallA));

        AllocationRequestDto request = new AllocationRequestDto(10L, Collections.singletonList(101L));
        request.setSortStrategy("DEPT_REGNO");

        AllocationResultDto result = allocationService.allocateSeats(request);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(15, result.getTotalAllocatedStudents());
        assertEquals(15, result.getTotalEligibleStudents());
        assertEquals(0, result.getUnallocatedStudents());
        assertEquals(30, result.getTotalAvailableSeats());
        assertEquals("CONFIRMED", result.getAllocationStatus());

        verify(allocationRepository, times(1)).saveAll(any());
        verify(examinationRepository, times(1)).save(examination);
        assertEquals("ALLOCATED", examination.getStatus());
    }

    @Test
    @DisplayName("Should throw InsufficientCapacityException with exact required message when seats are deficient")
    void testInsufficientCapacityException() {
        // Create 35 registered students but Hall A only has 30 seats
        List<ExaminationRegistration> largeRegistrations = new ArrayList<>();
        for (int i = 1; i <= 35; i++) {
            Student s = new Student();
            s.setId((long) i);
            s.setRegisterNumber(String.format("2024CSE%03d", i));
            s.setDepartment(department);
            s.setSemester(6);
            largeRegistrations.add(new ExaminationRegistration(examination, s));
        }

        when(examinationRepository.findById(10L)).thenReturn(Optional.of(examination));
        when(allocationRepository.existsByExaminationId(10L)).thenReturn(false);
        when(registrationRepository.findByExaminationId(10L)).thenReturn(largeRegistrations);
        when(hallRepository.findAllById(any())).thenReturn(Collections.singletonList(hallA));

        AllocationRequestDto request = new AllocationRequestDto(10L, Collections.singletonList(101L));

        InsufficientCapacityException ex = assertThrows(
                InsufficientCapacityException.class,
                () -> allocationService.allocateSeats(request)
        );

        String expected = "Allocation cannot be completed. Eligible students: 35. Available seats: 30. Additional seats required: 5.";
        assertEquals(expected, ex.getMessage());

        verify(allocationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Should prevent silent overwrite when allocations already exist without forceRegenerate")
    void testExistingAllocationConflictPrevention() {
        when(examinationRepository.findById(10L)).thenReturn(Optional.of(examination));
        when(allocationRepository.existsByExaminationId(10L)).thenReturn(true);

        AllocationRequestDto request = new AllocationRequestDto(10L, Collections.singletonList(101L));
        request.setForceRegenerate(false);

        AllocationException ex = assertThrows(
                AllocationException.class,
                () -> allocationService.allocateSeats(request)
        );

        assertTrue(ex.getMessage().contains("Allocations already exist"));
        verify(allocationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Should allow regeneration when forceRegenerate is true")
    void testForceRegenerationSuccess() {
        when(examinationRepository.findById(10L)).thenReturn(Optional.of(examination));
        when(allocationRepository.existsByExaminationId(10L)).thenReturn(true);
        when(registrationRepository.findByExaminationId(10L)).thenReturn(registrations);
        when(hallRepository.findAllById(any())).thenReturn(Collections.singletonList(hallA));

        AllocationRequestDto request = new AllocationRequestDto(10L, Collections.singletonList(101L));
        request.setForceRegenerate(true);

        AllocationResultDto result = allocationService.allocateSeats(request);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        verify(allocationRepository, times(1)).deleteByExaminationId(10L);
        verify(registrationRepository, times(1)).updateAllocationStatusByExaminationId(10L, false);
        verify(allocationRepository, times(1)).saveAll(any());
    }

    @Test
    @DisplayName("Should convert row numbers to alphabetical labels correctly")
    void testRowLetterConversion() {
        assertEquals("A", SeatAllocationService.getRowLetter(1));
        assertEquals("B", SeatAllocationService.getRowLetter(2));
        assertEquals("Z", SeatAllocationService.getRowLetter(26));
        assertEquals("AA", SeatAllocationService.getRowLetter(27));
        assertEquals("AB", SeatAllocationService.getRowLetter(28));
    }
}
