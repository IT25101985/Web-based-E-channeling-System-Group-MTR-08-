package com.echanneling.service;

import com.echanneling.entity.Feedback;
import org.springframework.stereotype.Service;

@Service
public class RatingService {
    
    /**
     * Beginner-friendly completion workflow.
     */
    public void submitFeedback(Feedback feedback) {
        System.out.println("Submitting rating of " + feedback.getRating() + " for Doctor " + feedback.getDoctor().getName());
        // Save feedback to repository logic here
    }
}
