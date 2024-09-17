package io.kit.ai.rabbit;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ImageGenerator {
    @Autowired
    PythonExecutor pythonExecutor;

    public void generate(Message message) {
        pythonExecutor.execute(message.processId, message);
    }

    @Data
    public static class Message {
        String processId;
        String text;
        String width;
        String height;
    }
}
