package com.moldtrial.web;

import com.moldtrial.domain.SampleEvidence;
import com.moldtrial.repo.Repositories.EvidenceRepository;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

/**
 * 样件“照片”：演示环境无真实图片，由后端按证据类型动态生成 SVG 检查照片，
 * 图上直接标注模腔、缺陷位置与复测工艺条件，便于在对照工作台中作为证据引用。
 */
@RestController
@RequestMapping("/api/evidence")
public class EvidenceController {

    private final EvidenceRepository evidences;

    public EvidenceController(EvidenceRepository evidences) {
        this.evidences = evidences;
    }

    @GetMapping(value = "/{id}/photo", produces = "image/svg+xml")
    public String photo(@PathVariable Long id) {
        SampleEvidence e = evidences.findById(id).orElseThrow(NoSuchElementException::new);
        String verdict = switch (e.getKind()) {
            case "ok" -> "判定：合格（范围内通过复测）";
            case "burn" -> "判定：不合格（烧焦复现）";
            case "sink" -> "判定：不合格（缩印）";
            case "mismatch" -> "判定：样件合格但复测条件不匹配";
            default -> "判定：对比标注";
        };
        return svg(e.getKind(), e.getLabel(),
                (e.getCavityNo() == null ? "" : e.getCavityNo() + "｜")
                        + nz(e.getLocation()) + "｜" + nz(e.getParameterCaption()),
                verdict);
    }

    @GetMapping(value = "/svg", produces = "image/svg+xml")
    public String dynamic(@RequestParam String kind, @RequestParam String title,
                          @RequestParam(required = false) String caption,
                          @RequestParam(required = false) String verdict) {
        return svg(kind, title, caption, verdict);
    }

    private String svg(String kind, String title, String caption, String verdict) {
        String mark;
        String verdictColor;
        switch (kind) {
            case "ok" -> {
                mark = """
                    <circle cx="240" cy="128" r="58" fill="none" stroke="#1a9850" stroke-width="6"/>
                    <path d="M212 128 l20 20 l38 -42" fill="none" stroke="#1a9850" stroke-width="7"
                          stroke-linecap="round" stroke-linejoin="round"/>
                    <text x="240" y="205" text-anchor="middle" font-size="15" fill="#1a9850">无烧痕 / 无缩印</text>
                """;
                verdictColor = "#1a9850";
            }
            case "burn" -> {
                mark = """
                    <circle cx="240" cy="128" r="58" fill="none" stroke="#b00020" stroke-width="4"/>
                    <path d="M240 96 c14 16 22 24 22 38 a22 22 0 1 1 -44 0 c0 -14 8 -22 22 -38z"
                          fill="#3b2325" stroke="#7a1f26" stroke-width="2"/>
                    <path d="M240 112 c7 8 10 13 10 20 a10 10 0 1 1 -20 0 c0 -7 3 -12 10 -20z"
                          fill="#1c1012"/>
                    <text x="240" y="205" text-anchor="middle" font-size="15" fill="#b00020">浇口黑褐色烧痕（积碳）</text>
                """;
                verdictColor = "#b00020";
            }
            case "sink" -> {
                mark = """
                    <circle cx="240" cy="128" r="58" fill="none" stroke="#d98e04" stroke-width="4"/>
                    <ellipse cx="240" cy="132" rx="34" ry="14" fill="#e9dcc0" stroke="#b8860b" stroke-width="3"/>
                    <path d="M212 132 q28 12 56 0" fill="none" stroke="#8a6d1b" stroke-width="2" stroke-dasharray="4 3"/>
                    <text x="240" y="205" text-anchor="middle" font-size="15" fill="#d98e04">筋背凹陷 / 补缩不足</text>
                """;
                verdictColor = "#d98e04";
            }
            case "mismatch" -> {
                mark = """
                    <path d="M186 92 h108 l18 18 v52 l-18 18 h-108 l-18 -18 v-52 z"
                          fill="#fff4e5" stroke="#e07b00" stroke-width="3"/>
                    <g stroke="#e07b00" stroke-width="5">
                        <line x1="196" y1="96" x2="182" y2="112"/>
                        <line x1="220" y1="96" x2="206" y2="112"/>
                        <line x1="244" y1="96" x2="230" y2="112"/>
                        <line x1="268" y1="96" x2="254" y2="112"/>
                        <line x1="292" y1="96" x2="278" y2="112"/>
                    </g>
                    <text x="240" y="145" text-anchor="middle" font-size="42" font-weight="bold" fill="#e07b00">!</text>
                    <text x="240" y="172" text-anchor="middle" font-size="14" fill="#b35c00">版次 / 工艺窗口不符</text>
                """;
                verdictColor = "#e07b00";
            }
            default -> {
                mark = """
                    <rect x="190" y="92" width="100" height="72" rx="8" fill="#eef3f8" stroke="#4a6a8a" stroke-width="3"/>
                    <line x1="190" y1="128" x2="290" y2="128" stroke="#4a6a8a" stroke-dasharray="5 4"/>
                    <text x="240" y="205" text-anchor="middle" font-size="14" fill="#4a6a8a">整改前后对比</text>
                """;
                verdictColor = "#4a6a8a";
            }
        }

        String[] lines = caption == null ? new String[0] : splitLines(caption);
        StringBuilder cap = new StringBuilder();
        int y = 262;
        for (String line : lines) {
            cap.append("<text x=\"24\" y=\"").append(y)
               .append("\" font-size=\"13\" fill=\"#444\">").append(esc(line)).append("</text>");
            y += 18;
        }

        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 480 %d" font-family="Noto Sans CJK SC, WenQuanYi Zen Hei, sans-serif">
                  <rect width="480" height="%d" fill="#fafbfc"/>
                  <rect x="0" y="0" width="480" height="46" fill="#243447"/>
                  <text x="24" y="30" font-size="18" font-weight="bold" fill="#fff">%s</text>
                  <text x="456" y="30" text-anchor="end" font-size="12" fill="#9fb3c8">虚构样件照片 · 试模对照工作台</text>
                  <rect x="150" y="64" width="180" height="130" rx="14" fill="#f0f2f5" stroke="#c9d2dc" stroke-width="2"/>
                  <text x="240" y="84" text-anchor="middle" font-size="11" fill="#8a97a5">端盖样件 · 浇口区</text>
                  %s
                  %s
                  <text x="24" y="%d" font-size="13" font-weight="bold" fill="%s">%s</text>
                </svg>
                """.formatted(Math.max(320, 280 + lines.length * 18),
                Math.max(320, 280 + lines.length * 18),
                esc(title), mark, cap.toString(),
                Math.max(320, 280 + lines.length * 18) - 14,
                verdictColor, esc(verdict == null ? "" : verdict));
    }

    private String[] splitLines(String caption) {
        String[] parts = caption.split("｜");
        StringBuilder line = new StringBuilder();
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String p : parts) {
            if (!p.isBlank() && line.length() + p.length() > 34) {
                out.add(line.toString());
                line.setLength(0);
            }
            if (!line.isEmpty()) line.append("｜");
            line.append(p);
        }
        if (!line.isEmpty()) out.add(line.toString());
        return out.toArray(new String[0]);
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }

    private String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
