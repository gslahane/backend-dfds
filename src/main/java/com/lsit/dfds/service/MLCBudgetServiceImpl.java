package com.lsit.dfds.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.lsit.dfds.dto.MLCBudgetPostDto;
import com.lsit.dfds.dto.MLCBudgetResponseDto;
import com.lsit.dfds.entity.FinancialYear;
import com.lsit.dfds.entity.MLC;
import com.lsit.dfds.entity.MLCBudget;
import com.lsit.dfds.entity.Schemes;
import com.lsit.dfds.enums.PlanType;
import com.lsit.dfds.enums.Statuses;
import com.lsit.dfds.repo.FinancialYearRepository;
import com.lsit.dfds.repo.MLCBudgetRepository;
import com.lsit.dfds.repo.MLCRepository;
import com.lsit.dfds.repo.SchemesRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MLCBudgetServiceImpl implements MLCBudgetService {

    private final MLCBudgetRepository mlcBudgetRepository;
    private final MLCRepository mlcRepository;
    private final FinancialYearRepository financialYearRepository;
    private final SchemesRepository schemesRepository;

    @Override
    public List<MLCBudgetResponseDto> getMLCBudgetSummary(String username, Long financialYearId) {
        List<MLCBudget> budgets;

        if (financialYearId != null) {
            budgets = mlcBudgetRepository.findByFinancialYear_Id(financialYearId);
        } else {
            budgets = mlcBudgetRepository.findAll();
        }

        // ✅ Only include PlanType.MLC budgets
        return budgets.stream()
                .filter(b -> b.getScheme() != null && b.getScheme().getPlanType() == PlanType.MLC)
                .map(budget -> {
                    MLCBudgetResponseDto dto = new MLCBudgetResponseDto();
                    dto.setId(budget.getId());
                    dto.setFinancialYear(budget.getFinancialYear().getFinacialYear());
                    dto.setMlcName(budget.getMlc().getMlcName());
                    dto.setCategory(budget.getMlc().getCategory());
                    dto.setDisrict(budget.getMlc().getDistrict().getDistrictName());
                    dto.setSchemeName(budget.getScheme().getSchemeName());
                    dto.setAllocatedLimit(budget.getAllocatedLimit());
                    dto.setUtilizedLimit(budget.getUtilizedLimit());
                    dto.setRemainingLimit(budget.getRemainingLimit());
                    dto.setStatus(budget.getStatus().name());
                    dto.setRemarks(budget.getRemarks());
                    return dto;
                })
                .toList();
    }

    @Override
    public MLCBudgetResponseDto saveMLCBudget(MLCBudgetPostDto dto, String createdBy) {
        FinancialYear fy = financialYearRepository.findById(dto.getFinancialYearId())
                .orElseThrow(() -> new RuntimeException("Financial Year not found"));

        MLC mlc = mlcRepository.findById(dto.getMlcId())
                .orElseThrow(() -> new RuntimeException("MLC not found"));

        // ✅ Find a scheme with PlanType = MLC
        Schemes scheme = schemesRepository.findAll().stream()
                .filter(s -> s.getPlanType() == PlanType.MLC)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No MLC scheme configured in the system"));

        MLCBudget budget = new MLCBudget();
        budget.setFinancialYear(fy);
        budget.setMlc(mlc);
        budget.setScheme(scheme);
        budget.setAllocatedLimit(dto.getAllocatedLimit());
        budget.setUtilizedLimit(0.0);
//        budget.setRemainingLimit(dto.getAllocatedLimit());
        budget.setStatus(Statuses.ACTIVE);
        budget.setRemarks(dto.getRemarks());
        budget.setCreatedBy(createdBy);

        MLCBudget savedBudget = mlcBudgetRepository.save(budget);

        MLCBudgetResponseDto responseDto = new MLCBudgetResponseDto();
        responseDto.setId(savedBudget.getId());
        responseDto.setFinancialYear(savedBudget.getFinancialYear().getFinacialYear());
        responseDto.setMlcName(savedBudget.getMlc().getMlcName());
        responseDto.setDisrict(mlc.getDistrict().getDistrictName());
        responseDto.setCategory(savedBudget.getMlc().getCategory());
        responseDto.setSchemeName(savedBudget.getScheme().getSchemeName());
        responseDto.setAllocatedLimit(savedBudget.getAllocatedLimit());
        responseDto.setUtilizedLimit(savedBudget.getUtilizedLimit());
        responseDto.setRemainingLimit(savedBudget.getRemainingLimit());
        responseDto.setStatus(savedBudget.getStatus().name());
        responseDto.setRemarks(savedBudget.getRemarks());

        return responseDto;
    }
}
