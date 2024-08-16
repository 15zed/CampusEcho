package com.hgc.campusechouserservice.client;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;

import com.hgc.campusechointerfaces.service.CountService;
import com.hgc.campusechointerfaces.service.UserService;
import com.hgc.campusechomodel.entity.Follower;
import com.hgc.campusechomodel.entity.Following;
import com.hgc.campusechouserservice.service.RedisService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class CanalClient {

    @Resource
    private UserService userService;

    @Resource
    private RedisService redisService;

    @DubboReference
    private CountService countService;

    private final CanalConnector connector;

    public CanalClient() {
        this.connector = CanalConnectors.newSingleConnector(
                new InetSocketAddress("127.0.0.1", 11111),  // Canal Server 地址和端口
                "example",  // Destination 名称
                "",  // 用户名
                ""   // 密码
        );
    }

    @PostConstruct
    private void startListening() {
        CompletableFuture.runAsync(() -> {
            int batchSize = 1000;
            try {
                connector.connect();
                connector.subscribe(".*\\..*");
                connector.rollback();

                while (true) {
                    Message message = connector.getWithoutAck(batchSize);
                    long batchId = message.getId();
                    int size = message.getEntries().size();

                    if (batchId != -1 && size != 0) {
                        processEntries(message.getEntries());
                    }

                    connector.ack(batchId);
                }
            }
            catch (Exception e) {
                connector.rollback();
            }
            finally {
                connector.disconnect();
            }
        });
    }

    private void processEntries(List<CanalEntry.Entry> entries) {
        for (CanalEntry.Entry entry : entries) {
            if (entry.getEntryType() == CanalEntry.EntryType.ROWDATA) {
                CanalEntry.RowChange rowChange = null;
                try {
                    rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
                }
                catch (Exception e) {
                    throw new RuntimeException("ERROR ## parser of eromanga-event has an error , data:" + entry.toString(), e);
                }

                CanalEntry.EventType eventType = rowChange.getEventType();
                String tableName = entry.getHeader().getTableName();

                if ("following".equals(tableName) || "following_0".equals(tableName) || "following_1".equals(tableName)) {
                    for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
                        if (eventType == CanalEntry.EventType.INSERT) {
                            handleInsert(rowData);
                        }
                        if (eventType == CanalEntry.EventType.UPDATE) {
                            handleUpdate(rowData);
                        }
                    }
                }
            }
        }
    }


    private void handleInsert(CanalEntry.RowData rowData) {
        Following following = new Following();
        Follower follower = new Follower();
        List<CanalEntry.Column> columnsList = rowData.getAfterColumnsList();
        for (CanalEntry.Column column : columnsList) {
            if ("id".equals(column.getName())) {
                following.setId(Integer.parseInt(column.getValue()));
                follower.setId(Integer.parseInt(column.getValue()));
            }
            else if ("from_user_id".equals(column.getName())) {
                following.setFromUserId(Integer.parseInt(column.getValue()));
                follower.setFromUserId(Integer.parseInt(column.getValue()));
            }
            else if ("to_user_id".equals(column.getName())) {
                following.setToUserId(Integer.parseInt(column.getValue()));
                follower.setToUserId(Integer.parseInt(column.getValue()));
            }
            else if ("type".equals(column.getName())) {
                following.setType(Integer.parseInt(column.getValue()));
                follower.setType(Integer.parseInt(column.getValue()));
            }
            else if ("update_time".equals(column.getName())) {
                following.setUpdateTime(Integer.valueOf(column.getValue()));
                follower.setUpdateTime(Integer.valueOf(column.getValue()));
            }
        }
        // 更新 follower 表
        userService.insertFollower(follower);
        // 异步更新计数服务（RPC 远程调用）
        CompletableFuture.runAsync(() -> {
            // 调用计数服务更新计数
            countService.addFollows(following.getFromUserId());
            countService.addFans(follower.getToUserId());
        });
        // 异步更新缓存
        CompletableFuture.runAsync(() -> {
            redisService.insertFollowing(following); // 更新关注列表缓存
            redisService.insertFollower(follower); // 更新粉丝列表缓存
        });
    }

    private void handleUpdate(CanalEntry.RowData rowData) {
        Following following = new Following();
        Follower follower = new Follower();
        List<CanalEntry.Column> columnsList = rowData.getAfterColumnsList();
        for (CanalEntry.Column column : columnsList) {
            if ("id".equals(column.getName())) {
                following.setId(Integer.parseInt(column.getValue()));
                follower.setId(Integer.parseInt(column.getValue()));
            }
            else if ("from_user_id".equals(column.getName())) {
                following.setFromUserId(Integer.parseInt(column.getValue()));
                follower.setFromUserId(Integer.parseInt(column.getValue()));
            }
            else if ("to_user_id".equals(column.getName())) {
                following.setToUserId(Integer.parseInt(column.getValue()));
                follower.setToUserId(Integer.parseInt(column.getValue()));
            }
            else if ("type".equals(column.getName())) {
                following.setType(Integer.parseInt(column.getValue()));
                follower.setType(Integer.parseInt(column.getValue()));
            }
            else if ("update_time".equals(column.getName())) {
                following.setUpdateTime(Integer.valueOf(column.getValue()));
                follower.setUpdateTime(Integer.valueOf(column.getValue()));
            }
        }
        // 更新 follower 表
        userService.updateFollower(follower);
        // 异步更新计数服务（RPC 远程调用）
        CompletableFuture.runAsync(() -> {
            if (following.getType() == 1) {
                countService.addFollows(following.getFromUserId());
                countService.addFans(follower.getToUserId());
            }
            else if (following.getType() == 2) {
                countService.reduceFollows(following.getFromUserId());
                countService.reduceFans(follower.getToUserId());
            }
        });
        // 异步更新缓存
        CompletableFuture.runAsync(() -> {
            redisService.updateFollowing(following); // 更新关注列表缓存
            redisService.updateFollower(follower); // 更新粉丝列表缓存
        });

    }
}

