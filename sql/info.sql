create table t_pub
(
    p_id     int auto_increment comment '发布信息id'
        primary key,
    p_time   datetime      null comment '发布时间',
    p_img    varchar(2000) null comment '图片',
    p_text   varchar(1500) null comment '文本',
    userId   int           not null comment '用户id',
    avatar   varchar(150)  null,
    likes    int default 0 null comment '点赞数',
    comments int default 0 null comment '评论数',
    category varchar(20)   null comment '分类'
)
    comment '发布信息表';