package com.example.examseating.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class AllocationRequestDto {

    @NotNull(message = "Examination must be selected")
    private Long examinationId;

    @NotEmpty(message = "At least one examination hall must be selected")
    private List<Long> hallIds;

    private String sortStrategy = "DEPT_REGNO"; // "DEPT_REGNO" or "REGNO_ONLY"

    private String allocationMode = "SEQUENTIAL"; // "SEQUENTIAL" or "MIXED"

    private boolean forceRegenerate = false;

    public AllocationRequestDto() {}

    public AllocationRequestDto(Long examinationId, List<Long> hallIds) {
        this.examinationId = examinationId;
        this.hallIds = hallIds;
    }

    public AllocationRequestDto(Long examinationId, List<Long> hallIds, String allocationMode) {
        this.examinationId = examinationId;
        this.hallIds = hallIds;
        this.allocationMode = allocationMode;
    }

    public Long getExaminationId() {
        return examinationId;
    }

    public void setExaminationId(Long examinationId) {
        this.examinationId = examinationId;
    }

    public List<Long> getHallIds() {
        return hallIds;
    }

    public void setHallIds(List<Long> hallIds) {
        this.hallIds = hallIds;
    }

    public String getSortStrategy() {
        return sortStrategy;
    }

    public void setSortStrategy(String sortStrategy) {
        this.sortStrategy = sortStrategy;
    }

    public String getAllocationMode() {
        return allocationMode;
    }

    public void setAllocationMode(String allocationMode) {
        this.allocationMode = allocationMode;
    }

    public boolean isForceRegenerate() {
        return forceRegenerate;
    }

    public void setForceRegenerate(boolean forceRegenerate) {
        this.forceRegenerate = forceRegenerate;
    }
}
