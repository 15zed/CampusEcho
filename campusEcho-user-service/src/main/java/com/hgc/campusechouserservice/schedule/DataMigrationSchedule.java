package com.hgc.campusechouserservice.schedule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

@Component
@Slf4j
public class DataMigrationSchedule {
    @Autowired
    private DataSource dataSource;

    // 记录是否已经执行过数据迁移
    private boolean executed = false;

    // 设置每批次处理的记录数量
    private static final int BATCH_SIZE = 1000;


    @Scheduled(fixedDelay = Long.MAX_VALUE)
    public void migrateTask() {
        if (!executed) {
            // 执行数据迁移
            migrateDataFollowing();
            migrateDataFollower();
            executed = true;
        }
    }


    public void migrateDataFollowing() {
        PreparedStatement selectStmt = null;
        PreparedStatement insertStmt0 = null;
        PreparedStatement insertStmt1 = null;
        PreparedStatement deleteStmt = null;
        ResultSet rs = null;
        try (Connection connection = dataSource.getConnection()) {
            // 定义分页查询和插入SQL
            String selectSql = "SELECT * FROM t_following LIMIT ?, ?";
            String insertSql0 = "INSERT INTO t_following_0 (id, from_user_id, to_user_id, type, update_time) VALUES (?, ?, ?, ?, ?)";
            String insertSql1 = "INSERT INTO t_following_1 (id, from_user_id, to_user_id, type, update_time) VALUES (?, ?, ?, ?, ?)";
            String deleteSql = "DELETE FROM t_following WHERE id = ?";

            insertStmt0 = connection.prepareStatement(insertSql0);
            insertStmt1 = connection.prepareStatement(insertSql1);
            deleteStmt = connection.prepareStatement(deleteSql);

            int offset = 0;
            int batchCount = 0;

            while (true) {
                // 3. 批量查询数据
                selectStmt = connection.prepareStatement(selectSql);
                selectStmt.setInt(1, offset);
                selectStmt.setInt(2, BATCH_SIZE);
                rs = selectStmt.executeQuery();

                // 如果没有更多数据，退出循环
                if (!rs.next()) {
                    break;
                }

                // 4. 批量插入和删除数据
                do {
                    int id = rs.getInt("id");
                    int fromUserId = rs.getInt("from_user_id");
                    int toUserId = rs.getInt("to_user_id");
                    int type = rs.getInt("type");
                    int updateTime = rs.getInt("update_time");


                    // 根据路由规则选择分表
                    if (fromUserId % 2 == 0) {
                        insertStmt0.setInt(1, id);
                        insertStmt0.setInt(2, fromUserId);
                        insertStmt0.setInt(3, toUserId);
                        insertStmt0.setInt(4, type);
                        insertStmt0.setInt(5, updateTime);
                        insertStmt0.addBatch();
                    }
                    else {
                        insertStmt1.setInt(1, id);
                        insertStmt1.setInt(2, fromUserId);
                        insertStmt1.setInt(3, toUserId);
                        insertStmt1.setInt(4, type);
                        insertStmt1.setInt(5, updateTime);
                        insertStmt1.addBatch();
                    }

                    // 记录需要删除的主键
                    deleteStmt.setInt(1, id);
                    deleteStmt.addBatch();

                    batchCount++;

                    // 每当批次达到设定值时，执行批处理
                    if (batchCount % BATCH_SIZE == 0) {
                        executeBatch(insertStmt0, insertStmt1, deleteStmt);
                    }

                } while (rs.next());

                // 更新分页偏移量
                offset += BATCH_SIZE;
            }

            // 5. 处理剩余数据
            if (batchCount % BATCH_SIZE != 0) {
                executeBatch(insertStmt0, insertStmt1, deleteStmt);
            }

            log.info("数据迁移完成！");

        }
        catch (SQLException e) {
            log.error("获取数据库连接失败！", e.getMessage());
        }
        finally {
            // 6. 关闭资源
            try {
                if (rs != null) rs.close();
                if (selectStmt != null) selectStmt.close();
                if (insertStmt0 != null) insertStmt0.close();
                if (insertStmt1 != null) insertStmt1.close();
                if (deleteStmt != null) deleteStmt.close();
            }
            catch (SQLException e) {
                log.error("关闭资源失败！", e.getMessage());
            }
        }
    }

    public void migrateDataFollower() {
        PreparedStatement selectStmt = null;
        PreparedStatement insertStmt0 = null;
        PreparedStatement insertStmt1 = null;
        PreparedStatement deleteStmt = null;
        ResultSet rs = null;
        try (Connection connection = dataSource.getConnection()) {
            // 定义分页查询和插入SQL
            String selectSql = "SELECT * FROM t_follower LIMIT ?, ?";
            String insertSql0 = "INSERT INTO t_follower_0 (id, from_user_id, to_user_id, type, update_time) VALUES (?, ?, ?, ?, ?)";
            String insertSql1 = "INSERT INTO t_follower_1 (id, from_user_id, to_user_id, type, update_time) VALUES (?, ?, ?, ?, ?)";
            String deleteSql = "DELETE FROM t_follower WHERE id = ?";

            insertStmt0 = connection.prepareStatement(insertSql0);
            insertStmt1 = connection.prepareStatement(insertSql1);
            deleteStmt = connection.prepareStatement(deleteSql);

            int offset = 0;
            int batchCount = 0;

            while (true) {
                // 3. 批量查询数据
                selectStmt = connection.prepareStatement(selectSql);
                selectStmt.setInt(1, offset);
                selectStmt.setInt(2, BATCH_SIZE);
                rs = selectStmt.executeQuery();

                // 如果没有更多数据，退出循环
                if (!rs.next()) {
                    break;
                }

                // 4. 批量插入和删除数据
                do {
                    int id = rs.getInt("id");
                    int fromUserId = rs.getInt("from_user_id");
                    int toUserId = rs.getInt("to_user_id");
                    int type = rs.getInt("type");
                    int updateTime = rs.getInt("update_time");


                    // 根据路由规则选择分表
                    if (fromUserId % 2 == 0) {
                        insertStmt0.setInt(1, id);
                        insertStmt0.setInt(2, fromUserId);
                        insertStmt0.setInt(3, toUserId);
                        insertStmt0.setInt(4, type);
                        insertStmt0.setInt(5, updateTime);
                        insertStmt0.addBatch();
                    }
                    else {
                        insertStmt1.setInt(1, id);
                        insertStmt1.setInt(2, fromUserId);
                        insertStmt1.setInt(3, toUserId);
                        insertStmt1.setInt(4, type);
                        insertStmt1.setInt(5, updateTime);
                        insertStmt1.addBatch();
                    }

                    // 记录需要删除的主键
                    deleteStmt.setInt(1, id);
                    deleteStmt.addBatch();

                    batchCount++;

                    // 每当批次达到设定值时，执行批处理
                    if (batchCount % BATCH_SIZE == 0) {
                        executeBatch(insertStmt0, insertStmt1, deleteStmt);
                    }

                } while (rs.next());

                // 更新分页偏移量
                offset += BATCH_SIZE;
            }

            // 5. 处理剩余数据
            if (batchCount % BATCH_SIZE != 0) {
                executeBatch(insertStmt0, insertStmt1, deleteStmt);
            }
            log.info("数据迁移完成！");

        }
        catch (SQLException e) {
            log.error("获取数据库连接失败！", e.getMessage());
        }
        finally {
            // 6. 关闭资源
            try {
                if (rs != null) rs.close();
                if (selectStmt != null) selectStmt.close();
                if (insertStmt0 != null) insertStmt0.close();
                if (insertStmt1 != null) insertStmt1.close();
                if (deleteStmt != null) deleteStmt.close();
            }
            catch (SQLException e) {
                log.error("关闭资源失败！", e.getMessage());
            }
        }
    }


    // 执行批处理操作
    private static void executeBatch(PreparedStatement insertStmt0, PreparedStatement insertStmt1, PreparedStatement deleteStmt) throws SQLException {
        insertStmt0.executeBatch();
        insertStmt1.executeBatch();
        deleteStmt.executeBatch();

        // 清空批处理缓存
        insertStmt0.clearBatch();
        insertStmt1.clearBatch();
        deleteStmt.clearBatch();
    }
}
