package net.enjoy.springboot.ihuzohr.service;

import net.enjoy.springboot.ihuzohr.dto.UserDto;
import net.enjoy.springboot.ihuzohr.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {
    void saveUser(UserDto userDto);
    User findUserByEmail(String email);
    List<User> findAllUsers();
    Page<User> findAllUsers(Pageable pageable);
    Page<User> searchUsers(String search, Pageable pageable);
    UserDto getUserById(Long id);
    void updateUser(UserDto userDto);
    void deleteUser(Long id);
}