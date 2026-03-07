package ee.finalthesis.clubmanagement.repository;

import ee.finalthesis.clubmanagement.domain.Pitch;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PitchRepository extends JpaRepository<Pitch, UUID> {

  List<Pitch> findByClubId(UUID clubId);

  Optional<Pitch> findByIdAndClubId(UUID pitchId, UUID clubId);

  boolean existsByIdAndClubId(UUID pitchId, UUID clubId);
}
