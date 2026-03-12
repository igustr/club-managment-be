package ee.finalthesis.clubmanagement.repository;

import ee.finalthesis.clubmanagement.domain.ConversationReadStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ConversationReadStatusRepository
    extends JpaRepository<ConversationReadStatus, UUID> {

  Optional<ConversationReadStatus> findByConversationIdAndUserId(UUID conversationId, UUID userId);

  List<ConversationReadStatus> findByConversationId(UUID conversationId);

  void deleteByConversationIdAndUserId(UUID conversationId, UUID userId);

  @Query(
      "SELECT COALESCE(SUM(crs.unreadCount), 0) FROM ConversationReadStatus crs"
          + " WHERE crs.user.id = :userId AND crs.conversation.club.id = :clubId")
  int sumUnreadCountByUserIdAndClubId(UUID userId, UUID clubId);
}
