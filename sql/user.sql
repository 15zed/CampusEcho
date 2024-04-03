create table t_user
(
    user_id  int auto_increment comment '用户id'
        primary key,
    head     varchar(150)  null comment '头像',
    username varchar(16)   not null comment '用户姓名',
    password varchar(20)   not null comment '密码',
    sex      varchar(2)    not null comment '性别',
    area     varchar(20)   null comment '地区',
    contact  varchar(30)   null comment '联系方式',
    status   int default 1 null comment '状态 0：禁用 1：正常',
    likelist varchar(200)  null comment '点赞列表',
    follows  varchar(200)  null comment '关注列表',
    fans     varchar(200)  null comment '粉丝列表'
)
    comment '用户信息表';

create index union__index
    on t_user (username, area, contact);