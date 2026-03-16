package ee.finalthesis.clubmanagement.repository;

import ee.finalthesis.clubmanagement.domain.Conversation;
import ee.finalthesis.clubmanagement.domain.ConversationParticipant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ConversationParticipantRepository
    extends JpaRepository<ConversationParticipant, UUID> {

  List<ConversationParticipant> findByConversationId(UUID conversationId);

  boolean existsByConversationIdAndUserId(UUID conversationId, UUID userId);

  void deleteByConversationIdAndUserId(UUID conversationId, UUID userId);

  @Query(
      "SELECT cp.conversation FROM ConversationParticipant cp"
          + " WHERE cp.user.id = :userId AND cp.conversation.club.id = :clubId"
          + " ORDER BY cp.conversation.lastMessageTime DESC NULLS LAST")
  List<Conversation> findConversationsByUserIdAndClubId(UUID userId, UUID clubId);

  void deleteByUserId(UUID userId);

  @Query(
      "SELECT cp1.conversation FROM ConversationParticipant cp1 JOIN ConversationParticipant cp2 ON"
          + " cp1.conversation = cp2.conversation WHERE cp1.user.id = :userId1 AND cp2.user.id ="
          + " :userId2 AND cp1.conversation.type ="
          + " ee.finalthesis.clubmanagement.domain.enumeration.ConversationType.DIRECT AND"
          + " cp1.conversation.club.id = :clubId")
  Optional<Conversation> findDirectConversation(UUID userId1, UUID userId2, UUID clubId);
}
