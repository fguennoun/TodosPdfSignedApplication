package be.cm.todoapplication.service;

import be.cm.todoapplication.dto.TodoDTO;
import be.cm.todoapplication.dto.UserDTO;
import be.cm.todoapplication.model.Todo;
import be.cm.todoapplication.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TodoService {

    private final TodoRepository todoRepository;
    private final RestTemplate restTemplate;
    private final TodoSyncService todoSyncService;

    private static final String TODOS_API = "https://jsonplaceholder.typicode.com/todos";
    private static final String USERS_API = "https://jsonplaceholder.typicode.com/users";

    public void syncFromJsonPlaceholder() {
        TodoDTO[] todoDTOs = restTemplate.getForObject(TODOS_API, TodoDTO[].class);
        UserDTO[] userDTOs = restTemplate.getForObject(USERS_API, UserDTO[].class);

        Map<Long, String> userMap = Arrays.stream(userDTOs)
                .collect(Collectors.toMap(UserDTO::getId, UserDTO::getName));

        for (TodoDTO dto : todoDTOs) {
            todoSyncService.syncTodo(dto, userMap);
        }
    }

    public List<Todo> getAllTodos() {
        return todoRepository.findAll();
    }

    public Optional<Todo> getTodoById(Long id) {
        return todoRepository.findById(id);
    }

    public Todo createTodo(Todo todo) {
        return todoRepository.save(todo);
    }

    @Transactional
    public Todo updateTodo(Long id, Todo todoDetails) {
        Todo todo = todoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Todo not found"));

        todo.setUsername(todoDetails.getUsername());
        todo.setTitle(todoDetails.getTitle());
        todo.setCompleted(todoDetails.getCompleted());

        return todoRepository.save(todo);
    }

    public void deleteTodo(Long id) {
        todoRepository.deleteById(id);
    }

    public List<UserDTO> getAllUsers() {
        UserDTO[] users = restTemplate.getForObject(USERS_API, UserDTO[].class);
        return Arrays.asList(users);
    }
}
