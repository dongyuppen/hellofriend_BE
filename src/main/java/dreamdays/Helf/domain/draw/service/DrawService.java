package dreamdays.Helf.domain.draw.service;

import dreamdays.Helf.domain.draw.dto.DrawResponse;
import dreamdays.Helf.domain.draw.repository.DrawRepository;
import dreamdays.Helf.domain.user.dto.CheckInfoResponse;
import dreamdays.Helf.domain.user.entity.User;
import dreamdays.Helf.domain.user.entity.enums.Gender;
import dreamdays.Helf.domain.user.repository.UserRepository;
import dreamdays.Helf.domain.user.service.UserService;
import dreamdays.Helf.exception.NoMatchingUserException;
import dreamdays.Helf.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DrawService {

    private final DrawRepository drawRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    //뽑고 싶은 성별에 맞는 사용자 조회
    public List<User> findByGender(Gender selectGender) {
        return drawRepository.findByGender(selectGender);
    }
    //뽑기 로직
    @Transactional
    public DrawResponse drawRandomUser(String name, String phoneNumber) {
        /**
         * 뽑는 유저 조회 일반
         * service에서 가져온 것을 dto에서 가져오는 걸로 변경!
         */
//        User user = userService.findByNameAndPhoneNumber(name, phoneNumber);
        CheckInfoResponse checkInfoResponse = userService.findByNameAndPhoneNumber(name, phoneNumber);

        //해당 정보 바탕으로 User 엔티티 조회
        User user = userRepository.findByNameAndPhoneNumber(checkInfoResponse.getName(), checkInfoResponse.getPhoneNumber())
                .orElseThrow(() -> new UserNotFoundException("해당 유저가 존재하지 않습니다."));

        //뽑고싶은 성별에 맞는 유저 리스트 조회
        List<User> availableUsers = findByGender(user.getSelectGender());

        // 1. picked가 false인 사람들 중에서 뽑기
        List<User> notPickedUsers = availableUsers.stream()
                .filter(u -> !u.equals(user))
                .filter(u -> !u.isPicked())
                .collect(Collectors.toList());

        User drawnUser = null;

        if (!notPickedUsers.isEmpty()) {
            // picked가 false인 사람들에서 랜덤으로 선택
            drawnUser = selectRandomUser(notPickedUsers);
        } else {
            // picked가 true인 사람들 중에서 랜덤으로 뽑기
            List<User> pickedUsers = availableUsers.stream()
                    .filter(u -> u.isPicked())
                    .collect(Collectors.toList());

            if (!pickedUsers.isEmpty()) {
                drawnUser = selectRandomUser(pickedUsers);
            } else {
                throw new NoMatchingUserException("조건에 맞는 사용자가 없습니다.");
            }
        }

        // 3. 뽑기 후 상태 변경
        user.setDraw(true);
        drawnUser.setPicked(true);

        return DrawResponse.from(drawnUser);
    }

    private User selectRandomUser(List<User> users) {
        Random random = new Random();
        return users.get(random.nextInt(users.size()));
    }

    //뽑기 종료를 누르면 다시 아무학생도 선택 안한 페이지로 돌아가나?
}
