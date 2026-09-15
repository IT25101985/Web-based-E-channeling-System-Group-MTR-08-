package com.echanneling.service;

import com.echanneling.entity.HospitalBranch;
import com.echanneling.repository.HospitalBranchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class HospitalBranchService {

    @Autowired
    private HospitalBranchRepository hospitalBranchRepository;

    public HospitalBranch save(HospitalBranch branch) {
        return hospitalBranchRepository.save(branch);
    }

    public List<HospitalBranch> getAllBranches() {
        return hospitalBranchRepository.findAll();
    }

    public Optional<HospitalBranch> getBranchById(Long id) {
        return hospitalBranchRepository.findById(id);
    }

    public void deleteBranch(Long id) {
        hospitalBranchRepository.deleteById(id);
    }

    public List<HospitalBranch> searchByArea(String area) {
        return hospitalBranchRepository.findByAreaContainingIgnoreCase(area);
    }
}
