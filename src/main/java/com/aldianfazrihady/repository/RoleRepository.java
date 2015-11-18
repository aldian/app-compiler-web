package com.aldianfazrihady.repository;

import com.aldianfazrihady.model.Role;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

/**
 * Created by AldianFazrihady on 11/14/15.
 */
public interface RoleRepository extends CrudRepository<Role, Long> {
    List<Role> findByName(String name);
}
