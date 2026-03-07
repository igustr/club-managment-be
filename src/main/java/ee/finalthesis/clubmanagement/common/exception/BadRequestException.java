package ee.finalthesis.clubmanagement.common.exception;

import java.io.Serial;
import lombok.Getter;

@Getter
public class BadRequestException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  private final String entityName;
  private final String errorKey;

  public BadRequestException(String message, String entityName, String errorKey) {
    super(message);
    this.entityName = entityName;
    this.errorKey = errorKey;
  }
}
