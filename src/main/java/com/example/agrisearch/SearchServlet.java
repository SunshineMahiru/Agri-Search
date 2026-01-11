package com.example.agrisearch;

import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/searchPests")
public class SearchServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        String keyword = req.getParameter("keyword");
        List<Map<String, Object>> list = new ArrayList<>();

        try (Connection conn = DBUtils.getConnection()) {
            String sql = "SELECT * FROM pests_diseases WHERE name LIKE ? OR symptoms LIKE ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            String searchStr = (keyword == null || keyword.isEmpty()) ? "%%" : "%" + keyword + "%";
            pstmt.setString(1, searchStr);
            pstmt.setString(2, searchStr);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", rs.getInt("id"));
                map.put("name", rs.getString("name"));
                map.put("cropId", rs.getInt("crop_id"));
                map.put("symptoms", rs.getString("symptoms"));
                map.put("prevention", rs.getString("prevention"));
                map.put("imageUrl", rs.getString("image_url"));
                list.add(map);
            }
            // ============================================================
            // 【个人贡献 Start】：大数据埋点 - 异步记录用户搜索行为
            //  说明：此逻辑用于支撑 dashboard 的“用户行为分析”模块
            // ============================================================
            if (keyword != null && !keyword.trim().isEmpty()) {
                // 使用独立的 try-catch，确保日志记录失败不会导致搜索功能崩溃
                try {
                    // 注意：这里不需要重开 conn，因为 conn 还在 try-with-resources 范围内
                    // 但为了保险起见，如果 DBUtils.getConnection() 返回新连接也可以
                    String logSql = "INSERT INTO search_logs (keyword, client_ip) VALUES (?, ?)";
                    try (PreparedStatement logStmt = conn.prepareStatement(logSql)) {
                        logStmt.setString(1, keyword); // 记录关键词
                        logStmt.setString(2, req.getRemoteAddr()); // 记录IP
                        logStmt.executeUpdate();
                    }
                } catch (SQLException e) {
                    System.err.println("[警告] 搜索日志记录失败: " + e.getMessage());
                    // 吃掉异常，保证前端能正常收到搜索结果
                }
            }
            // ============================================================
            // 【个人贡献 End】
            // ============================================================
            resp.getWriter().write(new Gson().toJson(list));
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("[]");
        }
    }
}