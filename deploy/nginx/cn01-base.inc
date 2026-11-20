# http 级公共配置（gzip、按路径的缓存策略）。cn01.conf / cn01-https.conf 都会 include 本文件。
# 安装位置：/etc/nginx/snippets/cn01-base.inc

# gzip on 写在 cn01-site.inc 的 server 里：Ubuntu/Debian 自带的 nginx.conf 在 http 级已有 gzip on，这里再写会报重复指令
gzip_comp_level 5;
gzip_min_length 1024;
gzip_vary on;
gzip_proxied any;
gzip_types text/plain text/css text/xml application/xml application/javascript application/json image/svg+xml;

# 缓存策略按路径决定，统一在 server 级输出。
# 注意：location 里一旦写 add_header，server 级的 add_header 会全部失效，所以下面的 location 都不写 add_header。
map $uri $cn01_cache_control {
    default                 "no-cache";                              # HTML：每次协商，改价/改内容后立即生效
    ~^/assets/              "public, max-age=31536000, immutable";   # 构建产物带内容哈希
    ~^/admin/assets/        "public, max-age=31536000, immutable";
    ~^/uploads/             "public, max-age=2592000";               # 上传文件名随机生成，内容不会变
    ~^/api/                 "no-store";                              # 接口不缓存（价格即时生效）
    ~^/images/              "public, max-age=2592000";               # 图片 30 天（替换图片请改文件名）
    ~^/favicon\.png$        "public, max-age=2592000";
    ~^/(sitemap\.xml|robots\.txt)$ "public, max-age=3600";
}
