package com.imran.client.controller;

import com.imran.client.entity.User;
import com.imran.client.entity.VerificationToken;
import com.imran.client.event.RegistrationCompleteEvent;
import com.imran.client.model.UserModel;
import com.imran.client.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

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
    public String resetPassword(@RequestBody){

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
