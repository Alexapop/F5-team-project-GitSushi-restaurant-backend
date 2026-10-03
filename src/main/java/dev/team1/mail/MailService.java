package dev.team1.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${frontend-domain}")
    private String frontendDomain;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOrderInTransitEmail(String to, Long orderId, String ticketAccessToken) {
        String trackingUrl = frontendDomain + "/tickets/" + orderId + "?token=" + ticketAccessToken;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Your order #" + orderId + " is on its way!");
        message.setText("Hi,\n\nYour order #" + orderId
                + " has just left the restaurant and is on its way to you.\n\n"
                + "Track your order here: " + trackingUrl + "\n\n"
                + "Thanks for ordering with GitSushi!");
        mailSender.send(message);
    }
}