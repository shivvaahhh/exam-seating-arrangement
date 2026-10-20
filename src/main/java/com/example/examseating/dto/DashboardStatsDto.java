package com.example.examseating.dto;

import java.util.HashMap;
import java.util.Map;

public class DashboardStatsDto {

    private long totalStudents;
    private long totalDepartments;
    private long totalHalls;
    private long upcomingExams;
    private long totalAllocatedStudents;
    private long studentsNotYetAllocated;

    private Long totalActiveCapacity;

    private Map<String, Long> departmentStudentDistribution = new HashMap<>();

    public DashboardStatsDto() {}

    public long getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(long totalStudents) {
        this.totalStudents = totalStudents;
    }

    public long getTotalDepartments() {
        return totalDepartments;
    }

    public void setTotalDepartments(long totalDepartments) {
        this.totalDepartments = totalDepartments;
    }

    public long getTotalHalls() {
        return totalHalls;
    }

    public void setTotalHalls(long totalHalls) {
        this.totalHalls = totalHalls;
    }

    public long getUpcomingExams() {
        return upcomingExams;
    }

    public void setUpcomingExams(long upcomingExams) {
        this.upcomingExams = upcomingExams;
    }

    public long getTotalAllocatedStudents() {
        return totalAllocatedStudents;
    }

    public void setTotalAllocatedStudents(long totalAllocatedStudents) {
        this.totalAllocatedStudents = totalAllocatedStudents;
    }

    public long getStudentsNotYetAllocated() {
        return studentsNotYetAllocated;
    }

    public void setStudentsNotYetAllocated(long studentsNotYetAllocated) {
        this.studentsNotYetAllocated = studentsNotYetAllocated;
    }

    public Long getTotalActiveCapacity() {
        return totalActiveCapacity;
    }

    public void setTotalActiveCapacity(Long totalActiveCapacity) {
        this.totalActiveCapacity = totalActiveCapacity;
    }

    public Map<String, Long> getDepartmentStudentDistribution() {
        return departmentStudentDistribution;
    }

    public void setDepartmentStudentDistribution(Map<String, Long> departmentStudentDistribution) {
        this.departmentStudentDistribution = departmentStudentDistribution;
    }
}
