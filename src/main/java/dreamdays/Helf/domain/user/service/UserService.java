package dreamdays.Helf.domain.user.service;

import dreamdays.Helf.domain.user.dto.CheckInfoResponse;
import dreamdays.Helf.domain.user.dto.InfoRequest;
import dreamdays.Helf.domain.user.entity.User;
import dreamdays.Helf.exception.AlreadyDrawnException;
import dreamdays.Helf.exception.UserNotFoundException;
import dreamdays.Helf.exception.UserAlreadyExistsException; // 커스텀 예외 추가
import dreamdays.Helf.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * 학생 정보 저장
     */
    @Transactional
    public void saveUser(InfoRequest infoRequest) { //User대신 InfoRequest
        User user = infoRequest.toEntity();
        // 중복 회원 검증
        if (userRepository.existsByNameAndPhoneNumber(user.getName(), user.getPhoneNumber())) {
            throw new UserAlreadyExistsException("이미 존재하는 회원입니다.");
        }
        userRepository.save(user);
    }

    /**
     * 이름과 전화번호로 회원 조회
     */
    public CheckInfoResponse findByNameAndPhoneNumber(String name, String phoneNumber) {
        User user = userRepository.findByNameAndPhoneNumber(name, phoneNumber)
                .orElseThrow(() -> new UserNotFoundException("해당 전화번호와 이름을 가진 사용자가 존재하지 않습니다."));

        if (user.isDraw()) {
            throw new AlreadyDrawnException("이미 뽑기를 진행한 사용자입니다.");
        }
        return CheckInfoResponse.from(user);
    }

    // 👇 전체 유저 조회 메서드 추가
    public List<User> getAllUsers() {
        return userRepository.findAll(); // JPA가 기본으로 제공하는 전체 조회 기능입니다.
    }
}
