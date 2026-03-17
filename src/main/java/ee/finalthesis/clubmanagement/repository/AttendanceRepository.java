package ee.finalthesis.clubmanagement.repository;

import ee.finalthesis.clubmanagement.domain.Attendance;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {

  List<Attendance> findByTrainingSessionId(UUID trainingSessionId);

  Optional<Attendance> findByTrainingSessionIdAndUserId(UUID trainingSessionId, UUID userId);

  boolean existsByTrainingSessionIdAndUserId(UUID trainingSessionId, UUID userId);

  void deleteByTrainingSessionId(UUID trainingSessionId);

  List<Attendance> findByTrainingSessionTeamClubId(UUID clubId);

  List<Attendance> findByUserIdAndTrainingSessionTeamClubId(UUID userId, UUID clubId);

  List<Attendance> findByTrainingSessionTeamIdAndTrainingSessionTeamClubId(
      UUID teamId, UUID clubId);

  @Query(
      "SELECT a.status, COUNT(a) FROM Attendance a"
          + " WHERE a.user.id = :userId AND a.trainingSession.team.club.id = :clubId"
          + " GROUP BY a.status")
  List<Object[]> countByStatusForUserAndClub(
      @Param("userId") UUID userId, @Param("clubId") UUID clubId);

  @Query(
      "SELECT a.status, COUNT(a) FROM Attendance a"
          + " WHERE a.trainingSession.team.club.id = :clubId GROUP BY a.status")
  List<Object[]> countByStatusForClub(@Param("clubId") UUID clubId);

  @Query(
      "SELECT a.trainingSession.team.id, a.status, COUNT(a) FROM Attendance a"
          + " WHERE a.trainingSession.team.club.id = :clubId"
          + " GROUP BY a.trainingSession.team.id, a.status")
  List<Object[]> countByTeamAndStatusForClub(@Param("clubId") UUID clubId);

  @Query(
      "SELECT SUBSTRING(CAST(a.trainingSession.date AS string), 1, 7), a.status, COUNT(a)"
          + " FROM Attendance a WHERE a.trainingSession.team.club.id = :clubId"
          + " GROUP BY SUBSTRING(CAST(a.trainingSession.date AS string), 1, 7), a.status")
  List<Object[]> countByMonthAndStatusForClub(@Param("clubId") UUID clubId);

  @Query(
      "SELECT COUNT(DISTINCT a.trainingSession.id)"
          + " FROM Attendance a WHERE a.trainingSession.team.club.id = :clubId"
          + " AND SUBSTRING(CAST(a.trainingSession.date AS string), 1, 7) = :month")
  int countDistinctTrainingSessionsByClubAndMonth(
      @Param("clubId") UUID clubId, @Param("month") String month);

  @Query("SELECT a.user.id FROM Attendance a WHERE a.trainingSession.id = :trainingSessionId")
  List<UUID> findUserIdsByTrainingSessionId(@Param("trainingSessionId") UUID trainingSessionId);
}
