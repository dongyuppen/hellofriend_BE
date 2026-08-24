package dreamdays.Helf.domain.user.dto;

import dreamdays.Helf.domain.user.entity.User;
import lombok.Getter;

@Getter
public class CheckInfoResponse {
    private final String name;
    private final String phoneNumber;
    private final boolean isDraw;

    public CheckInfoResponse(String name, String phoneNumber, boolean isDraw) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.isDraw = isDraw;
    }

    public static CheckInfoResponse from(User user) {
        return new CheckInfoResponse(user.getName(), user.getPhoneNumber(), user.isDraw());
    }
}
