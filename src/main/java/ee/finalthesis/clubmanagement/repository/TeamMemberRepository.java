package ee.finalthesis.clubmanagement.repository;

import ee.finalthesis.clubmanagement.domain.TeamMember;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, UUID> {

  boolean existsByTeamIdAndUserId(UUID teamId, UUID userId);

  boolean existsByTeamClubIdAndUserId(UUID clubId, UUID userId);
}
