-- 一零科技官网 初始表结构（第三、四阶段）
-- 约定：金额一律 DECIMAL(12,2)；时间 DATETIME；状态用 TINYINT 或短字符串；utf8mb4。

-- ===================== 第三阶段：解决方案（售卖对象） =====================

CREATE TABLE solution_category (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    code        VARCHAR(50)  NOT NULL COMMENT '分类编码，前台筛选用',
    name        VARCHAR(50)  NOT NULL COMMENT '分类名',
    sort        INT          NOT NULL DEFAULT 0 COMMENT '排序，小的在前',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1 启用 0 停用',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_category_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '解决方案分类';

CREATE TABLE solution (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    category_id     BIGINT        NOT NULL COMMENT '分类 id',
    slug            VARCHAR(64)   NOT NULL COMMENT 'URL 标识，/solutions/{slug}',
    name            VARCHAR(100)  NOT NULL COMMENT '标题',
    subtitle        VARCHAR(200)  NULL COMMENT '副标题',
    summary         VARCHAR(500)  NULL COMMENT '一句话简介',
    main_image      VARCHAR(255)  NULL COMMENT '主图',
    gallery         JSON          NULL COMMENT '图集，字符串数组',
    content         MEDIUMTEXT    NULL COMMENT '富文本详细介绍（保存前已做白名单清洗）',
    delivery_mode   VARCHAR(20)   NOT NULL DEFAULT 'REMOTE_DEPLOY' COMMENT '交付方式 ONLINE_OPEN 在线开通 / DOWNLOAD_LICENSE 下载授权 / REMOTE_DEPLOY 远程部署',
    after_sales     VARCHAR(500)  NULL COMMENT '售后支持',
    service_period  VARCHAR(200)  NULL COMMENT '服务期限',
    recommended     TINYINT       NOT NULL DEFAULT 0 COMMENT '1 首页推荐',
    status          TINYINT       NOT NULL DEFAULT 0 COMMENT '1 上架 0 下架',
    sort            INT           NOT NULL DEFAULT 0,
    deleted         TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_solution_slug (slug),
    KEY idx_solution_category (category_id),
    KEY idx_solution_status_sort (status, sort)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '解决方案';

CREATE TABLE solution_version (
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    solution_id     BIGINT         NOT NULL,
    name            VARCHAR(50)    NOT NULL COMMENT '版本名，如 标准版',
    price           DECIMAL(12, 2) NOT NULL COMMENT '售价',
    original_price  DECIMAL(12, 2) NULL COMMENT '原价（划线价）',
    billing_type    VARCHAR(20)    NOT NULL COMMENT 'ONE_TIME 一次性买断 / YEARLY 按年订阅',
    description     VARCHAR(500)   NULL COMMENT '版本差异说明',
    status          TINYINT        NOT NULL DEFAULT 1 COMMENT '1 在售 0 停售',
    sort            INT            NOT NULL DEFAULT 0,
    created_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_version_solution (solution_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '解决方案版本与定价';

CREATE TABLE solution_service (
    id           BIGINT         NOT NULL AUTO_INCREMENT,
    solution_id  BIGINT         NOT NULL,
    name         VARCHAR(50)    NOT NULL COMMENT '增值服务名，如 延长运维期',
    price        DECIMAL(12, 2) NOT NULL,
    unit         VARCHAR(20)    NOT NULL DEFAULT '次' COMMENT '计价单位：年 / 次 / 人天 …',
    description  VARCHAR(500)   NULL,
    status       TINYINT        NOT NULL DEFAULT 1 COMMENT '1 在售 0 停售',
    sort         INT            NOT NULL DEFAULT 0,
    created_at   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_service_solution (solution_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '解决方案增值服务（运维期、部署、培训等）';

-- ===================== 第三阶段：新闻动态 =====================

CREATE TABLE news (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    title         VARCHAR(200)  NOT NULL,
    summary       VARCHAR(500)  NULL,
    cover         VARCHAR(255)  NULL,
    content       MEDIUMTEXT    NULL COMMENT '富文本正文（已清洗）',
    published_at  DATETIME      NOT NULL COMMENT '发布时间，列表按此倒序',
    top           TINYINT       NOT NULL DEFAULT 0 COMMENT '1 置顶',
    status        TINYINT       NOT NULL DEFAULT 0 COMMENT '1 发布 0 草稿',
    views         INT           NOT NULL DEFAULT 0,
    deleted       TINYINT       NOT NULL DEFAULT 0,
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_news_list (status, top, published_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '新闻动态';

-- ===================== 账号 =====================

CREATE TABLE admin_user (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    username       VARCHAR(50)  NOT NULL,
    password_hash  VARCHAR(100) NOT NULL COMMENT 'BCrypt',
    real_name      VARCHAR(50)  NULL,
    status         TINYINT      NOT NULL DEFAULT 1 COMMENT '1 正常 0 禁用',
    last_login_at  DATETIME     NULL,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '后台管理员';

CREATE TABLE app_user (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    phone          VARCHAR(20)  NOT NULL COMMENT '手机号（注册时短信验证）',
    username       VARCHAR(50)  NULL COMMENT '账号，可用于密码登录',
    password_hash  VARCHAR(100) NULL COMMENT 'BCrypt；仅验证码登录的用户可为空',
    nickname       VARCHAR(50)  NULL,
    status         TINYINT      NOT NULL DEFAULT 1 COMMENT '1 正常 0 禁用',
    last_login_at  DATETIME     NULL,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_phone (phone),
    UNIQUE KEY uk_user_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '前台用户';

-- ===================== 第四阶段：订单与支付 =====================

CREATE TABLE orders (
    id                 BIGINT         NOT NULL AUTO_INCREMENT,
    order_no           VARCHAR(32)    NOT NULL COMMENT '日期时间(14) + 业务码(2) + 随机数(6)',
    access_token       VARCHAR(64)    NOT NULL COMMENT '游客查看/支付订单的凭证',
    user_id            BIGINT         NULL COMMENT '下单时已登录则记录',
    contact_name       VARCHAR(50)    NOT NULL,
    contact_phone      VARCHAR(20)    NOT NULL,
    contact_email      VARCHAR(100)   NOT NULL,
    company_name       VARCHAR(100)   NULL,
    remark             VARCHAR(500)   NULL,
    solution_id        BIGINT         NOT NULL,
    solution_name      VARCHAR(100)   NOT NULL COMMENT '下单时快照',
    total_amount       DECIMAL(12, 2) NOT NULL,
    status             VARCHAR(20)    NOT NULL COMMENT 'PENDING_PAY / PAID / IN_SERVICE / COMPLETED / CANCELLED',
    agreement_version  VARCHAR(20)    NOT NULL COMMENT '下单时同意的《产品购买服务协议》版本',
    pay_channel        VARCHAR(20)    NULL COMMENT 'WECHAT_NATIVE / MOCK',
    code_url           VARCHAR(255)   NULL COMMENT 'Native 支付二维码链接（有效期内复用）',
    pay_expire_at      DATETIME       NOT NULL COMMENT '超过此时间未支付自动关单',
    paid_at            DATETIME       NULL,
    transaction_id     VARCHAR(64)    NULL COMMENT '支付平台交易号',
    cancelled_at       DATETIME       NULL,
    cancel_reason      VARCHAR(100)   NULL,
    completed_at       DATETIME       NULL,
    created_at         DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    UNIQUE KEY uk_order_transaction (transaction_id),
    KEY idx_order_user (user_id),
    KEY idx_order_phone (contact_phone),
    KEY idx_order_status_expire (status, pay_expire_at),
    KEY idx_order_created (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单';

CREATE TABLE order_item (
    id             BIGINT         NOT NULL AUTO_INCREMENT,
    order_id       BIGINT         NOT NULL,
    item_type      VARCHAR(20)    NOT NULL COMMENT 'VERSION 版本 / SERVICE 增值服务',
    ref_id         BIGINT         NOT NULL COMMENT 'solution_version.id 或 solution_service.id',
    item_name      VARCHAR(100)   NOT NULL COMMENT '下单时快照',
    billing_type   VARCHAR(20)    NULL COMMENT '版本的计费方式快照',
    unit           VARCHAR(20)    NOT NULL,
    unit_price     DECIMAL(12, 2) NOT NULL,
    quantity       INT            NOT NULL,
    amount         DECIMAL(12, 2) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_item_order (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单明细';

CREATE TABLE order_log (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    order_id       BIGINT       NOT NULL,
    from_status    VARCHAR(20)  NULL,
    to_status      VARCHAR(20)  NOT NULL,
    note           VARCHAR(500) NULL,
    operator_type  VARCHAR(10)  NOT NULL COMMENT 'USER / ADMIN / SYSTEM',
    operator_name  VARCHAR(50)  NULL,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_log_order (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单状态流转与服务进度';

CREATE TABLE payment_notify (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    channel         VARCHAR(20)  NOT NULL,
    order_no        VARCHAR(32)  NOT NULL,
    transaction_id  VARCHAR(64)  NOT NULL,
    trade_state     VARCHAR(32)  NOT NULL,
    amount_fen      BIGINT       NOT NULL COMMENT '回调中的支付金额（分）',
    result          VARCHAR(20)  NOT NULL COMMENT 'APPLIED 已入账 / DUPLICATE 重复 / REJECTED 拒绝',
    note            VARCHAR(255) NULL,
    raw             TEXT         NULL COMMENT '解密后的回调报文',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_notify_order (order_no),
    KEY idx_notify_tx (transaction_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '支付回调流水（审计与排查）';
