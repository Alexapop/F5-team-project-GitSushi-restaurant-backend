package dev.team1.mail;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private MailService mailService;

    private void setFrontendDomain() {
        ReflectionTestUtils.setField(mailService, "frontendDomain", "https://localhost:5173");
    }

    @Test
    void sendOrderInTransitEmailBuildsTrackingLinkAndSendsOnce() {
        setFrontendDomain();

        mailService.sendOrderInTransitEmail("customer@example.com", 42L, "token-123");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertTrue(sent.getTo() != null && sent.getTo()[0].equals("customer@example.com"));
        assertTrue(sent.getText().contains("https://localhost:5173/tickets/42?token=token-123"));
    }

    @Test
    void sendOrderInTransitEmailRetriesOnFailureThenSucceeds() {
        setFrontendDomain();
        doThrow(new MailSendException("temporary failure"))
                .doThrow(new MailSendException("temporary failure"))
                .doNothing()
                .when(mailSender).send(any(SimpleMailMessage.class));

        mailService.sendOrderInTransitEmail("customer@example.com", 42L, "token-123");

        verify(mailSender, times(3)).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendOrderInTransitEmailGivesUpAfterMaxAttemptsWithoutThrowing() {
        setFrontendDomain();
        doThrow(new MailSendException("permanent failure"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        mailService.sendOrderInTransitEmail("customer@example.com", 42L, "token-123");

        verify(mailSender, times(3)).send(any(SimpleMailMessage.class));
    }
}