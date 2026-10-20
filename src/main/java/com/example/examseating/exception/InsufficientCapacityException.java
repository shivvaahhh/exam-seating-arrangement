package com.example.examseating.exception;

public class InsufficientCapacityException extends RuntimeException {

    private final int eligibleCount;
    private final int availableSeats;
    private final int requiredSeats;

    public InsufficientCapacityException(int eligibleCount, int availableSeats) {
        super(String.format("Allocation cannot be completed. Eligible students: %d. Available seats: %d. Additional seats required: %d.",
                eligibleCount, availableSeats, (eligibleCount - availableSeats)));
        this.eligibleCount = eligibleCount;
        this.availableSeats = availableSeats;
        this.requiredSeats = Math.max(0, eligibleCount - availableSeats);
    }

    public InsufficientCapacityException(String message) {
        super(message);
        this.eligibleCount = 0;
        this.availableSeats = 0;
        this.requiredSeats = 0;
    }

    public int getEligibleCount() {
        return eligibleCount;
    }

    public int getAvailableSeats() {
        return availableSeats;
    }

    public int getRequiredSeats() {
        return requiredSeats;
    }
}
