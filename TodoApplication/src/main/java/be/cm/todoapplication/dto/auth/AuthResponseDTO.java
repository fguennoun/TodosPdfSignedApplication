package be.cm.todoapplication.dto.auth;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AuthResponseDTO {
    private String token;
    private String type = "Bearer";
    private String username;
    private String email;
    private String role;

    public AuthResponseDTO() {}

    public AuthResponseDTO(String token, String type, String username, String email, String role) {
        this.token = token;
        this.type = type;
        this.username = username;
        this.email = email;
        this.role = role;
    }
}
