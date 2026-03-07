package ee.finalthesis.clubmanagement.common.exception;

import java.io.Serial;
import lombok.Getter;

@Getter
public class ResourceNotFoundException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  private final String entityName;
  private final String fieldName;
  private final Object fieldValue;

  public ResourceNotFoundException(String entityName, String fieldName, Object fieldValue) {
    super(String.format("%s not found with %s: '%s'", entityName, fieldName, fieldValue));
    this.entityName = entityName;
    this.fieldName = fieldName;
    this.fieldValue = fieldValue;
  }
}
