package com.aldianfazrihady.service;

import com.aldianfazrihady.model.User;
import com.aldianfazrihady.repository.RoleRepository;
import com.aldianfazrihady.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Created by AldianFazrihady on 11/14/15.
 */
@Service
@Transactional
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    public Iterable<User> findAll() {
        return userRepository.findAll();
    }

    public User create(User user) {
        user = userRepository.save(user);
        user.getRoles().add(roleRepository.findByName("USER").get(0));
        user = userRepository.save(user);
        return user;
    }

    public User findById(long id) {
        return userRepository.findOne(id);
    }

    public User login(String username, String password) {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        User user = userRepository.findByUsername(username);
        if (encoder.matches(password, user.getPassword())) {
            return user;
        }
        return null;
    }

    public User update(User user) {
        return userRepository.save(user);
    }

    public void deleteUser(long id) {
        userRepository.delete(id);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User generateWebServiceToken(User user) {
        user.setWsToken(UUID.randomUUID().toString());
        return userRepository.save(user);
    }

    public User findByWsToken(String wsToken) {
        return userRepository.findByWsToken(wsToken);
    }
}
