package dreamdays.Helf.domain.user.repository;

import dreamdays.Helf.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByNameAndPhoneNumber(String name, String phoneNumber);
    boolean existsByNameAndPhoneNumber(String name, String phoneNumber);
}
