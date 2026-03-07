package ee.finalthesis.clubmanagement.service.mapper;

import ee.finalthesis.clubmanagement.domain.Pitch;
import ee.finalthesis.clubmanagement.service.dto.pitch.PitchDTO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PitchMapper {

  @Mapping(source = "club.id", target = "clubId")
  PitchDTO toDto(Pitch pitch);

  List<PitchDTO> toDto(List<Pitch> pitches);
}
