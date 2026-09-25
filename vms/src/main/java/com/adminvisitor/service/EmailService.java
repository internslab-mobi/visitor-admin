package com.adminvisitor.service;

import com.adminvisitor.dto.responsedto.CancellationEmailData;
import com.adminvisitor.entity.Visit;
import com.adminvisitor.dto.responsedto.BadgeEmailData;
import com.adminvisitor.dto.responsedto.HostCheckInEmailData;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.springframework.scheduling.annotation.Async;

import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

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

    @Async
    public void sendVisitConfirmationEmail(Visit visit) {

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

        String visitorName =
                visit.getVisitor().getFirstName()
                        + " "
                        + visit.getVisitor().getLastName();

        Context context = new Context();

        context.setVariable("visitorName", visitorName);
        context.setVariable("visitReference", visit.getVisitReference());
        context.setVariable("purpose", visit.getPurpose());
        context.setVariable(
                "expectedArrival",
                visit.getExpectedArrivalAt().format(formatter)
        );
        context.setVariable(
                "expectedDeparture",
                visit.getExpectedDepartureAt().format(formatter)
        );
        context.setVariable("hostName", hostName);
        context.setVariable("departmentName", departmentName);

        // Measure Thymeleaf template processing separately.
        long templateStart = System.currentTimeMillis();

        String htmlBody = templateEngine.process(
                "emails/visit-confirmation",
                context
        );

        log.debug(
                "Timing: emailTemplateProcessing={} ms",
                System.currentTimeMillis() - templateStart
        );

        try {
            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(visit.getVisitor().getEmail());
            helper.setCc(hostEmail);

            helper.setSubject(
                    "Visit Registration Confirmation - "
                            + visit.getVisitReference()
            );

            helper.setText(htmlBody, true);

            // Measure the actual SMTP sending operation separately.
            long smtpStart = System.currentTimeMillis();

            mailSender.send(message);

            log.debug(
                    "Timing: smtpSend={} ms",
                    System.currentTimeMillis() - smtpStart
            );

        } catch (MessagingException exception) {

            throw new IllegalStateException(
                    "Failed to prepare visit confirmation email",
                    exception
            );
        }
    }

    @Async
    public void sendVisitBadgeEmail(BadgeEmailData data) {

        Context context = new Context();

        context.setVariable(
                "visitorName",
                data.visitorName()
        );

        context.setVariable(
                "visitReference",
                data.visitReference()
        );

        context.setVariable(
                "hostName",
                data.hostName()
        );

        context.setVariable(
                "issuedAt",
                data.issuedAt()
        );

        context.setVariable(
                "validUntil",
                data.validUntil()
        );

        /*
         * Visitor photo is not implemented yet.
         * For now this remains null and the template
         * displays a placeholder.
         */
        context.setVariable(
                "visitorPhoto",
                data.visitorPhoto()
        );

        String htmlBody =
                templateEngine.process(
                        "emails/visit-badge",
                        context
                );

        try {

            MimeMessage message =
                    mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(
                            message,
                            true,
                            "UTF-8"
                    );

            helper.setFrom(fromEmail);

            helper.setTo(
                    data.visitorEmail()
            );

            helper.setCc(hostEmail);

            helper.setSubject(
                    "Your Visitor Badge - "
                            + data.visitReference()
            );

            byte[] qrImageBytes =
                    Base64.getDecoder()
                            .decode(data.qrCode());

            helper.setText(
                    htmlBody,
                    true
            );

            helper.addInline(
                    "qrCode",
                    new ByteArrayResource(qrImageBytes),
                    "image/png"
            );

            mailSender.send(message);

            log.info(
                    "Visitor badge email sent successfully. visitReference={}, recipient={}",
                    data.visitReference(),
                    data.visitorEmail()
            );

        } catch (MessagingException exception) {

            log.error(
                    "Failed to send visitor badge email. visitReference={}, recipient={}",
                    data.visitReference(),
                    data.visitorEmail(),
                    exception
            );

            throw new IllegalStateException(
                    "Failed to prepare visitor badge email",
                    exception
            );
        }
    }

    @Async
    public void sendHostCheckInNotification(
            HostCheckInEmailData data
    ) {

        Context context = new Context();

        context.setVariable(
                "hostName",
                data.hostName()
        );

        context.setVariable(
                "visitorName",
                data.visitorName()
        );

        context.setVariable(
                "visitorType",
                data.visitorType()
        );

        context.setVariable(
                "companyName",
                data.companyName()
        );

        context.setVariable(
                "visitReference",
                data.visitReference()
        );

        context.setVariable(
                "purpose",
                data.purpose()
        );

        context.setVariable(
                "checkedInAt",
                data.checkedInAt()
        );

        String htmlBody =
                templateEngine.process(
                        "emails/host-check-in",
                        context
                );

        try {

            MimeMessage message =
                    mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(
                            message,
                            true,
                            "UTF-8"
                    );

            helper.setFrom(fromEmail);

            helper.setTo(
                    data.hostEmail()
            );

            helper.setSubject(
                    "Visitor Checked In - "
                            + data.visitReference()
            );

            helper.setText(
                    htmlBody,
                    true
            );

            mailSender.send(message);

            log.info(
                    "Host check-in notification sent successfully. visitReference={}, hostEmail={}",
                    data.visitReference(),
                    data.hostEmail()
            );

        } catch (MessagingException exception) {

            log.error(
                    "Failed to send host check-in notification. visitReference={}, hostEmail={}",
                    data.visitReference(),
                    data.hostEmail(),
                    exception
            );

            throw new IllegalStateException(
                    "Failed to send host check-in notification",
                    exception
            );
        }
    }

    @Async
    public void sendVisitCancellationEmail(CancellationEmailData data) {

        Context context = new Context();

        context.setVariable("visitorName", data.visitorName());
        context.setVariable("visitReference", data.visitReference());
        context.setVariable("purpose", data.purpose());
        context.setVariable("expectedArrival", data.expectedArrival());
        context.setVariable("expectedDeparture", data.expectedDeparture());
        context.setVariable("hostName", data.hostName());

        String htmlBody = templateEngine.process(
                "emails/visit-cancellation",
                context
        );

        try {
            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(
                            message,
                            true,
                            "UTF-8"
                    );

            helper.setFrom(fromEmail);
            helper.setTo(data.visitorEmail());

            helper.setSubject(
                    "Visit Cancellation - "
                            + data.visitReference()
            );

            helper.setText(htmlBody, true);

            mailSender.send(message);

        } catch (MessagingException exception) {

            throw new IllegalStateException(
                    "Failed to prepare visit cancellation email",
                    exception
            );
        }
    }

}