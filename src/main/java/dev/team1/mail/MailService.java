package dev.team1.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private static final Logger logger = LoggerFactory.getLogger(MailService.class);
    private static final int MAX_ATTEMPTS = 3;

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

        sendWithRetry(message, orderId);
    }

    private void sendWithRetry(SimpleMailMessage message, Long orderId) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                mailSender.send(message);
                return;
            } catch (MailException e) {
                logger.error("Failed to send in-transit email for order #{} (attempt {}/{}): {}",
                        orderId, attempt, MAX_ATTEMPTS, e.getMessage());
                if (attempt == MAX_ATTEMPTS) {
                    logger.error("Giving up sending in-transit email for order #{} after {} attempts",
                            orderId, MAX_ATTEMPTS);
                }
            }
        }
    }
}