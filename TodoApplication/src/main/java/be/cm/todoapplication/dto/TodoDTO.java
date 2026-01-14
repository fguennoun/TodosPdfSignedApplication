package be.cm.todoapplication.dto;

import lombok.Data;

@Data
public class TodoDTO {
    private Long id;
    private Long userId;
    private String title;
    private Boolean completed;
}