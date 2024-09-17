package io.kit.ai.rabbit;

import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RabbitMQListener {
    @Autowired
    ImageGenerator imageGenerator;

    @RabbitListener(queues = "test_queue")
    public void receiveMessage(String message) {
        try {
            ImageGenerator.Message m = JSONObject.parseObject(message, ImageGenerator.Message.class);
            imageGenerator.generate(m);
        } catch (Exception e) {
            log.warn(e.getMessage() + message);
        }
    }
}
