package com.adminvisitor.service;

import com.adminvisitor.entity.Visit;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import com.adminvisitor.entity.VisitBadge;
import org.springframework.core.io.ByteArrayResource;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
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

        String htmlBody = templateEngine.process(
                "emails/visit-confirmation",
                context
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

            mailSender.send(message);

        } catch (MessagingException exception) {

            throw new IllegalStateException(
                    "Failed to prepare visit confirmation email",
                    exception
            );
        }
    }


    public void sendVisitBadgeEmail(
            Visit visit,
            VisitBadge badge,
            String qrCode,
            String visitorPhoto
    ) {

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

        String visitorName =
                visit.getVisitor().getFirstName()
                        + " "
                        + visit.getVisitor().getLastName();

        Context context = new Context();

        context.setVariable("visitorName", visitorName);
        context.setVariable(
                "visitReference",
                visit.getVisitReference()
        );

        context.setVariable(
                "hostId",
                visit.getHostId()
        );

        context.setVariable(
                "issuedAt",
                badge.getIssuedAt().format(formatter)
        );

        context.setVariable(
                "validFrom",
                badge.getValidFrom().format(formatter)
        );

        context.setVariable(
                "validUntil",
                badge.getValidUntil().format(formatter)
        );

        /*
         * Visitor photo is not implemented yet.
         * For now this remains null and the template
         * displays a placeholder.
         */
        context.setVariable(
                "visitorPhoto",
                visitorPhoto
        );

        String htmlBody = templateEngine.process(
                "emails/visit-badge",
                context
        );

        System.out.println("========== BADGE EMAIL BODY ==========");
        System.out.println(htmlBody);
        System.out.println("======================================");

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
                    visit.getVisitor().getEmail()
            );

            helper.setCc(hostEmail);

            helper.setSubject(
                    "Your Visitor Badge - "
                            + visit.getVisitReference()
            );

            /*
             * Convert the Base64 QR code generated by
             * QrCodeService back into PNG bytes.
             */
            byte[] qrImageBytes =
                    java.util.Base64.getDecoder()
                            .decode(qrCode);

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

        } catch (MessagingException exception) {

            throw new IllegalStateException(
                    "Failed to prepare visitor badge email",
                    exception
            );
        }
    }
}