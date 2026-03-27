package com.hasan.marketplace.service;

import com.hasan.marketplace.dto.UserRegistrationRequest;
import com.hasan.marketplace.entity.RoleName;
import com.hasan.marketplace.entity.User;

public interface UserService {

    User registerUser(UserRegistrationRequest request);

    User findByEmail(String email);

    User getAuthenticatedUser();

    boolean hasRole(User user, RoleName roleName);
}
