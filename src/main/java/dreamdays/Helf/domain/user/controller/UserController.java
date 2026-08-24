package dreamdays.Helf.domain.user.controller;

import dreamdays.Helf.domain.user.dto.CheckInfoResponse;
import dreamdays.Helf.domain.user.dto.InfoRequest;
import dreamdays.Helf.domain.user.entity.User;
import dreamdays.Helf.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class UserController {
    private final UserService userService;

    @PostMapping("/users/create-info")
    public ResponseEntity<String> postInfoUser(@Valid @RequestBody InfoRequest infoRequest) {
        userService.saveUser(infoRequest);
        return ResponseEntity.ok("정보 입력 완료");
    }

    @GetMapping("/users/check-info")
    public ResponseEntity<CheckInfoResponse> getCheckUser(
            @RequestParam String name,
            @RequestParam String phoneNumber
    ) {
        CheckInfoResponse response = userService.findByNameAndPhoneNumber(name, phoneNumber);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/all")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }
}
