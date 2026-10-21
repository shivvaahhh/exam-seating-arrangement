package com.example.examseating.service;

import com.example.examseating.entity.Department;
import com.example.examseating.entity.ExaminationHall;
import com.example.examseating.entity.Student;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class MixedSeatAllocationTest {

    private Student createStudent(String registerNumber, String name, String deptCode) {
        Department dept = new Department(deptCode + " Dept", deptCode, deptCode);
        return new Student(registerNumber, name, registerNumber.toLowerCase() + "@college.edu", "9999999999", dept, 6, "2026-2027");
    }

    @Test
    @DisplayName("Balanced Case: 3 departments (8 CSE, 8 IT, 8 ECE) across 24 seats in a 4x6 grid")
    void testBalancedThreeDepartments() {
        ExaminationHall hall = new ExaminationHall("Main Hall", "Academic Block", "Ground", "101", 24, 4, 6);

        Map<String, LinkedList<Student>> deptQueues = new LinkedHashMap<>();
        deptQueues.put("CSE", new LinkedList<>());
        deptQueues.put("IT", new LinkedList<>());
        deptQueues.put("ECE", new LinkedList<>());

        for (int i = 1; i <= 8; i++) {
            deptQueues.get("CSE").add(createStudent(String.format("2024CSE%03d", i), "CSE Student " + i, "CSE"));
            deptQueues.get("IT").add(createStudent(String.format("2024IT%03d", i), "IT Student " + i, "IT"));
            deptQueues.get("ECE").add(createStudent(String.format("2024ECE%03d", i), "ECE Student " + i, "ECE"));
        }

        SeatAllocationService.HallGridMixedResult outcome = SeatAllocationService.allocateHallGridMixed(hall, deptQueues, 24);
        assertNotNull(outcome);
        assertNotNull(outcome.grid);

        // Verify all 24 students are allocated
        int allocatedCount = 0;
        for (int r = 1; r <= 4; r++) {
            for (int c = 1; c <= 6; c++) {
                if (outcome.grid[r][c] != null) {
                    allocatedCount++;
                }
            }
        }
        assertEquals(24, allocatedCount, "All 24 students should be allocated in the 24 available seats");

        // Validate adjacency
        SeatAllocationService.AdjacencyValidationResult validation =
                SeatAllocationService.validateStudentMatrix(outcome.grid, 4, 6);

        assertEquals(0, validation.getHorizontalPairs(), "Balanced 3-department arrangement must have 0 horizontal conflicts");
        assertEquals(0, validation.getVerticalPairs(), "Balanced 3-department arrangement must have 0 vertical conflicts");
        assertEquals(0, validation.getTotalAdjacentPairs(), "Balanced 3-department arrangement must have 0 direct adjacent same-dept pairs");
    }

    @Test
    @DisplayName("Skewed Case: Dominant department (14 IT, 4 CS, 2 EC) across 24 seats with 4 spare seats")
    void testSkewedDepartmentDistribution() {
        ExaminationHall hall = new ExaminationHall("Hall B", "Science Block", "1st Floor", "201", 24, 4, 6);

        Map<String, LinkedList<Student>> deptQueues = new LinkedHashMap<>();
        deptQueues.put("IT", new LinkedList<>());
        deptQueues.put("CS", new LinkedList<>());
        deptQueues.put("EC", new LinkedList<>());

        for (int i = 1; i <= 14; i++) {
            deptQueues.get("IT").add(createStudent(String.format("2024IT%03d", i), "IT Student " + i, "IT"));
        }
        for (int i = 1; i <= 4; i++) {
            deptQueues.get("CS").add(createStudent(String.format("2024CS%03d", i), "CS Student " + i, "CS"));
        }
        for (int i = 1; i <= 2; i++) {
            deptQueues.get("EC").add(createStudent(String.format("2024EC%03d", i), "EC Student " + i, "EC"));
        }

        SeatAllocationService.HallGridMixedResult outcome = SeatAllocationService.allocateHallGridMixed(hall, deptQueues, 24);
        assertNotNull(outcome);
        assertNotNull(outcome.grid);

        // Verify all 20 students are allocated
        int allocatedCount = 0;
        for (int r = 1; r <= 4; r++) {
            for (int c = 1; c <= 6; c++) {
                if (outcome.grid[r][c] != null) {
                    allocatedCount++;
                }
            }
        }
        assertEquals(20, allocatedCount, "All 20 students must be allocated");

        SeatAllocationService.AdjacencyValidationResult validation =
                SeatAllocationService.validateStudentMatrix(outcome.grid, 4, 6);

        // Because IT has 14 students out of 20 in 24 seats (checkerboard max capacity for 1 dept is 12),
        // perfect separation is mathematically impossible (14 > 12).
        // The algorithm minimizes same-department adjacency (achieving 6 instead of 19 in sequential).
        assertTrue(validation.getTotalAdjacentPairs() <= 6,
                "Direct adjacent same-department pairs should be minimized (got: " + validation.getTotalAdjacentPairs() + ")");
        assertTrue(validation.getHorizontalPairs() <= 4,
                "Horizontal same-department pairs should be minimized (got: " + validation.getHorizontalPairs() + ")");
    }

    @Test
    @DisplayName("Single Department Case with Alternate Empty Seats (10 IT students in 24 seats)")
    void testSingleDepartmentAlternateEmptySeats() {
        ExaminationHall hall = new ExaminationHall("Hall C", "Engineering Block", "2nd Floor", "301", 24, 4, 6);

        Map<String, LinkedList<Student>> deptQueues = new LinkedHashMap<>();
        deptQueues.put("IT", new LinkedList<>());
        for (int i = 1; i <= 10; i++) {
            deptQueues.get("IT").add(createStudent(String.format("2024IT%03d", i), "IT Student " + i, "IT"));
        }

        SeatAllocationService.HallGridMixedResult outcome = SeatAllocationService.allocateHallGridMixed(hall, deptQueues, 24);
        assertNotNull(outcome);
        assertNull(outcome.warning, "Should not produce a capacity warning when capacity allows alternate spacing");

        int allocatedCount = 0;
        for (int r = 1; r <= 4; r++) {
            for (int c = 1; c <= 6; c++) {
                if (outcome.grid[r][c] != null) {
                    allocatedCount++;
                    // Verify alternate placement: only allocated on (r + c) % 2 == 0
                    assertEquals(0, (r + c) % 2, "Student must be allocated only on alternate checkerboard seats");
                }
            }
        }
        assertEquals(10, allocatedCount, "All 10 students must be seated");

        SeatAllocationService.AdjacencyValidationResult validation =
                SeatAllocationService.validateStudentMatrix(outcome.grid, 4, 6);

        assertEquals(0, validation.getHorizontalPairs(), "Alternate seats must have 0 horizontal conflicts");
        assertEquals(0, validation.getVerticalPairs(), "Alternate seats must have 0 vertical conflicts");
        assertEquals(0, validation.getTotalAdjacentPairs(), "Direct adjacent same-dept pairs must be 0");
    }

    @Test
    @DisplayName("Single Department Case when capacity does NOT allow alternate seats: warning returned and all seated")
    void testSingleDepartmentConsecutiveFallbackWithWarning() {
        // Hall has 12 seats, 10 IT students (needs at least 19 seats for alternate spacing)
        ExaminationHall hall = new ExaminationHall("Small Hall", "Annex", "Ground", "001", 12, 3, 4);

        Map<String, LinkedList<Student>> deptQueues = new LinkedHashMap<>();
        deptQueues.put("IT", new LinkedList<>());
        for (int i = 1; i <= 10; i++) {
            deptQueues.get("IT").add(createStudent(String.format("2024IT%03d", i), "IT Student " + i, "IT"));
        }

        SeatAllocationService.HallGridMixedResult outcome = SeatAllocationService.allocateHallGridMixed(hall, deptQueues, 12);
        assertNotNull(outcome);
        assertNotNull(outcome.warning, "Should return a warning when capacity does not allow alternate empty seats");
        assertTrue(outcome.warning.contains("Only one department exists"), "Warning message should explain single-department constraint");

        int allocatedCount = 0;
        for (int r = 1; r <= 3; r++) {
            for (int c = 1; c <= 4; c++) {
                if (outcome.grid[r][c] != null) {
                    allocatedCount++;
                }
            }
        }
        assertEquals(10, allocatedCount, "All 10 students must be seated normally without failure");
    }

    @Test
    @DisplayName("Derived department code helper: uses Department entity or parses roll number")
    void testDepartmentCodeResolution() {
        // With department entity
        Student s1 = createStudent("2024IT001", "Student One", "IT");
        assertEquals("IT", s1.getDerivedDepartmentCode());

        // Without department entity (null department), derived from register number
        Student s2 = new Student("2024CSE042", "Student Two", "s2@college.edu", "9999999999", null, 6, "2026-2027");
        assertEquals("CSE", s2.getDerivedDepartmentCode());

        Student s3 = new Student("MECH101", "Student Three", "s3@college.edu", "9999999999", null, 6, "2026-2027");
        assertEquals("MECH", s3.getDerivedDepartmentCode());
    }
}
