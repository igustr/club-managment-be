package ee.finalthesis.clubmanagement.service.mapper;

import ee.finalthesis.clubmanagement.domain.Club;
import ee.finalthesis.clubmanagement.service.dto.club.ClubDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ClubMapper {

  ClubDTO toDto(Club club);
}
