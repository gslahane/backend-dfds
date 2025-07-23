package com.lsit.dfds.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lsit.dfds.entity.Sector;

@Repository
public interface SectorRepository extends JpaRepository<Sector, Long> {
}
