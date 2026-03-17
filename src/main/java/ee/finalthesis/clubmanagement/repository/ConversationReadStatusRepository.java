package ee.finalthesis.clubmanagement.repository;

import ee.finalthesis.clubmanagement.domain.ConversationReadStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ConversationReadStatusRepository
    extends JpaRepository<ConversationReadStatus, UUID> {

  Optional<ConversationReadStatus> findByConversationIdAndUserId(UUID conversationId, UUID userId);

  List<ConversationReadStatus> findByConversationId(UUID conversationId);

  void deleteByConversationIdAndUserId(UUID conversationId, UUID userId);

  void deleteByUserId(UUID userId);

  @Query(
      "SELECT COALESCE(SUM(crs.unreadCount), 0) FROM ConversationReadStatus crs"
          + " WHERE crs.user.id = :userId AND crs.conversation.club.id = :clubId")
  int sumUnreadCountByUserIdAndClubId(UUID userId, UUID clubId);

  @Query(
      "SELECT crs FROM ConversationReadStatus crs"
          + " WHERE crs.user.id = :userId AND crs.conversation.id IN :conversationIds")
  List<ConversationReadStatus> findByUserIdAndConversationIdIn(
      @Param("userId") UUID userId, @Param("conversationIds") Collection<UUID> conversationIds);

  @Modifying
  @Query(
      "UPDATE ConversationReadStatus crs SET crs.unreadCount = crs.unreadCount + 1"
          + " WHERE crs.conversation.id = :conversationId AND crs.user.id <> :excludeUserId")
  int incrementUnreadCountExcludingUser(
      @Param("conversationId") UUID conversationId, @Param("excludeUserId") UUID excludeUserId);
}
