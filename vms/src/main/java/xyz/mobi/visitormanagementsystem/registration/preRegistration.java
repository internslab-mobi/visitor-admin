package xyz.mobi.visitormanagementsystem.registration;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
public class preRegistration {



        @GetMapping("/Registration")
        public String health() {
            return "registration up";
        }
    }
