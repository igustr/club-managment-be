package ee.finalthesis.clubmanagement.service.dto.club;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClubDTO {

  private UUID id;
  private String name;
  private String registrationCode;
  private String address;
  private String contactEmail;
  private String contactPhone;
}
