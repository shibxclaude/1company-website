package com.cn01.common;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

/** 后台富文本保存前做白名单清洗，防止存储型 XSS（前台用 v-html 渲染）。 */
public final class RichText {
    private static final Safelist SAFELIST = Safelist.relaxed()
            .addTags("span", "figure", "figcaption", "hr", "video", "source")
            .addAttributes(":all", "style", "class")
            .addAttributes("a", "target", "rel")
            .addAttributes("img", "width", "height", "data-href")
            .addAttributes("video", "src", "controls", "poster", "width", "height")
            .addAttributes("source", "src", "type")
            .addAttributes("td", "colspan", "rowspan", "width")
            .addAttributes("th", "colspan", "rowspan", "width")
            .addProtocols("img", "src", "http", "https", "data")
            .addProtocols("video", "src", "http", "https")
            .addProtocols("source", "src", "http", "https")
            .preserveRelativeLinks(true);

    private RichText() {}

    public static String clean(String html) {
        if (html == null || html.isBlank()) return html;
        Document.OutputSettings out = new Document.OutputSettings().prettyPrint(false);
        // baseUri 让 /uploads/xx.png 这类站内相对路径得以保留
        return Jsoup.clean(html, "https://localhost", SAFELIST, out);
    }
}
