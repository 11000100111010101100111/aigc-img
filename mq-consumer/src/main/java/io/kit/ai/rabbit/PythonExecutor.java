package io.kit.ai.rabbit;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson2.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class PythonExecutor {
    @Value("${mq.pyPath}")
    String pyPath;

    @Value("${mq.scriptPath}")
    String scriptPath;

    @Value("${mq.outPutPath}")
    String outPutPath;

    @Value("${mq.url.escalation}")
    String urlEscalation;

    @Value("${mq.url.upload}")
    String urlUpload;

    @Value("${mq.url.token}")
    String token;

    @Resource(name = "threadPoolTaskExecutor")
    private ThreadPoolTaskExecutor executor;

    @Value("${mq.serverUrl.upload}")
    String serverUploadUrl;
    @Value("${mq.serverUrl.step}")
    String serverStepUrl;

    public void execute(String processId, ImageGenerator.Message message) {
        String describe = Optional.ofNullable(message.getText()).orElse("A futuristic city with flying cars at sunset");
        File file = new File(outPutPath + "\\" + processId + "\\img_" + processId + ".png");
        if (!file.getParentFile().exists() || !file.getParentFile().isDirectory()) {
            file.getParentFile().mkdirs();
        }
        String absolutePath = file.getAbsolutePath();
        Process process = null;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    this.pyPath,
                    this.scriptPath,
                    "--name", absolutePath,
                    "--depth", "50",
                    "--scale", "15",
                    "--height", Optional.ofNullable(message.getHeight()).orElse("512"),
                    "--width", Optional.ofNullable(message.getWidth()).orElse("512"),
                    "--module", "CompVis/stable-diffusion-v1-4",
                    "--prompt", describe);
            processBuilder.redirectErrorStream(true);

            process = processBuilder.start();
            try (InputStreamReader streamReader = new InputStreamReader(process.getInputStream());
                    BufferedReader reader = new BufferedReader(streamReader)) {
                String line;
                ProcessInfo info = new ProcessInfo();
                info.setProcessId(processId);
                info.setType("1");
                info.setMark("生成进行中，请稍后");
                while ((line = reader.readLine()) != null) {
                    if (!ProgressParser.analyse(line, info)) continue;
                    if (line.startsWith("Loading pipeline components...: ")) {
                        info.setTitle("模型准备");
                    } else {
                        info.setTitle("深度推理");
                    }
                    // 上报进度
                    pushProcess(info);
                }
            }
            // 等待脚本执行完成
            process.waitFor();
            //System.out.println("Exited with code: " + exitCode);
            //上传生成结果
            pushData(processId, absolutePath);
        } catch (IOException | InterruptedException e) {
            ProcessInfo info = new ProcessInfo();
            info.setProcessId(processId);
            info.setTitle("生成失败");
            info.setPercentage("100%");
            info.setType("9");
            info.setMark(e.getMessage());
            this.pushProcess(info);
            log.warn(e.getMessage());
        } finally {
            try {
                Optional.ofNullable(process).ifPresent(Process::destroy);
            } catch (Exception e) {
                log.warn(e.getMessage());
            }
        }
    }

    /**
     * 异步推送生成进度
     * */
    protected void pushProcess(ProcessInfo info) {
        info.setTime(System.currentTimeMillis());
        executor.execute(() -> {
            log.info("[进度上报开始] " + info.toString());
            HttpResponse execute = null;
            try {
                execute = HttpRequest.post(serverStepUrl)
                        .body(JSONObject.toJSONString(info))
                        .execute();
                log.info("[进度上报结束] " +execute.body());
            } catch (Exception e) {
                log.warn(e.getMessage());
            } finally {
                Optional.ofNullable(execute).ifPresent(HttpResponse::close);
            }
        });
    }

    /**
     * 推送生成结果，上传文件
     * */
    protected void pushData(String processId, String path) {
        ProcessInfo info = new ProcessInfo();
        info.setProcessId(processId);
        info.setTime(System.currentTimeMillis());
        File file = new File(path);
        executor.execute(() -> {
            if (!file.exists()) {
                info.setTitle("生成失败");
                info.setPercentage("0%");
                info.setType("9");
                info.setMark("图像生成失败：模型未生成");
            } else {
                info.setTitle("生成成功");
                info.setPercentage("100%");
                info.setType("2");
                info.setMark("图像生成完成");
                HttpResponse execute = null;
                log.info("[推演结果上传开始] " + path);
                try {
                    execute = HttpRequest.post(serverUploadUrl)
                            .form("processId", processId)
                            .form("file", file)
                            .execute();
                    log.info("[推演结果上传结束] " +execute.body());
                } catch (Exception e) {
                    log.warn(e.getMessage());
                }  finally {
                    Optional.ofNullable(execute).ifPresent(HttpResponse::close);
                }
            }
            this.pushProcess(info);
        });
    }

    @Data
    public static class ProcessInfo {
        String processId;
        String type = "0"; //处理结果，0：等待处理，1：处理中，2：处理完成，9：处理失败
        String title = "-";
        String percentage = "0%";
        String mark = "";
        String currentStep = "-";
        String totalSteps = "-";
        String estimatedTime = "0"; //剩余执行时间
        String elapsedTime = "0"; //累计执行时间
        long time;

        @Override
        public String toString() {
            return String.format("作业ID：%s, 进度百分比: %s(%s/%s), 剩余执行时间：%s，累计执行时间：%s, 信息：%s-%s, 当前时间：%s",
                    Optional.ofNullable(processId).orElse("-"),
                    Optional.ofNullable(percentage).orElse("-"),
                    Optional.ofNullable(currentStep).orElse("-"),
                    Optional.ofNullable(totalSteps).orElse("-"),
                    Optional.ofNullable(estimatedTime).orElse("-"),
                    Optional.ofNullable(elapsedTime).orElse("-"),
                    Optional.ofNullable(title).orElse("-"),
                    Optional.ofNullable(mark).orElse("-"),
                    time);
        }
    }

    public static class ProgressParser {
        ProgressParser() {}
        public static final String REGEX = "(.*?)(\\d+)%\\|(.*?)\\|\\s*(\\d+)/(\\d+)\\s*\\[(\\d{2}:\\d{2})<(\\d{2}:\\d{2}),\\s+(\\d+\\.\\d+)(.*?)\\](.*?)";
        public static boolean analyse(String progressText, ProcessInfo info) {
            Pattern pattern = Pattern.compile(REGEX);
            Matcher matcher = pattern.matcher(progressText.trim());
            if (!matcher.matches()) {
               return false;
            }
            String percentage = matcher.group(2);
            String currentStep = matcher.group(4);
            String totalSteps = matcher.group(5);
            String elapsedTime = matcher.group(6);
            String estimatedTime = matcher.group(7);
            info.setPercentage(percentage + "%");
            info.setCurrentStep(currentStep);
            info.setTotalSteps(totalSteps);
            info.setElapsedTime(elapsedTime);
            info.setEstimatedTime(estimatedTime);
            return true;
        }
    }
}
