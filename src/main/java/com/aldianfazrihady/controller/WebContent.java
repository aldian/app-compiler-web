package com.aldianfazrihady.controller;

import com.aldianfazrihady.api.AppCompilerWSAPI;
import com.aldianfazrihady.model.CompilationResult;
import com.aldianfazrihady.model.User;
import com.aldianfazrihady.security.SecurityUser;
import com.aldianfazrihady.service.CompilationResultService;
import com.aldianfazrihady.service.UserService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Created by AldianFazrihady on 11/13/15.
 */
@Controller
public class WebContent {
    @Autowired
    private UserService userService;

    @Autowired
    private CompilationResultService compilationResultService;

    @Value("${app.ws.host}")
    private String wsHost;

    @Value("${app.ws.port}")
    private int wsPort;

    @RequestMapping("/")
    public String home(Model model, HttpSession session) {
        String msg = (String) session.getAttribute("msg");
        if (msg != null) {
            model.addAttribute("msg", msg);
            session.removeAttribute("msg");
        }
        Long logId = (Long) session.getAttribute("logId");
        System.out.println("LOG ID: " + logId);
        if (logId != null) {
            CompilationResult res = compilationResultService.findById(logId);
            System.out.println("WILL DISPLAY COMPILATION RESULT: " + res);
            model.addAttribute("compilationResult", res);
            session.removeAttribute("logId");
        }
        return "home";
    }

    @RequestMapping("/login")
    public String login(Model model, HttpSession session) {
        String msg = (String) session.getAttribute("msg");
        if (msg != null) {
            model.addAttribute("msg", msg);
            session.removeAttribute("msg");
        }
        return "login";
    }

    @RequestMapping("/registration")
    public String registration(Model model) {
        model.addAttribute("user", new User());
        return "registration";
    }

    @RequestMapping(value = "/registration", method = {RequestMethod.POST})
    public String registrationPost(@ModelAttribute User user, Model model, HttpSession session) {
        User oldUser = userService.findByUsername(user.getUsername());
        if (oldUser != null) {
            model.addAttribute("msg", "Please choose different username");
            return "registration";
            //return "redirect:/login";
        }
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        user.setPassword(encoder.encode(user.getPassword()));
        userService.create(user);
        session.setAttribute("msg", "Registration successful. Please login.");
        return "redirect:/login";
    }

    @RequestMapping(value = "/upload", method = {RequestMethod.POST})
    public String handleFileUpload(@RequestParam("file") MultipartFile file, Model model, HttpSession session) throws IOException {
        if (file.isEmpty()) {
            session.setAttribute("msg", "You failed to upload " + file.getOriginalFilename() + " because the file was empty");
        } else {
            byte[] bytes = file.getBytes();
            SecurityUser secUser = (SecurityUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            User user = userService.findByUsername(secUser.getUsername());
            System.out.println("UPLOADING USER: " + user);
            System.out.println("UPLOADING USER CLASS: " + user.getClass());
            user = userService.generateWebServiceToken(user);
            AppCompilerWSAPI api = new AppCompilerWSAPI(wsHost, wsPort);
            api.setAccessToken(user.getWsToken());
            String compilationLog = api.compile(bytes);
            JSONObject jsonObj = new JSONObject(compilationLog);
            session.setAttribute("logId", jsonObj.getLong("id"));
            session.setAttribute("msg", "Successfully uploaded " + file.getOriginalFilename());
        }
        return "redirect:/";
    }
}
