package com.dems.common.exception;

import com.dems.common.response.ApiResponse;
import com.dems.common.response.ErrorDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @Mock
    private BindingResult bindingResult;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/test-endpoint");
        request.setRemoteAddr("127.0.0.1");
    }

    @Test
    @DisplayName("ResourceNotFoundException returns 404 with correct details")
    void handleResourceNotFoundException() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User", "abc-123");

        ResponseEntity<ApiResponse<Void>> response = handler.handleResourceNotFoundException(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals(404, response.getBody().getStatus());
        ErrorDetails error = response.getBody().getError();
        assertNotNull(error);
        assertEquals("RESOURCE_NOT_FOUND", error.getCode());
        assertEquals("/api/test-endpoint", error.getPath());
    }

    @Test
    @DisplayName("ApiException returns correct status")
    void handleApiException() {
        ApiException ex = new ApiException("Something went wrong", HttpStatus.I_AM_A_TEAPOT, "TEAPOT");

        ResponseEntity<ApiResponse<Void>> response = handler.handleApiException(ex, request);

        assertEquals(HttpStatus.I_AM_A_TEAPOT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("TEAPOT", response.getBody().getError().getCode());
    }

    @Test
    @DisplayName("MethodArgumentNotValidException returns 400 with details")
    void handleValidationException() {
        FieldError fieldError = new FieldError("object", "username", "must not be blank");
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<ApiResponse<Void>> response = handler.handleValidationException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("VALIDATION_ERROR", response.getBody().getError().getCode());
        assertNotNull(response.getBody().getError().getDetails());
        assertFalse(response.getBody().getError().getDetails().isEmpty());
    }

    @Test
    @DisplayName("BadCredentialsException returns 401")
    void handleBadCredentialsException() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");

        ResponseEntity<ApiResponse<Void>> response = handler.handleBadCredentialsException(ex, request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("INVALID_CREDENTIALS", response.getBody().getError().getCode());
    }

    @Test
    @DisplayName("AccessDeniedException returns 403")
    void handleAccessDeniedException() {
        AccessDeniedException ex = new AccessDeniedException("Forbidden");

        ResponseEntity<ApiResponse<Void>> response = handler.handleAccessDeniedException(ex, request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("ACCESS_DENIED", response.getBody().getError().getCode());
    }

    @Test
    @DisplayName("Generic Exception returns 500")
    void handleGenericException() {
        RuntimeException ex = new RuntimeException("Unexpected boom");

        ResponseEntity<ApiResponse<Void>> response = handler.handleGenericException(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("INTERNAL_SERVER_ERROR", response.getBody().getError().getCode());
    }

    @Test
    @DisplayName("IllegalArgumentException returns 400")
    void handleIllegalArgumentException() {
        IllegalArgumentException ex = new IllegalArgumentException("Bad argument");

        ResponseEntity<ApiResponse<Void>> response = handler.handleIllegalArgumentException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("INVALID_ARGUMENT", response.getBody().getError().getCode());
    }
}
