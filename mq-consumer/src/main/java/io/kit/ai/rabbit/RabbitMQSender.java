package io.kit.ai.rabbit;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RabbitMQSender {
    @Value("${mq.exchange}")
    String exchange;
    @Value("${mq.routing}")
    String routing;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void sendMessage(String message) {
        rabbitTemplate.convertAndSend(exchange, routing, message);
        System.out.println("发送消息: " + message);
    }
}
