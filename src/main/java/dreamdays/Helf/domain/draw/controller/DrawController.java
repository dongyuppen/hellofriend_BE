package dreamdays.Helf.domain.draw.controller;

import dreamdays.Helf.domain.draw.dto.DrawRequest;
import dreamdays.Helf.domain.draw.dto.DrawResponse;
import dreamdays.Helf.domain.draw.service.DrawService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class DrawController {
    private final DrawService drawService;

    @PostMapping("/draw")
    public ResponseEntity<DrawResponse> postRandomDraw(@RequestBody DrawRequest request) {
        DrawResponse response = drawService.drawRandomUser(request.getName(), request.getPhoneNumber());
        return ResponseEntity.ok(response);
    }
}
