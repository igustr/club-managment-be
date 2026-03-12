package ee.finalthesis.clubmanagement.service.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendMessageDTO {

  @NotBlank @Size(max = 5000) private String text;
}
