package ee.finalthesis.clubmanagement.common.exception;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class ExceptionTranslator extends ResponseEntityExceptionHandler {

  private static final URI PROBLEM_BASE = URI.create("about:blank");

  private final MessageSource messageSource;

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<ProblemDetail> handleBadRequest(BadRequestException ex) {
    Locale locale = LocaleContextHolder.getLocale();
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    problem.setType(PROBLEM_BASE);
    problem.setTitle(msg("title.badRequest", locale));
    problem.setProperty("entityName", ex.getEntityName());
    problem.setProperty("errorKey", ex.getErrorKey());
    problem.setProperty("message", ex.getMessage());
    problem.setProperty("timestamp", Instant.now());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleNotFound(ResourceNotFoundException ex) {
    Locale locale = LocaleContextHolder.getLocale();
    String detail =
        msg("error.notFound", locale, ex.getEntityName(), ex.getFieldName(), ex.getFieldValue());
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, detail);
    problem.setType(PROBLEM_BASE);
    problem.setTitle(msg("title.notFound", locale));
    problem.setProperty("entityName", ex.getEntityName());
    problem.setProperty("message", detail);
    problem.setProperty("timestamp", Instant.now());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ProblemDetail> handleConflict(ConflictException ex) {
    Locale locale = LocaleContextHolder.getLocale();
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    problem.setType(PROBLEM_BASE);
    problem.setTitle(msg("title.conflict", locale));
    problem.setProperty("entityName", ex.getEntityName());
    problem.setProperty("errorKey", ex.getErrorKey());
    problem.setProperty("message", ex.getMessage());
    problem.setProperty("timestamp", Instant.now());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ProblemDetail> handleBadCredentials(BadCredentialsException ex) {
    Locale locale = LocaleContextHolder.getLocale();
    String detail = msg("error.auth.invalidCredentials", locale);
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, detail);
    problem.setType(PROBLEM_BASE);
    problem.setTitle(msg("title.unauthorized", locale));
    problem.setProperty("message", detail);
    problem.setProperty("timestamp", Instant.now());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException ex) {
    Locale locale = LocaleContextHolder.getLocale();
    String detail = msg("error.unauthorized", locale);
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, detail);
    problem.setType(PROBLEM_BASE);
    problem.setTitle(msg("title.unauthorized", locale));
    problem.setProperty("message", detail);
    problem.setProperty("timestamp", Instant.now());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex) {
    Locale locale = LocaleContextHolder.getLocale();
    String detail = msg("error.forbidden", locale);
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, detail);
    problem.setType(PROBLEM_BASE);
    problem.setTitle(msg("title.forbidden", locale));
    problem.setProperty("message", detail);
    problem.setProperty("timestamp", Instant.now());
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      @NonNull MethodArgumentNotValidException ex,
      @NonNull HttpHeaders headers,
      @NonNull HttpStatusCode status,
      @NonNull WebRequest request) {

    Locale locale = LocaleContextHolder.getLocale();
    String detail = msg("error.validation", locale);
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    problem.setType(PROBLEM_BASE);
    problem.setTitle(msg("title.validationError", locale));
    problem.setProperty("message", detail);
    problem.setProperty("timestamp", Instant.now());

    List<Map<String, String>> fieldErrors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(
                (FieldError fieldError) -> {
                  Map<String, String> error = new HashMap<>();
                  error.put("field", fieldError.getField());
                  error.put("message", fieldError.getDefaultMessage());
                  return error;
                })
            .toList();

    problem.setProperty("fieldErrors", fieldErrors);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleGenericException(Exception ex) {
    log.error("Unhandled exception", ex);
    Locale locale = LocaleContextHolder.getLocale();
    String detail = msg("error.internalServerError", locale);
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, detail);
    problem.setType(PROBLEM_BASE);
    problem.setTitle(msg("title.internalServerError", locale));
    problem.setProperty("message", detail);
    problem.setProperty("timestamp", Instant.now());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
  }

  private String msg(String key, Locale locale, Object... args) {
    return messageSource.getMessage(key, args, key, locale);
  }
}
