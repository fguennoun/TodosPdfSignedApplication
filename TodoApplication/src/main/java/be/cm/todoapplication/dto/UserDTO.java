package be.cm.todoapplication.dto;

import lombok.Data;

@Data
public class UserDTO {
    private Long id;
    private String name;
    private String username;
    private String email;
    private String phone;
    private String website;
}