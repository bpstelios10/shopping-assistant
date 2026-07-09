package org.learnings.ai.shoppingassistant.controllers;


import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ControllerExceptionHandlerTest {

    private final ControllerExceptionHandler controllerExceptionHandler = new ControllerExceptionHandler();

    @Test
    void handleExceptions() {
        Exception someException = new RuntimeException("test exception thrown");

        ResponseEntity<ControllerExceptionHandler.ErrorResponse> actualResponse =
                controllerExceptionHandler.handleExceptions(someException);

        assertThat(actualResponse.getStatusCode().value()).isEqualTo(500);
        ControllerExceptionHandler.ErrorResponse body = actualResponse.getBody();
        assertThat(body).isNotNull();
        assertThat(body.errorCode()).isEqualTo("UNHANDLED_EXCEPTION");
        assertThat(body.errorMessage()).isEqualTo("An unexpected error occurred. Please try again later.");
    }

    @Test
    void handleMethodArgumentNotValid_whenFieldError_returnsCustomErrorResponse() {
        BindingResult bindingResult = new BeanPropertyBindingResult(new DummyObject("", " "), "createChat");
        bindingResult.rejectValue("message", "NotBlank", "must not be blank");
        bindingResult.rejectValue("message2", "NotBlank", "must not be blank");
        Method method = mock(Method.class);
        MethodParameter methodParameter = new MethodParameter(method, -1, 0);

        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(methodParameter, bindingResult);

        ResponseEntity<Object> response = controllerExceptionHandler.handleMethodArgumentNotValid(
                ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, mock(org.springframework.web.context.request.WebRequest.class));

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ControllerExceptionHandler.ErrorResponse body =
                (ControllerExceptionHandler.ErrorResponse) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.errorCode()).isEqualTo("INVALID_USER_INPUT");
        assertThat(body.errorMessage()).isEqualTo("message: must not be blank, message2: must not be blank");
    }

    record DummyObject(String message, String message2) {
    }

    ;
}
