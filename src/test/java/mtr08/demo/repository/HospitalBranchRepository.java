package com.echanneling.repository;

import com.echanneling.entity.HospitalBranch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface HospitalBranchRepository extends JpaRepository<HospitalBranch, Long> {
    List<HospitalBranch> findByAreaContainingIgnoreCase(String area);
    List<HospitalBranch> findByBranchNameContainingIgnoreCase(String name);
}
