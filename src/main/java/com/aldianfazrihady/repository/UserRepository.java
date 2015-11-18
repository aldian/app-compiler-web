package com.aldianfazrihady.repository;

import com.aldianfazrihady.model.User;
import org.springframework.data.repository.CrudRepository;

/**
 * Created by AldianFazrihady on 11/14/15.
 */
public interface UserRepository extends CrudRepository<User, Long> {
    User findByUsernameAndPassword(String username, String password);
    User findByUsername(String username);
    User findByWsToken(String wsToken);
}
