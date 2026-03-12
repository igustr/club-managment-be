package ee.finalthesis.clubmanagement.repository;

import ee.finalthesis.clubmanagement.domain.Conversation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

  Optional<Conversation> findByIdAndClubId(UUID id, UUID clubId);

  Optional<Conversation> findByTeamId(UUID teamId);
}
