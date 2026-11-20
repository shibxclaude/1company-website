package com.cn01.file;

import com.cn01.common.BizException;
import com.cn01.common.Result;
import com.cn01.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

/**
 * 后台图片上传（主图、图集、富文本内图片、新闻封面）。
 * 只接受 jpg/png/gif/webp，按文件头魔数校验真实类型，文件名随机生成，按月份分目录。
 */
@RestController
@RequiredArgsConstructor
public class FileController {
    private final AppProperties props;

    public record Uploaded(String url, String name, long size) {}

    @PostMapping("/api/admin/files")
    public Result<Uploaded> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) throw BizException.badRequest("请选择文件");
        String ext;
        try (InputStream in = file.getInputStream()) {
            ext = detectImage(in.readNBytes(12));
        }
        if (ext == null) throw BizException.badRequest("只支持 jpg、png、gif、webp 图片");

        String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String name = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path dir = Path.of(props.getUpload().getDir(), month);
        Files.createDirectories(dir);
        file.transferTo(dir.resolve(name));
        String url = props.getUpload().getUrlPrefix() + "/" + month + "/" + name;
        return Result.ok(new Uploaded(url, file.getOriginalFilename(), file.getSize()));
    }

    private static final Map<String, int[]> MAGIC = Map.of(
            "jpg", new int[]{0xFF, 0xD8, 0xFF},
            "png", new int[]{0x89, 0x50, 0x4E, 0x47},
            "gif", new int[]{0x47, 0x49, 0x46, 0x38});

    static String detectImage(byte[] head) {
        for (var e : MAGIC.entrySet()) {
            int[] m = e.getValue();
            if (head.length >= m.length) {
                boolean ok = true;
                for (int i = 0; i < m.length; i++) if ((head[i] & 0xFF) != m[i]) { ok = false; break; }
                if (ok) return e.getKey();
            }
        }
        // WEBP: "RIFF" .... "WEBP"
        if (head.length >= 12 && head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P') return "webp";
        return null;
    }
}
