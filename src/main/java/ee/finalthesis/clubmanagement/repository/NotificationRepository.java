package ee.finalthesis.clubmanagement.repository;

import ee.finalthesis.clubmanagement.domain.Notification;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

  Page<Notification> findByUserIdAndClubIdOrderByCreatedAtDesc(
      UUID userId, UUID clubId, Pageable pageable);

  long countByUserIdAndClubIdAndReadFalse(UUID userId, UUID clubId);

  @Modifying
  @Query(
      "UPDATE Notification n SET n.read = true"
          + " WHERE n.user.id = :userId AND n.club.id = :clubId AND n.read = false")
  void markAllAsRead(@Param("userId") UUID userId, @Param("clubId") UUID clubId);
}
