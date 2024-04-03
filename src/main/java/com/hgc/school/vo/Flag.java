package com.hgc.school.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *  标志类 和数据库的标志表对应
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Flag {
    
    private Integer id;
    private Integer mysqlFlag;
    private Integer redisFlag;
    private Integer esFlag;
    private Integer userId;//操作人的id，只有在点赞操作的时候需要记录这个字段的实际值，其他时候都为0
    /**
     * 操作类型：
     * 1：发帖
     * 2：发评论
     * 3：点赞
     * 4：删除帖子和评论
     */
    private Integer type;

}
