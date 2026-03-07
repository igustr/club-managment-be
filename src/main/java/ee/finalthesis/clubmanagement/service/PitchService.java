package ee.finalthesis.clubmanagement.service;

import ee.finalthesis.clubmanagement.common.exception.ResourceNotFoundException;
import ee.finalthesis.clubmanagement.domain.Club;
import ee.finalthesis.clubmanagement.domain.Pitch;
import ee.finalthesis.clubmanagement.repository.ClubRepository;
import ee.finalthesis.clubmanagement.repository.PitchRepository;
import ee.finalthesis.clubmanagement.repository.TrainingSessionRepository;
import ee.finalthesis.clubmanagement.service.dto.pitch.CreatePitchDTO;
import ee.finalthesis.clubmanagement.service.dto.pitch.PitchDTO;
import ee.finalthesis.clubmanagement.service.dto.pitch.UpdatePitchDTO;
import ee.finalthesis.clubmanagement.service.dto.training.TrainingSessionDTO;
import ee.finalthesis.clubmanagement.service.mapper.PitchMapper;
import ee.finalthesis.clubmanagement.service.mapper.TrainingSessionMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PitchService {

  private final PitchRepository pitchRepository;
  private final ClubRepository clubRepository;
  private final TrainingSessionRepository trainingSessionRepository;
  private final PitchMapper pitchMapper;
  private final TrainingSessionMapper trainingSessionMapper;
  private final MessageSource messageSource;

  @Transactional(readOnly = true)
  public List<PitchDTO> listPitches(UUID clubId) {
    List<Pitch> pitches = pitchRepository.findByClubId(clubId);
    return pitchMapper.toDto(pitches);
  }

  @Transactional(readOnly = true)
  public PitchDTO getPitch(UUID clubId, UUID pitchId) {
    Pitch pitch =
        pitchRepository
            .findByIdAndClubId(pitchId, clubId)
            .orElseThrow(() -> new ResourceNotFoundException("Pitch", "id", pitchId));
    return pitchMapper.toDto(pitch);
  }

  @Transactional
  public PitchDTO createPitch(UUID clubId, CreatePitchDTO request) {
    Club club =
        clubRepository
            .findById(clubId)
            .orElseThrow(() -> new ResourceNotFoundException("Club", "id", clubId));

    Pitch pitch =
        Pitch.builder()
            .name(request.getName())
            .address(request.getAddress())
            .surfaceType(request.getSurfaceType())
            .capacity(request.getCapacity())
            .club(club)
            .build();

    pitch = pitchRepository.save(pitch);
    return pitchMapper.toDto(pitch);
  }

  @Transactional
  public PitchDTO updatePitch(UUID clubId, UUID pitchId, UpdatePitchDTO request) {
    Pitch pitch =
        pitchRepository
            .findByIdAndClubId(pitchId, clubId)
            .orElseThrow(() -> new ResourceNotFoundException("Pitch", "id", pitchId));

    pitch.setName(request.getName());
    pitch.setAddress(request.getAddress());
    pitch.setSurfaceType(request.getSurfaceType());
    pitch.setCapacity(request.getCapacity());

    pitch = pitchRepository.save(pitch);
    return pitchMapper.toDto(pitch);
  }

  @Transactional
  public void deletePitch(UUID clubId, UUID pitchId) {
    Pitch pitch =
        pitchRepository
            .findByIdAndClubId(pitchId, clubId)
            .orElseThrow(() -> new ResourceNotFoundException("Pitch", "id", pitchId));
    pitchRepository.delete(pitch);
  }

  @Transactional(readOnly = true)
  public List<TrainingSessionDTO> getPitchSchedule(
      UUID clubId, UUID pitchId, LocalDate startDate, LocalDate endDate) {
    if (!pitchRepository.existsByIdAndClubId(pitchId, clubId)) {
      throw new ResourceNotFoundException("Pitch", "id", pitchId);
    }
    return trainingSessionMapper.toDto(
        trainingSessionRepository.findByPitchIdAndDateBetween(pitchId, startDate, endDate));
  }

  private String msg(String key, Object... args) {
    Locale locale = LocaleContextHolder.getLocale();
    return messageSource.getMessage(key, args, key, locale);
  }
}
