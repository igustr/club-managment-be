package ee.finalthesis.clubmanagement.service;

import ee.finalthesis.clubmanagement.common.exception.ConflictException;
import ee.finalthesis.clubmanagement.domain.Game;
import ee.finalthesis.clubmanagement.domain.TrainingSession;
import ee.finalthesis.clubmanagement.repository.GameRepository;
import ee.finalthesis.clubmanagement.repository.TrainingSessionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PitchConflictValidator {

  private final TrainingSessionRepository trainingSessionRepository;
  private final GameRepository gameRepository;
  private final MessageSource messageSource;

  /**
   * Validates that a booking on the given pitch fits within the available capacity for the given
   * time window. Trainings consume their {@code pitchPortion}; games are treated as occupying the
   * full pitch (1.0). The check fails if the sum of overlapping portions plus the requested portion
   * exceeds 1.0.
   */
  public void checkConflict(
      UUID pitchId,
      LocalDate date,
      LocalTime startTime,
      LocalTime endTime,
      BigDecimal requestedPortion,
      UUID excludeTrainingId,
      UUID excludeGameId) {
    BigDecimal portion = requestedPortion != null ? requestedPortion : BigDecimal.ONE;

    BigDecimal usedByTrainings =
        findOverlappingTrainings(pitchId, date, startTime, endTime, excludeTrainingId).stream()
            .map(t -> t.getPitchPortion() != null ? t.getPitchPortion() : BigDecimal.ONE)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    List<Game> games = findOverlappingGames(pitchId, date, startTime, endTime, excludeGameId);
    BigDecimal usedByGames = BigDecimal.valueOf(games.size());

    BigDecimal total = usedByTrainings.add(usedByGames).add(portion);

    if (total.compareTo(BigDecimal.ONE) > 0) {
      throw new ConflictException(msg("error.pitch.conflict"), "pitch", "conflict");
    }
  }

  private List<TrainingSession> findOverlappingTrainings(
      UUID pitchId, LocalDate date, LocalTime startTime, LocalTime endTime, UUID excludeId) {
    return excludeId != null
        ? trainingSessionRepository.findConflictingBookingsExcluding(
            pitchId, date, startTime, endTime, excludeId)
        : trainingSessionRepository.findConflictingBookings(pitchId, date, startTime, endTime);
  }

  private List<Game> findOverlappingGames(
      UUID pitchId, LocalDate date, LocalTime startTime, LocalTime endTime, UUID excludeId) {
    return excludeId != null
        ? gameRepository.findConflictingBookingsExcluding(
            pitchId, date, startTime, endTime, excludeId)
        : gameRepository.findConflictingBookings(pitchId, date, startTime, endTime);
  }

  private String msg(String key, Object... args) {
    Locale locale = LocaleContextHolder.getLocale();
    return messageSource.getMessage(key, args, key, locale);
  }
}
