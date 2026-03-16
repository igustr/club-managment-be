package ee.finalthesis.clubmanagement.repository;

import ee.finalthesis.clubmanagement.domain.Attendance;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
