create table t_comment
(
    commentId int auto_increment comment '评论的唯一标识'
        primary key,
    userId    int          null comment '评论人的id',
    pubId     int          null comment '被评论帖子的id',
    text      varchar(150) null comment '评论的内容',
    time      datetime     null comment '评论时间'
)
    comment '评论表';