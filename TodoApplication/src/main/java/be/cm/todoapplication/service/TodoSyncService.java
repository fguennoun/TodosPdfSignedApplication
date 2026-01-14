package be.cm.todoapplication.service;

import be.cm.todoapplication.dto.TodoDTO;
import be.cm.todoapplication.model.Todo;
import be.cm.todoapplication.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TodoSyncService {

    private final TodoRepository todoRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void syncTodo(TodoDTO dto, Map<Long, String> userMap) {
        int maxRetries = 3;
        int attempts = 0;

        while (attempts < maxRetries) {
            try {
                Optional<Todo> existingTodoOptional = todoRepository.findById(dto.getId());
                if (existingTodoOptional.isPresent()) {
                    Todo todo = existingTodoOptional.get();
                    todo.setUserId(dto.getUserId());
                    todo.setUsername(userMap.get(dto.getUserId()));
                    todo.setTitle(dto.getTitle());
                    todo.setCompleted(dto.getCompleted());
                    todoRepository.save(todo);
                } else {
                    Todo newTodo = new Todo();
                    newTodo.setId(dto.getId());
                    newTodo.setUserId(dto.getUserId());
                    newTodo.setUsername(userMap.get(dto.getUserId()));
                    newTodo.setTitle(dto.getTitle());
                    newTodo.setCompleted(dto.getCompleted());
                    todoRepository.save(newTodo);
                }
                return; // Succès
            } catch (ObjectOptimisticLockingFailureException e) {
                attempts++;
                if (attempts >= maxRetries) {
                    System.err.println("Failed to sync todo " + dto.getId() + " after " + maxRetries + " attempts");
                    return;
                }
                // Attendre avant de réessayer
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
}

