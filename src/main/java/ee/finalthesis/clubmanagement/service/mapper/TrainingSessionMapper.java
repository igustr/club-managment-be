package ee.finalthesis.clubmanagement.service.mapper;

import ee.finalthesis.clubmanagement.domain.TrainingSession;
import ee.finalthesis.clubmanagement.service.dto.training.TrainingSessionDTO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TrainingSessionMapper {

  @Mapping(source = "team.id", target = "teamId")
  @Mapping(source = "team.name", target = "teamName")
  @Mapping(source = "pitch.id", target = "pitchId")
  @Mapping(source = "pitch.name", target = "pitchName")
  TrainingSessionDTO toDto(TrainingSession trainingSession);

  List<TrainingSessionDTO> toDto(List<TrainingSession> sessions);
}
