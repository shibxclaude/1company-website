-- 登录凭证版本号：改密码时 +1，之前签发的 JWT 全部失效
ALTER TABLE admin_user ADD COLUMN token_version INT NOT NULL DEFAULT 0 COMMENT '登录凭证版本，改密码时递增';
ALTER TABLE app_user   ADD COLUMN token_version INT NOT NULL DEFAULT 0 COMMENT '登录凭证版本，改密码时递增';
