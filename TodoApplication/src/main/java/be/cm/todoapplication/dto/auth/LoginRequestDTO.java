package be.cm.todoapplication.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class LoginRequestDTO {

    @NotBlank(message = "Le nom d'utilisateur ou email est obligatoire")
    private String username; // peut être username ou email

    @NotBlank(message = "Le mot de passe est obligatoire")
    private String password;

    public LoginRequestDTO() {}

    public LoginRequestDTO(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
