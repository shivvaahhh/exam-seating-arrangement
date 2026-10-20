package com.example.examseating.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ExaminationHallTest {

    @Test
    @DisplayName("Should calculate max physical seats as rows * columns")
    void testPhysicalSeatsCalculation() {
        ExaminationHall hall = new ExaminationHall("Hall A", "Block 1", "Ground", "101", 30, 5, 6);
        assertEquals(30, hall.getMaxPhysicalSeats());
        assertEquals(30, hall.getEffectiveCapacity());
    }

    @Test
    @DisplayName("Effective capacity should not exceed total configured capacity even if physical grid is larger")
    void testEffectiveCapacityWithSmallerConfiguredCapacity() {
        // Grid is 5 * 10 = 50, but configured exam capacity is 40
        ExaminationHall hall = new ExaminationHall("Hall Large", "Block 2", "1st", "201", 40, 5, 10);
        assertEquals(50, hall.getMaxPhysicalSeats());
        assertEquals(40, hall.getEffectiveCapacity());
    }

    @Test
    @DisplayName("Effective capacity should not exceed physical grid even if configured capacity is set higher")
    void testEffectiveCapacityWithSmallerPhysicalGrid() {
        // Configured capacity is 50, but physical grid is only 4 * 6 = 24
        ExaminationHall hall = new ExaminationHall("Hall Small", "Block 2", "1st", "202", 50, 4, 6);
        assertEquals(24, hall.getMaxPhysicalSeats());
        assertEquals(24, hall.getEffectiveCapacity());
    }
}
