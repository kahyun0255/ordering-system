package com.orderingsystem.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(value = {Exception.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public ErrorDTO handleException(Exception exception) {
        log.error(exception.getMessage(), exception);
        return ErrorDTO.builder()
                .code(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message("Unexpected error") //보안상의 이유로 내부 메시지는 공유하지 않는게 좋음. 또한 이미 메시지를 기록했기에 로그 보기
                .build();
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ErrorDTO handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.error("유니크 키 중복 오류 발생", e);

        return ErrorDTO.builder()
                .code(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message("Unexpected error")
                .build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorDTO> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error(e.getMessage());

        return ResponseEntity.badRequest()
                .body(ErrorDTO.builder()
                        .code(HttpStatus.BAD_REQUEST.getReasonPhrase())
                        .message(e.getMessage())
                        .build());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorDTO> handleNotFoundException(NotFoundException e) {
        log.error(e.getMessage());

        return ResponseEntity.badRequest()
                .body(ErrorDTO.builder()
                        .code(HttpStatus.NOT_FOUND.getReasonPhrase())
                        .message(e.getMessage())
                        .build());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ErrorDTO> handleDuplicateKeyException(DuplicateKeyException e) {
        log.error(e.getMessage());

        return ResponseEntity.badRequest()
                .body(ErrorDTO.builder()
                        .code(HttpStatus.CONFLICT.getReasonPhrase())
                        .message(e.getMessage())
                        .build());
    }

}
