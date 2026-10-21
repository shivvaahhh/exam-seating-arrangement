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

        // Step 5: Prepare student list with Round-Robin interleaving across departments
        List<Student> students = registrations.stream()
                .map(ExaminationRegistration::getStudent)
                .collect(Collectors.toList());

        if ("REGNO_ONLY".equalsIgnoreCase(request.getSortStrategy())) {
            students.sort(Comparator.comparing(Student::getRegisterNumber));
        } else {
            // Group students by department
            Map<Long, List<Student>> deptBuckets = new LinkedHashMap<>();
            for (Student s : students) {
                Long deptId = s.getDepartment() != null ? s.getDepartment().getId() : 0L;
                deptBuckets.computeIfAbsent(deptId, k -> new ArrayList<>()).add(s);
            }

            // Sort within each department bucket by register number
            for (List<Student> bucket : deptBuckets.values()) {
                bucket.sort(Comparator.comparing(Student::getRegisterNumber));
            }

            // If students are from multiple departments, interleave them round-robin
            // so adjacent seats (left/right/front/back) do not have same department students!
            if (deptBuckets.size() > 1) {
                List<Student> interleaved = new ArrayList<>();
                boolean hasMore = true;
                int round = 0;
                while (hasMore) {
                    hasMore = false;
                    for (List<Student> bucket : deptBuckets.values()) {
                        if (round < bucket.size()) {
                            interleaved.add(bucket.get(round));
                            hasMore = true;
                        }
                    }
                    round++;
                }
                students = interleaved;
            } else {
                students.sort(Comparator.comparing(Student::getRegisterNumber));
            }
        }

        // Step 6 & 7: Distribute students across available halls respecting capacities, row/col positions and clashes
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

                    // Clash Check 1: Check if this physical seat in this hall is already booked by another exam on the same date + session
                    List<SeatAllocation> seatClashes = allocationRepository.findClashingSeatAllocations(
                            hall.getId(), seatNumber, exam.getExaminationDate(), exam.getSession(), exam.getId()
                    );
                    if (!seatClashes.isEmpty()) {
                        throw new AllocationException("Seat conflict: Hall '" + hall.getHallName() + "' Seat '" + seatNumber +
                                "' is already booked for another examination ('" +
                                seatClashes.get(0).getExamination().getExaminationName() + "') on " +
                                exam.getExaminationDate() + " (" + exam.getSession() + ").");
                    }

                    // Clash Check 2: Check if this student is already allocated in another exam on the same date + session
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
        result.setHalls(hallSummaries);

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
                    cell.setDepartmentCode(sa.getStudent().getDepartment().getDepartmentCode());
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
        return grid;
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

    private String getRowLetter(int rowNumber) {
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
