package com.example.agrisearch;

import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;
import java.util.*;

/**
 * 农业大数据决策看板数据接口
 * 功能：统一处理 GIS地图、农事日历、热搜行为的数据请求
 * 路由：/dashboard/data
 */
@WebServlet("/dashboard/data")
public class DashboardServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // 1. 设置响应头，确保中文不乱码，且返回的是 JSON
        resp.setContentType("application/json;charset=UTF-8");

        // 2. 获取前端想要的动作指令
        String action = req.getParameter("action");

        // 3. 路由分发 (Routing)
        try (Connection conn = DBUtils.getConnection()) {
            if ("map".equals(action)) {
                getMapData(conn, resp);      // 获取 GIS 分布数据
            } else if ("calendar".equals(action)) {
                getCalendarData(conn, resp); // 获取时间预警数据
            } else if ("logs".equals(action)) {
                getLogData(conn, resp);      // 获取热搜行为数据
            } else {
                resp.getWriter().write("{\"error\": \"Invalid Action\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("{\"error\": \"Database Connection Failed\"}");
        }
    }

    // --- 子功能 1：获取 GIS 地图数据 ---
    private void getMapData(Connection conn, HttpServletResponse resp) throws SQLException, IOException {
        String sql = "SELECT province, pest_count FROM pest_distribution";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<Map<String, Object>> list = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("name", rs.getString("province"));      // ECharts 地图对应省份名
                map.put("value", rs.getInt("pest_count"));      // 风险数值
                list.add(map);
            }
            resp.getWriter().write(new Gson().toJson(list));
        }
    }

    // --- 子功能 2：获取农事预警日历数据 ---
    private void getCalendarData(Connection conn, HttpServletResponse resp) throws SQLException, IOException {
        // 按月份正序排列
        String sql = "SELECT month, risk_index FROM pest_calendar ORDER BY month ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<Integer> months = new ArrayList<>();
            List<Integer> values = new ArrayList<>();
            while (rs.next()) {
                months.add(rs.getInt("month"));
                values.add(rs.getInt("risk_index"));
            }

            // 构造成 ECharts 需要的分类结构
            Map<String, Object> data = new HashMap<>();
            data.put("months", months);
            data.put("values", values);
            resp.getWriter().write(new Gson().toJson(data));
        }
    }

    // --- 子功能 3：获取热搜词云/排行数据 ---
    private void getLogData(Connection conn, HttpServletResponse resp) throws SQLException, IOException {
        // 聚合统计：计算每个关键词出现的次数，取前10名
        String sql = "SELECT keyword, COUNT(*) as cnt FROM search_logs " +
                "WHERE keyword IS NOT NULL AND keyword != '' " +
                "GROUP BY keyword " +
                "ORDER BY cnt DESC " +
                "LIMIT 10";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<Map<String, Object>> list = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("name", rs.getString("keyword"));
                map.put("value", rs.getInt("cnt"));
                list.add(map);
            }
            resp.getWriter().write(new Gson().toJson(list));
        }
    }
}