package com.aldianfazrihady;

import com.aldianfazrihady.model.Role;
import com.aldianfazrihady.model.User;
import com.aldianfazrihady.repository.RoleRepository;
import com.aldianfazrihady.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@SpringBootApplication
public class AppCompilerWebApplication {
    public static void main(String[] args) {
        SpringApplication.run(AppCompilerWebApplication.class, args);
    }

    @Bean
    public CommandLineRunner initializeData(UserRepository userRepo, RoleRepository roleRepo) {
        return new CommandLineRunner() {
            @Override
            public void run(String... strings) throws Exception {
                List<Role> roles = roleRepo.findByName("USER");
                if (roles.size() < 1) {
                    roleRepo.save(new Role("USER"));
                }

                User user = userRepo.findByUsername("aldian");
                if (user == null) {
                    PasswordEncoder encoder = new BCryptPasswordEncoder();
                    user = new User("aldian", encoder.encode("password"));
                    user = userRepo.save(user);
                }
                if (user.getRoles().size() < 1) {
                    roles = roleRepo.findByName("USER");
                    if (roles.size() > 0) {
                        user.getRoles().add(roles.get(0));
                        userRepo.save(user);
                    }
                }
            }
        };
    }
}
