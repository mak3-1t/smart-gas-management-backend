package com.gasmanagement.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Ném ra khi vi phạm business rule.
 * Tương ứng HTTP 422 Unprocessable Entity.
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
