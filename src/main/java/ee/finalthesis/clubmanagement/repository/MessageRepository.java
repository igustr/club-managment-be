package ee.finalthesis.clubmanagement.repository;

import ee.finalthesis.clubmanagement.domain.Message;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

  Page<Message> findByConversationIdOrderByCreatedAtDesc(UUID conversationId, Pageable pageable);

  void deleteByConversationId(UUID conversationId);
}
