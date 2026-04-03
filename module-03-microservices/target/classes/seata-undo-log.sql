-- Seata AT 模式必须在每个参与服务的数据库中创建 undo_log 表
-- 官方建议使用此脚本，不同数据库有不同版本（此处为 MySQL）
-- 参考：https://github.com/apache/incubator-seata/tree/develop/script/client/at/db

CREATE TABLE IF NOT EXISTS `undo_log` (
    `branch_id`     BIGINT       NOT NULL COMMENT '分支事务ID',
    `xid`           VARCHAR(128) NOT NULL COMMENT '全局事务ID',
    `context`       VARCHAR(128) NOT NULL COMMENT '序列化方式',
    `rollback_info` LONGBLOB     NOT NULL COMMENT '回滚信息（before-image + after-image）',
    `log_status`    INT(11)      NOT NULL COMMENT '0:正常状态，1:防止全局回滚时被重复处理',
    `log_created`   DATETIME(6)  NOT NULL COMMENT '创建时间',
    `log_modified`  DATETIME(6)  NOT NULL COMMENT '更新时间',
    UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COMMENT = 'Seata AT 模式 undo_log 表（每个参与事务的数据库都需要）';

-- 说明：
-- 1. 此表由 Seata RM 自动管理，业务代码无需关心
-- 2. 一阶段：Seata 在执行业务 SQL 前后保存 before/after image 到此表
-- 3. 二阶段提交：清理 undo_log 记录
-- 4. 二阶段回滚：读取 undo_log，生成反向 SQL 恢复数据，然后删除记录
