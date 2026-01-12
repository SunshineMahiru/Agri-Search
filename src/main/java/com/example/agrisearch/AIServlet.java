package com.example.agrisearch;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@WebServlet("/aiProxy")
public class AIServlet extends HttpServlet {

    // 填入你的配置
    private static final String API_KEY = "";
    private static final String MODEL_ID = "doubao-seed-1-6-251015";
    private static final String ARK_URL = "https://ark.cn-beijing.volces.com/api/v3/chat/completions";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 设置响应头，允许前端接收 JSON
        response.setContentType("application/json;charset=UTF-8");
        request.setCharacterEncoding("UTF-8");

        // 1. 读取前端传过来的症状描述
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        String userSymptoms = sb.toString();

        try {
            // 2. 构造发送给豆包 API 的 JSON 字符串
            // 注意：这里使用了转义字符来确保 JSON 格式正确
            String jsonPayload = "{"
                    + "\"model\": \"" + MODEL_ID + "\","
                    + "\"messages\": ["
                    + "  {\"role\": \"system\", \"content\": \"你是一个农业专家。请根据用户描述的作物症状进行诊断。必须只返回一个纯 JSON 字符串，格式如下：{ \\\"name\\\": \\\"病害名称\\\", \\\"crop\\\": \\\"作物名称\\\", \\\"confidence\\\": 95, \\\"analysis\\\": \\\"详细病理分析\\\", \\\"prevention\\\": \\\"具体的防治建议\\\" }\"},"
                    + "  {\"role\": \"user\", \"content\": \"" + userSymptoms + "\"}"
                    + "]"
                    + "}";

            // 3. 建立与火山引擎服务器的连接
            URL url = new URL(ARK_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
            conn.setDoOutput(true);

            // 4. 发送数据
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // 5. 获取并读取 AI 的返回结果
            int status = conn.getResponseCode();
            InputStream is = (status < 400) ? conn.getInputStream() : conn.getErrorStream();

            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder aiResponse = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    aiResponse.append(responseLine.trim());
                }
                // 直接将 AI 的原始 JSON 返回给前端浏览器
                response.getWriter().write(aiResponse.toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            response.getWriter().write("{\"success\":false, \"message\":\"后端代理请求 AI 失败\"}");
        }
    }
}