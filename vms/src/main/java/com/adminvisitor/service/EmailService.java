package com.adminvisitor.service;

import com.adminvisitor.entity.Visit;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${vms.notification.host-email}")
    private String hostEmail;

    /*
     * Temporary values until Employee/Department tables are available.
     */
    @Value("${vms.notification.host-name}")
    private String hostName;

    @Value("${vms.notification.department-name}")
    private String departmentName;

    public void sendVisitConfirmationEmail(Visit visit) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(fromEmail);

        message.setTo(visit.getVisitor().getEmail());

        message.setCc(hostEmail);

        message.setSubject("Visit Registration Confirmation - "
                + visit.getVisitReference());

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

        String visitorName =
                visit.getVisitor().getFirstName()
                        + " "
                        + visit.getVisitor().getLastName();

        String emailBody =
                "Dear " + visitorName + ",\n\n" +

                        "Your visit has been successfully registered.\n\n" +

                        "Visit Details\n" +
                        "------------------------------\n" +
                        "Visit Reference  : " + visit.getVisitReference() + "\n" +
                        "Purpose          : " + visit.getPurpose() + "\n" +
                        "Expected Arrival : "
                        + visit.getExpectedArrivalAt().format(formatter) + "\n" +
                        "Expected Departure: "
                        + visit.getExpectedDepartureAt().format(formatter) + "\n" +
                        "Host             : " + hostName + "\n" +
                        "Department       : " + departmentName + "\n" +
                        "------------------------------\n\n" +

                        "Please carry your ID proof for verification during check-in.\n\n" +

                        "Regards,\n" +
                        "Visitor Management System";

        message.setText(emailBody);

        mailSender.send(message);
    }
}