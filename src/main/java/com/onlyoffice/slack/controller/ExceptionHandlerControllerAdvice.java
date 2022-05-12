package com.onlyoffice.slack.controller;

import com.onlyoffice.slack.exception.OnlyofficeRegistryResponseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
@Slf4j
public class ExceptionHandlerControllerAdvice extends ResponseEntityExceptionHandler {
    @ExceptionHandler(value = IllegalAccessException.class)
    public void ResilienceAccessExceptionHandler(IllegalAccessException e) {
        log.debug("resilience access exception: {}", e.getMessage());
    }

    @ExceptionHandler(value = OnlyofficeRegistryResponseException.class)
    public void OnlyofficeRegistryResponseExceptionHandler(OnlyofficeRegistryResponseException e) {
        log.error(e.getMessage());
    }
}
