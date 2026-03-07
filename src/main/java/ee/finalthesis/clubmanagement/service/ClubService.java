package ee.finalthesis.clubmanagement.service;

import ee.finalthesis.clubmanagement.common.exception.ResourceNotFoundException;
import ee.finalthesis.clubmanagement.domain.Club;
import ee.finalthesis.clubmanagement.repository.ClubRepository;
import ee.finalthesis.clubmanagement.service.dto.club.ClubDTO;
import ee.finalthesis.clubmanagement.service.dto.club.UpdateClubDTO;
import ee.finalthesis.clubmanagement.service.mapper.ClubMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClubService {

  private final ClubRepository clubRepository;
  private final ClubMapper clubMapper;

  @Transactional(readOnly = true)
  public ClubDTO getClub(UUID clubId) {
    Club club =
        clubRepository
            .findById(clubId)
            .orElseThrow(() -> new ResourceNotFoundException("Club", "id", clubId));
    return clubMapper.toDto(club);
  }

  @Transactional
  public ClubDTO updateClub(UUID clubId, UpdateClubDTO request) {
    Club club =
        clubRepository
            .findById(clubId)
            .orElseThrow(() -> new ResourceNotFoundException("Club", "id", clubId));

    club.setName(request.getName());
    club.setRegistrationCode(request.getRegistrationCode());
    club.setAddress(request.getAddress());
    club.setContactEmail(request.getContactEmail());
    club.setContactPhone(request.getContactPhone());

    club = clubRepository.save(club);
    return clubMapper.toDto(club);
  }
}
