package ee.finalthesis.clubmanagement.repository;

import ee.finalthesis.clubmanagement.domain.Club;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClubRepository extends JpaRepository<Club, UUID> {}
