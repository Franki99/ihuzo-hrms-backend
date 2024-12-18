package net.enjoy.springboot.ihuzohr.controller;

import net.enjoy.springboot.ihuzohr.dto.UserDto;
import net.enjoy.springboot.ihuzohr.entity.User;
import net.enjoy.springboot.ihuzohr.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<Page<User>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "firstName,asc") String sort) {

        String[] sortParams = sort.split(",");
        String sortField = sortParams[0];
        String sortDirection = sortParams.length > 1 ? sortParams[1] : "asc";

        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Sort sorting;

        // Special handling for role sorting
        if (sortField.equals("roles")) {
            sorting = Sort.by(direction, "roles[0].name");
        } else {
            sorting = Sort.by(direction, sortField);
        }

        Pageable pageable = PageRequest.of(page, size, sorting);

        System.out.println("Applying sort: field=" + sortField + ", direction=" + direction);

        Page<User> users;
        try {
            if (search != null && !search.isEmpty()) {
                users = userService.searchUsers(search, pageable);
            } else {
                users = userService.findAllUsers(pageable);
            }
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            System.err.println("Error during sorting: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            UserDto userDto = userService.getUserById(id);
            return ResponseEntity.ok(userDto);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UserDto userDto) {
        try {
            userDto.setId(id); // Ensure the ID is set
            userService.updateUser(userDto);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating user: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok().build();
    }
}