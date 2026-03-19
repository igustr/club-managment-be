package ee.finalthesis.clubmanagement.repository;

import ee.finalthesis.clubmanagement.domain.TournamentSquadMember;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TournamentSquadMemberRepository
    extends JpaRepository<TournamentSquadMember, UUID> {

  @Query(
      "SELECT sm FROM TournamentSquadMember sm JOIN FETCH sm.user"
          + " WHERE sm.tournament.id = :tournamentId")
  List<TournamentSquadMember> findByTournamentIdWithUser(@Param("tournamentId") UUID tournamentId);

  Optional<TournamentSquadMember> findByTournamentIdAndUserId(UUID tournamentId, UUID userId);

  boolean existsByTournamentIdAndUserId(UUID tournamentId, UUID userId);

  void deleteByTournamentId(UUID tournamentId);

  @Query(
      "SELECT DISTINCT sm.tournament.id FROM TournamentSquadMember sm"
          + " WHERE sm.user.id = :userId AND sm.tournament.club.id = :clubId")
  List<UUID> findTournamentIdsByUserIdAndClubId(
      @Param("userId") UUID userId, @Param("clubId") UUID clubId);
}
