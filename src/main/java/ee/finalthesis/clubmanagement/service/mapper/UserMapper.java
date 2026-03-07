package ee.finalthesis.clubmanagement.service.mapper;

import ee.finalthesis.clubmanagement.domain.User;
import ee.finalthesis.clubmanagement.service.dto.auth.UserDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

  @Mapping(source = "club.id", target = "clubId")
  UserDTO toDto(User user);
}
