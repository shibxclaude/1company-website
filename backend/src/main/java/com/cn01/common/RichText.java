package com.cn01.common;

import java.util.Set;
import java.util.Locale;
import java.util.List;
import java.util.ArrayList;
import org.jsoup.nodes.Element;
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

    /**
     * style 只保留排版相关属性。不限制的话，后台内容可以用 position:fixed 盖住整个页面（伪造登录框、支付码等），
     * 或用 background:url() 向外部地址发请求。
     */
    private static final Set<String> CSS_ALLOWED = Set.of(
            "color", "background-color", "text-align", "text-indent", "text-decoration", "vertical-align",
            "font-size", "font-weight", "font-style", "font-family", "line-height", "letter-spacing", "white-space",
            "margin", "margin-top", "margin-right", "margin-bottom", "margin-left",
            "padding", "padding-top", "padding-right", "padding-bottom", "padding-left",
            "width", "height", "max-width", "list-style-type",
            "border", "border-color", "border-style", "border-width", "border-collapse");

    public static String clean(String html) {
        if (html == null || html.isBlank()) return html;
        Document.OutputSettings out = new Document.OutputSettings().prettyPrint(false);
        // baseUri 让 /uploads/xx.png 这类站内相对路径得以保留
        String cleaned = Jsoup.clean(html, "https://localhost", SAFELIST, out);
        if (!cleaned.contains("style=")) return cleaned;
        Document doc = Jsoup.parseBodyFragment(cleaned);
        doc.outputSettings(out);
        for (Element el : doc.select("[style]")) {
            String style = filterStyle(el.attr("style"));
            if (style.isEmpty()) el.removeAttr("style");
            else el.attr("style", style);
        }
        return doc.body().html();
    }

    static String filterStyle(String style) {
        List<String> kept = new ArrayList<>();
        for (String decl : style.split(";")) {
            int i = decl.indexOf(':');
            if (i <= 0) continue;
            String prop = decl.substring(0, i).trim().toLowerCase(Locale.ROOT);
            String value = decl.substring(i + 1).trim();
            String v = value.toLowerCase(Locale.ROOT);
            if (!CSS_ALLOWED.contains(prop) || value.isEmpty()
                    || v.contains("url(") || v.contains("expression(") || v.contains("javascript:") || v.contains("\\")) continue;
            kept.add(prop + ":" + value);
        }
        return String.join("; ", kept);
    }
}
