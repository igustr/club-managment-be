package ee.finalthesis.clubmanagement.service.mapper;

import ee.finalthesis.clubmanagement.domain.Message;
import ee.finalthesis.clubmanagement.service.dto.chat.MessageDTO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MessageMapper {

  @Mapping(source = "conversation.id", target = "conversationId")
  @Mapping(source = "sender.id", target = "senderId")
  @Mapping(source = "sender.firstName", target = "senderFirstName")
  @Mapping(source = "sender.lastName", target = "senderLastName")
  MessageDTO toDto(Message message);

  List<MessageDTO> toDto(List<Message> messages);
}
