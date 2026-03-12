package ee.finalthesis.clubmanagement.api.controller;

import ee.finalthesis.clubmanagement.service.ConversationService;
import ee.finalthesis.clubmanagement.service.MessageService;
import ee.finalthesis.clubmanagement.service.dto.chat.ConversationDTO;
import ee.finalthesis.clubmanagement.service.dto.chat.CreateDirectConversationDTO;
import ee.finalthesis.clubmanagement.service.dto.chat.MessageDTO;
import ee.finalthesis.clubmanagement.service.dto.chat.SendMessageDTO;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clubs/{clubId}/conversations")
@RequiredArgsConstructor
public class ConversationController {

  private final ConversationService conversationService;
  private final MessageService messageService;

  @GetMapping
  @PreAuthorize("@clubSecurity.isMemberOfClub(#clubId)")
  public ResponseEntity<List<ConversationDTO>> listConversations(@PathVariable UUID clubId) {
    return ResponseEntity.ok(conversationService.listConversations(clubId));
  }

  @PostMapping
  @PreAuthorize("@clubSecurity.isMemberOfClub(#clubId)")
  public ResponseEntity<ConversationDTO> createDirectConversation(
      @PathVariable UUID clubId, @Valid @RequestBody CreateDirectConversationDTO request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(conversationService.createDirectConversation(clubId, request));
  }

  @GetMapping("/{conversationId}")
  @PreAuthorize("@clubSecurity.isMemberOfClub(#clubId)")
  public ResponseEntity<ConversationDTO> getConversation(
      @PathVariable UUID clubId, @PathVariable UUID conversationId) {
    return ResponseEntity.ok(conversationService.getConversation(clubId, conversationId));
  }

  @GetMapping("/{conversationId}/messages")
  @PreAuthorize("@clubSecurity.isMemberOfClub(#clubId)")
  public ResponseEntity<Page<MessageDTO>> getMessages(
      @PathVariable UUID clubId, @PathVariable UUID conversationId, Pageable pageable) {
    return ResponseEntity.ok(messageService.getMessages(clubId, conversationId, pageable));
  }

  @PostMapping("/{conversationId}/messages")
  @PreAuthorize("@clubSecurity.isMemberOfClub(#clubId)")
  public ResponseEntity<MessageDTO> sendMessage(
      @PathVariable UUID clubId,
      @PathVariable UUID conversationId,
      @Valid @RequestBody SendMessageDTO request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(messageService.sendMessage(clubId, conversationId, request));
  }

  @PutMapping("/{conversationId}/read")
  @PreAuthorize("@clubSecurity.isMemberOfClub(#clubId)")
  public ResponseEntity<Void> markAsRead(
      @PathVariable UUID clubId, @PathVariable UUID conversationId) {
    messageService.markAsRead(clubId, conversationId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/unread-count")
  @PreAuthorize("@clubSecurity.isMemberOfClub(#clubId)")
  public ResponseEntity<Map<String, Integer>> getUnreadCount(@PathVariable UUID clubId) {
    return ResponseEntity.ok(Map.of("unreadCount", conversationService.getUnreadCount(clubId)));
  }
}
