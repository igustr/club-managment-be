package ee.finalthesis.clubmanagement.service.mapper;

import ee.finalthesis.clubmanagement.domain.Notification;
import ee.finalthesis.clubmanagement.service.dto.notification.NotificationDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

  NotificationDTO toDto(Notification notification);
}
