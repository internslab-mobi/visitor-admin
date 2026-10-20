package com.adminvisitor.notification.controller;

import com.adminvisitor.notification.dto.request.NotificationRequest;
import com.adminvisitor.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/email")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void sendEmail(
            @Valid @RequestBody NotificationRequest request
    ) {
        notificationService.sendNotification(request);
    }
}