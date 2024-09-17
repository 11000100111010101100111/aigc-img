package io.kit.ai.rabbit;

import com.alibaba.fastjson2.JSONObject;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Map;

@RestController
public class RabbitMQTestController {

    @Resource
    private RabbitMQSender rabbitMQSender;

    @PostMapping("/send")
    public String send(@RequestBody Map<String, Object> map) {
        String message = JSONObject.toJSONString(map);
        rabbitMQSender.sendMessage(message);
        return "消息已发送: " + message;
    }
}
