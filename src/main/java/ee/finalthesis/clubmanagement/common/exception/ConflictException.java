package ee.finalthesis.clubmanagement.common.exception;

import java.io.Serial;
import lombok.Getter;

@Getter
public class ConflictException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  private final String entityName;
  private final String errorKey;

  public ConflictException(String message, String entityName, String errorKey) {
    super(message);
    this.entityName = entityName;
    this.errorKey = errorKey;
  }
}
