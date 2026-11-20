-- 退款：后台发起、原路退回。支持部分退款；重复付款等异常收款可整笔退回。
ALTER TABLE orders
    ADD COLUMN refunded_amount DECIMAL(12, 2) NOT NULL DEFAULT 0 COMMENT '累计已退款金额（退款成功后累加）' AFTER total_amount,
    MODIFY COLUMN status VARCHAR(20) NOT NULL COMMENT 'PENDING_PAY / PAID / IN_SERVICE / COMPLETED / CANCELLED / REFUNDED';

CREATE TABLE order_refund (
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    refund_no       VARCHAR(32)    NOT NULL COMMENT '我方退款单号（支付平台 out_refund_no，幂等键）',
    order_id        BIGINT         NOT NULL,
    order_no        VARCHAR(32)    NOT NULL,
    kind            VARCHAR(10)    NOT NULL COMMENT 'ORDER 退订单货款 / STRAY 退回异常收款（重复付款、金额不符）',
    channel         VARCHAR(20)    NOT NULL COMMENT '原支付渠道',
    transaction_id  VARCHAR(64)    NOT NULL COMMENT '被退款的支付平台交易号',
    total_fen       BIGINT         NOT NULL COMMENT '被退款交易的原金额（分）',
    amount          DECIMAL(12, 2) NOT NULL COMMENT '本次退款金额',
    reason          VARCHAR(200)   NOT NULL,
    status          VARCHAR(20)    NOT NULL COMMENT 'PROCESSING / SUCCESS / FAILED',
    refund_id       VARCHAR(64)    NULL COMMENT '支付平台退款单号',
    fail_reason     VARCHAR(255)   NULL,
    operator_name   VARCHAR(50)    NOT NULL,
    finished_at     DATETIME       NULL,
    created_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_refund_no (refund_no),
    KEY idx_refund_order (order_id),
    KEY idx_refund_status (status, created_at),
    KEY idx_refund_tx (transaction_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '退款单';
