package ee.finalthesis.clubmanagement.service;

import ee.finalthesis.clubmanagement.common.exception.ResourceNotFoundException;
import ee.finalthesis.clubmanagement.domain.Club;
import ee.finalthesis.clubmanagement.domain.Game;
import ee.finalthesis.clubmanagement.domain.Pitch;
import ee.finalthesis.clubmanagement.domain.TrainingSession;
import ee.finalthesis.clubmanagement.repository.ClubRepository;
import ee.finalthesis.clubmanagement.repository.GameRepository;
import ee.finalthesis.clubmanagement.repository.PitchRepository;
import ee.finalthesis.clubmanagement.repository.TrainingSessionRepository;
import ee.finalthesis.clubmanagement.service.dto.pitch.CreatePitchDTO;
import ee.finalthesis.clubmanagement.service.dto.pitch.PitchConflictDTO;
import ee.finalthesis.clubmanagement.service.dto.pitch.PitchDTO;
import ee.finalthesis.clubmanagement.service.dto.pitch.PitchOccupancyDTO;
import ee.finalthesis.clubmanagement.service.dto.pitch.PitchScheduleDTO;
import ee.finalthesis.clubmanagement.service.dto.pitch.PitchScheduleEntryDTO;
import ee.finalthesis.clubmanagement.service.dto.pitch.PitchScheduleEventDTO;
import ee.finalthesis.clubmanagement.service.dto.pitch.UpdatePitchDTO;
import ee.finalthesis.clubmanagement.service.dto.training.TrainingSessionDTO;
import ee.finalthesis.clubmanagement.service.mapper.PitchMapper;
import ee.finalthesis.clubmanagement.service.mapper.TrainingSessionMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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
  private final GameRepository gameRepository;
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

  @Transactional(readOnly = true)
  public List<PitchOccupancyDTO> getPitchOverview(UUID clubId, LocalDate startDate, LocalDate endDate) {
    List<Pitch> pitches = pitchRepository.findByClubId(clubId);
    List<PitchOccupancyDTO> result = new ArrayList<>();

    for (Pitch pitch : pitches) {
      List<TrainingSession> sessions =
          trainingSessionRepository.findByPitchIdAndDateBetween(pitch.getId(), startDate, endDate);

      // Group sessions by date
      Map<LocalDate, List<TrainingSession>> byDate =
          sessions.stream().collect(Collectors.groupingBy(TrainingSession::getDate));

      for (Map.Entry<LocalDate, List<TrainingSession>> entry : byDate.entrySet()) {
        BigDecimal totalOccupancy = entry.getValue().stream()
            .map(TrainingSession::getPitchPortion)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        result.add(new PitchOccupancyDTO(
            pitch.getId(),
            pitch.getName(),
            entry.getKey(),
            totalOccupancy,
            trainingSessionMapper.toDto(entry.getValue())));
      }
    }

    return result;
  }

  /**
   * Returns club-wide pitch usage and detected scheduling conflicts within the given date range.
   * Conflicts are detected per pitch per day using a sweep-line algorithm: an overbooking arises
   * when the sum of overlapping pitch portions (trainings) and games (each treated as 1.0) exceeds
   * 1.0 at any moment in time.
   */
  @Transactional(readOnly = true)
  public PitchScheduleDTO getClubPitchSchedule(UUID clubId, LocalDate from, LocalDate to) {
    if (!clubRepository.existsById(clubId)) {
      throw new ResourceNotFoundException("Club", "id", clubId);
    }

    List<Pitch> pitches = pitchRepository.findByClubId(clubId);
    List<Game> homeGames =
        gameRepository.findHomeGamesByClubIdAndDateBetween(clubId, from, to);

    List<PitchScheduleEntryDTO> entries = new ArrayList<>();
    List<PitchConflictDTO> allConflicts = new ArrayList<>();

    for (Pitch pitch : pitches) {
      List<TrainingSession> trainings =
          trainingSessionRepository.findByPitchIdAndDateBetween(pitch.getId(), from, to);

      List<PitchScheduleEventDTO> events = new ArrayList<>();
      for (TrainingSession ts : trainings) {
        events.add(
            new PitchScheduleEventDTO(
                ts.getId(),
                PitchScheduleEventDTO.EventType.TRAINING,
                ts.getDate(),
                ts.getStartTime(),
                ts.getEndTime(),
                ts.getTeam().getId(),
                ts.getTeam().getName(),
                ts.getPitchPortion() != null ? ts.getPitchPortion() : BigDecimal.ONE,
                null));
      }
      for (Game g : homeGames) {
        if (g.getPitch() != null && pitch.getId().equals(g.getPitch().getId())) {
          events.add(
              new PitchScheduleEventDTO(
                  g.getId(),
                  PitchScheduleEventDTO.EventType.GAME,
                  g.getDate(),
                  g.getStartTime(),
                  g.getEndTime(),
                  g.getTeam().getId(),
                  g.getTeam().getName(),
                  BigDecimal.ONE,
                  g.getOpponent()));
        }
      }
      events.sort(
          Comparator.comparing(PitchScheduleEventDTO::getDate)
              .thenComparing(PitchScheduleEventDTO::getStartTime));

      entries.add(new PitchScheduleEntryDTO(pitch.getId(), pitch.getName(), events));
      allConflicts.addAll(detectConflicts(pitch.getId(), pitch.getName(), events));
    }

    return new PitchScheduleDTO(from, to, entries, allConflicts);
  }

  /** Sweep-line conflict detection per pitch, grouped by date. */
  private List<PitchConflictDTO> detectConflicts(
      UUID pitchId, String pitchName, List<PitchScheduleEventDTO> events) {
    List<PitchConflictDTO> result = new ArrayList<>();
    Map<LocalDate, List<PitchScheduleEventDTO>> byDate =
        events.stream().collect(Collectors.groupingBy(PitchScheduleEventDTO::getDate));

    for (Map.Entry<LocalDate, List<PitchScheduleEventDTO>> entry : byDate.entrySet()) {
      LocalDate date = entry.getKey();
      List<PitchScheduleEventDTO> dayEvents = entry.getValue();
      if (dayEvents.size() < 2) continue;

      // Build sweep points: type=END before type=START at equal time → no false conflict at boundary
      List<SweepPoint> points = new ArrayList<>();
      for (PitchScheduleEventDTO e : dayEvents) {
        BigDecimal portion = e.getPitchPortion() != null ? e.getPitchPortion() : BigDecimal.ONE;
        points.add(new SweepPoint(e.getStartTime(), portion, true, e.getId()));
        points.add(new SweepPoint(e.getEndTime(), portion, false, e.getId()));
      }
      points.sort(
          Comparator.comparing((SweepPoint p) -> p.time)
              .thenComparing(p -> p.isStart ? 1 : 0));

      BigDecimal occupancy = BigDecimal.ZERO;
      Set<UUID> active = new LinkedHashSet<>();
      LocalTime conflictStart = null;
      BigDecimal conflictMax = BigDecimal.ZERO;
      Set<UUID> conflictIds = new LinkedHashSet<>();

      for (SweepPoint p : points) {
        if (p.isStart) {
          occupancy = occupancy.add(p.portion);
          active.add(p.eventId);
        } else {
          occupancy = occupancy.subtract(p.portion);
          active.remove(p.eventId);
        }

        boolean inConflict = occupancy.compareTo(BigDecimal.ONE) > 0;

        if (inConflict && conflictStart == null) {
          conflictStart = p.time;
          conflictIds = new LinkedHashSet<>(active);
          conflictMax = occupancy;
        } else if (inConflict) {
          conflictIds.addAll(active);
          if (occupancy.compareTo(conflictMax) > 0) conflictMax = occupancy;
        } else if (conflictStart != null) {
          result.add(
              new PitchConflictDTO(
                  pitchId,
                  pitchName,
                  date,
                  conflictStart,
                  p.time,
                  conflictMax,
                  new ArrayList<>(conflictIds)));
          conflictStart = null;
          conflictMax = BigDecimal.ZERO;
          conflictIds = new LinkedHashSet<>();
        }
      }
    }
    return result;
  }

  private static final class SweepPoint {
    final LocalTime time;
    final BigDecimal portion;
    final boolean isStart;
    final UUID eventId;

    SweepPoint(LocalTime time, BigDecimal portion, boolean isStart, UUID eventId) {
      this.time = time;
      this.portion = portion;
      this.isStart = isStart;
      this.eventId = eventId;
    }
  }

  private String msg(String key, Object... args) {
    Locale locale = LocaleContextHolder.getLocale();
    return messageSource.getMessage(key, args, key, locale);
  }
}
