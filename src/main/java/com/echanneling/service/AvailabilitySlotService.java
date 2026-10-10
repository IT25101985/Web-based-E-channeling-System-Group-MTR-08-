package com.echanneling.service;

import com.echanneling.entity.AvailabilitySlot;
import com.echanneling.entity.Doctor;
import com.echanneling.repository.AvailabilitySlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class AvailabilitySlotService {

    @Autowired
    private AvailabilitySlotRepository slotRepository;

    public AvailabilitySlot save(AvailabilitySlot slot) {
        return slotRepository.save(slot);
    }

    public List<AvailabilitySlot> getSlotsByDoctor(Doctor doctor) {
        return slotRepository.findByDoctor(doctor);
    }

    public List<AvailabilitySlot> getAvailableSlotsByDate(LocalDate date) {
        return slotRepository.findBySlotDateAndIsActive(date, true);
    }

    /**
     * Atomic booking with double-booking prevention (synchronized)
     * Returns true if booking succeeded, false if slot is full
     */
    @Transactional
    public synchronized boolean bookSlot(Long slotId) {
        Optional<AvailabilitySlot> opt = slotRepository.findById(slotId);
        if (opt.isPresent()) {
            AvailabilitySlot slot = opt.get();
            if (slot.getBookedCount() < slot.getMaxPatients()) {
                slot.setBookedCount(slot.getBookedCount() + 1);
                if (slot.getBookedCount().equals(slot.getMaxPatients())) {
                    slot.setIsActive(false); // Mark full
                }
                slotRepository.save(slot);
                return true; // Booking successful
            }
        }
        return false; // Slot full - double-booking prevented
    }

    public Optional<AvailabilitySlot> getById(Long id) {
        return slotRepository.findById(id);
    }

    public void delete(Long id) {
        slotRepository.deleteById(id);
    }
}
