package com.hackathon.platform.service;

import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

@SpringJUnitConfig(AnnouncementEmailEventListenerIntegrationTest.TestConfig.class)
class AnnouncementEmailEventListenerIntegrationTest {
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private AnnouncementEmailDeliveryService emailService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        reset(emailService);
    }

    @Test
    void commitedEventTriggersEmailProcessing() {
        UUID eventId = UUID.randomUUID();
        UUID msgId = UUID.randomUUID();

        AnnouncementCreatedEvent event = new AnnouncementCreatedEvent(eventId, msgId);
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.executeWithoutResult(status -> eventPublisher.publishEvent(event));
        verify(emailService, timeout(2000)).processMessage(msgId);
    }

    @Configuration
    @EnableAsync
    @Import(AnnouncementEmailEventListener.class)
    static class TestConfig {
        @Bean
        AnnouncementEmailDeliveryService emailService() {
            return mock(AnnouncementEmailDeliveryService.class);
        }

        @Bean
        PlatformTransactionManager transactionManager() {
            return new TestTransactionManager();
        }
    }

    static class TestTransactionManager extends AbstractPlatformTransactionManager {
        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition def) {}

        @Override
        protected void doCommit(DefaultTransactionStatus status){}

        @Override
        protected void doRollback(DefaultTransactionStatus status){}
    }
}