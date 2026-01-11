USE agri_db;
USE agri_db;

-- =======================================================
-- 模块一：空间维度 - 区域风险分布表
-- 用于：ECharts 中国地图热力展示
-- =======================================================
DROP TABLE IF EXISTS `pest_distribution`;
CREATE TABLE `pest_distribution` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `province` VARCHAR(50) NOT NULL COMMENT '省份名称',
    `pest_count` INT DEFAULT 0 COMMENT '监测到的病害样本数',
    `risk_level` INT DEFAULT 1 COMMENT '风险等级(1:低 - 5:极高)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- [预置数据] 模拟全国各地的病害上报数据
INSERT INTO `pest_distribution` (province, pest_count, risk_level) VALUES
('湖北', 120, 5), ('湖南', 115, 5), ('江西', 95, 4), ('安徽', 80, 4),
('江苏', 78, 3), ('黑龙江', 60, 3), ('河南', 105, 5), ('山东', 88, 4),
('四川', 92, 4), ('广东', 70, 3), ('广西', 65, 3), ('云南', 50, 2),
('浙江', 45, 2), ('福建', 40, 2), ('河北', 85, 4), ('新疆', 30, 1);


-- =======================================================
-- 模块二：时间维度 - 农事季节预警表
-- 用于：ECharts 全年趋势折线图
-- =======================================================
DROP TABLE IF EXISTS `pest_calendar`;
CREATE TABLE `pest_calendar` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `month` INT NOT NULL COMMENT '月份 (1-12)',
    `high_risk_pest` VARCHAR(100) NOT NULL COMMENT '当月主要高发病害',
    `risk_index` INT DEFAULT 0 COMMENT '综合爆发指数 (0-100)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- [预置数据] 模拟全年的病虫害爆发趋势
INSERT INTO `pest_calendar` (month, high_risk_pest, risk_index) VALUES
(1, '越冬螟虫基数', 15), 
(2, '小麦纹枯病始发', 25), 
(3, '小麦蚜虫/红蜘蛛', 50),
(4, '小麦赤霉病', 85), 
(5, '一代二化螟', 90), 
(6, '稻飞虱迁入', 95),
(7, '稻纹枯病/稻瘟病', 98), 
(8, '玉米螟/卷叶螟', 92), 
(9, '稻飞虱回迁', 80),
(10, '棉铃虫', 65), 
(11, '地下害虫', 35), 
(12, '越冬虫卵调查', 20);


-- =======================================================
-- 模块三：行为维度 - 用户搜索埋点表
-- 用于：实时热搜榜 / 词云
-- =======================================================
DROP TABLE IF EXISTS `search_logs`;
CREATE TABLE `search_logs` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `keyword` VARCHAR(100) COMMENT '用户搜索词',
    `search_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '搜索时间',
    `client_ip` VARCHAR(50) DEFAULT '127.0.0.1' COMMENT '来源IP'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- [预置数据] 模拟一些初始热度，避免图表在演示时是空的
INSERT INTO search_logs (keyword, client_ip) VALUES 
('稻瘟病', '192.168.1.101'), ('稻瘟病', '192.168.1.102'), ('稻瘟病', '192.168.1.103'),
('草地贪夜蛾', '192.168.1.104'), ('草地贪夜蛾', '192.168.1.105'),
('红蜘蛛', '192.168.1.106'), ('红蜘蛛', '192.168.1.107'), 
('小麦赤霉病', '192.168.1.108'), ('蚜虫', '192.168.1.109'),
('玉米螟', '192.168.1.110'), ('枯萎病', '192.168.1.111');