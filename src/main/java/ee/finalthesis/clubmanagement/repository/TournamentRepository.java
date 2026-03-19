package ee.finalthesis.clubmanagement.repository;

import ee.finalthesis.clubmanagement.domain.Tournament;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TournamentRepository extends JpaRepository<Tournament, UUID> {

  @Query("SELECT t FROM Tournament t JOIN FETCH t.team WHERE t.club.id = :clubId")
  List<Tournament> findByClubIdWithTeam(@Param("clubId") UUID clubId);

  @Query(
      "SELECT t FROM Tournament t JOIN FETCH t.team LEFT JOIN FETCH t.pitch"
          + " WHERE t.id = :tournamentId AND t.club.id = :clubId")
  Optional<Tournament> findByIdAndClubIdWithTeamAndPitch(
      @Param("tournamentId") UUID tournamentId, @Param("clubId") UUID clubId);

  Optional<Tournament> findByIdAndClubId(UUID tournamentId, UUID clubId);

  @Query("SELECT t FROM Tournament t JOIN FETCH t.team" + " WHERE t.team.id IN :teamIds")
  List<Tournament> findByTeamIdInWithTeam(@Param("teamIds") List<UUID> teamIds);
}
