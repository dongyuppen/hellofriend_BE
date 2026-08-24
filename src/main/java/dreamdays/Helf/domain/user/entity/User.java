package dreamdays.Helf.domain.user.entity;

import dreamdays.Helf.domain.user.entity.enums.Gender;
import dreamdays.Helf.domain.user.entity.enums.Mbti;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@Getter @Setter
@Table(name = "User")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "student_id")
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 11)
    private String phoneNumber;

    @Column(nullable = false)
    private String instagramId;

    @Column(nullable = false)
    private int age;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Gender gender;

    @Column(nullable = false)
    private String school;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Mbti mbti;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Gender selectGender;

    private String bio;
    private boolean picked;
    private boolean isDraw;
}
