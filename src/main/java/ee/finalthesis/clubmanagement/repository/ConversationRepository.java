package ee.finalthesis.clubmanagement.repository;

import ee.finalthesis.clubmanagement.domain.Conversation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

  Optional<Conversation> findByIdAndClubId(UUID id, UUID clubId);

  Optional<Conversation> findByTeamId(UUID teamId);

  @Query(
      "SELECT t.id FROM Team t WHERE t.id NOT IN (SELECT c.team.id FROM Conversation c WHERE c.team"
          + " IS NOT NULL)")
  List<UUID> findTeamIdsWithoutConversation();
}
