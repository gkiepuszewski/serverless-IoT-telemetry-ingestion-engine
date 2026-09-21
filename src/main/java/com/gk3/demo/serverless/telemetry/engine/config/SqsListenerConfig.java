package com.gk3.demo.serverless.telemetry.engine.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.listener.errorhandler.AsyncErrorHandler;
import io.awspring.cloud.sqs.listener.errorhandler.ErrorHandler;
import io.awspring.cloud.sqs.listener.interceptor.AsyncMessageInterceptor;
import io.awspring.cloud.sqs.listener.interceptor.MessageInterceptor;
import io.awspring.cloud.sqs.support.converter.MessagingMessageConverter;
import io.awspring.cloud.sqs.support.converter.SqsMessagingMessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

/**
 * Overrides the default auto-configured SQS container factory (Spring Cloud AWS) to add one
 * practical capability: controlling the listener's auto-start via the
 * {@code app.sqs.listener.auto-startup} property. This is useful in lightweight context tests
 * where we don't want the application to actually try connecting to SQS on startup.
 */
@Configuration
public class SqsListenerConfig {

    @Bean
    public SqsMessageListenerContainerFactory<Object> defaultSqsListenerContainerFactory(
            ObjectProvider<SqsAsyncClient> sqsAsyncClient,
            ObjectProvider<AsyncErrorHandler<Object>> asyncErrorHandler,
            ObjectProvider<ErrorHandler<Object>> errorHandler,
            ObjectProvider<AsyncMessageInterceptor<Object>> asyncInterceptors,
            ObjectProvider<MessageInterceptor<Object>> interceptors,
            ObjectProvider<ObjectMapper> objectMapperProvider,
            MessagingMessageConverter<?> messagingMessageConverter,
            @Value("${app.sqs.listener.auto-startup:true}") boolean autoStartup) {

        SqsMessageListenerContainerFactory<Object> factory = new SqsMessageListenerContainerFactory<>();
        sqsAsyncClient.ifAvailable(factory::setSqsAsyncClient);
        asyncErrorHandler.ifAvailable(factory::setErrorHandler);
        errorHandler.ifAvailable(factory::setErrorHandler);
        interceptors.forEach(factory::addMessageInterceptor);
        asyncInterceptors.forEach(factory::addMessageInterceptor);
        objectMapperProvider.ifAvailable(om -> {
            if (messagingMessageConverter instanceof SqsMessagingMessageConverter sqsConverter) {
                sqsConverter.setObjectMapper(om);
            }
        });
        factory.configure(options -> options
                .messageConverter(messagingMessageConverter)
                .autoStartup(autoStartup));
        return factory;
    }
}
