package com.example.agrisearch;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

@MultipartConfig(
        fileSizeThreshold = 1024 * 1024 * 2,
        maxFileSize = 1024 * 1024 * 10,
        maxRequestSize = 1024 * 1024 * 20
)
@WebServlet("/addPests") // 统一使用这个已经验证过的路径
public class AddPestsServlet extends HttpServlet {

    private static final String UPLOAD_DIR = "images";

    /**
     * 【处理删除】当前端发送 fetch('./addPests?deleteId=xxx') 时触发
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        String deleteId = request.getParameter("deleteId");

        if (deleteId != null) {
            String sql = "DELETE FROM pests_diseases WHERE id = ?";
            try (Connection conn = DBUtils.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setInt(1, Integer.parseInt(deleteId));
                int rows = pstmt.executeUpdate();

                if (rows > 0) {
                    response.getWriter().write("{\"success\":true,\"message\":\"删除成功\"}");
                } else {
                    response.getWriter().write("{\"success\":false,\"message\":\"未找到记录\"}");
                }
            } catch (Exception e) {
                e.printStackTrace();
                response.getWriter().write("{\"success\":false,\"message\":\"数据库错误\"}");
            }
        }
    }

    /**
     * 【处理添加】当前端通过表单提交数据时触发
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        request.setCharacterEncoding("UTF-8");

        String cropId = request.getParameter("cropId");
        String diseaseName = request.getParameter("diseaseName");
        String symptoms = request.getParameter("symptoms");
        String prevention = request.getParameter("prevention");

        try {
            // 路径处理
            String applicationPath = getServletContext().getRealPath("");
            String uploadPath = applicationPath + File.separator + UPLOAD_DIR;
            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists()) uploadDir.mkdirs();

            // 文件处理
            Part filePart = request.getPart("imageFile");
            String fileName = "default.jpg";
            if (filePart != null && filePart.getSize() > 0) {
                String originalFileName = filePart.getSubmittedFileName();
                String suffix = originalFileName.substring(originalFileName.lastIndexOf("."));
                fileName = UUID.randomUUID().toString() + suffix;
                filePart.write(uploadPath + File.separator + fileName);
            }

            // 数据库路径（写死正斜杠）
            String imageUrl = UPLOAD_DIR + "/" + fileName;

            // 插入数据库
            try (Connection conn = DBUtils.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                         "INSERT INTO pests_diseases (crop_id, name, symptoms, prevention, image_url) VALUES (?, ?, ?, ?, ?)")) {

                pstmt.setInt(1, Integer.parseInt(cropId));
                pstmt.setString(2, diseaseName);
                pstmt.setString(3, symptoms);
                pstmt.setString(4, prevention);
                pstmt.setString(5, imageUrl);
                pstmt.executeUpdate();

                response.getWriter().write("{\"success\":true}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().write("{\"success\":false,\"message\":\"添加失败\"}");
        }
    }
}