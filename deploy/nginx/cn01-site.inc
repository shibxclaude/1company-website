# 站点主体（静态文件、预渲染页、SPA 空壳、/api 反代、上传目录、管理后台）。
# HTTP 阶段（cn01.conf）与 HTTPS 阶段（cn01-https.conf）共用，改规则只改这一个文件。
# 安装位置：/etc/nginx/snippets/cn01-site.inc

    # 主域名 www.yilinga.com：yilinga.com 与旧域名 cn01.com.cn / www.cn01.com.cn 一律 301 过来
    if ($host != www.yilinga.com) {
        return 301 $scheme://www.yilinga.com$request_uri;
    }

    root /var/www/cn01/current/www;
    charset utf-8;
    server_tokens off;

    add_header Cache-Control $cn01_cache_control always;
    add_header X-Content-Type-Options nosniff always;
    add_header X-Frame-Options SAMEORIGIN always;
    add_header Referrer-Policy strict-origin-when-cross-origin always;

    location /assets/ { try_files $uri =404; }
    location /images/ { try_files $uri =404; }
    location = /favicon.png { access_log off; }
    location = /robots.txt  { access_log off; }

    # 预渲染产物的物理路径不对外暴露：/index.html -> /，/about/index.html -> /about
    location = /index.html { return 301 /; }
    location ~ ^(.+)/index\.html$ { return 301 $1; }

    # 统一去掉结尾斜杠：/about/ -> /about（canonical 也不带斜杠）
    location ~ ^(.+)/$ {
        return 301 $1$is_args$args;
    }

    # 只在客户端渲染的路由（不做预渲染）：返回空壳页，由前端路由渲染
    location ~ ^/(user|order|pay)(/|$) {
        try_files /spa.html =404;
    }

    # 解决方案/新闻详情：优先返回预渲染页面；后台新上架、尚未重新构建的，返回空壳页由前端渲染（无需发版即可访问）
    location ~ ^/(solutions|news)/[^/]+$ {
        try_files $uri/index.html /spa.html;
    }

    # 后端接口
    location ^~ /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 30s;
        client_max_body_size 20m;
    }

    # 后台上传的图片
    location ^~ /uploads/ {
        alias /var/www/cn01/uploads/;
        try_files $uri =404;
    }

    # 管理后台（独立 SPA）
    location ^~ /admin/ {
        root /var/www/cn01/current;
        try_files $uri $uri/ /admin/index.html;
    }
    location = /admin { return 301 /admin/; }

    # 预渲染页面：/about -> /about/index.html
    location / {
        try_files $uri $uri/index.html =404;
    }

    # 不存在的地址：返回 404 状态码 + 空壳页（前端显示 404 页面）
    error_page 404 /spa.html;
    location = /spa.html {
        internal;
    }
