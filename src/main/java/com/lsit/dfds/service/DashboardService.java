package com.lsit.dfds.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lsit.dfds.dto.AdminDAPBudgetRespDto;
import com.lsit.dfds.dto.AdminDAPDashboardRequest;
import com.lsit.dfds.dto.AdminDAPDashboardResponce;
import com.lsit.dfds.dto.AdminDAPSchemesDto;
import com.lsit.dfds.dto.AdminMLDashboardDto;
import com.lsit.dfds.dto.AdminMLrequestDto;
import com.lsit.dfds.dto.OverallBudgetDto;
import com.lsit.dfds.entity.Constituency;
import com.lsit.dfds.entity.DemandCode;
import com.lsit.dfds.entity.District;
import com.lsit.dfds.entity.DistrictLimitAllocation;
import com.lsit.dfds.entity.FinancialYear;
import com.lsit.dfds.entity.FinancialYearBudget;
import com.lsit.dfds.entity.FundDemand;
import com.lsit.dfds.entity.MLA;
import com.lsit.dfds.entity.MLABudget;
import com.lsit.dfds.entity.MLATerm;
import com.lsit.dfds.entity.MLC;
import com.lsit.dfds.entity.MLCBudget;
import com.lsit.dfds.entity.SchemeBudget;
import com.lsit.dfds.entity.Schemes;
import com.lsit.dfds.entity.User;
import com.lsit.dfds.entity.Work;
import com.lsit.dfds.enums.DemandStatuses;
import com.lsit.dfds.enums.PlanType;
import com.lsit.dfds.enums.Roles;
import com.lsit.dfds.repo.ConstituencyRepository;
import com.lsit.dfds.repo.DemandCodeRepository;
import com.lsit.dfds.repo.DistrictLimitAllocationRepository;
import com.lsit.dfds.repo.DistrictRepository;
import com.lsit.dfds.repo.FinancialYearBudgetRepository;
import com.lsit.dfds.repo.FinancialYearRepository;
import com.lsit.dfds.repo.FundDemandRepository;
import com.lsit.dfds.repo.MLABudgetRepository;
import com.lsit.dfds.repo.MLARepository;
import com.lsit.dfds.repo.MLATermRepository;
import com.lsit.dfds.repo.MLCBudgetRepository;
import com.lsit.dfds.repo.MLCRepository;
import com.lsit.dfds.repo.SchemeBudgetRepository;
import com.lsit.dfds.repo.SchemesRepository;
import com.lsit.dfds.repo.UserRepository;
import com.lsit.dfds.repo.WorkRepository;

@Service
public class DashboardService implements IDashboardService {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private DemandCodeRepository schemeTypeRepository;

	@Autowired
	private FinancialYearBudgetRepository financialYearBudgetRepository;

	@Autowired
	private DistrictLimitAllocationRepository districtLimitAllocationRepository;

	@Autowired
	private FundDemandRepository fundDemandRepository;

	@Autowired
	private WorkRepository workRepository;

	@Autowired
	private SchemesRepository schemesRepository;

	@Autowired
	private SchemeBudgetRepository schemeBudgetRepository;

	@Autowired
	private MLARepository mLARepository;

	@Autowired
	private MLABudgetRepository mLABudgetRepository;

	@Autowired
	private MLCRepository mLCRepository;
	@Autowired
	private MLCBudgetRepository mLCBudgetRepository;

	@Autowired
	private ConstituencyRepository constituencyRepository;

	@Autowired
	private FinancialYearRepository financialYearRepository;

	@Autowired
	private DistrictRepository districtRepository;

	@Autowired
	private MLATermRepository mlaTermRepository;

	@Override
	public List<OverallBudgetDto> getOverallStateBudget(String username, Long financialYear_Id) {
		// Fetch the user based on the username
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new RuntimeException("User not found: " + username));

		Roles role = user.getRole();
		// Check if the user has the STATE_ADMIN role
		if (!(role == Roles.STATE_ADMIN || role == Roles.STATE_CHECKER || role == Roles.STATE_MAKER)) {
			throw new RuntimeException("Unauthorized user: " + username);
		}

		// Fetch FinancialYearBudget records based on the financialYear_Id
		List<FinancialYearBudget> financialYearBudgets = financialYearBudgetRepository
				.findByFinancialYear_Id(financialYear_Id);
		if (financialYearBudgets.isEmpty()) {
			throw new RuntimeException("Financial year budgets not found for ID: " + financialYear_Id);
		}

		// Using Java Streams to map FinancialYearBudget to OverallBudgetDto
		return financialYearBudgets.stream().map(financialYearBudget -> {
			OverallBudgetDto overallBudgetDto = new OverallBudgetDto();
			overallBudgetDto.setPlanType(financialYearBudget.getPlanType());
			overallBudgetDto.setAllocatedLimit(financialYearBudget.getAllocatedLimit());
			overallBudgetDto.setUtilizedLimit(financialYearBudget.getUtilizedLimit());
			return overallBudgetDto;
		}).collect(Collectors.toList()); // Collect the result into a List<OverallBudgetDto>
	}

	@Override
	public List<AdminDAPDashboardResponce> getAdminDAPDashboard(String username, AdminDAPDashboardRequest request) {
		// Fetch the user
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new RuntimeException("User not found: " + username));

		Roles role = user.getRole();
		if (!(role == Roles.STATE_ADMIN || role == Roles.STATE_CHECKER || role == Roles.STATE_MAKER)) {
			throw new RuntimeException("Unauthorized user: " + username);
		}

		FinancialYear fy = financialYearRepository.findById(request.getFinancialYearId())
				.orElseThrow(() -> new RuntimeException("Financial Year not found"));

		District district = request.getDistrictId() != null
				? districtRepository.findById(request.getDistrictId()).orElse(null)
				: null;

		// ✅ Fetch relevant SchemeTypes
		List<DemandCode> schemeTypes = new ArrayList<>();
		if (request.getSchemeTypeId() != null) {
			schemeTypeRepository.findById(request.getSchemeTypeId()).ifPresent(schemeTypes::add);
		} else if (district != null) {
			schemeTypes = schemeTypeRepository.findAllByDistricts_Id(district.getId()); // ✅ UPDATED
		} else {
			schemeTypes = schemeTypeRepository.findAll();
		}

		List<AdminDAPDashboardResponce> resp = new ArrayList<>();

		// ✅ Iterate through each SchemeType
		for (DemandCode schemeType : schemeTypes) {

			// ✅ Iterate through each district mapped to this SchemeType
			for (District mappedDistrict : schemeType.getDistricts()) {

				// ✅ If a specific district filter is applied, skip others
				if (district != null && !mappedDistrict.getId().equals(district.getId())) {
					continue;
				}

				// Fetch District Limit Allocation for this district
				DistrictLimitAllocation districtLimitAllocation = districtLimitAllocationRepository
						.findFirstByDistrict_IdAndFinancialYear_Id(mappedDistrict.getId(), fy.getId());

				Double pendingDemand = 0.0;

				// ✅ Fetch Schemes for this SchemeType & District
				List<Schemes> schemes = schemesRepository.findSchemesBySchemeTypeAndDistrictId(mappedDistrict.getId(),
						schemeType.getId());

				for (Schemes scheme : schemes) {
					List<Work> works = workRepository.findByScheme_Id(scheme.getId());

					for (Work work : works) {
						List<FundDemand> fundDemands = fundDemandRepository.findByWork_Id(work.getId());

						for (FundDemand fundDemand : fundDemands) {
							if (fundDemand.getStatus() == DemandStatuses.PENDING
									&& fundDemand.getFinancialYear().getId().equals(fy.getId())) {
								pendingDemand += fundDemand.getAmount();
							}
						}
					}
				}

				// ✅ Create response per district per schemeType
				AdminDAPDashboardResponce response = new AdminDAPDashboardResponce();
				response.setSchemeType(schemeType);
				response.setDistrict(mappedDistrict);
				response.setPendingDemand(pendingDemand);
				response.setAllocatedLimit(
						districtLimitAllocation != null ? districtLimitAllocation.getAllocatedLimit() : 0.0);
				response.setUtilizedLimit(
						districtLimitAllocation != null ? districtLimitAllocation.getUtilizedLimit() : 0.0);

				resp.add(response);
			}
		}

		return resp;
	}

	@Override
	public AdminDAPBudgetRespDto getAdminDAPBudget(String username, Long financialYear_Id) {
		// Fetch the user based on the username
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new RuntimeException("User not found: " + username));

		Roles role = user.getRole();
		// Check if the user has the STATE_ADMIN, STATE_CHECKER, or STATE_MAKER role
		if (!(role == Roles.STATE_ADMIN || role == Roles.STATE_CHECKER || role == Roles.STATE_MAKER)) {
			throw new RuntimeException("Unauthorized user: " + username);
		}

		// Fetch the financial year budget for the given PlanType and financialYearId
		FinancialYearBudget financialYearBudget = financialYearBudgetRepository
				.findByPlanTypeAndFinancialYear_Id(PlanType.DAP, financialYear_Id);

		// Check if the result is null
		if (financialYearBudget == null) {
			throw new RuntimeException("Financial year budget not found for ID: " + financialYear_Id);
		}
		Double pendingDemand = 0.0;
		List<Schemes> schemes = schemesRepository.findByPlanType(PlanType.DAP);

		// Loop through each Scheme
		for (Schemes scheme : schemes) {
			// Fetch all works associated with the current scheme
			List<Work> works = workRepository.findByScheme_Id(scheme.getId());

			// Loop through each Work
			for (Work work : works) {
				// Fetch all FundDemand records related to the current Work
				List<FundDemand> fundDemands = fundDemandRepository.findByWork_Id(work.getId());

				// Loop through each FundDemand and check for the pending status
				for (FundDemand fundDemand : fundDemands) {
					// If the FundDemand status is pending, add its amount to the pending demand
					if (fundDemand.getStatus() == DemandStatuses.PENDING
							&& fundDemand.getFinancialYear().getId() == financialYear_Id) {
						pendingDemand += fundDemand.getAmount();
					}
				}
			}
		}

		// Logic to convert the financialYearBudget to AdminDAPBudgetRespDto
		AdminDAPBudgetRespDto responseDto = new AdminDAPBudgetRespDto();
		responseDto.setTotalBudget(financialYearBudget.getAllocatedLimit());
		responseDto.setFundsDistributed(financialYearBudget.getUtilizedLimit());
		responseDto
				.setRemainingBalance(financialYearBudget.getAllocatedLimit() - financialYearBudget.getUtilizedLimit());
		responseDto.setPendingDemandAmount(pendingDemand);
		// If you want to return a list, create a list with one response object
		return responseDto;
	}

	@Override
	public List<AdminDAPSchemesDto> getAdminDAPSchemes(String username, Long district_Id, Long schemeType_Id,
			Long financialYear_Id) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new RuntimeException("User not found: " + username));

		Roles role = user.getRole();
		// Check if the user has the STATE_ADMIN, STATE_CHECKER, or STATE_MAKER role
		if (!(role == Roles.STATE_ADMIN || role == Roles.STATE_CHECKER || role == Roles.STATE_MAKER)) {
			throw new RuntimeException("Unauthorized user: " + username);
		}

		List<Schemes> schemes = schemesRepository.findSchemesBySchemeTypeAndDistrictId(schemeType_Id, district_Id);
		List<AdminDAPSchemesDto> resp = new ArrayList<>();
		// Loop through each Scheme
		for (Schemes scheme : schemes) {
			double pendingDemand = 0.0;
			SchemeBudget schemeBudget = schemeBudgetRepository.findByFinancialYear_IdAndScheme_Id(financialYear_Id,
					scheme.getId());
			// Fetch all works associated with the current scheme
			List<Work> works = workRepository.findByScheme_Id(scheme.getId());

			// Loop through each Work
			for (Work work : works) {
				// Fetch all FundDemand records related to the current Work
				List<FundDemand> fundDemands = fundDemandRepository.findByWork_Id(work.getId());

				// Loop through each FundDemand and check for the pending status
				for (FundDemand fundDemand : fundDemands) {
					// If the FundDemand status is pending, add its amount to the pending demand
					if (fundDemand.getStatus() == DemandStatuses.PENDING
							&& fundDemand.getFinancialYear().getId() == financialYear_Id) {
						pendingDemand += fundDemand.getAmount();
					}
				}
			}
			AdminDAPSchemesDto responce = new AdminDAPSchemesDto();
			responce.setScheme(scheme.getSchemeName());
			responce.setAllocatedLimit(schemeBudget.getAllocatedLimit());
			responce.setUtilizedLimit(schemeBudget.getUtilizedLimit());
			responce.setPendingAmount(pendingDemand);

		}
		return resp;
	}

	@Override
	public List<AdminMLDashboardDto> getAdminMLDashboard(String username, AdminMLrequestDto request) {
		// Fetch the user based on the username
		Optional<User> userOpt = userRepository.findByUsername(username);
		if (userOpt.isEmpty()) {
			throw new RuntimeException("User not found: " + username);
		}

		User user = userOpt.get();
		Roles role = user.getRole();

		// Check if the user has one of the allowed roles (STATE_ADMIN, STATE_CHECKER,
		// or STATE_MAKER)
		if (!(role == Roles.STATE_ADMIN || role == Roles.STATE_CHECKER || role == Roles.STATE_MAKER)) {
			throw new RuntimeException("Unauthorized user: " + username);
		}

		List<AdminMLDashboardDto> responseList = new ArrayList<>();

		// Handle MLA or MLC based on the provided request
		if (request.getMlaId() != null) {
			Optional<MLA> mlaOpt = mLARepository.findMLAById(request.getMlaId());
			if (mlaOpt.isPresent()) {
				MLA mla = mlaOpt.get();
				AdminMLDashboardDto dto = new AdminMLDashboardDto();
				// Populate the DTO fields
				dto.setMlaMlcName(mla.getMlaName());
				dto.setType("MLA"); // Set the type as "MLA"
				dto.setNodalDistrict(mla.getConstituency().getDistrict().getDistrictName());
				dto.setAssemblyConstituency(mla.getConstituency().getConstituencyName());
				// Populate other fields like budgetedAmount, fundsDisbursed, balanceFunds,
				// pendingDemandAmount
				// Assuming these values are in MLABudget or related entities
				MLABudget mlaBudget = getMLABudget(mla.getId(), request.getFinancialYearId());
				if (mlaBudget != null) {
					dto.setBudgetedAmount(mlaBudget.getAllocatedLimit());
					dto.setFundsDisbursed(mlaBudget.getUtilizedLimit());
					dto.setBalanceFunds(mlaBudget.getRemainingLimit());
				}
				Optional<MLATerm> currentTermOpt = mlaTermRepository.findFirstByMla_IdAndActiveTrue(mla.getId());
				if (currentTermOpt.isPresent()) {
					dto.setTerm(currentTermOpt.get().getTermNumber());
				} else {
					dto.setTerm(0); // or null if you change DTO type
				}
				responseList.add(dto);
			} else {
				throw new RuntimeException("MLA not found for ID: " + request.getMlaId());
			}
		} else if (request.getMlcId() != null) {
			Optional<MLC> mlcOpt = mLCRepository.findMLCById(request.getMlcId());
			if (mlcOpt.isPresent()) {
				MLC mlc = mlcOpt.get();
				AdminMLDashboardDto dto = new AdminMLDashboardDto();
				// Populate the DTO fields
				dto.setMlaMlcName(mlc.getMlcName());
				dto.setType("MLC"); // Set the type as "MLC"
				dto.setNodalDistrict(null);
				dto.setAssemblyConstituency(null);
				// Populate other fields like budgetedAmount, fundsDisbursed, balanceFunds,
				// pendingDemandAmount
				MLCBudget mlcBudget = getMLCBudget(mlc.getId(), request.getFinancialYearId());
				if (mlcBudget != null) {
					dto.setBudgetedAmount(mlcBudget.getAllocatedLimit());
					dto.setFundsDisbursed(mlcBudget.getUtilizedLimit());
					dto.setBalanceFunds(mlcBudget.getRemainingLimit());
				}
				responseList.add(dto);
			} else {
				throw new RuntimeException("MLC not found for ID: " + request.getMlcId());
			}
		} else if (request.getDistrictId() != null) {
			// Fetch Constituencies by district
			List<Constituency> constituencies = constituencyRepository.findByDistrict_Id(request.getDistrictId());
			if (constituencies.isEmpty()) {
				throw new RuntimeException("No constituencies found for district ID: " + request.getDistrictId());
			}

			// For each constituency, get the MLA and MLC data
			for (Constituency constituency : constituencies) {
				List<MLA> mlas = mLARepository.findByConstituency_Id(constituency.getId());
				for (MLA mla : mlas) {
					AdminMLDashboardDto dto = new AdminMLDashboardDto();
					// Populate the DTO fields
					dto.setMlaMlcName(mla.getMlaName());
					dto.setType("MLA");
					dto.setNodalDistrict(mla.getConstituency().getDistrict().getDistrictName());
					dto.setAssemblyConstituency(mla.getConstituency().getConstituencyName());
					// Set more fields like budgetedAmount, fundsDisbursed, balanceFunds,
					// pendingDemandAmount
					MLABudget mlaBudget = getMLABudget(mla.getId(), request.getFinancialYearId());
					if (mlaBudget != null) {
						dto.setBudgetedAmount(mlaBudget.getAllocatedLimit());
						dto.setFundsDisbursed(mlaBudget.getUtilizedLimit());
						dto.setBalanceFunds(mlaBudget.getRemainingLimit());
					}
					Optional<MLATerm> currentTermOpt = mlaTermRepository.findFirstByMla_IdAndActiveTrue(mla.getId());
					if (currentTermOpt.isPresent()) {
						dto.setTerm(currentTermOpt.get().getTermNumber());
					} else {
						dto.setTerm(0);
					}
					responseList.add(dto);
				}

			}
		} else {
			// Fetch all MLAs or MLCs if no specific filter is provided
			List<MLA> allMlas = mLARepository.findAll();
			for (MLA mla : allMlas) {
				AdminMLDashboardDto dto = new AdminMLDashboardDto();
				// Populate the DTO fields
				dto.setMlaMlcName(mla.getMlaName());
				dto.setType("MLA");
				dto.setNodalDistrict(mla.getConstituency().getDistrict().getDistrictName());
				dto.setAssemblyConstituency(mla.getConstituency().getConstituencyName());
				// Set more fields like budgetedAmount, fundsDisbursed, balanceFunds,
				// pendingDemandAmount
				MLABudget mlaBudget = getMLABudget(mla.getId(), request.getFinancialYearId());
				if (mlaBudget != null) {
					dto.setBudgetedAmount(mlaBudget.getAllocatedLimit());
					dto.setFundsDisbursed(mlaBudget.getUtilizedLimit());
					dto.setBalanceFunds(mlaBudget.getRemainingLimit());
				}
				Optional<MLATerm> currentTermOpt = mlaTermRepository.findFirstByMla_IdAndActiveTrue(mla.getId());
				if (currentTermOpt.isPresent()) {
					dto.setTerm(currentTermOpt.get().getTermNumber());
				} else {
					dto.setTerm(0);
				}
				responseList.add(dto);
			}
		}

		return responseList;
	}

	public MLABudget getMLABudget(Long mlaId, Long financialYearId) {
		// Fetch the MLA by its ID
		Optional<MLA> mlaOpt = mLARepository.findMLAById(mlaId);
		if (mlaOpt.isEmpty()) {
			throw new RuntimeException("MLA not found for ID: " + mlaId);
		}

		// Retrieve the MLABudget for this MLA for a specific financial year
		MLABudget mlaBudget = mLABudgetRepository.findByMlaIdAndFinancialYearId(financialYearId, mlaId);
		if (mlaBudget == null) {
			throw new RuntimeException(
					"MLA Budget not found for MLA ID: " + mlaId + " and Financial Year ID: " + financialYearId);
		}

		return mlaBudget;
	}

	public MLCBudget getMLCBudget(Long mlcId, Long financialYearId) {
		// Fetch the MLC by its ID
		Optional<MLC> mlcOpt = mLCRepository.findMLCById(mlcId);
		if (mlcOpt.isEmpty()) {
			throw new RuntimeException("MLC not found for ID: " + mlcId);
		}

		// Retrieve the MLCBudget for this MLC for a specific financial year
		MLCBudget mlcBudget = mLCBudgetRepository.findByMlcIdAndFinancialYearId(financialYearId, mlcId);
		if (mlcBudget == null) {
			throw new RuntimeException(
					"MLC Budget not found for MLC ID: " + mlcId + " and Financial Year ID: " + financialYearId);
		}

		return mlcBudget;
	}

	@Override
	public AdminDAPBudgetRespDto getAdminMLBudget(String username, Long financialYear_Id) {
		// Fetch the user based on the username
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new RuntimeException("User not found: " + username));

		Roles role = user.getRole();
		// Check if the user has the STATE_ADMIN, STATE_CHECKER, or STATE_MAKER role
		if (!(role == Roles.STATE_ADMIN || role == Roles.STATE_CHECKER || role == Roles.STATE_MAKER)) {
			throw new RuntimeException("Unauthorized user: " + username);
		}

		// Fetch the financial year budget for the given PlanType and financialYearId
		FinancialYearBudget financialYearBudget = financialYearBudgetRepository
				.findByPlanTypeAndFinancialYear_Id(PlanType.MLA, financialYear_Id);

		// Check if the result is null
		if (financialYearBudget == null) {
			throw new RuntimeException("Financial year budget not found for ID: " + financialYear_Id);
		}
		Double pendingDemand = 0.0;
		List<Schemes> schemes = schemesRepository.findByPlanType(PlanType.MLA);

		// Loop through each Scheme
		for (Schemes scheme : schemes) {
			// Fetch all works associated with the current scheme
			List<Work> works = workRepository.findByScheme_Id(scheme.getId());

			// Loop through each Work
			for (Work work : works) {
				// Fetch all FundDemand records related to the current Work
				List<FundDemand> fundDemands = fundDemandRepository.findByWork_Id(work.getId());

				// Loop through each FundDemand and check for the pending status
				for (FundDemand fundDemand : fundDemands) {
					// If the FundDemand status is pending, add its amount to the pending demand
					if (fundDemand.getStatus() == DemandStatuses.PENDING
							&& fundDemand.getFinancialYear().getId() == financialYear_Id) {
						pendingDemand += fundDemand.getAmount();
					}
				}
			}
		}

		// Logic to convert the financialYearBudget to AdminDAPBudgetRespDto
		AdminDAPBudgetRespDto responseDto = new AdminDAPBudgetRespDto();
		responseDto.setTotalBudget(financialYearBudget.getAllocatedLimit());
		responseDto.setFundsDistributed(financialYearBudget.getUtilizedLimit());
		responseDto
				.setRemainingBalance(financialYearBudget.getAllocatedLimit() - financialYearBudget.getUtilizedLimit());
		responseDto.setPendingDemandAmount(pendingDemand);
		// If you want to return a list, create a list with one response object
		return responseDto;
	}

	@Override
	public AdminDAPBudgetRespDto getAdminDistrictMLBudget(String username, Long financialYear_Id) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new RuntimeException("User not found: " + username));

		Roles role = user.getRole();
		// Check if the user has the STATE_ADMIN, STATE_CHECKER, or STATE_MAKER role
		if (!(role == Roles.STATE_ADMIN || role == Roles.STATE_CHECKER || role == Roles.STATE_MAKER)) {
			throw new RuntimeException("Unauthorized user: " + username);
		}

		// Fetch the financial year budget for the given PlanType and financialYearId
		DistrictLimitAllocation allocation = districtLimitAllocationRepository
				.findByDistrict_IdAndFinancialYear_IdAndPlanType(user.getDistrict().getId(), financialYear_Id,
						PlanType.MLA)
				.orElseThrow(() -> new RuntimeException("Financial year budget not found for ID: " + financialYear_Id));
		// Check if the result is null

		Double pendingDemand = 0.0;
		List<Schemes> schemes = schemesRepository.findByDistricts_IdAndPlanType(user.getDistrict().getId(),
				PlanType.MLA);

		// Loop through each Scheme
		for (Schemes scheme : schemes) {
			// Fetch all works associated with the current scheme
			List<Work> works = workRepository.findByScheme_Id(scheme.getId());

			// Loop through each Work
			for (Work work : works) {
				// Fetch all FundDemand records related to the current Work
				List<FundDemand> fundDemands = fundDemandRepository.findByWork_Id(work.getId());

				// Loop through each FundDemand and check for the pending status
				for (FundDemand fundDemand : fundDemands) {
					// If the FundDemand status is pending, add its amount to the pending demand
					if (fundDemand.getStatus() == DemandStatuses.PENDING
							&& fundDemand.getFinancialYear().getId() == financialYear_Id) {
						pendingDemand += fundDemand.getAmount();
					}
				}
			}
		}

		// Logic to convert the financialYearBudget to AdminDAPBudgetRespDto
		AdminDAPBudgetRespDto responseDto = new AdminDAPBudgetRespDto();
		responseDto.setTotalBudget(allocation.getAllocatedLimit());
		responseDto.setFundsDistributed(allocation.getUtilizedLimit());
		responseDto.setRemainingBalance(allocation.getAllocatedLimit() - allocation.getUtilizedLimit());
		responseDto.setPendingDemandAmount(pendingDemand);
		// If you want to return a list, create a list with one response object
		return responseDto;

	}
}
