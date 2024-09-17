package io.kit.ai.rabbit;

import com.alibaba.fastjson2.JSONObject;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RabbitMQListener {
    @Autowired
    ImageGenerator imageGenerator;

    @RabbitListener(queues = "test_queue")
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag){
        try {
            channel.basicAck(tag, false);
            ImageGenerator.Message m = JSONObject.parseObject(message, ImageGenerator.Message.class);
            imageGenerator.generate(m);
        } catch (Exception e) {
            log.warn(e.getMessage() + message);
            try {
                channel.basicNack(tag, false, true);
            } catch (Exception e1) {
                log.warn(e1.getMessage() + message);
            }
        }
    }
}
