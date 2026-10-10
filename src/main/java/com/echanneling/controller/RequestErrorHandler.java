package com.echanneling.controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.http.HttpStatus;
import org.slf4j.LoggerFactory;
@ControllerAdvice
public class RequestErrorHandler {
    @ExceptionHandler({IllegalArgumentException.class, jakarta.validation.ConstraintViolationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String invalidInput(Exception error, Model model) {
        model.addAttribute("friendlyMessage", "Some details are invalid. Go back, check the required fields and try again.");
        return "error";
    }
    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public String databaseFailure(Exception error, Model model) {
        LoggerFactory.getLogger(getClass()).error("Database operation failed", error);
        model.addAttribute("friendlyMessage", "We could not load or save your data. Please try again, or contact the hospital administrator.");
        return "error";
    }
}
