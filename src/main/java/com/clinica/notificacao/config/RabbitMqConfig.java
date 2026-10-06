package com.clinica.notificacao.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RabbitMqConfig {

    public static final String EXCHANGE = "clinica.eventos";

    public static final String FILA_AGENDADA  = "clinica.notificacao.consulta.agendada";
    public static final String FILA_CANCELADA  = "clinica.notificacao.consulta.cancelada";
    public static final String FILA_REAGENDADA = "clinica.notificacao.consulta.reagendada";
    public static final String FILA_LEMBRETE   = "clinica.notificacao.lembrete.gerado";

    public static final String DLX = "clinica.eventos.dlx";
    public static final String FILA_DLQ = "clinica.notificacao.dlq";


    @Bean
    public FanoutExchange deadLetterExchange() {
        return new FanoutExchange(DLX, true, false);
    }

    @Bean
    public Binding dlqBinding(Queue dlq, FanoutExchange deadLetterExchange) {
        return BindingBuilder.bind(dlq).to(deadLetterExchange);
    }


    @Bean
    public TopicExchange eventosExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue agendadaQueue() {
        return QueueBuilder.durable(FILA_AGENDADA)
                .deadLetterExchange(DLX)
                .build();
    }
    @Bean
    public Queue canceladaQueue() {
        return QueueBuilder.durable(FILA_CANCELADA)
                .deadLetterExchange(DLX)
                .build();
    }

    @Bean
    public Queue reagendadaQueue() {
        return QueueBuilder.durable(FILA_REAGENDADA)
                .deadLetterExchange(DLX)
                .build();
    }

    @Bean
    public Queue lembreteQueue() {
        return QueueBuilder.durable(FILA_LEMBRETE)
                .deadLetterExchange(DLX)
                .build();
    }

    @Bean
    public Binding agendadaBinding(Queue agendadaQueue, TopicExchange eventosExchange) {
        return BindingBuilder.bind(agendadaQueue).to(eventosExchange).with("consulta.agendada");
    }

    @Bean
    public Binding canceladaBinding(Queue canceladaQueue, TopicExchange eventosExchange) {
        return BindingBuilder.bind(canceladaQueue).to(eventosExchange).with("consulta.cancelada");
    }

    @Bean
    public Binding reagendadaBinding(Queue reagendadaQueue, TopicExchange eventosExchange) {
        return BindingBuilder.bind(reagendadaQueue).to(eventosExchange).with("consulta.reagendada");
    }

    @Bean
    public Binding lembreteBinding(Queue lembreteQueue, TopicExchange eventosExchange) {
        return BindingBuilder.bind(lembreteQueue).to(eventosExchange).with("lembrete.gerado");
    }


    @Bean
    public Queue dlq() {
        return new Queue(FILA_DLQ, true);
    }


    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}


