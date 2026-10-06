package com.example.taskmanager;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {
    private final TaskRepository repository;

    public TaskService(TaskRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> list(String status) {
        List<Task> tasks = switch (status) {
            case "all" -> repository.findAll(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
            case "active" -> repository.findAllByCompletedOrderByCreatedAtDescIdDesc(false);
            case "completed" -> repository.findAllByCompletedOrderByCreatedAtDescIdDesc(true);
            default -> throw new IllegalArgumentException("未知的筛选状态：" + status);
        };
        return tasks.stream().map(TaskResponse::from).toList();
    }

    @Transactional
    public TaskResponse create(TaskRequest request) {
        Task task = new Task(request.title().trim(), normalize(request.description()));
        return TaskResponse.from(repository.save(task));
    }

    @Transactional
    public TaskResponse update(Long id, UpdateTaskRequest request) {
        Task task = repository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
        task.update(request.title().trim(), normalize(request.description()), request.completed());
        return TaskResponse.from(task);
    }

    @Transactional
    public void delete(Long id) {
        Task task = repository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
        repository.delete(task);
    }

    private String normalize(String description) {
        return description == null ? "" : description.trim();
    }
}
