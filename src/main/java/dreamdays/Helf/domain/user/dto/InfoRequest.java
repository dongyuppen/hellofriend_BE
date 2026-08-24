package dreamdays.Helf.domain.user.dto;

import dreamdays.Helf.domain.user.entity.User;
import dreamdays.Helf.domain.user.entity.enums.Gender;
import dreamdays.Helf.domain.user.entity.enums.Mbti;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class InfoRequest {
    @NotBlank
    private String name;

    @NotBlank
    @Pattern(regexp = "^\\d{11}$", message = "전화번호는 숫자만 11자리여야 합니다.")
    private String phoneNumber;

    @NotBlank
    private String instagramId;

    private int age;
    private Gender gender;

    @NotBlank
    private String school;

    private Mbti mbti;
    private Gender selectGender;
    private String bio;

    public User toEntity() {
        return User.builder()
                .name(this.name)
                .phoneNumber(this.phoneNumber)
                .instagramId(this.instagramId)
                .age(this.age)
                .gender(this.gender)
                .school(this.school)
                .mbti(this.mbti)
                .selectGender(this.selectGender)
                .bio(this.bio)
                .picked(false)
                .isDraw(false)
                .build();
    }
}
