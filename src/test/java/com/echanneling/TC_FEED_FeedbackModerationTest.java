package com.echanneling;

import com.echanneling.entity.*;
import com.echanneling.repository.*;
import com.echanneling.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** FR-05: Feedback & Moderation | TC_FEED_001-010 | Metaramba M.K.K.N. IT25101983 */
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:tc_feed;MODE=MSSQLServer;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE","spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=","spring.jpa.database-platform=org.hibernate.dialect.H2Dialect","spring.jpa.hibernate.ddl-auto=create-drop","spring.jpa.show-sql=false","app.seed-demo=true","app.notifications.scheduling-enabled=false","app.mail.enabled=false"})
@ActiveProfiles("test") @AutoConfigureMockMvc @Transactional @TestMethodOrder(MethodOrderer.DisplayName.class)
class TC_FEED_FeedbackModerationTest {
    @Autowired MockMvc mvc;
    @Autowired FeedbackService feedbackService;
    @Autowired FeedbackRepository feedbackRepository;
    @Autowired UserRepository userRepository;
    @Autowired DoctorRepository doctorRepository;
    @MockBean JavaMailSender mail;

    private Feedback buildFeedback(int rating, String comments) {
        Feedback f = new Feedback();
        f.setPatient(userRepository.findByUsername("dhanu").orElseThrow());
        f.setDoctor(doctorRepository.findAll().get(0));
        f.setRating(rating); f.setComments(comments); f.setModerationStatus("PENDING");
        return f;
    }

    /** TC_FEED_001 - Submit Post-Consultation Rating (5 Stars) */
    @Test @DisplayName("TC_FEED_001 - Submit Post-Consultation Rating (5 Stars)")
    void TC_FEED_001_submitPostConsultationRating() {
        Feedback f = feedbackService.saveFeedback(buildFeedback(5, "Great doctor!"));
        assertNotNull(f.getId()); assertEquals("PENDING", f.getModerationStatus()); assertEquals(5, f.getRating());
    }

    /** TC_FEED_002 - Feedback Submission Recorded Per Patient */
    @Test @DisplayName("TC_FEED_002 - Feedback Submission Recorded Per Patient")
    void TC_FEED_002_feedbackRecordedPerPatient() {
        feedbackService.saveFeedback(buildFeedback(4, "Good service"));
        assertFalse(feedbackService.getFeedbackByPatient(
            userRepository.findByUsername("dhanu").orElseThrow()).isEmpty());
    }

    /** TC_FEED_003 - Admin Approve Feedback */
    @Test @DisplayName("TC_FEED_003 - Admin Approve Feedback")
    void TC_FEED_003_adminApproveFeedback() {
        Feedback saved = feedbackService.saveFeedback(buildFeedback(4, "Excellent consultation"));
        feedbackService.approveFeedback(saved.getId());
        assertEquals("APPROVED", feedbackRepository.findById(saved.getId()).orElseThrow().getModerationStatus());
    }

    /** TC_FEED_004 - Admin Reject Feedback */
    @Test @DisplayName("TC_FEED_004 - Admin Reject Feedback")
    void TC_FEED_004_adminRejectFeedback() {
        Feedback saved = feedbackService.saveFeedback(buildFeedback(1, "Abusive content"));
        feedbackService.rejectFeedback(saved.getId());
        assertEquals("REJECTED", feedbackRepository.findById(saved.getId()).orElseThrow().getModerationStatus());
    }

    /** TC_FEED_005 - Doctor Average Rating Calculation (4.25 with seeded rating 5) */
    @Test @DisplayName("TC_FEED_005 - Doctor Average Rating Calculation")
    void TC_FEED_005_doctorAverageRatingCalculation() {
        Doctor doctor = doctorRepository.findAll().get(0);
        User patient = userRepository.findByUsername("dhanu").orElseThrow();
        for (int r : new int[]{5, 4, 3}) {
            Feedback f = new Feedback(); f.setPatient(patient); f.setDoctor(doctor);
            f.setRating(r); f.setModerationStatus("APPROVED"); feedbackRepository.save(f);
        }
        double avg = feedbackService.getFeedbackByDoctor(doctor).stream()
            .filter(f -> "APPROVED".equals(f.getModerationStatus()))
            .mapToInt(Feedback::getRating).average().orElse(0);
        assertEquals(4.25, avg, 0.01);
    }

    /** TC_FEED_006 - Review Comment Stored and Retrievable */
    @Test @DisplayName("TC_FEED_006 - Review Comment Stored and Retrievable")
    void TC_FEED_006_reviewCommentStoredAndRetrievable() {
        String longComment = "A".repeat(200);
        Feedback f = feedbackService.saveFeedback(buildFeedback(3, longComment));
        assertEquals(longComment, feedbackRepository.findById(f.getId()).orElseThrow().getComments());
    }

    /** TC_FEED_007 - Public View Shows Only Approved Feedbacks */
    @Test @DisplayName("TC_FEED_007 - Public View Shows Only Approved Feedbacks")
    void TC_FEED_007_publicViewShowsOnlyApprovedFeedbacks() {
        feedbackService.saveFeedback(buildFeedback(2, "Pending review"));
        Feedback f2 = feedbackService.saveFeedback(buildFeedback(5, "Great!"));
        feedbackService.approveFeedback(f2.getId());
        List<Feedback> publicList = feedbackService.getApprovedFeedback();
        assertTrue(publicList.stream().allMatch(f -> "APPROVED".equals(f.getModerationStatus())));
    }

    /** TC_FEED_008 - Feedback Retrievable by Doctor */
    @Test @DisplayName("TC_FEED_008 - Feedback Retrievable by Doctor")
    void TC_FEED_008_feedbackRetrievableByDoctor() {
        feedbackService.saveFeedback(buildFeedback(4, "Good doctor"));
        assertFalse(feedbackService.getFeedbackByDoctor(doctorRepository.findAll().get(0)).isEmpty());
    }

    /** TC_FEED_009 - Admin Permanently Deletes Feedback */
    @Test @DisplayName("TC_FEED_009 - Admin Permanently Deletes Feedback")
    void TC_FEED_009_adminPermanentlyDeletesFeedback() {
        Feedback saved = feedbackService.saveFeedback(buildFeedback(1, "Inappropriate content"));
        Long id = saved.getId();
        feedbackService.deleteFeedback(id);
        assertFalse(feedbackRepository.existsById(id));
    }

    /** TC_FEED_010 - Patient Cannot Access Admin Feedback Panel */
    @Test @DisplayName("TC_FEED_010 - Patient Cannot Access Admin Feedback Panel")
    void TC_FEED_010_patientCannotAccessAdminFeedbackPanel() throws Exception {
        mvc.perform(get("/admin/feedback").with(user("dhanu").roles("PATIENT")))
            .andExpect(status().isForbidden());
    }
}
