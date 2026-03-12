package ee.finalthesis.clubmanagement.service.mapper;

import ee.finalthesis.clubmanagement.domain.Conversation;
import ee.finalthesis.clubmanagement.domain.ConversationParticipant;
import ee.finalthesis.clubmanagement.service.dto.chat.ConversationDTO;
import ee.finalthesis.clubmanagement.service.dto.chat.ParticipantDTO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ConversationMapper {

  @Mapping(source = "team.id", target = "teamId")
  @Mapping(target = "name", ignore = true)
  @Mapping(target = "unreadCount", ignore = true)
  @Mapping(target = "participants", ignore = true)
  @Mapping(target = "lastMessageSenderName", ignore = true)
  ConversationDTO toDto(Conversation conversation);

  List<ConversationDTO> toDto(List<Conversation> conversations);

  @Mapping(source = "user.id", target = "userId")
  @Mapping(source = "user.firstName", target = "firstName")
  @Mapping(source = "user.lastName", target = "lastName")
  @Mapping(source = "user.email", target = "email")
  ParticipantDTO toParticipantDto(ConversationParticipant participant);

  List<ParticipantDTO> toParticipantDto(List<ConversationParticipant> participants);
}
