package com.example.examseating.service;

import com.example.examseating.dto.AllocationRequestDto;
import com.example.examseating.dto.AllocationResultDto;
import com.example.examseating.dto.HallSeatGridDto;
import com.example.examseating.entity.*;
import com.example.examseating.exception.AllocationException;
import com.example.examseating.exception.InsufficientCapacityException;
import com.example.examseating.exception.ResourceNotFoundException;
import com.example.examseating.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SeatAllocationService {

    private final ExaminationRepository examinationRepository;
    private final ExaminationHallRepository hallRepository;
    private final ExaminationRegistrationRepository registrationRepository;
    private final SeatAllocationRepository allocationRepository;

    public SeatAllocationService(ExaminationRepository examinationRepository,
                                 ExaminationHallRepository hallRepository,
                                 ExaminationRegistrationRepository registrationRepository,
                                 SeatAllocationRepository allocationRepository) {
        this.examinationRepository = examinationRepository;
        this.hallRepository = hallRepository;
        this.registrationRepository = registrationRepository;
        this.allocationRepository = allocationRepository;
    }

    /**
     * Executes the automatic seat allocation engine for a given examination and selected halls.
     * Uses Isolation.SERIALIZABLE to prevent concurrent allocation race conditions.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = Exception.class)
    public AllocationResultDto allocateSeats(AllocationRequestDto request) {
        Long examId = request.getExaminationId();
        Examination exam = examinationRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Examination not found with ID: " + examId));

        if ("CANCELLED".equalsIgnoreCase(exam.getStatus())) {
            throw new AllocationException("Cannot allocate seats for a cancelled examination.");
        }

        // Check if allocations already exist
        boolean existingAllocationsExist = allocationRepository.existsByExaminationId(examId);
        if (existingAllocationsExist) {
            if (!request.isForceRegenerate()) {
                throw new AllocationException(
                        "Allocations already exist for this examination ('" + exam.getExaminationName() +
                        "'). Please review the consequences and confirm regeneration to re-allocate."
                );
            }
            // If explicit regeneration requested, clear existing allocations safely
            allocationRepository.deleteByExaminationId(examId);
            registrationRepository.updateAllocationStatusByExaminationId(examId, false);
            allocationRepository.flush();
        }

        // Step 1: Retrieve eligible students registered for the exam
        List<ExaminationRegistration> registrations = registrationRepository.findByExaminationId(examId);
        if (registrations.isEmpty()) {
            throw new AllocationException("No eligible students are registered for examination: " + exam.getExaminationName() +
                    ". Please add eligible students first.");
        }

        int eligibleStudentCount = registrations.size();

        // Step 2: Retrieve active examination halls selected for this examination
        List<Long> selectedHallIds = request.getHallIds();
        if (selectedHallIds == null || selectedHallIds.isEmpty()) {
            throw new AllocationException("At least one examination hall must be selected for allocation.");
        }

        List<ExaminationHall> halls = hallRepository.findAllById(selectedHallIds).stream()
                .filter(h -> "ACTIVE".equalsIgnoreCase(h.getStatus()))
                .sorted(Comparator.comparing(ExaminationHall::getHallName))
                .collect(Collectors.toList());

        if (halls.isEmpty()) {
            throw new AllocationException("None of the selected examination halls are currently ACTIVE.");
        }

        // Step 3: Calculate total usable seat capacity across the selected halls
        int totalAvailableSeats = halls.stream()
                .mapToInt(ExaminationHall::getEffectiveCapacity)
                .sum();

        // Step 4: Validate capacity. If insufficient, NEVER partially allocate silently!
        if (eligibleStudentCount > totalAvailableSeats) {
            throw new InsufficientCapacityException(eligibleStudentCount, totalAvailableSeats);
        }

        // Step 5: Sort students deterministically based on sorting strategy
        List<Student> students = registrations.stream()
                .map(ExaminationRegistration::getStudent)
                .collect(Collectors.toList());

        if ("REGNO_ONLY".equalsIgnoreCase(request.getSortStrategy())) {
            students.sort(Comparator.comparing(Student::getRegisterNumber));
        } else {
            // Default "DEPT_REGNO": Sort by Department Name, Semester, then Register Number
            students.sort(Comparator
                    .comparing((Student s) -> s.getDepartment().getDepartmentName())
                    .thenComparing(Student::getSemester)
                    .thenComparing(Student::getRegisterNumber));
        }

        // Step 6 & 7: Distribute students across available halls respecting capacities and row/col positions
        List<SeatAllocation> createdAllocations = new ArrayList<>();
        List<AllocationResultDto.HallSummaryDto> hallSummaries = new ArrayList<>();

        int studentIndex = 0;
        Set<String> assignedSeatTracker = new HashSet<>();

        for (ExaminationHall hall : halls) {
            int hallCapacity = hall.getEffectiveCapacity();
            int rows = hall.getNumberOfRows();
            int cols = hall.getNumberOfColumns();
            int hallAllocatedCount = 0;

            // Iterate row-by-row and col-by-col
            rowLoop:
            for (int r = 1; r <= rows; r++) {
                String rowLetter = getRowLetter(r);
                for (int c = 1; c <= cols; c++) {
                    if (hallAllocatedCount >= hallCapacity) {
                        break rowLoop; // Hall capacity limit reached
                    }
                    if (studentIndex >= students.size()) {
                        break rowLoop; // All students allocated
                    }

                    Student currentStudent = students.get(studentIndex);
                    String seatNumber = rowLetter + "-" + String.format("%02d", c);
                    String globalSeatKey = hall.getId() + "_" + seatNumber;

                    if (assignedSeatTracker.contains(globalSeatKey)) {
                        throw new AllocationException("Duplicate seat allocation attempt detected for " +
                                hall.getHallName() + " / " + seatNumber);
                    }
                    assignedSeatTracker.add(globalSeatKey);

                    SeatAllocation allocation = new SeatAllocation(
                            exam,
                            currentStudent,
                            hall,
                            seatNumber,
                            r,
                            c
                    );
                    createdAllocations.add(allocation);

                    studentIndex++;
                    hallAllocatedCount++;
                }
            }

            hallSummaries.add(new AllocationResultDto.HallSummaryDto(
                    hall.getId(),
                    hall.getHallName(),
                    hall.getBuildingName(),
                    hall.getRoomNumber(),
                    hallCapacity,
                    hallAllocatedCount
            ));

            if (studentIndex >= students.size()) {
                break;
            }
        }

        // Step 8: Persist allocations in batch and mark registrations as allocated
        allocationRepository.saveAll(createdAllocations);
        registrationRepository.updateAllocationStatusByExaminationId(examId, true);

        // Update exam status to ALLOCATED
        exam.setStatus("ALLOCATED");
        examinationRepository.save(exam);

        // Step 9: Build and return allocation summary result
        AllocationResultDto result = new AllocationResultDto();
        result.setExaminationId(exam.getId());
        result.setExaminationName(exam.getExaminationName());
        result.setSubjectCode(exam.getSubject().getSubjectCode());
        result.setSubjectName(exam.getSubject().getSubjectName());
        result.setExamDate(exam.getExaminationDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        result.setExamTime(exam.getStartTime() + " - " + exam.getEndTime() + " (" + exam.getSession() + ")");
        result.setTotalEligibleStudents(eligibleStudentCount);
        result.setTotalAllocatedStudents(createdAllocations.size());
        result.setTotalHallsUsed(hallSummaries.size());
        result.setTotalAvailableSeats(totalAvailableSeats);
        result.setUnallocatedStudents(eligibleStudentCount - createdAllocations.size());
        result.setAllocationStatus("CONFIRMED");
        result.setSuccess(true);
        result.setMessage("Seating arrangement generated successfully for " + createdAllocations.size() +
                " student(s) across " + hallSummaries.size() + " examination hall(s).");
        result.setHallSummaries(hallSummaries);

        return result;
    }

    /**
     * Clears all seat allocations for an examination and resets registration status.
     */
    @Transactional
    public void clearAllocations(Long examId) {
        Examination exam = examinationRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Examination not found with ID: " + examId));

        allocationRepository.deleteByExaminationId(examId);
        registrationRepository.updateAllocationStatusByExaminationId(examId, false);

        if (!"CANCELLED".equalsIgnoreCase(exam.getStatus())) {
            exam.setStatus("SCHEDULED");
            examinationRepository.save(exam);
        }
    }

    /**
     * Constructs a 2D Hall Seat Grid layout for visual display and printing.
     */
    @Transactional(readOnly = true)
    public HallSeatGridDto getHallSeatGrid(Long examId, Long hallId) {
        Examination exam = examinationRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Examination not found with ID: " + examId));

        ExaminationHall hall = hallRepository.findById(hallId)
                .orElseThrow(() -> new ResourceNotFoundException("Hall not found with ID: " + hallId));

        List<SeatAllocation> allocations = allocationRepository
                .findByExaminationIdAndExaminationHallIdOrderByRowNumberAscColumnNumberAsc(examId, hallId);

        // Map (row, col) to SeatAllocation
        Map<String, SeatAllocation> posMap = new HashMap<>();
        for (SeatAllocation sa : allocations) {
            posMap.put(sa.getRowNumber() + "_" + sa.getColumnNumber(), sa);
        }

        HallSeatGridDto gridDto = new HallSeatGridDto();
        gridDto.setHallId(hall.getId());
        gridDto.setHallName(hall.getHallName());
        gridDto.setBuildingName(hall.getBuildingName());
        gridDto.setFloor(hall.getFloor());
        gridDto.setRoomNumber(hall.getRoomNumber());
        gridDto.setNumberOfRows(hall.getNumberOfRows());
        gridDto.setNumberOfColumns(hall.getNumberOfColumns());
        gridDto.setTotalCapacity(hall.getTotalCapacity());
        gridDto.setAllocatedSeatsCount(allocations.size());
        gridDto.setEmptySeatsCount(Math.max(0, hall.getEffectiveCapacity() - allocations.size()));

        gridDto.setExaminationId(exam.getId());
        gridDto.setExaminationName(exam.getExaminationName());
        gridDto.setSubjectName(exam.getSubject().getSubjectName() + " (" + exam.getSubject().getSubjectCode() + ")");
        gridDto.setExamDate(exam.getExaminationDate().toString());
        gridDto.setExamTime(exam.getStartTime() + " - " + exam.getEndTime() + " (" + exam.getSession() + ")");

        List<List<HallSeatGridDto.SeatCellDto>> matrix = new ArrayList<>();
        int effectiveCap = hall.getEffectiveCapacity();
        int seatCounter = 0;

        for (int r = 1; r <= hall.getNumberOfRows(); r++) {
            List<HallSeatGridDto.SeatCellDto> rowCells = new ArrayList<>();
            String rowLetter = getRowLetter(r);

            for (int c = 1; c <= hall.getNumberOfColumns(); c++) {
                seatCounter++;
                String seatLabel = rowLetter + "-" + String.format("%02d", c);
                HallSeatGridDto.SeatCellDto cell = new HallSeatGridDto.SeatCellDto(r, c, rowLetter, seatLabel);

                if (seatCounter > effectiveCap) {
                    cell.setAvailableSeat(false);
                }

                SeatAllocation alloc = posMap.get(r + "_" + c);
                if (alloc != null) {
                    cell.setOccupied(true);
                    cell.setStudentId(alloc.getStudent().getId());
                    cell.setRegisterNumber(alloc.getStudent().getRegisterNumber());
                    cell.setStudentName(alloc.getStudent().getFullName());
                    cell.setDepartmentCode(alloc.getStudent().getDepartment().getDepartmentCode());
                    cell.setAttendanceStatus(alloc.getAttendanceStatus());
                } else {
                    cell.setOccupied(false);
                }

                rowCells.add(cell);
            }
            matrix.add(rowCells);
        }

        gridDto.setGrid(matrix);
        return gridDto;
    }

    public List<SeatAllocation> getAllocationsForExam(Long examId) {
        return allocationRepository.findByExaminationIdOrderByExaminationHall_HallNameAscRowNumberAscColumnNumberAsc(examId);
    }

    public List<SeatAllocation> getAllocationsForStudent(Long studentId) {
        return allocationRepository.findByStudentId(studentId);
    }

    public List<SeatAllocation> getAllocationsForUsername(String username) {
        return allocationRepository.findByStudent_User_UsernameOrderByExamination_ExaminationDateAscExamination_StartTimeAsc(username);
    }

    @Transactional
    public void updateAttendance(Long allocationId, String attendanceStatus) {
        SeatAllocation allocation = allocationRepository.findById(allocationId)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation not found with ID: " + allocationId));

        if ("PRESENT".equalsIgnoreCase(attendanceStatus) || "ABSENT".equalsIgnoreCase(attendanceStatus)) {
            allocation.setAttendanceStatus(attendanceStatus.toUpperCase());
        } else {
            allocation.setAttendanceStatus("PENDING");
        }
        allocationRepository.save(allocation);
    }

    /**
     * Converts a 1-based row number to an alphabetical label (1->A, 2->B, ..., 26->Z, 27->AA...)
     */
    public static String getRowLetter(int rowNumber) {
        StringBuilder result = new StringBuilder();
        while (rowNumber > 0) {
            rowNumber--;
            result.insert(0, (char) ('A' + (rowNumber % 26)));
            rowNumber /= 26;
        }
        return result.toString();
    }
}
