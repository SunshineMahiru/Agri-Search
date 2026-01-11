package com.example.agrisearch;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    // 模拟数据库：存储账号和哈希密码
    private static final Map<String, String> userDatabase = new ConcurrentHashMap<>();

    static {
        // 初始化默认管理员：admin / 123456
        // 123456 的哈希值（对应你前端 btoa 传输后的处理）
        userDatabase.put("admin", "8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        String action = request.getParameter("action");
        String username = request.getParameter("username");
        String passwordBase64 = request.getParameter("password"); // 接收前端 btoa 值

        Map<String, Object> res = new HashMap<>();

        // 1. 解码并哈希处理
        byte[] decodedBytes = Base64.getDecoder().decode(passwordBase64);
        String rawPassword = new String(decodedBytes, StandardCharsets.UTF_8);
        String hashedPassword = sha256(rawPassword);

        if ("addAccount".equals(action)) {
            // 权限校验
            String operator = request.getParameter("operator");
            if (!"admin".equals(operator)) {
                res.put("success", false);
                res.put("message", "权限不足：非管理员无法授权新用户");
            } else {
                // 将新用户存入模拟数据库
                userDatabase.put(username, hashedPassword);
                saveSecurityLog(username, hashedPassword, "ADMIN_CREATE_USER");
                res.put("success", true);
                res.put("message", "用户 [" + username + "] 授权成功");
            }
        } else {
            // 登录验证：去模拟数据库里查
            if (userDatabase.containsKey(username) && userDatabase.get(username).equals(hashedPassword)) {
                HttpSession session = request.getSession(true);
                session.setAttribute("user", username);
                saveSecurityLog(username, hashedPassword, "LOGIN_SUCCESS");
                res.put("success", true);
            } else {
                saveSecurityLog(username, hashedPassword, "LOGIN_FAILED");
                res.put("success", false);
                res.put("message", "账号未激活或密钥错误");
            }
        }
        response.getWriter().print(new Gson().toJson(res));
    }

    // 保存日志到 WEB-INF/security_logs.txt
    private void saveSecurityLog(String user, String hash, String type) throws IOException {
        String path = getServletContext().getRealPath("/WEB-INF/security_logs.txt");
        File file = new File(path);
        if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
        try (PrintWriter writer = new PrintWriter(new FileWriter(path, true))) {
            String time = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            writer.println("[" + time + "] [" + type + "] User: " + user + " | Hash: " + hash);
        }
    }

    private String sha256(String base) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) { return "HASH_ERROR"; }
    }
}