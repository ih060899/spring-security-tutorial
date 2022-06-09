package com.imran.client.controller;

import com.imran.client.entity.User;
import com.imran.client.entity.VerificationToken;
import com.imran.client.event.RegistrationCompleteEvent;
import com.imran.client.model.PasswordModel;
import com.imran.client.model.UserModel;
import com.imran.client.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;

@RestController
@Slf4j
public class RegistrationController {

    @Autowired
    private UserService userService;

    @Autowired
    private ApplicationEventPublisher publisher;

    @PostMapping("/register")
    public String registeruser(@RequestBody UserModel userModel, final HttpServletRequest request){
        User user = userService.registerUser(userModel);
        publisher.publishEvent(new RegistrationCompleteEvent(
                user, applicationUrl(request)
        ));
        return "Success";
    }

    @GetMapping("/verifyRegistration")
    public String verifyRegistration(@RequestParam("token") String token){
            String result = userService.validateVerificationToken(token);

            if(result.equalsIgnoreCase("valid"))
                return "User verified Successfully";

            return "Bad User";
    }

    @GetMapping("/resendVerifyToken")
    public String resendVerificationToken(@RequestParam("token") String oldToken, HttpServletRequest request, Object VerificationToken){
        VerificationToken verificationToken = userService.generateNewVerificationToken(oldToken);
        User user = verificationToken.getUser();
        resendVerificationTokenMail(user, applicationUrl(request), verificationToken);
        return "Verification Link Send";
    }

    @PostMapping("/resetPassword")
    public String resetPassword(@RequestBody PasswordModel passwordModel, HttpServletRequest request){
            User user = userService.findUserByEmail(passwordModel.getEmail());
            String url = "";
            if(user != null){
                String token = UUID.randomUUID().toString();
                userService.createPasswordResetTokenForUser(user, token);
                url = passwordResetTokenMail(user, applicationUrl(request), token);
            }
            return url;
    }

    @PostMapping("/savePassword")
    public String savePassword(@RequestParam("token") String token,
                               @RequestBody PasswordModel passwordModel){
        String result = userService.validatePasswordResetToken(token);
        if(!result.equalsIgnoreCase("valid")){
            return "Invalid Token";
        }
        Optional<User> user = userService.getUserByPasswordResetToken(token);
        if(user.isPresent()){
            userService.changePassword(user.get(), passwordModel.getNewPassword());
            return "Password Reset Successfully";
        }
        else
            return "Invalid Token";

    }

    @PostMapping("/changePassword")
    public String changePassword(@RequestBody PasswordModel passwordModel){
        User user = userService.findUserByEmail(passwordModel.getEmail());
        if(user == null)
            return "invalid user";
        if(!userService.checkIfValidOldPassword(user, passwordModel.getOldPassword())){
           return "Invalid Old Password";
        }

        // Save New Password
        userService.changePassword(user, passwordModel.getNewPassword());
        return "Password Changed Successfully";

    }

    private String passwordResetTokenMail(User user, String applicationUrl, String token) {
        // Send mail to user
        String url = applicationUrl + "/savePassword?token=" + token;

        // sendVerificationEmail()
        log.info("Click the link to reset your password : {}", url);

        return url;
    }

    private void resendVerificationTokenMail(User user, String applicationUrl, VerificationToken verificationToken) {
        // Send mail to user
        String url = applicationUrl + "/verifyRegistration?token=" + verificationToken.getToken();

        // sendVerificationEmail()
        log.info("Click the link to verify your account : {}", url);
    }

    private String applicationUrl(HttpServletRequest request) {
        return "http://" +
                request.getServerName() +
                ":" +
                request.getServerPort() +
                request.getContextPath();
    }
}
