package fullstack2.journalsystem_user.controller;

import fullstack2.journalsystem_user.Models.CreateUserModel;
import fullstack2.journalsystem_user.Models.LocalUser;
import fullstack2.journalsystem_user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.fasterxml.jackson.databind.type.LogicalType.Map;

@RestController
public class TestController {

    private final UserService userService;

    public TestController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/getUserByUserName")
    public LocalUser getUserByUserName(@RequestParam String username) {
        System.out.println("Testing query for user: " + username);
        try {
            return userService.findUserByUsername(username);
        } catch (Exception e) {
            System.out.println("Error communicating with Keycloak: " + e.getMessage());
        }
        return null;
    }

    @GetMapping("/login")
    public LocalUser login(@RequestParam String username, @RequestParam String password) {
        try {
            System.out.println("Testing login for user: " + username);
            System.out.println("Testing login for password: " + password);
            LocalUser user = userService.verifyAndGetUser(username, password);
            System.out.println(user.toString());
            return user;
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    public record LoginRequest(String username, String password) {}

    @PostMapping("/signup")
    public LocalUser signUp(@RequestBody CreateUserModel request) {
        try {
            userService.registerNewUser(request);
            return userService.findUserByUsername(request.getUsername());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    public record UserRegistrationRequest(
            String username,
            String email,
            String password,
            String firstName,
            String lastName,
            String role
    ) {}
}