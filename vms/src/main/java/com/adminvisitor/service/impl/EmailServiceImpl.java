package com.adminvisitor.service.impl;

import com.adminvisitor.dto.responsedto.BadgeEmailData;
import com.adminvisitor.dto.responsedto.CancellationEmailData;
import com.adminvisitor.dto.responsedto.HostCheckInEmailData;
import com.adminvisitor.dto.responsedto.HostDailyReminderEmailData;
import com.adminvisitor.entity.Visit;
import com.adminvisitor.exception.EmailDeliveryException;
import com.adminvisitor.service.EmailService;
import com.adminvisitor.util.DateTimeFormats;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    @Override
    public void sendVisitConfirmationEmail(Visit visit) {

        String visitorName =
                visit.getVisitor().getFirstName() + " "
                        + visit.getVisitor().getLastName();

        String hostName =
                visit.getHost().getFirstName() + " "
                        + visit.getHost().getLastName();

        String hostEmail = visit.getHost().getEmail();

        String departmentName =
                visit.getHost().getDepartment().getDepartmentName();

        Context context = new Context();
        context.setVariable("visitorName", visitorName);
        context.setVariable("visitReference", visit.getVisitReference());
        context.setVariable("purpose", visit.getPurpose());

        context.setVariable(
                "expectedArrival",
                visit.getExpectedArrivalAt()
                        .format(DateTimeFormats.DISPLAY_DATE_TIME)
        );

        context.setVariable(
                "expectedDeparture",
                visit.getExpectedDepartureAt()
                        .format(DateTimeFormats.DISPLAY_DATE_TIME)
        );

        context.setVariable("hostName", hostName);
        context.setVariable("departmentName", departmentName);

        long templateStart = System.currentTimeMillis();
        final String htmlBody;

        try {
            htmlBody = templateEngine.process(
                    "emails/visit-confirmation",
                    context
            );
        } catch (RuntimeException exception) {
            throw new EmailDeliveryException(
                    "Failed to prepare visit confirmation email content.",
                    exception
            );
        } finally {
            log.debug(
                    "Timing: emailTemplateProcessing={} ms",
                    System.currentTimeMillis() - templateStart
            );
        }

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

            long smtpStart = System.currentTimeMillis();

            try {
                mailSender.send(message);
            } finally {
                log.debug(
                        "Timing: smtpSend={} ms",
                        System.currentTimeMillis() - smtpStart
                );
            }

        }
        catch (MessagingException exception) {
            throw new EmailDeliveryException(
                    "Failed to prepare visit confirmation email message.",
                    exception
            );
        } catch (MailException exception) {
            throw new EmailDeliveryException(
                    "Failed to deliver visit confirmation email.",
                    exception
            );
        }

    }

    @Async
    @Override
    public void sendVisitBadgeEmail(BadgeEmailData data) {

        Context context = new Context();
        context.setVariable("visitorName", data.visitorName());
        context.setVariable("visitReference", data.visitReference());
        context.setVariable("hostName", data.hostName());
        context.setVariable("issuedAt", data.issuedAt());
        context.setVariable("validUntil", data.validUntil());
        context.setVariable("visitorPhoto", data.visitorPhoto());

        final String htmlBody;

        try {
            htmlBody = templateEngine.process(
                    "emails/visit-badge",
                    context
            );
        } catch (RuntimeException exception) {
            throw new EmailDeliveryException(
                    "Failed to prepare visitor badge email content.",
                    exception
            );
        }

        final byte[] qrImageBytes;

        try {
            qrImageBytes = Base64.getDecoder().decode(data.qrCode());
        } catch (IllegalArgumentException exception) {
            throw new EmailDeliveryException(
                    "Failed to prepare visitor badge email because the QR code data is invalid.",
                    exception
            );
        }

        FileSystemResource visitorPhoto =
                new FileSystemResource(data.visitorPhoto());

        if (!visitorPhoto.exists() || !visitorPhoto.isReadable()) {
            throw new EmailDeliveryException(
                    "Failed to prepare visitor badge email because the visitor photo is unavailable."
            );
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(data.visitorEmail());
            helper.setCc(data.hostEmail());
            helper.setSubject(
                    "Your Visitor Badge - " + data.visitReference()
            );
            helper.setText(htmlBody, true);

            helper.addInline(
                    "qrCode",
                    new ByteArrayResource(qrImageBytes),
                    "image/png"
            );

            helper.addInline(
                    "visitorPhoto",
                    visitorPhoto,
                    "image/jpeg"
            );

            mailSender.send(message);

        } catch (MessagingException exception) {
            throw new EmailDeliveryException(
                    "Failed to prepare visitor badge email message.",
                    exception
            );
        } catch (MailException exception) {
            throw new EmailDeliveryException(
                    "Failed to deliver visitor badge email.",
                    exception
            );
        }
    }

    @Async
    @Override
    public void sendHostCheckInNotification(
            HostCheckInEmailData data
    ) {

        Context context = new Context();
        context.setVariable("hostName", data.hostName());
        context.setVariable("visitorName", data.visitorName());
        context.setVariable("visitorType", data.visitorType());
        context.setVariable("companyName", data.companyName());
        context.setVariable("visitReference", data.visitReference());
        context.setVariable("purpose", data.purpose());
        context.setVariable("checkedInAt", data.checkedInAt());

        final String htmlBody;

        try {
            htmlBody = templateEngine.process(
                    "emails/host-check-in",
                    context
            );
        } catch (RuntimeException exception) {
            throw new EmailDeliveryException(
                    "Failed to prepare host check-in notification content.",
                    exception
            );
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(data.hostEmail());
            helper.setSubject(
                    "Visitor Checked In - " + data.visitReference()
            );
            helper.setText(htmlBody, true);

            mailSender.send(message);

        } catch (MessagingException exception) {
            throw new EmailDeliveryException(
                    "Failed to prepare host check-in notification message.",
                    exception
            );
        } catch (MailException exception) {
            throw new EmailDeliveryException(
                    "Failed to deliver host check-in notification.",
                    exception
            );
        }
    }

    @Async
    @Override
    public void sendVisitCancellationEmail(
            CancellationEmailData data
    ) {

        Context context = new Context();
        context.setVariable("visitorName", data.visitorName());
        context.setVariable("visitReference", data.visitReference());
        context.setVariable("purpose", data.purpose());
        context.setVariable("expectedArrival", data.expectedArrival());
        context.setVariable("expectedDeparture", data.expectedDeparture());
        context.setVariable("hostName", data.hostName());

        final String htmlBody;

        try {
            htmlBody = templateEngine.process(
                    "emails/visit-cancellation",
                    context
            );
        } catch (RuntimeException exception) {
            throw new EmailDeliveryException(
                    "Failed to prepare visit cancellation email content.",
                    exception
            );
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(data.visitorEmail());
            helper.setSubject(
                    "Visit Cancellation - " + data.visitReference()
            );
            helper.setText(htmlBody, true);

            mailSender.send(message);

        } catch (MessagingException exception) {
            throw new EmailDeliveryException(
                    "Failed to prepare visit cancellation email message.",
                    exception
            );
        } catch (MailException exception) {
            throw new EmailDeliveryException(
                    "Failed to deliver visit cancellation email.",
                    exception
            );
        }
    }

    @Async
    @Override
    public void sendHostDailyReminderEmail(
            HostDailyReminderEmailData data
    ) {

        Context context = new Context();
        context.setVariable("hostName", data.hostName());
        context.setVariable("reminderDate", data.reminderDate());
        context.setVariable("visits", data.visits());

        final String htmlBody;

        try {
            htmlBody = templateEngine.process(
                    "emails/host-daily-reminder",
                    context
            );
        } catch (RuntimeException exception) {
            throw new EmailDeliveryException(
                    "Failed to prepare host daily reminder email content.",
                    exception
            );
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(data.hostEmail());
            helper.setSubject(
                    "Today's Expected Visitors - " + data.reminderDate()
            );
            helper.setText(htmlBody, true);

            mailSender.send(message);

        } catch (MessagingException exception) {
            throw new EmailDeliveryException(
                    "Failed to prepare host daily reminder email message.",
                    exception
            );
        } catch (MailException exception) {
            throw new EmailDeliveryException(
                    "Failed to deliver host daily reminder email.",
                    exception
            );
        }
    }
}
