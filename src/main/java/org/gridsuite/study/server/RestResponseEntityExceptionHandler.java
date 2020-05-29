package org.gridsuite.study.server;

import static org.gridsuite.study.server.StudyConstants.*;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ServerWebExchange;

@ControllerAdvice
@RequiredArgsConstructor
public class RestResponseEntityExceptionHandler {

    @ExceptionHandler(value = {StudyException.class})
    protected ResponseEntity<Object> handleException(RuntimeException ex, ServerWebExchange exchange) {
        String errorMessage = ex.getMessage();
        if (errorMessage.equals(STUDY_DOESNT_EXISTS)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(STUDY_DOESNT_EXISTS);
        } else if (errorMessage.equals(CASE_DOESNT_EXISTS)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(CASE_DOESNT_EXISTS);
        } else if (errorMessage.equals(STUDY_ALREADY_EXISTS)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(STUDY_ALREADY_EXISTS);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}
