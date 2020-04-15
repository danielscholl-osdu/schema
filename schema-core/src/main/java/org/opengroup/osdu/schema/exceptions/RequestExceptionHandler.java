package org.opengroup.osdu.schema.exceptions;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolationException;

import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.errors.Error;
import org.opengroup.osdu.schema.errors.model.AuthorizationError;
import org.opengroup.osdu.schema.errors.model.BadRequestError;
import org.opengroup.osdu.schema.errors.model.InternalServerError;
import org.opengroup.osdu.schema.errors.model.MediaTypeError;
import org.opengroup.osdu.schema.errors.model.NotFoundError;
import org.opengroup.osdu.schema.errors.model.UnauthorizedError;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import lombok.extern.java.Log;

@Log
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestExceptionHandler extends ResponseEntityExceptionHandler {
    private static final String CORRELATION_ID = "correlation-id";

    // Triggered when a 'required' request parameter is missing.
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
            HttpHeaders headers, HttpStatus status, WebRequest request) {
        String errorMessage = ex.getParameterName() + " parameter is missing";
        Error error = new Error(HttpStatus.BAD_REQUEST);
        error.setCode(400);
        error.setMessage(errorMessage);
        error.addErrors(new BadRequestError(errorMessage));

        errorMessage = errorMessage + getCorrelationId(request);
        log.log(Level.SEVERE, errorMessage);
        log.log(Level.INFO, errorMessage, ex);
        return buildResponseEntity(error);
    }

    // Triggered when a 'required' request header is missing.
    @Override
    protected ResponseEntity<Object> handleServletRequestBindingException(ServletRequestBindingException ex,
            HttpHeaders headers, HttpStatus status, WebRequest request) {
        // todo: find a better way to extract which header is missing
        String missingHeader = extractMissingHeaderName(ex.getMessage());
        String errorMessage = "Missing " + missingHeader + " header";
        Error error = new Error(HttpStatus.BAD_REQUEST);
        error.setCode(400);
        error.setMessage(errorMessage);
        error.addErrors(new BadRequestError(errorMessage));

        errorMessage = errorMessage + getCorrelationId(request);
        log.log(Level.SEVERE, errorMessage);
        log.log(Level.INFO, errorMessage, ex);
        return buildResponseEntity(error);
    }

    // triggered when unsupported media type is passed or passed JSON is
    // invalid.
    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
            HttpHeaders headers, HttpStatus status, WebRequest request) {
        StringBuilder builder = new StringBuilder();
        builder.append(ex.getContentType());
        builder.append(" media type is not supported.");

        String errorMessage = builder.toString();
        Error error = new Error(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        error.setCode(415);
        error.setMessage(errorMessage);
        error.addErrors(new MediaTypeError(errorMessage));

        errorMessage = errorMessage + getCorrelationId(request);
        log.log(Level.SEVERE, errorMessage);
        log.log(Level.INFO, errorMessage, ex);
        return buildResponseEntity(error);
    }

    // Triggered when an object fails validation.
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatus status, WebRequest request) {
        String errorMessage = "Parameter validation error :" + ex.getBindingResult().getFieldErrors().toString();
        Error error = new Error(HttpStatus.BAD_REQUEST);
        error.setCode(400);
        error.setMessage("Validation Error");
        error.addValidationErrors(ex.getBindingResult().getFieldErrors());

        errorMessage = errorMessage + getCorrelationId(request);
        log.log(Level.SEVERE, errorMessage);
        log.log(Level.INFO, errorMessage, ex);
        return buildResponseEntity(error);
    }

    // Triggered when no handler is found.
    // will work only if (spring.mvc.throw-exception-if-no-handler-found=true)
    // is set in service's application.properties
    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(NoHandlerFoundException ex, HttpHeaders headers,
            HttpStatus status, WebRequest request) {
        String errorMessage = "Resource not found";
        Error error = new Error(HttpStatus.NOT_FOUND);
        error.setCode(404);
        error.setMessage(errorMessage);
        error.addErrors(new NotFoundError(errorMessage));

        errorMessage = errorMessage + getCorrelationId(request);
        log.log(Level.SEVERE, errorMessage);
        log.log(Level.INFO, errorMessage, ex);
        return buildResponseEntity(error);
    }

    // Triggered when a response message not available in case of error/exception.
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatus status, WebRequest request) {
        String errorMessage = "Bad Request. Invalid Input.";
        Error error = new Error(HttpStatus.BAD_REQUEST);
        error.setCode(400);
        error.setMessage(errorMessage);
        error.addErrors(new BadRequestError(errorMessage));

        errorMessage = errorMessage + getCorrelationId(request);
        log.log(Level.SEVERE, errorMessage);
        log.log(Level.INFO, errorMessage, ex);
        return buildResponseEntity(error);
    }

    @ExceptionHandler(ApplicationException.class)
    protected ResponseEntity<Object> handleApplicationException(ApplicationException ex, WebRequest request) {
        String errorMessage = ex.getErrorMsg();
        Error error = new Error(HttpStatus.INTERNAL_SERVER_ERROR);
        error.setCode(500);
        error.setMessage(ex.getErrorMsg());
        error.addErrors(new InternalServerError(errorMessage));

        errorMessage = errorMessage + getCorrelationId(request);
        log.log(Level.SEVERE, errorMessage);
        log.log(Level.INFO, errorMessage, ex);
        return buildResponseEntity(error);
    }

    // Triggered when a runtime exception is thrown
    @ExceptionHandler(RuntimeException.class)
    protected ResponseEntity<Object> handleRuntimeException(RuntimeException ex, WebRequest request) {
        String errorMessage = "Internal server error";
        Error error = new Error(HttpStatus.INTERNAL_SERVER_ERROR);
        error.setCode(500);
        error.setMessage(errorMessage);
        error.addErrors(new InternalServerError(errorMessage));

        errorMessage = errorMessage + getCorrelationId(request);
        log.log(Level.SEVERE, errorMessage);
        log.log(Level.INFO, errorMessage, ex);
        return buildResponseEntity(error);
    }

    @ExceptionHandler(BadRequestException.class)
    protected ResponseEntity<Object> handleBadRequest(BadRequestException ex, WebRequest request) {
        String errorMessage = ex.getErrorMsg();
        Error error = new Error(HttpStatus.BAD_REQUEST);
        error.setCode(400);
        error.setMessage(ex.getErrorMsg());
        error.addErrors(new BadRequestError(errorMessage));

        errorMessage = errorMessage + getCorrelationId(request);
        log.log(Level.SEVERE, errorMessage);
        log.log(Level.INFO, errorMessage, ex);
        return buildResponseEntity(error);
    }

    @ExceptionHandler(NotFoundException.class)
    protected ResponseEntity<Object> handleNotFoundException(NotFoundException ex, WebRequest request) {
        String errorMessage = ex.getErrorMsg();
        Error error = new Error(HttpStatus.NOT_FOUND);
        error.setCode(404);
        error.setMessage(errorMessage);
        error.addErrors(new NotFoundError(errorMessage));

        errorMessage = errorMessage + getCorrelationId(request);
        log.log(Level.SEVERE, errorMessage);
        log.log(Level.INFO, errorMessage, ex);
        return buildResponseEntity(error);
    }

    @ExceptionHandler(UnauthorizedException.class)
    protected ResponseEntity<Object> handleUnauthorizedException(UnauthorizedException ex, WebRequest request) {
        String errorMessage = ex.getErrorMsg();
        Error error = new Error(HttpStatus.UNAUTHORIZED);
        error.setCode(401);
        error.setMessage(errorMessage);
        error.addErrors(new UnauthorizedError(errorMessage));

        errorMessage = errorMessage + getCorrelationId(request);
        log.log(Level.SEVERE, errorMessage);
        log.log(Level.INFO, errorMessage, ex);
        return buildResponseEntity(error);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    protected ResponseEntity<Object> handleValidationException(ConstraintViolationException ex, WebRequest request) {
        List<String> errors = ex.getConstraintViolations().stream().map(x -> x.getMessage())
                .collect(Collectors.toList());
        String errorMessage = errors.get(0);
        Error error = new Error(HttpStatus.BAD_REQUEST);
        error.setCode(400);
        error.setMessage(errorMessage);
        error.addErrors(new BadRequestError(errorMessage));

        errorMessage = errorMessage + getCorrelationId(request);
        log.log(Level.SEVERE, errorMessage);
        log.log(Level.INFO, errorMessage, ex);
        return buildResponseEntity(error);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<Object> handleTypeMisMatchException(MethodArgumentTypeMismatchException ex,
            WebRequest request) {
        String errorMessage = String.format("%s should be of type %s", ex.getName(),
                ex.getRequiredType().getSimpleName());
        Error error = new Error(HttpStatus.BAD_REQUEST);
        error.setCode(400);
        error.setMessage(errorMessage);
        error.addErrors(new BadRequestError(errorMessage));

        errorMessage = errorMessage + getCorrelationId(request);
        log.log(Level.SEVERE, errorMessage);
        log.log(Level.INFO, errorMessage, ex);
        return buildResponseEntity(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    protected ResponseEntity<Object> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        String errorMessage = SchemaConstants.MSG_FORBIDDEN;
        Error error = new Error(HttpStatus.FORBIDDEN);
        error.setCode(403);
        error.setMessage(errorMessage);
        error.addErrors(new AuthorizationError(errorMessage));

        errorMessage = getCorrelationId(request) + errorMessage;
        log.log(Level.SEVERE, errorMessage);
        log.log(Level.FINE, errorMessage, ex);
        return buildResponseEntity(error);
    }

    protected ResponseEntity<Object> buildResponseEntity(Error error) {
        return new ResponseEntity<>(error, error.getStatus());
    }

    protected String getCorrelationId(WebRequest request) {
        if (request == null)
            return "";
        return Optional.ofNullable(request.getHeader(CORRELATION_ID)).map(value -> value + " : ").orElse("");
    }

    // todo: fix scope of this method
    String extractMissingHeaderName(String msg) {
        if (msg == null || msg.isEmpty())
            return "";
        Pattern p = Pattern.compile("\'([^\']*)\'");
        Matcher m = p.matcher(msg);
        if (!m.find())
            return "";
        return Optional.ofNullable(m.group(1)).orElse("");
    }
}
