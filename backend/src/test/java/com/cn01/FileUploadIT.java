package com.cn01;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

class FileUploadIT extends IntegrationTestBase {
    @Test
    void 上传图片_按魔数校验类型() throws Exception {
        String admin = adminToken();
        byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
        var ok = ok(auth(multipart("/api/admin/files").file(new MockMultipartFile("file", "a.png", "image/png", png)), admin));
        assertThat(ok.get("url").asText()).matches("/uploads/\\d{6}/[0-9a-f]{32}\\.png");

        byte[] fake = "<?php echo 1; ?>".getBytes();
        var bad = call(auth(multipart("/api/admin/files").file(new MockMultipartFile("file", "a.png", "image/png", fake)), admin), 400);
        assertThat(bad.get("msg").asText()).contains("只支持");

        call(multipart("/api/admin/files").file(new MockMultipartFile("file", "a.png", "image/png", png)), 401);
    }
}
