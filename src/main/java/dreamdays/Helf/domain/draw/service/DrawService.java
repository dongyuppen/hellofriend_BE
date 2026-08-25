package dreamdays.Helf.domain.draw.service;

import dreamdays.Helf.domain.draw.dto.DrawResponse;
import dreamdays.Helf.domain.draw.repository.DrawRepository;
import dreamdays.Helf.domain.user.entity.User;
import dreamdays.Helf.domain.user.entity.enums.Gender;
import dreamdays.Helf.domain.user.repository.UserRepository;
import dreamdays.Helf.exception.NoMatchingUserException;
import dreamdays.Helf.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DrawService {

    private final DrawRepository drawRepository;
    private final UserRepository userRepository;

    //뽑고 싶은 성별에 맞는 사용자 조회
    public List<User> findByGender(Gender selectGender) {
        return drawRepository.findByGender(selectGender);
    }

    //뽑기 로직 (쌍방 매칭: A가 B를 뽑으면 B도 자동으로 A와 맺어진다)
    @Transactional
    public DrawResponse drawRandomUser(String name, String phoneNumber) {
        User user = userRepository.findByNameAndPhoneNumber(name, phoneNumber)
                .orElseThrow(() -> new UserNotFoundException("해당 유저가 존재하지 않습니다."));

        // 이미 짝이 정해진 경우 (본인이 직접 뽑았든, 상대방이 나를 뽑아서 쌍방으로 맺어졌든)
        // → 새로 뽑지 않고 이미 맺어진 상대를 그대로 반환한다. 뽑기는 유저당 1회만 "결정"되고,
        //   그 이후엔 몇 번을 호출해도 같은 결과가 나와야 한다 (멱등성).
        if (user.getPartner() != null) {
            return DrawResponse.from(user.getPartner());
        }

        //뽑고싶은 성별에 맞는, 아직 아무와도 맺어지지 않은 후보만 조회 (자기 자신 제외)
        // + 상대방도 나의 성별을 원하는 경우만 후보로 포함 (양쪽 선호가 서로 맞아야 매칭됨).
        //   이 체크가 없으면 "내가 원하는 성별"만 보고, 그 상대가 실제로 원하는 성별은 무시한 채 매칭돼버린다.
        List<User> candidates = findByGender(user.getSelectGender()).stream()
                .filter(u -> !u.equals(user))
                .filter(u -> u.getPartner() == null)
                .filter(u -> u.getSelectGender().equals(user.getGender()))
                .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            throw new NoMatchingUserException("조건에 맞는 사용자가 없습니다.");
        }

        User drawnUser = selectRandomUser(candidates);

        // 쌍방 매칭: 나 → 상대, 상대 → 나를 동시에 맺어준다.
        user.setPartner(drawnUser);
        user.setDraw(true);

        drawnUser.setPartner(user);
        drawnUser.setDraw(true);
        drawnUser.setPicked(true);

        return DrawResponse.from(drawnUser);
    }

    private User selectRandomUser(List<User> users) {
        Random random = new Random();
        return users.get(random.nextInt(users.size()));
    }
}
