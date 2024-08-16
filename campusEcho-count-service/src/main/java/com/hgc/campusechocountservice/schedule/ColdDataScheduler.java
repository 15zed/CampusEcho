package com.hgc.campusechocountservice.schedule;

import com.hgc.campusechocountservice.Util.DataMigrator;
import com.hgc.campusechocountservice.Util.RedisColdDataScanner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.sql.SQLException;
import java.util.Set;

/**
 * 定时任务，每天凌晨2点执行, 迁移冷数据
 */
@Component
public class ColdDataScheduler {

    @Resource
    private RedisColdDataScanner redisColdDataScanner;
    @Resource
    private DataMigrator dataMigrator;


    @Scheduled(cron = "0 0 2 * * ?")  // 每天凌晨2点执行
    public void migrateColdData() throws SQLException {
        long thresholdTime = 30 * 24 * 60 * 60 * 1000L;  // 30天未访问的数据
        int fansThreshold = 10000;  // 设定一个粉丝数阈值
        Set<String> coldDataKeys = redisColdDataScanner.getColdDataKeys(thresholdTime, fansThreshold);
        dataMigrator.migrateColdData(coldDataKeys);
    }
}
