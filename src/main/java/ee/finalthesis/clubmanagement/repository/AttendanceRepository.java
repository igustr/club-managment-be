package ee.finalthesis.clubmanagement.repository;

import ee.finalthesis.clubmanagement.domain.Attendance;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {}
