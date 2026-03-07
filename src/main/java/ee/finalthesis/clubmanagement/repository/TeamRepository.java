package ee.finalthesis.clubmanagement.repository;

import ee.finalthesis.clubmanagement.domain.Team;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamRepository extends JpaRepository<Team, UUID> {

  List<Team> findByClubId(UUID clubId);

  Optional<Team> findByIdAndClubId(UUID teamId, UUID clubId);

  boolean existsByIdAndClubId(UUID teamId, UUID clubId);
}
