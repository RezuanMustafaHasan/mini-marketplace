package com.hasan.marketplace.repository;

import com.hasan.marketplace.entity.Role;
import com.hasan.marketplace.entity.RoleName;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);

    boolean existsByName(RoleName name);
}

