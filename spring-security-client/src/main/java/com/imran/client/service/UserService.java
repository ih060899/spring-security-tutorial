package com.imran.client.service;

import com.imran.client.entity.User;
import com.imran.client.model.UserModel;

public interface UserService {
    User registerUser(UserModel userModel);
}
