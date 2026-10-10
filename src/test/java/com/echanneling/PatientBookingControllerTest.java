package com.echanneling;

import com.echanneling.controller.PatientBookingController;
import com.echanneling.entity.Appointment;
import com.echanneling.entity.User;
import com.echanneling.service.BookingService;
import com.echanneling.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import java.security.Principal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientBookingControllerTest {
    @Mock UserService users;
    @Mock BookingService bookings;
    @Mock BindingResult binding;
    @InjectMocks PatientBookingController controller;
    Appointment appointment;
    User patient;
    Principal principal = () -> "patient";
    RedirectAttributesModelMap flash;

    @BeforeEach void setUp() {
        appointment = new Appointment();
        patient = new User();
        flash = new RedirectAttributesModelMap();
    }
    void signedIn() {
        when(users.findByUsername("patient")).thenReturn(Optional.of(patient));
    }
    @Test void invalidRescheduleRetainsAppointmentId() {
        appointment.setId(42L);
        when(binding.hasErrors()).thenReturn(true);
        assertEquals("redirect:/patient/dashboard?section=booking&reschedule=42",
                controller.saveAppointment(appointment, binding, principal, flash));
        assertEquals("Please check your appointment details.", flash.getFlashAttributes().get("errorMessage"));
        verifyNoInteractions(bookings);
    }
    @Test void unavailableRescheduleRetainsAppointmentIdAndValidationMessage() {
        signedIn();
        appointment.setId(42L);
        when(bookings.book(patient, appointment)).thenThrow(new IllegalArgumentException("That time is unavailable."));
        assertEquals("redirect:/patient/dashboard?section=booking&reschedule=42",
                controller.saveAppointment(appointment, binding, principal, flash));
        assertEquals("That time is unavailable.", flash.getFlashAttributes().get("errorMessage"));
    }
    @Test void successfulRescheduleHasDistinctConfirmation() {
        signedIn();
        appointment.setId(42L);
        assertEquals("redirect:/patient/dashboard?section=history",
                controller.saveAppointment(appointment, binding, principal, flash));
        assertEquals("Your appointment was rescheduled successfully.", flash.getFlashAttributes().get("successMessage"));
        verify(bookings).book(patient, appointment);
    }
    @Test void newBookingKeepsBookingConfirmation() {
        signedIn();
        controller.saveAppointment(appointment, binding, principal, flash);
        assertEquals("Your appointment is confirmed.", flash.getFlashAttributes().get("successMessage"));
    }
    @Test void unavailableOrForbiddenAppointmentGetsSafeFeedback() {
        signedIn();
        appointment.setId(42L);
        when(bookings.book(patient, appointment)).thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));
        assertEquals("redirect:/patient/dashboard?section=booking&reschedule=42",
                controller.saveAppointment(appointment, binding, principal, flash));
        assertEquals("The appointment could not be updated. Check your appointment and try again.",
                flash.getFlashAttributes().get("errorMessage"));
    }
    @Test void unexpectedFailureDoesNotExposeInternalMessage() {
        signedIn();
        when(bookings.book(patient, appointment)).thenThrow(new IllegalStateException("private database details"));
        assertEquals("redirect:/patient/dashboard?section=booking",
                controller.saveAppointment(appointment, binding, principal, flash));
        assertEquals("Could not save your appointment. Please try again.", flash.getFlashAttributes().get("errorMessage"));
    }
    @Test void anonymousUserReturnsToLoginWithoutSaving() {
        assertEquals("redirect:/login", controller.saveAppointment(appointment, binding, null, flash));
        verifyNoInteractions(bookings, users);
    }
}
