package ee.finalthesis.clubmanagement.repository;

import ee.finalthesis.clubmanagement.domain.TrainingSession;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TrainingSessionRepository extends JpaRepository<TrainingSession, UUID> {

  List<TrainingSession> findByTeamId(UUID teamId);

  List<TrainingSession> findByTeamClubId(UUID clubId);

  Optional<TrainingSession> findByIdAndTeamClubId(UUID trainingId, UUID clubId);

  List<TrainingSession> findByRecurrenceGroupId(UUID recurrenceGroupId);

  @Query(
      "SELECT ts FROM TrainingSession ts WHERE ts.pitch.id = :pitchId "
          + "AND ts.date = :date AND ts.status = 'SCHEDULED' "
          + "AND ts.startTime < :endTime AND ts.endTime > :startTime")
  List<TrainingSession> findConflictingBookings(
      @Param("pitchId") UUID pitchId,
      @Param("date") LocalDate date,
      @Param("startTime") LocalTime startTime,
      @Param("endTime") LocalTime endTime);

  @Query(
      "SELECT ts FROM TrainingSession ts WHERE ts.pitch.id = :pitchId "
          + "AND ts.date = :date AND ts.status = 'SCHEDULED' "
          + "AND ts.startTime < :endTime AND ts.endTime > :startTime "
          + "AND ts.id <> :excludeId")
  List<TrainingSession> findConflictingBookingsExcluding(
      @Param("pitchId") UUID pitchId,
      @Param("date") LocalDate date,
      @Param("startTime") LocalTime startTime,
      @Param("endTime") LocalTime endTime,
      @Param("excludeId") UUID excludeId);

  @Query(
      "SELECT ts FROM TrainingSession ts WHERE ts.pitch.id = :pitchId "
          + "AND ts.date BETWEEN :startDate AND :endDate AND ts.status = 'SCHEDULED' "
          + "ORDER BY ts.date, ts.startTime")
  List<TrainingSession> findByPitchIdAndDateBetween(
      @Param("pitchId") UUID pitchId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);
}
