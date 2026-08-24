package dreamdays.Helf.domain.user.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    // 뽑기 상대(쌍방 매칭). A가 B를 뽑으면 A.partner=B, B.partner=A 로 동시에 맺어진다.
    // /api/users/all 응답에서 상대방 엔티티까지 통째로 직렬화되면 순환 참조(A->B->A->...)로
    // StackOverflow가 나기 때문에 JsonIgnore로 막는다. 매칭 상대 정보는 DrawResponse로만 노출한다.
    @ManyToOne
    @JoinColumn(name = "partner_id", unique = true)
    @JsonIgnore
    private User partner;
}
