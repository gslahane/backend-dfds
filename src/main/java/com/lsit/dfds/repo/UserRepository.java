package com.lsit.dfds.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lsit.dfds.entity.User;
import com.lsit.dfds.enums.Roles;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByUsername(String username);

	List<User> findByRole(Roles viewerRole);

	List<User> findBySupervisor_IdAndRole(Long id, Roles iaAdmin);

	List<User> findByDistrict_IdAndRole(Long districtId, Roles role);

	@Query("""
			select u from User u
			left join u.district d
			where (:districtId is null or d.id = :districtId)
			and (:role is null or u.role = :role)
			and (:search is null or
			    lower(u.username) like lower(concat('%', :search, '%')) or
			    lower(u.fullname) like lower(concat('%', :search, '%')) or
			    lower(u.designation) like lower(concat('%', :search, '%')))
			""")
	List<User> findUsersByFilter(@Param("districtId") Long districtId, @Param("role") Roles role,
			@Param("search") String search);

}
