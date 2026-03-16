package ee.finalthesis.clubmanagement.service;

import ee.finalthesis.clubmanagement.common.exception.BadRequestException;
import ee.finalthesis.clubmanagement.common.exception.ConflictException;
import ee.finalthesis.clubmanagement.common.exception.ResourceNotFoundException;
import ee.finalthesis.clubmanagement.domain.Club;
import ee.finalthesis.clubmanagement.domain.User;
import ee.finalthesis.clubmanagement.domain.enumeration.ClubRole;
import ee.finalthesis.clubmanagement.repository.ClubRepository;
import ee.finalthesis.clubmanagement.repository.UserRepository;
import ee.finalthesis.clubmanagement.service.dto.admin.AdminCreateUserDTO;
import ee.finalthesis.clubmanagement.service.dto.admin.AssignAdminDTO;
import ee.finalthesis.clubmanagement.service.dto.admin.CreateClubDTO;
import ee.finalthesis.clubmanagement.service.dto.auth.UserDTO;
import ee.finalthesis.clubmanagement.service.dto.club.ClubDTO;
import ee.finalthesis.clubmanagement.service.mapper.ClubMapper;
import ee.finalthesis.clubmanagement.service.mapper.UserMapper;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

  private final ClubRepository clubRepository;
  private final UserRepository userRepository;
  private final ClubMapper clubMapper;
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;
  private final MessageSource messageSource;

  @Transactional(readOnly = true)
  public Page<ClubDTO> listAllClubs(String search, Pageable pageable) {
    if (search != null && !search.isBlank()) {
      return clubRepository
          .findByNameContainingIgnoreCase(search.trim(), pageable)
          .map(clubMapper::toDto);
    }
    return clubRepository.findAll(pageable).map(clubMapper::toDto);
  }

  @Transactional
  public ClubDTO createClub(CreateClubDTO request) {
    Club club =
        Club.builder()
            .name(request.getName())
            .registrationCode(request.getRegistrationCode())
            .address(request.getAddress())
            .contactEmail(request.getContactEmail())
            .contactPhone(request.getContactPhone())
            .logoUrl(request.getLogoUrl())
            .build();

    club = clubRepository.save(club);
    return clubMapper.toDto(club);
  }

  @Transactional
  public void deleteClub(UUID clubId) {
    Club club =
        clubRepository
            .findById(clubId)
            .orElseThrow(() -> new ResourceNotFoundException("Club", "id", clubId));

    Page<User> members = userRepository.findByClubId(clubId, Pageable.ofSize(1));
    if (members.getTotalElements() > 0) {
      throw new BadRequestException(
          msg("error.admin.cannotDeleteClubWithMembers"), "club", "cannotDeleteClubWithMembers");
    }

    clubRepository.delete(club);
  }

  @Transactional(readOnly = true)
  public Page<UserDTO> listAllUsers(
      String search, UUID clubId, Boolean unaffiliated, Pageable pageable) {
    if (Boolean.TRUE.equals(unaffiliated)) {
      if (search != null && !search.isBlank()) {
        return userRepository
            .findUnaffiliatedBySearch(search.trim(), pageable)
            .map(userMapper::toDto);
      }
      return userRepository.findByClubIdIsNull(pageable).map(userMapper::toDto);
    }
    if (clubId != null) {
      if (search != null && !search.isBlank()) {
        return userRepository
            .findByClubIdAndSearch(clubId, search.trim(), pageable)
            .map(userMapper::toDto);
      }
      return userRepository.findByClubId(clubId, pageable).map(userMapper::toDto);
    }
    if (search != null && !search.isBlank()) {
      return userRepository.findBySearch(search.trim(), pageable).map(userMapper::toDto);
    }
    return userRepository.findAll(pageable).map(userMapper::toDto);
  }

  @Transactional
  public UserDTO createUser(AdminCreateUserDTO request) {
    if (userRepository.existsByEmail(request.getEmail())) {
      throw new ConflictException(msg("error.auth.emailExists"), "user", "emailExists");
    }

    User user =
        User.builder()
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .dateOfBirth(request.getDateOfBirth())
            .phone(request.getPhone())
            .active(true)
            .build();

    user = userRepository.save(user);
    return userMapper.toDto(user);
  }

  @Transactional
  public UserDTO assignClubAdmin(UUID clubId, AssignAdminDTO request) {
    Club club =
        clubRepository
            .findById(clubId)
            .orElseThrow(() -> new ResourceNotFoundException("Club", "id", clubId));

    User user =
        userRepository
            .findById(request.getUserId())
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getUserId()));

    if (user.getClub() != null) {
      throw new BadRequestException(
          msg("error.admin.userAlreadyAffiliated"), "user", "userAlreadyAffiliated");
    }

    user.setClub(club);
    user.setRole(ClubRole.CLUB_ADMIN);
    user = userRepository.save(user);
    return userMapper.toDto(user);
  }

  private String msg(String key, Object... args) {
    Locale locale = LocaleContextHolder.getLocale();
    return messageSource.getMessage(key, args, key, locale);
  }
}
