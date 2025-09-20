package com.lsit.dfds.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lsit.dfds.dto.HADPBudgetAllocationDto;
import com.lsit.dfds.dto.HADPBudgetResponseDto;
import com.lsit.dfds.dto.IdNameDto;
import com.lsit.dfds.entity.District;
import com.lsit.dfds.entity.FinancialYear;
import com.lsit.dfds.entity.HADP;
import com.lsit.dfds.entity.HADPBudget;
import com.lsit.dfds.entity.Schemes;
import com.lsit.dfds.entity.Taluka;
import com.lsit.dfds.entity.User;
import com.lsit.dfds.enums.HadpType;
import com.lsit.dfds.enums.Roles;
import com.lsit.dfds.enums.Statuses;
import com.lsit.dfds.repo.DistrictRepository;
import com.lsit.dfds.repo.FinancialYearRepository;
import com.lsit.dfds.repo.HADPBudgetRepository;
import com.lsit.dfds.repo.HADPRepository;
import com.lsit.dfds.repo.SchemesRepository;
import com.lsit.dfds.repo.TalukaRepository;
import com.lsit.dfds.repo.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HADPBudgetServiceImpl implements HADPBudgetService {

    private final HADPBudgetRepository hadpBudgetRepo;
    private final HADPRepository hadpRepo;
    private final DistrictRepository districtRepo;
    private final TalukaRepository talukaRepo;
    private final FinancialYearRepository fyRepo;
    private final SchemesRepository schemesRepo;
    private final UserRepository userRepo;

    @Override
    @Transactional
    public String allocateBudget(HADPBudgetAllocationDto dto, String createdBy) {

        // Fetch Taluka
        Taluka taluka = talukaRepo.findById(dto.getTalukaId())
                .orElseThrow(() -> new RuntimeException("Taluka not found: " + dto.getTalukaId()));

        // Fetch HADP by taluka
        HADP hadp = hadpRepo.findByTaluka_Id(dto.getTalukaId())
                .orElseThrow(() -> new RuntimeException("HADP not found for taluka: " + dto.getTalukaId()));

        // District from taluka
        District district = taluka.getDistrict();

        // Financial year
        FinancialYear fy = fyRepo.findById(dto.getFinancialYearId())
                .orElseThrow(() -> new RuntimeException("Financial Year not found: " + dto.getFinancialYearId()));

        // ✅ Scheme: prefer schemeId from DTO, else fallback to HADP
        Schemes scheme;
        if (dto.getSchemeId() != null) {
            scheme = schemesRepo.findById(dto.getSchemeId())
                    .orElseThrow(() -> new RuntimeException("Scheme not found: " + dto.getSchemeId()));
        } else {
            scheme = hadp.getScheme();
            if (scheme == null) {
                throw new RuntimeException("Scheme is not linked to HADP record for taluka: " + dto.getTalukaId());
            }
        }

        // ✅ Create new OR update existing budget
        HADPBudget budget = hadpBudgetRepo
                .findByTaluka_IdAndFinancialYear_Id(dto.getTalukaId(), dto.getFinancialYearId())
                .orElseGet(() -> {
                    HADPBudget newBudget = new HADPBudget();
                    newBudget.setHadp(hadp);
                    newBudget.setTaluka(taluka);
                    newBudget.setDistrict(district);
                    newBudget.setFinancialYear(fy);
                    newBudget.setScheme(scheme);
                    newBudget.setUtilizedLimit(0.0);  // first time, nothing utilized
                    newBudget.setStatus(Statuses.ACTIVE);
                    newBudget.setCreatedBy(createdBy);
                    return newBudget;
                });

        // Always update allocation info
        budget.setAllocatedLimit(dto.getAllocatedLimit());
        // Recalculate remaining = allocated - utilized
        budget.setRemainingLimit(dto.getAllocatedLimit() - budget.getUtilizedLimit());
        budget.setScheme(scheme);
        budget.setRemarks(dto.getRemarks());

        hadpBudgetRepo.save(budget);

        return "✅ HADP Budget allocated successfully to Taluka " + taluka.getTalukaName();
    }


    @Override
    public List<HADPBudgetResponseDto> fetchBudgets(String username, Long districtId, Long hadpTalukaId,
                                                    Long financialYearId) {

        User user = userRepo.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        Roles role = user.getRole();

        if (role == Roles.DISTRICT_ADMIN) {
            districtId = user.getDistrict().getId();
        } else if (role == Roles.IA_ADMIN) {
            districtId = user.getDistrict().getId();
        }

        List<HADPBudget> budgets;

        if (districtId != null && hadpTalukaId != null) {
            budgets = hadpBudgetRepo.findByDistrict_IdAndTaluka_Id(districtId, hadpTalukaId);
        } else if (districtId != null) {
            budgets = hadpBudgetRepo.findByDistrict_Id(districtId);
        } else if (hadpTalukaId != null) {
            budgets = hadpBudgetRepo.findByTaluka_Id(hadpTalukaId);
        } else if (financialYearId != null) {
            budgets = hadpBudgetRepo.findByFinancialYear_Id(financialYearId);
        } else {
            budgets = hadpBudgetRepo.findAll();
        }

        return budgets.stream().map(b -> {
            HADPBudgetResponseDto res = new HADPBudgetResponseDto();
            res.setId(b.getId());
            res.setHadpId(b.getHadp().getId());
            res.setSchemeId(b.getScheme().getId());           // ✅ scheme always included
            res.setSchemeName(b.getScheme().getSchemeName()); // ✅ scheme always included

            District d = b.getTaluka() != null ? b.getTaluka().getDistrict() : null;
            res.setDistrictId(d != null ? d.getId() : null);
            res.setDistrictName(d != null ? d.getDistrictName() : null);
            res.setTalukaId(b.getTaluka() != null ? b.getTaluka().getId() : null);
            res.setTalukaName(b.getTaluka() != null ? b.getTaluka().getTalukaName() : null);

            res.setFinancialYear(b.getFinancialYear().getFinacialYear());
            res.setAllocatedLimit(b.getAllocatedLimit());
            res.setUtilizedLimit(b.getUtilizedLimit());
            res.setRemainingLimit(b.getRemainingLimit());
            res.setStatus(b.getStatus().name());
            res.setRemarks(b.getRemarks());
            return res;
        }).collect(Collectors.toList());
    }

    @Transactional
    public List<IdNameDto> getDistrictsByHadpPlanType(HadpType planType) {
        List<HADP> hadpList = hadpRepo.findAll();

        return hadpList.stream()
                .filter(h -> h.getType() == planType)
                .map(h -> new IdNameDto(h.getDistrict().getId(), h.getDistrict().getDistrictName()))
                .distinct()
                .collect(Collectors.toList());
    }

    @Transactional
    public List<IdNameDto> getTalukasByDistrictId(Long districtId) {
        List<Taluka> talukas = talukaRepo.findByDistrict_Id(districtId);
        return talukas.stream()
                .map(t -> new IdNameDto(t.getId(), t.getTalukaName()))
                .collect(Collectors.toList());
    }

    @Transactional
    public List<IdNameDto> getTalukasByHadpType(HadpType planType) {
        List<HADP> hadpList = hadpRepo.findAll();

        return hadpList.stream()
                .filter(h -> h.getType() == planType)
                .map(h -> new IdNameDto(h.getTaluka().getId(), h.getTaluka().getTalukaName()))
                .distinct()
                .collect(Collectors.toList());
    }
}
