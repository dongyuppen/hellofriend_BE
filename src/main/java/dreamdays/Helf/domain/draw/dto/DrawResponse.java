package dreamdays.Helf.domain.draw.dto;

import dreamdays.Helf.domain.user.entity.User;
import dreamdays.Helf.domain.user.entity.enums.Gender;
import dreamdays.Helf.domain.user.entity.enums.Mbti;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
public class DrawResponse {
    private String name;
    private int age;
    private String instagramId;
    private String school;
    private Gender gender;
    private Mbti mbti;
    private String bio;

    public static DrawResponse from(User user) {
        return DrawResponse.builder()
                .name(user.getName())
                .age(user.getAge())
                .instagramId(user.getInstagramId())
                .school(user.getSchool())
                .gender(user.getGender())
                .mbti(user.getMbti())
                .bio(user.getBio())
                .build();
    }
}
