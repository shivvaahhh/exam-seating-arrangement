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
    private final StudentRepository studentRepository;

    public SeatAllocationService(ExaminationRepository examinationRepository,
                                 ExaminationHallRepository hallRepository,
                                 ExaminationRegistrationRepository registrationRepository,
                                 SeatAllocationRepository allocationRepository,
                                 StudentRepository studentRepository) {
        this.examinationRepository = examinationRepository;
        this.hallRepository = hallRepository;
        this.registrationRepository = registrationRepository;
        this.allocationRepository = allocationRepository;
        this.studentRepository = studentRepository;
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
            // Auto-populate eligible candidates from enrolled students for department + semester
            List<Student> enrolledStudents = studentRepository.findByDepartmentIdAndSemester(
                    exam.getDepartment().getId(), exam.getSemester()
            ).stream()
             .filter(s -> "ACTIVE".equalsIgnoreCase(s.getStatus()))
             .collect(Collectors.toList());

            if (enrolledStudents.isEmpty()) {
                throw new AllocationException("No active students found in " + exam.getDepartment().getDepartmentName() +
                        " Semester " + exam.getSemester() + " to register for this examination.");
            }

            for (Student s : enrolledStudents) {
                ExaminationRegistration reg = new ExaminationRegistration(exam, s);
                registrations.add(registrationRepository.save(reg));
            }
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

        // Step 4: Validate capacity BEFORE allocating. Show exact shortfall and block if insufficient!
        if (eligibleStudentCount > totalAvailableSeats) {
            throw new InsufficientCapacityException(eligibleStudentCount, totalAvailableSeats);
        }

        // Step 5: Check allocation mode
        boolean isMixed = "MIXED".equalsIgnoreCase(request.getAllocationMode())
                || "MIXED".equalsIgnoreCase(request.getSortStrategy());

        List<Student> students = registrations.stream()
                .map(ExaminationRegistration::getStudent)
                .collect(Collectors.toList());

        List<SeatAllocation> createdAllocations = new ArrayList<>();
        List<AllocationResultDto.HallSummaryDto> hallSummaries = new ArrayList<>();
        Set<String> assignedSeatTracker = new HashSet<>();

        int totalHorizontalPairs = 0;
        int totalVerticalPairs = 0;
        int totalDiagonalPairs = 0;
        String singleDeptWarning = null;

        if (isMixed) {
            // ==========================================
            // MIXED ALLOCATION ENGINE
            // ==========================================
            // 1. Group students by derived department code
            Map<String, LinkedList<Student>> deptQueues = new LinkedHashMap<>();
            for (Student s : students) {
                String dept = s.getDerivedDepartmentCode();
                deptQueues.computeIfAbsent(dept, k -> new LinkedList<>()).add(s);
            }
            // Sort each bucket by register number
            for (LinkedList<Student> q : deptQueues.values()) {
                q.sort(Comparator.comparing(Student::getRegisterNumber));
            }

            int usableSeatsRemainingInAllHalls = totalAvailableSeats;

            for (ExaminationHall hall : halls) {
                int hallCapacity = hall.getEffectiveCapacity();
                int rows = hall.getNumberOfRows();
                int cols = hall.getNumberOfColumns();

                HallGridMixedResult outcome = allocateHallGridMixed(hall, deptQueues, usableSeatsRemainingInAllHalls);
                Student[][] grid = outcome.grid;
                if (outcome.warning != null && singleDeptWarning == null) {
                    singleDeptWarning = outcome.warning;
                }

                int hallAllocatedCount = 0;
                for (int r = 1; r <= rows; r++) {
                    String rowLetter = getRowLetter(r);
                    for (int c = 1; c <= cols; c++) {
                        Student currentStudent = grid[r][c];
                        if (currentStudent == null) continue;

                        String seatNumber = rowLetter + "-" + String.format("%02d", c);
                        String globalSeatKey = hall.getId() + "_" + seatNumber;

                        if (assignedSeatTracker.contains(globalSeatKey)) {
                            throw new AllocationException("Duplicate seat allocation attempt detected for " +
                                    hall.getHallName() + " / " + seatNumber);
                        }

                        // Clash Check 1: Seat booking clash
                        List<SeatAllocation> seatClashes = allocationRepository.findClashingSeatAllocations(
                                hall.getId(), seatNumber, exam.getExaminationDate(), exam.getSession(), exam.getId()
                        );
                        if (!seatClashes.isEmpty()) {
                            throw new AllocationException("Seat conflict: Hall '" + hall.getHallName() + "' Seat '" + seatNumber +
                                    "' is already booked for another examination ('" +
                                    seatClashes.get(0).getExamination().getExaminationName() + "') on " +
                                    exam.getExaminationDate() + " (" + exam.getSession() + ").");
                        }

                        // Clash Check 2: Student double booking clash
                        List<SeatAllocation> studentClashes = allocationRepository.findClashingStudentAllocations(
                                currentStudent.getId(), exam.getExaminationDate(), exam.getSession(), exam.getId()
                        );
                        if (!studentClashes.isEmpty()) {
                            throw new AllocationException("Student conflict: Student " + currentStudent.getRegisterNumber() +
                                    " (" + currentStudent.getFullName() + ") is already allocated to seat in another exam ('" +
                                    studentClashes.get(0).getExamination().getExaminationName() + "') on " +
                                    exam.getExaminationDate() + " (" + exam.getSession() + ").");
                        }

                        assignedSeatTracker.add(globalSeatKey);
                        SeatAllocation allocation = new SeatAllocation(exam, currentStudent, hall, seatNumber, r, c);
                        createdAllocations.add(allocation);
                        hallAllocatedCount++;
                    }
                }

                // Validate layout for this hall
                AdjacencyValidationResult hallVal = validateStudentMatrix(grid, rows, cols);
                totalHorizontalPairs += hallVal.getHorizontalPairs();
                totalVerticalPairs += hallVal.getVerticalPairs();
                totalDiagonalPairs += hallVal.getDiagonalPairs();

                hallSummaries.add(new AllocationResultDto.HallSummaryDto(
                        hall.getId(),
                        hall.getHallName(),
                        hall.getBuildingName(),
                        hall.getRoomNumber(),
                        hallCapacity,
                        hallAllocatedCount
                ));

                usableSeatsRemainingInAllHalls -= Math.min(hallCapacity, rows * cols);
            }
        } else {
            // ==========================================
            // SEQUENTIAL ALLOCATION ENGINE (Preserved)
            // ==========================================
            if ("REGNO_ONLY".equalsIgnoreCase(request.getSortStrategy())) {
                students.sort(Comparator.comparing(Student::getRegisterNumber));
            } else {
                Map<Long, List<Student>> deptBuckets = new LinkedHashMap<>();
                for (Student s : students) {
                    Long deptId = s.getDepartment() != null ? s.getDepartment().getId() : 0L;
                    deptBuckets.computeIfAbsent(deptId, k -> new ArrayList<>()).add(s);
                }
                for (List<Student> bucket : deptBuckets.values()) {
                    bucket.sort(Comparator.comparing(Student::getRegisterNumber));
                }
                List<Student> sequentialList = new ArrayList<>();
                for (List<Student> bucket : deptBuckets.values()) {
                    sequentialList.addAll(bucket);
                }
                students = sequentialList;
            }

            int studentIndex = 0;
            for (ExaminationHall hall : halls) {
                int hallCapacity = hall.getEffectiveCapacity();
                int rows = hall.getNumberOfRows();
                int cols = hall.getNumberOfColumns();
                int hallAllocatedCount = 0;
                Student[][] hallGrid = new Student[rows + 1][cols + 1];

                rowLoop:
                for (int r = 1; r <= rows; r++) {
                    String rowLetter = getRowLetter(r);
                    for (int c = 1; c <= cols; c++) {
                        if (hallAllocatedCount >= hallCapacity) {
                            break rowLoop;
                        }
                        if (studentIndex >= students.size()) {
                            break rowLoop;
                        }

                        Student currentStudent = students.get(studentIndex);
                        String seatNumber = rowLetter + "-" + String.format("%02d", c);
                        String globalSeatKey = hall.getId() + "_" + seatNumber;

                        if (assignedSeatTracker.contains(globalSeatKey)) {
                            throw new AllocationException("Duplicate seat allocation attempt detected for " +
                                    hall.getHallName() + " / " + seatNumber);
                        }

                        List<SeatAllocation> seatClashes = allocationRepository.findClashingSeatAllocations(
                                hall.getId(), seatNumber, exam.getExaminationDate(), exam.getSession(), exam.getId()
                        );
                        if (!seatClashes.isEmpty()) {
                            throw new AllocationException("Seat conflict: Hall '" + hall.getHallName() + "' Seat '" + seatNumber +
                                    "' is already booked for another examination ('" +
                                    seatClashes.get(0).getExamination().getExaminationName() + "') on " +
                                    exam.getExaminationDate() + " (" + exam.getSession() + ").");
                        }

                        List<SeatAllocation> studentClashes = allocationRepository.findClashingStudentAllocations(
                                currentStudent.getId(), exam.getExaminationDate(), exam.getSession(), exam.getId()
                        );
                        if (!studentClashes.isEmpty()) {
                            throw new AllocationException("Student conflict: Student " + currentStudent.getRegisterNumber() +
                                    " (" + currentStudent.getFullName() + ") is already allocated to seat in another exam ('" +
                                    studentClashes.get(0).getExamination().getExaminationName() + "') on " +
                                    exam.getExaminationDate() + " (" + exam.getSession() + ").");
                        }

                        assignedSeatTracker.add(globalSeatKey);
                        SeatAllocation allocation = new SeatAllocation(exam, currentStudent, hall, seatNumber, r, c);
                        createdAllocations.add(allocation);
                        hallGrid[r][c] = currentStudent;

                        studentIndex++;
                        hallAllocatedCount++;
                    }
                }

                AdjacencyValidationResult hallVal = validateStudentMatrix(hallGrid, rows, cols);
                totalHorizontalPairs += hallVal.getHorizontalPairs();
                totalVerticalPairs += hallVal.getVerticalPairs();
                totalDiagonalPairs += hallVal.getDiagonalPairs();

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

        String timeSession;
        if (exam.getStartTime() != null && exam.getEndTime() != null) {
            timeSession = exam.getStartTime() + " - " + exam.getEndTime();
            if (exam.getSession() != null && !exam.getSession().isBlank()) {
                timeSession += " (" + exam.getSession() + ")";
            }
        } else if (exam.getSession() != null && !exam.getSession().isBlank()) {
            timeSession = exam.getSession();
        } else {
            timeSession = "Standard Session";
        }
        result.setExamTime(timeSession);

        result.setTotalEligibleStudents(eligibleStudentCount);
        result.setTotalAllocatedStudents(createdAllocations.size());
        result.setTotalHallsUsed(hallSummaries.size());
        result.setTotalAvailableSeats(totalAvailableSeats);
        result.setUnallocatedStudents(eligibleStudentCount - createdAllocations.size());
        result.setAllocationStatus("CONFIRMED");
        result.setHalls(hallSummaries);

        int totalDirectAdjacent = totalHorizontalPairs + totalVerticalPairs;
        result.setAllocationMode(isMixed ? "MIXED" : "SEQUENTIAL");
        result.setHorizontalAdjacentPairs(totalHorizontalPairs);
        result.setVerticalAdjacentPairs(totalVerticalPairs);
        result.setDiagonalAdjacentPairs(totalDiagonalPairs);
        result.setTotalAdjacentPairs(totalDirectAdjacent);
        result.setValidationSummary("Adjacent same-dept pairs: " + totalDirectAdjacent +
                " (H: " + totalHorizontalPairs + ", V: " + totalVerticalPairs + ", D: " + totalDiagonalPairs + ")");
        result.setValidationWarning(singleDeptWarning);

        String successMsg = "Successfully allocated " + createdAllocations.size() + " seats using " +
                (isMixed ? "MIXED" : "SEQUENTIAL") + " mode. " + result.getValidationSummary();
        if (singleDeptWarning != null) {
            successMsg += " " + singleDeptWarning;
        }
        result.setMessage(successMsg);
        result.setSuccess(true);

        return result;
    }

    /**
     * Builds a 2D matrix model representing the hall layout for visual inspection.
     */
    public HallSeatGridDto getHallSeatGrid(Long examId, Long hallId) {
        Examination exam = examinationRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Examination not found with ID: " + examId));

        ExaminationHall hall = hallRepository.findById(hallId)
                .orElseThrow(() -> new ResourceNotFoundException("Hall not found with ID: " + hallId));

        List<SeatAllocation> allocations = allocationRepository
                .findByExaminationIdAndExaminationHallIdOrderByRowNumberAscColumnNumberAsc(examId, hallId);

        Map<String, SeatAllocation> seatMap = new HashMap<>();
        for (SeatAllocation a : allocations) {
            seatMap.put(a.getRowNumber() + "_" + a.getColumnNumber(), a);
        }

        HallSeatGridDto grid = new HallSeatGridDto();
        grid.setHallId(hall.getId());
        grid.setHallName(hall.getHallName());
        grid.setBuildingName(hall.getBuildingName());
        grid.setRoomNumber(hall.getRoomNumber());
        grid.setTotalCapacity(hall.getTotalCapacity());
        grid.setRows(hall.getNumberOfRows());
        grid.setColumns(hall.getNumberOfColumns());
        grid.setExaminationId(exam.getId());
        grid.setExaminationName(exam.getExaminationName());
        grid.setSubjectCode(exam.getSubject().getSubjectCode());
        grid.setSubjectName(exam.getSubject().getSubjectName());
        grid.setExamDate(exam.getExaminationDate().format(DateTimeFormatter.ISO_LOCAL_DATE));

        // Fix Bug 1: examTime was missing / null
        String timeSession;
        if (exam.getStartTime() != null && exam.getEndTime() != null) {
            timeSession = exam.getStartTime() + " - " + exam.getEndTime();
            if (exam.getSession() != null && !exam.getSession().isBlank()) {
                timeSession += " (" + exam.getSession() + ")";
            }
        } else if (exam.getSession() != null && !exam.getSession().isBlank()) {
            timeSession = exam.getSession();
        } else {
            timeSession = "Standard Session";
        }
        grid.setExamTime(timeSession);
        grid.setExamSession(exam.getSession());
        grid.setAllocatedCount(allocations.size());

        List<HallSeatGridDto.RowDto> rowDtos = new ArrayList<>();
        int effectiveCap = hall.getEffectiveCapacity();
        int runningSeatCount = 0;

        for (int r = 1; r <= hall.getNumberOfRows(); r++) {
            HallSeatGridDto.RowDto rowDto = new HallSeatGridDto.RowDto();
            rowDto.setRowNumber(r);
            rowDto.setRowLetter(getRowLetter(r));

            List<HallSeatGridDto.SeatCellDto> cells = new ArrayList<>();
            for (int c = 1; c <= hall.getNumberOfColumns(); c++) {
                runningSeatCount++;
                String cellKey = r + "_" + c;
                String seatNumber = rowDto.getRowLetter() + "-" + String.format("%02d", c);

                HallSeatGridDto.SeatCellDto cell = new HallSeatGridDto.SeatCellDto();
                cell.setRow(r);
                cell.setColumn(c);
                cell.setSeatNumber(seatNumber);

                if (runningSeatCount > effectiveCap) {
                    cell.setStatus("UNAVAILABLE");
                } else if (seatMap.containsKey(cellKey)) {
                    SeatAllocation sa = seatMap.get(cellKey);
                    cell.setStatus("OCCUPIED");
                    cell.setStudentId(sa.getStudent().getId());
                    cell.setRegisterNumber(sa.getStudent().getRegisterNumber());
                    cell.setStudentName(sa.getStudent().getFullName());
                    cell.setDepartmentCode(sa.getStudent().getDerivedDepartmentCode());
                    cell.setSemester(sa.getStudent().getSemester());
                    cell.setAttendanceStatus(sa.getAttendanceStatus());
                } else {
                    cell.setStatus("EMPTY");
                }
                cells.add(cell);
            }
            rowDto.setSeats(cells);
            rowDtos.add(rowDto);
        }

        List<List<HallSeatGridDto.SeatCellDto>> gridList = new ArrayList<>();
        for (HallSeatGridDto.RowDto r : rowDtos) {
            gridList.add(r.getSeats());
        }
        grid.setGrid(gridList);
        grid.setGridRows(rowDtos);

        // Validation for this hall's layout
        AdjacencyValidationResult validation = validateLayout(gridList);
        grid.setHorizontalAdjacentPairs(validation.getHorizontalPairs());
        grid.setVerticalAdjacentPairs(validation.getVerticalPairs());
        grid.setDiagonalAdjacentPairs(validation.getDiagonalPairs());
        grid.setTotalAdjacentPairs(validation.getTotalAdjacentPairs());
        grid.setValidationSummary(validation.getSummary());
        grid.setValidationWarning(validation.getWarning());

        return grid;
    }

    // =========================================================================
    // MIXED ALLOCATION & ADJACENCY VALIDATION HELPERS
    // =========================================================================

    public static class AdjacencyValidationResult {
        private int horizontalPairs = 0;
        private int verticalPairs = 0;
        private int diagonalPairs = 0;
        private int totalAdjacentPairs = 0;
        private String summary = "Adjacent same-dept pairs: 0 (H: 0, V: 0, D: 0)";
        private String warning;

        public int getHorizontalPairs() { return horizontalPairs; }
        public void setHorizontalPairs(int horizontalPairs) { this.horizontalPairs = horizontalPairs; }
        public int getVerticalPairs() { return verticalPairs; }
        public void setVerticalPairs(int verticalPairs) { this.verticalPairs = verticalPairs; }
        public int getDiagonalPairs() { return diagonalPairs; }
        public void setDiagonalPairs(int diagonalPairs) { this.diagonalPairs = diagonalPairs; }
        public int getTotalAdjacentPairs() { return totalAdjacentPairs; }
        public void setTotalAdjacentPairs(int totalAdjacentPairs) { this.totalAdjacentPairs = totalAdjacentPairs; }
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        public String getWarning() { return warning; }
        public void setWarning(String warning) { this.warning = warning; }
    }

    public static class HallGridMixedResult {
        public Student[][] grid;
        public String warning;
    }

    /**
     * Mixed Allocation Algorithm:
     * - Groups students by department.
     * - Fills rows x cols so no two same-department students are adjacent horizontally or vertically.
     * - Uses round-robin interleaving with alternate row offset (checkerboard style).
     * - Greedy selection picks the department with the most remaining students without conflict.
     * - Uses spare empty seats as separators when capacity allows. Never fails.
     * - Single department: uses alternate empty seats when capacity allows, else consecutive with warning.
     */
    public static HallGridMixedResult allocateHallGridMixed(
            ExaminationHall hall,
            Map<String, LinkedList<Student>> deptQueues,
            int remainingUsableSeatsInExam
    ) {
        int rows = hall.getNumberOfRows();
        int cols = hall.getNumberOfColumns();
        int capacity = hall.getEffectiveCapacity();

        Student[][] grid = new Student[rows + 1][cols + 1];
        HallGridMixedResult result = new HallGridMixedResult();
        result.grid = grid;

        int totalStudentsRemaining = deptQueues.values().stream().mapToInt(Collection::size).sum();
        if (totalStudentsRemaining == 0) {
            return result;
        }

        List<String> activeDepts = deptQueues.entrySet().stream()
                .filter(e -> !e.getValue().isEmpty())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        List<String> allDeptKeys = new ArrayList<>(deptQueues.keySet());
        int usableSeatsInThisHall = Math.min(capacity, rows * cols);
        int usableSeatsLaterHalls = remainingUsableSeatsInExam - usableSeatsInThisHall;

        // Single department case
        if (activeDepts.size() <= 1) {
            int alternateSeatsCount = 0;
            int seatIdx = 0;
            for (int r = 1; r <= rows; r++) {
                for (int c = 1; c <= cols; c++) {
                    seatIdx++;
                    if (seatIdx <= capacity && ((r + c) % 2 == 0)) {
                        alternateSeatsCount++;
                    }
                }
            }

            boolean canUseAlternate = (alternateSeatsCount + (usableSeatsLaterHalls / 2) >= totalStudentsRemaining);
            if (!canUseAlternate) {
                result.warning = "Warning: Only one department exists and hall capacity does not allow alternate empty seats; students allocated consecutively.";
            }

            String soleDept = activeDepts.isEmpty() ? null : activeDepts.get(0);
            LinkedList<Student> queue = (soleDept != null) ? deptQueues.get(soleDept) : null;

            seatIdx = 0;
            for (int r = 1; r <= rows; r++) {
                for (int c = 1; c <= cols; c++) {
                    seatIdx++;
                    if (seatIdx > capacity || queue == null || queue.isEmpty()) {
                        break;
                    }
                    if (canUseAlternate) {
                        if ((r + c) % 2 == 0) {
                            grid[r][c] = queue.poll();
                        }
                    } else {
                        grid[r][c] = queue.poll();
                    }
                }
            }
            return result;
        }

        // Multiple departments: Round-robin checkerboard with greedy conflict minimization
        int usableSeatsRemainingInThisHall = usableSeatsInThisHall;
        int runningSeat = 0;

        for (int r = 1; r <= rows; r++) {
            for (int c = 1; c <= cols; c++) {
                runningSeat++;
                if (runningSeat > capacity) {
                    break;
                }

                int currentTotalUnallocated = deptQueues.values().stream().mapToInt(Collection::size).sum();
                if (currentTotalUnallocated == 0) {
                    return result;
                }

                int totalSeatsAhead = usableSeatsRemainingInThisHall + usableSeatsLaterHalls;
                boolean canLeaveEmpty = (totalSeatsAhead - 1 >= currentTotalUnallocated);

                String bestDept = null;
                int bestScore = Integer.MIN_VALUE;
                int bestPenalty = 0;

                for (Map.Entry<String, LinkedList<Student>> entry : deptQueues.entrySet()) {
                    if (entry.getValue().isEmpty()) continue;
                    String dept = entry.getKey();
                    int count = entry.getValue().size();

                    int penalty = 0;
                    // Horizontal neighbor (r, c - 1)
                    if (c > 1 && grid[r][c - 1] != null && dept.equalsIgnoreCase(grid[r][c - 1].getDerivedDepartmentCode())) {
                        penalty += 10000;
                    }
                    // Vertical neighbor (r - 1, c)
                    if (r > 1 && grid[r - 1][c] != null && dept.equalsIgnoreCase(grid[r - 1][c].getDerivedDepartmentCode())) {
                        penalty += 10000;
                    }
                    // Diagonal top-left (r - 1, c - 1)
                    if (r > 1 && c > 1 && grid[r - 1][c - 1] != null && dept.equalsIgnoreCase(grid[r - 1][c - 1].getDerivedDepartmentCode())) {
                        penalty += 50;
                    }
                    // Diagonal top-right (r - 1, c + 1)
                    if (r > 1 && c < cols && grid[r - 1][c + 1] != null && dept.equalsIgnoreCase(grid[r - 1][c + 1].getDerivedDepartmentCode())) {
                        penalty += 50;
                    }

                    // Score: prioritize department with highest remaining students
                    int score = count * 100 - penalty;

                    // Checkerboard round-robin bonus
                    int targetIdx = ((r - 1) + (c - 1)) % allDeptKeys.size();
                    if (allDeptKeys.get(targetIdx).equalsIgnoreCase(dept)) {
                        score += 25;
                    }

                    if (score > bestScore) {
                        bestScore = score;
                        bestDept = dept;
                        bestPenalty = penalty;
                    }
                }

                // If best candidate causes horizontal/vertical conflict and we have spare capacity, leave empty separator
                if (bestPenalty >= 10000 && canLeaveEmpty) {
                    grid[r][c] = null;
                } else if (bestDept != null) {
                    Student s = deptQueues.get(bestDept).poll();
                    grid[r][c] = s;
                }

                usableSeatsRemainingInThisHall--;
            }
        }

        return result;
    }

    /**
     * Validates 2D layout of SeatCellDto and returns counts of adjacent same-department pairs.
     */
    public static AdjacencyValidationResult validateLayout(List<List<HallSeatGridDto.SeatCellDto>> grid) {
        int hConflicts = 0;
        int vConflicts = 0;
        int dConflicts = 0;

        if (grid == null || grid.isEmpty()) {
            AdjacencyValidationResult res = new AdjacencyValidationResult();
            res.setSummary("Adjacent same-dept pairs: 0 (H: 0, V: 0, D: 0)");
            return res;
        }

        int rows = grid.size();
        for (int r = 0; r < rows; r++) {
            List<HallSeatGridDto.SeatCellDto> row = grid.get(r);
            int cols = row.size();
            for (int c = 0; c < cols; c++) {
                HallSeatGridDto.SeatCellDto curr = row.get(c);
                if (curr == null || !curr.isOccupied() || curr.getDepartmentCode() == null) continue;
                String dept = curr.getDepartmentCode().trim().toUpperCase();

                // Horizontal (right)
                if (c + 1 < cols) {
                    HallSeatGridDto.SeatCellDto right = row.get(c + 1);
                    if (right != null && right.isOccupied() && right.getDepartmentCode() != null) {
                        if (dept.equalsIgnoreCase(right.getDepartmentCode().trim())) {
                            hConflicts++;
                        }
                    }
                }

                // Vertical (down)
                if (r + 1 < rows) {
                    HallSeatGridDto.SeatCellDto down = grid.get(r + 1).get(c);
                    if (down != null && down.isOccupied() && down.getDepartmentCode() != null) {
                        if (dept.equalsIgnoreCase(down.getDepartmentCode().trim())) {
                            vConflicts++;
                        }
                    }
                }

                // Diagonal bottom-right
                if (r + 1 < rows && c + 1 < cols) {
                    HallSeatGridDto.SeatCellDto diagBR = grid.get(r + 1).get(c + 1);
                    if (diagBR != null && diagBR.isOccupied() && diagBR.getDepartmentCode() != null) {
                        if (dept.equalsIgnoreCase(diagBR.getDepartmentCode().trim())) {
                            dConflicts++;
                        }
                    }
                }

                // Diagonal bottom-left
                if (r + 1 < rows && c - 1 >= 0) {
                    HallSeatGridDto.SeatCellDto diagBL = grid.get(r + 1).get(c - 1);
                    if (diagBL != null && diagBL.isOccupied() && diagBL.getDepartmentCode() != null) {
                        if (dept.equalsIgnoreCase(diagBL.getDepartmentCode().trim())) {
                            dConflicts++;
                        }
                    }
                }
            }
        }

        AdjacencyValidationResult res = new AdjacencyValidationResult();
        res.setHorizontalPairs(hConflicts);
        res.setVerticalPairs(vConflicts);
        res.setDiagonalPairs(dConflicts);
        res.setTotalAdjacentPairs(hConflicts + vConflicts);
        res.setSummary("Adjacent same-dept pairs: " + (hConflicts + vConflicts) +
                " (H: " + hConflicts + ", V: " + vConflicts + ", D: " + dConflicts + ")");
        return res;
    }

    /**
     * Validates 2D matrix of Student entities and returns counts of adjacent same-department pairs.
     */
    public static AdjacencyValidationResult validateStudentMatrix(Student[][] matrix, int rows, int cols) {
        int hConflicts = 0;
        int vConflicts = 0;
        int dConflicts = 0;

        for (int r = 1; r <= rows; r++) {
            for (int c = 1; c <= cols; c++) {
                Student curr = matrix[r][c];
                if (curr == null) continue;
                String dept = curr.getDerivedDepartmentCode();

                // Horizontal (right)
                if (c + 1 <= cols && matrix[r][c + 1] != null) {
                    if (dept.equalsIgnoreCase(matrix[r][c + 1].getDerivedDepartmentCode())) {
                        hConflicts++;
                    }
                }

                // Vertical (down)
                if (r + 1 <= rows && matrix[r + 1][c] != null) {
                    if (dept.equalsIgnoreCase(matrix[r + 1][c].getDerivedDepartmentCode())) {
                        vConflicts++;
                    }
                }

                // Diagonal bottom-right
                if (r + 1 <= rows && c + 1 <= cols && matrix[r + 1][c + 1] != null) {
                    if (dept.equalsIgnoreCase(matrix[r + 1][c + 1].getDerivedDepartmentCode())) {
                        dConflicts++;
                    }
                }

                // Diagonal bottom-left
                if (r + 1 <= rows && c - 1 >= 1 && matrix[r + 1][c - 1] != null) {
                    if (dept.equalsIgnoreCase(matrix[r + 1][c - 1].getDerivedDepartmentCode())) {
                        dConflicts++;
                    }
                }
            }
        }

        AdjacencyValidationResult res = new AdjacencyValidationResult();
        res.setHorizontalPairs(hConflicts);
        res.setVerticalPairs(vConflicts);
        res.setDiagonalPairs(dConflicts);
        res.setTotalAdjacentPairs(hConflicts + vConflicts);
        res.setSummary("Adjacent same-dept pairs: " + (hConflicts + vConflicts) +
                " (H: " + hConflicts + ", V: " + vConflicts + ", D: " + dConflicts + ")");
        return res;
    }

    public List<SeatAllocation> getAllocationsForExam(Long examId) {
        return allocationRepository.findByExaminationIdOrderByExaminationHall_HallNameAscRowNumberAscColumnNumberAsc(examId);
    }

    @Transactional
    public void clearAllocations(Long examId) {
        allocationRepository.deleteByExaminationId(examId);
    }

    @Transactional
    public void updateAttendance(Long allocationId, String status) {
        SeatAllocation sa = allocationRepository.findById(allocationId)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation not found with ID: " + allocationId));
        sa.setAttendanceStatus(status);
        allocationRepository.save(sa);
    }

    public List<SeatAllocation> getAllocationsForStudent(Long studentId) {
        return allocationRepository.findByStudentId(studentId);
    }

    public List<SeatAllocation> getAllocationsForStudentUsername(String username) {
        return allocationRepository.findByStudent_User_UsernameOrderByExamination_ExaminationDateAscExamination_StartTimeAsc(username);
    }

    public static String getRowLetter(int rowNumber) {
        StringBuilder sb = new StringBuilder();
        int r = rowNumber;
        while (r > 0) {
            int rem = (r - 1) % 26;
            sb.insert(0, (char) ('A' + rem));
            r = (r - 1) / 26;
        }
        return sb.toString();
    }
}
