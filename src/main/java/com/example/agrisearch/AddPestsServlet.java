package com.example.agrisearch;

// 关键修改：将javax.servlet替换为jakarta.servlet
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

// 支持文件上传的注解配置
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024 * 2, // 2MB缓冲区
        maxFileSize = 1024 * 1024 * 5,      // 单个文件最大5MB
        maxRequestSize = 1024 * 1024 * 10   // 请求总大小最大10MB
)
@WebServlet("/addPests")
public class AddPestsServlet extends HttpServlet {
    // 图片存储路径（对应前端image_url字段，需与主页图片加载路径一致）
    private static final String UPLOAD_DIR = "images";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        request.setCharacterEncoding("UTF-8");

        // 1. 获取表单文本数据
        String cropId = request.getParameter("cropId");
        String diseaseName = request.getParameter("diseaseName");
        String symptoms = request.getParameter("symptoms");
        String prevention = request.getParameter("prevention");

        // 2. 处理图片上传
        String uploadPath = getServletContext().getRealPath("") + File.separator + UPLOAD_DIR;
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs(); // 若目录不存在则创建
        }

        Part filePart = request.getPart("imageFile");
        String originalFileName = filePart.getSubmittedFileName();
        // 生成唯一文件名（避免重复）
        String uniqueFileName = UUID.randomUUID().toString() + originalFileName.substring(originalFileName.lastIndexOf("."));
        String imageUrl = UPLOAD_DIR + File.separator + uniqueFileName;
        String filePath = uploadPath + File.separator + uniqueFileName;

        // 3. 保存图片到服务器
        filePart.write(filePath);

        // 4. 插入数据到数据库
        String sql = "INSERT INTO pests_diseases (crop_id, name, symptoms, prevention, image_url) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, Integer.parseInt(cropId));
            pstmt.setString(2, diseaseName);
            pstmt.setString(3, symptoms);
            pstmt.setString(4, prevention);
            pstmt.setString(5, imageUrl);

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                response.getWriter().write("{\"success\":true,\"message\":\"数据添加成功\"}");
            } else {
                response.getWriter().write("{\"success\":false,\"message\":\"数据插入失败，请重试\"}");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            response.getWriter().write("{\"success\":false,\"message\":\"数据库错误：" + e.getMessage() + "\"}");
        } catch (NumberFormatException e) {
            response.getWriter().write("{\"success\":false,\"message\":\"作物分类选择错误\"}");
        }
    }
}