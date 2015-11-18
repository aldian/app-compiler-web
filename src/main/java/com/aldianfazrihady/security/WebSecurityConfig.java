package com.aldianfazrihady.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.servlet.configuration.EnableWebMvcSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by AldianFazrihady on 11/13/15.
 */

@Configuration
@EnableWebMvcSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
    @Autowired
    @Qualifier("userDetailsService")
    public UserDetailsService userDetailsService;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        RequestMatcher csrfRequestMatcher = new RequestMatcher() {
            private RegexRequestMatcher requestMatcher = new RegexRequestMatcher("/ws/.*", null);
            @Override
            public boolean matches(HttpServletRequest httpServletRequest) {
                if (requestMatcher.matches(httpServletRequest)) {
                    // Disable CSRF
                    return false;
                }
                return false;
            }
        };

        http
                .csrf().requireCsrfProtectionMatcher(csrfRequestMatcher).and()
                .authorizeRequests().antMatchers("/registration", "/ws/*").permitAll().anyRequest().authenticated().and()
                .formLogin().loginPage("/login").permitAll().usernameParameter("username").passwordParameter("password").and()
                .logout().logoutUrl("/logout").permitAll().logoutSuccessUrl("/login");
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
