package de.mcmodersd.unipensum.domain.text;

import java.text.Normalizer;
import java.util.Locale;

public final class TextSanitizer {

    public static final int MAX_NAME = 100;
    public static final int MAX_ROOM = 60;
    public static final int MAX_NOTE = 2000;
    public static final int MAX_LINK = 2048;
    public static final int MAX_EMAIL = 254;
    public static final int MAX_PHONE = 40;

    private TextSanitizer() { }

    public static String line(String raw, int maxLength) {
        if (raw == null) return "";
        var out = new StringBuilder(raw.length());
        var pendingSpace = false;
        for (var i = 0; i < raw.length(); ) {
            var cp = raw.codePointAt(i);
            i += Character.charCount(cp);
            if (isSpace(cp)) {
                pendingSpace = out.length() > 0;
            } else if (!isDropped(cp)) {
                if (pendingSpace) out.append(' ');
                pendingSpace = false;
                out.appendCodePoint(cp);
            }
        }
        return limit(Normalizer.normalize(out, Normalizer.Form.NFC), maxLength);
    }

    public static String lineOrNull(String raw, int maxLength) {
        var clean = line(raw, maxLength);
        return clean.isEmpty() ? null : clean;
    }

    public static String text(String raw, int maxLength) {
        if (raw == null) return null;
        var lines = raw.replace("\r\n", "\n").replace('\r', '\n').replace('\u0085', '\n')
                .replace(' ', '\n').replace(' ', '\n').split("\n", -1);
        var out = new StringBuilder(raw.length());
        var pendingBreak = false;
        for (var each : lines) {
            var clean = line(each, Integer.MAX_VALUE);
            if (clean.isEmpty()) {
                // A blank line is kept once, between two lines of text.
                if (out.length() > 0) pendingBreak = true;
                continue;
            }
            if (out.length() > 0) out.append(pendingBreak ? "\n\n" : "\n");
            pendingBreak = false;
            out.append(clean);
        }
        var clean = limit(out.toString(), maxLength);
        return clean.isEmpty() ? null : clean;
    }

    public static String email(String raw) {
        var clean = limit(stripAll(raw), MAX_EMAIL);
        return clean.isEmpty() ? null : clean;
    }

    public static String phone(String raw) {
        if (raw == null) return null;
        var kept = new StringBuilder(raw.length());
        for (var i = 0; i < raw.length(); i++) {
            var c = raw.charAt(i);
            if (isSpace(c)) {
                kept.append(' ');
            } else if ((c >= '0' && c <= '9') || "+-()/.*#".indexOf(c) >= 0) {
                kept.append(c);
            }
        }

        var clean = line(kept.toString(), MAX_PHONE);
        for (var i = 0; i < clean.length(); i++) {
            if (Character.isDigit(clean.charAt(i))) return clean;
        }
        return null;
    }

    public static String webLink(String raw) {
        var text = limit(stripAll(raw), MAX_LINK);
        if (text.isEmpty()) return null;

        String url;
        var lower = text.toLowerCase(Locale.ROOT);
        if (lower.startsWith("https://")) {
            url = "https://" + text.substring("https://".length());
        } else if (lower.startsWith("http://")) {
            url = "http://" + text.substring("http://".length());
        } else if (hasSchemeWithSlashes(text)) {
            throw new IllegalArgumentException("Not a web link: " + text);
        } else {
            url = "https://" + text;
        }
        if (!hasValidAuthority(url) || containsForbidden(url)) {
            throw new IllegalArgumentException("Not a valid web link: " + text);
        }
        return url;
    }

    public static boolean isValidWebLink(String raw) {
        try {
            webLink(raw);
            return true;
        } catch (IllegalArgumentException invalid) {
            return false;
        }
    }

    private static boolean isSpace(int cp) {
        return Character.isWhitespace(cp) || Character.isSpaceChar(cp);
    }

    private static boolean isDropped(int cp) {
        return switch (Character.getType(cp)) {
            case Character.CONTROL, Character.SURROGATE, Character.PRIVATE_USE -> true;
            default -> cp == 0x00AD                     // soft hyphen
                    || cp == 0x200B                     // zero-width space
                    || (cp >= 0x2060 && cp <= 0x2064)   // word joiner, invisible operators
                    || cp == 0xFEFF                     // byte order mark
                    || (cp >= 0x202A && cp <= 0x202E)   // bidirectional embeddings and overrides
                    || (cp >= 0x2066 && cp <= 0x2069)   // bidirectional isolates
                    || (cp >= 0xFFF9 && cp <= 0xFFFC);
        };
    }

    private static String stripAll(String raw) {
        if (raw == null) return "";
         var out = new StringBuilder(raw.length());
        for (var i = 0; i < raw.length(); ) {
            var cp = raw.codePointAt(i);
            i += Character.charCount(cp);
            if (!isSpace(cp) && !isDropped(cp)) out.appendCodePoint(cp);
        }
        return Normalizer.normalize(out, Normalizer.Form.NFC);
    }

    private static String limit(String text, int max) {
        var cut = text;
        if (text.codePointCount(0, text.length()) > max) {
            cut = text.substring(0, text.offsetByCodePoints(0, max));
        }
        return cut.trim();
    }

    private static boolean hasSchemeWithSlashes(String text) {
        var colon = text.indexOf("://");
        if (colon <= 0) return false;
        if (!Character.isLetter(text.charAt(0))) return false;
        for (var i = 1; i < colon; i++) {
            var c = text.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '+' && c != '-' && c != '.') return false;
        }
        return true;
    }

    private static boolean hasValidAuthority(String url) {
        var start = url.indexOf("://") + 3;
        var end = url.length();
        for (var i = start; i < url.length(); i++) {
            var c = url.charAt(i);
            if (c == '/' || c == '?' || c == '#') {
                end = i;
                break;
            }
        }

        var authority = url.substring(start, end);
        if (authority.isEmpty() || authority.indexOf('@') >= 0) return false;

        if (authority.startsWith("[")) {                      // IPv6 literal
            var close = authority.indexOf(']');
            if (close < 3) return false;
            var rest = authority.substring(close + 1);
            return rest.isEmpty() || isPort(rest);
        }
        var host = authority;
        var colon = authority.lastIndexOf(':');
        if (colon >= 0) {
            if (!isPort(authority.substring(colon))) return false;
            host = authority.substring(0, colon);
        }
        if (host.isEmpty()) return false;
        for (var i = 0; i < host.length(); i++) {
            var c = host.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '-' && c != '.' && c != '_') return false;
        }
        return true;
    }

    private static boolean isPort(String colonAndPort) {
        if (colonAndPort.isEmpty() || colonAndPort.charAt(0) != ':') return false;
        for (var i = 1; i < colonAndPort.length(); i++) {
            var c = colonAndPort.charAt(i);
            if (c < '0' || c > '9') return false;
        }
        return colonAndPort.length() <= 6;
    }

    private static boolean containsForbidden(String url) {
        return url.indexOf('<') >= 0 || url.indexOf('>') >= 0 || url.indexOf('"') >= 0 || url.indexOf('\\') >= 0;
    }
}