package com.tasknotes.shared.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

public class HtmlSanitizer {

    private static final Safelist SAFELIST = Safelist.relaxed()
            .addTags("u", "s", "span", "mark")
            .addAttributes("span", "style")
            .addAttributes("mark", "style")
            .addAttributes("p", "style")
            .addAttributes("li", "style")
            .addProtocols("a", "href", "http", "https", "mailto")
            .addAttributes("a", "rel", "target");

    private static final Document.OutputSettings OUTPUT_SETTINGS =
            new Document.OutputSettings().prettyPrint(false);

    public static String sanitize(String html) {
        if (html == null || html.isBlank()) return "";
        String cleaned = Jsoup.clean(html, "https://", SAFELIST, OUTPUT_SETTINGS);
        // Belt-and-suspenders: strip CSS expressions and javascript: from style values
        cleaned = cleaned.replaceAll("(?i)expression\\s*\\(", "");
        cleaned = cleaned.replaceAll("(?i)(href|src)\\s*=\\s*[\"']\\s*javascript:[^\"']*[\"']", "$1=\"#\"");
        return cleaned;
    }

    public static String toPlainText(String html, int maxLength) {
        if (html == null || html.isBlank()) return "";
        String text = Jsoup.parse(html).text();
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }
}
