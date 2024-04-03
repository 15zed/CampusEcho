create table school.t_flag
(
    id        int null,
    mysqlFlag int null,
    redisFlag int null,
    esFlag    int null,
    userId    int null comment '操作用户的id，只有在进行点赞操作的时候，该字段才有用',
    type      int null comment '1.发帖 2.发评论 3.点赞 4.删除帖子和评论'
)
    comment '本地消息表';

