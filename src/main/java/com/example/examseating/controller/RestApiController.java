package com.example.examseating.controller;

import com.example.examseating.dto.*;
import com.example.examseating.entity.SeatAllocation;
import com.example.examseating.entity.Student;
import com.example.examseating.service.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class RestApiController {

    private final DepartmentService departmentService;
    private final SubjectService subjectService;
    private final StudentService studentService;
    private final ExaminationHallService hallService;
    private final ExaminationService examinationService;
    private final SeatAllocationService allocationService;
    private final ReportService reportService;

    public RestApiController(DepartmentService departmentService,
                             SubjectService subjectService,
                             StudentService studentService,
                             ExaminationHallService hallService,
                             ExaminationService examinationService,
                             SeatAllocationService allocationService,
                             ReportService reportService) {
        this.departmentService = departmentService;
        this.subjectService = subjectService;
        this.studentService = studentService;
        this.hallService = hallService;
        this.examinationService = examinationService;
        this.allocationService = allocationService;
        this.reportService = reportService;
    }

    // Departments
    @GetMapping("/departments")
    public List<DepartmentDto> getAllDepartments() {
        return departmentService.findAll();
    }

    @PostMapping("/departments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DepartmentDto> createDepartment(@Valid @RequestBody DepartmentDto dto) {
        return new ResponseEntity<>(departmentService.create(dto), HttpStatus.CREATED);
    }

    @PutMapping("/departments/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public DepartmentDto updateDepartment(@PathVariable Long id, @Valid @RequestBody DepartmentDto dto) {
        return departmentService.update(id, dto);
    }

    @DeleteMapping("/departments/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDepartment(@PathVariable Long id) {
        departmentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Subjects
    @GetMapping("/subjects")
    public List<SubjectDto> getAllSubjects(@RequestParam(value = "deptId", required = false) Long deptId,
                                           @RequestParam(value = "semester", required = false) Integer semester) {
        return subjectService.findByDepartmentAndSemester(deptId, semester);
    }

    @PostMapping("/subjects")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SubjectDto> createSubject(@Valid @RequestBody SubjectDto dto) {
        return new ResponseEntity<>(subjectService.create(dto), HttpStatus.CREATED);
    }

    @PutMapping("/subjects/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public SubjectDto updateSubject(@PathVariable Long id, @Valid @RequestBody SubjectDto dto) {
        return subjectService.update(id, dto);
    }

    @DeleteMapping("/subjects/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSubject(@PathVariable Long id) {
        subjectService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Students
    @GetMapping("/students")
    @PreAuthorize("hasRole('ADMIN')")
    public List<StudentDto> getAllStudents(@RequestParam(value = "deptId", required = false) Long deptId,
                                          @RequestParam(value = "semester", required = false) Integer semester,
                                          @RequestParam(value = "keyword", required = false) String keyword) {
        return studentService.findWithFilters(deptId, semester, keyword);
    }

    @GetMapping("/students/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public StudentDto getStudentById(@PathVariable Long id) {
        return studentService.findById(id);
    }

    @PostMapping("/students")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StudentDto> createStudent(@Valid @RequestBody StudentDto dto) {
        return new ResponseEntity<>(studentService.create(dto), HttpStatus.CREATED);
    }

    @PutMapping("/students/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public StudentDto updateStudent(@PathVariable Long id, @Valid @RequestBody StudentDto dto) {
        return studentService.update(id, dto);
    }

    @DeleteMapping("/students/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        studentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Examination Halls
    @GetMapping("/halls")
    public List<ExaminationHallDto> getAllHalls() {
        return hallService.findAll();
    }

    @PostMapping("/halls")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ExaminationHallDto> createHall(@Valid @RequestBody ExaminationHallDto dto) {
        return new ResponseEntity<>(hallService.create(dto), HttpStatus.CREATED);
    }

    @PutMapping("/halls/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ExaminationHallDto updateHall(@PathVariable Long id, @Valid @RequestBody ExaminationHallDto dto) {
        return hallService.update(id, dto);
    }

    @DeleteMapping("/halls/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteHall(@PathVariable Long id) {
        hallService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Examinations
    @GetMapping("/examinations")
    public List<ExaminationDto> getAllExaminations() {
        return examinationService.findAll();
    }

    @PostMapping("/examinations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ExaminationDto> createExamination(@Valid @RequestBody ExaminationDto dto) {
        return new ResponseEntity<>(examinationService.create(dto), HttpStatus.CREATED);
    }

    @PutMapping("/examinations/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ExaminationDto updateExamination(@PathVariable Long id, @Valid @RequestBody ExaminationDto dto) {
        return examinationService.update(id, dto);
    }

    @DeleteMapping("/examinations/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteExamination(@PathVariable Long id) {
        examinationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Automatic Allocation Engine REST API
    @PostMapping("/examinations/{id}/allocate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AllocationResultDto> allocateSeatsForExam(@PathVariable Long id,
                                                                   @Valid @RequestBody AllocationRequestDto requestDto) {
        requestDto.setExaminationId(id);
        AllocationResultDto result = allocationService.allocateSeats(requestDto);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/examinations/{id}/allocations")
    @PreAuthorize("hasRole('ADMIN')")
    public List<SeatAllocation> getAllocations(@PathVariable Long id) {
        return allocationService.getAllocationsForExam(id);
    }

    @GetMapping("/examinations/{id}/halls/{hallId}/seating")
    public HallSeatGridDto getHallSeatingGrid(@PathVariable Long id, @PathVariable Long hallId) {
        return allocationService.getHallSeatGrid(id, hallId);
    }

    // Student self-service REST API
    @GetMapping("/students/me/allocations")
    @PreAuthorize("hasRole('STUDENT')")
    public List<SeatAllocation> getMyAllocations(@AuthenticationPrincipal UserDetails userDetails) {
        Student student = studentService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Student not found for current session"));
        return allocationService.getAllocationsForStudent(student.getId());
    }

    // Reports REST APIs
    @GetMapping("/reports/halls/utilization")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ReportService.HallUtilizationDto> getHallUtilization() {
        return reportService.getHallUtilizationReport();
    }
}
