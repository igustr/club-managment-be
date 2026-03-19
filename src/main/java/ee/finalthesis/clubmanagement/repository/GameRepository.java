package ee.finalthesis.clubmanagement.repository;

import ee.finalthesis.clubmanagement.domain.Game;
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
public interface GameRepository extends JpaRepository<Game, UUID> {

  @Query("SELECT g FROM Game g JOIN FETCH g.team WHERE g.club.id = :clubId")
  List<Game> findByClubIdWithTeam(@Param("clubId") UUID clubId);

  @Query(
      "SELECT g FROM Game g JOIN FETCH g.team LEFT JOIN FETCH g.pitch"
          + " WHERE g.id = :gameId AND g.club.id = :clubId")
  Optional<Game> findByIdAndClubIdWithTeamAndPitch(
      @Param("gameId") UUID gameId, @Param("clubId") UUID clubId);

  Optional<Game> findByIdAndClubId(UUID gameId, UUID clubId);

  @Query("SELECT g FROM Game g JOIN FETCH g.team" + " WHERE g.team.id IN :teamIds")
  List<Game> findByTeamIdInWithTeam(@Param("teamIds") List<UUID> teamIds);

  @Query(
      "SELECT g FROM Game g WHERE g.pitch.id = :pitchId"
          + " AND g.date = :date AND g.status = 'SCHEDULED'"
          + " AND g.startTime < :endTime AND g.endTime > :startTime")
  List<Game> findConflictingBookings(
      @Param("pitchId") UUID pitchId,
      @Param("date") LocalDate date,
      @Param("startTime") LocalTime startTime,
      @Param("endTime") LocalTime endTime);

  @Query(
      "SELECT g FROM Game g WHERE g.pitch.id = :pitchId"
          + " AND g.date = :date AND g.status = 'SCHEDULED'"
          + " AND g.startTime < :endTime AND g.endTime > :startTime"
          + " AND g.id <> :excludeId")
  List<Game> findConflictingBookingsExcluding(
      @Param("pitchId") UUID pitchId,
      @Param("date") LocalDate date,
      @Param("startTime") LocalTime startTime,
      @Param("endTime") LocalTime endTime,
      @Param("excludeId") UUID excludeId);
}
