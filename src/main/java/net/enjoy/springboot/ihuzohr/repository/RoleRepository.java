package net.enjoy.springboot.ihuzohr.repository;

import net.enjoy.springboot.ihuzohr.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Role findByName(String name);
}