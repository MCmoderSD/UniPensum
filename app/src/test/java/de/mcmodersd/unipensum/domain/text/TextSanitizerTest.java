package de.mcmodersd.unipensum.domain.text;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TextSanitizerTest {

    private static final String FAMILY = "👨‍👩‍👧";

    // --- line ---

    @Test
    public void line_trimsAndCollapsesWhitespace() {
        assertEquals("Anna Weber", TextSanitizer.line("  Anna \t  Weber \n", 100));
    }

    @Test
    public void line_turnsLineBreaksAndOddSpacesIntoOneSpace() {
        assertEquals("a b c d", TextSanitizer.line("a\r\nb c d", 100));
    }

    @Test
    public void line_dropsControlAndInvisibleCharacters() {
        assertEquals("Weber", TextSanitizer.line("We\u0000b​e\u0007r﻿", 100));
        // A right-to-left override can make a name read backwards on screen.
        assertEquals("Weber", TextSanitizer.line("‮Weber‬", 100));
        assertEquals("Weber", TextSanitizer.line("⁦We­ber⁩", 100));
    }

    @Test
    public void line_dropsAnUnpairedSurrogateButKeepsEmoji() {
        assertEquals("ab", TextSanitizer.line("a\uD83Db", 100));
        assertEquals("a😀b", TextSanitizer.line("a😀b", 100));
    }

    @Test
    public void line_keepsTheZeroWidthJoinerOfEmojiSequences() {
        assertEquals(FAMILY, TextSanitizer.line(FAMILY, 100));
    }

    @Test
    public void line_composesAccents() {
        assertEquals("Müller", TextSanitizer.line("Müller", 100));
    }

    @Test
    public void line_cutsAtTheLimitWithoutSplittingACharacter() {
        assertEquals(100, TextSanitizer.line("a".repeat(500), 100).length());
        String emoji = "😀".repeat(60);
        String cut = TextSanitizer.line(emoji, 50);
        assertEquals(50, cut.codePointCount(0, cut.length()));
        assertEquals("a", TextSanitizer.line("a b", 2));          // the cut would leave a trailing space
    }

    @Test
    public void line_handlesNullAndBlank() {
        assertEquals("", TextSanitizer.line(null, 10));
        assertEquals("", TextSanitizer.line(" ​\n\t ", 10));
    }

    @Test
    public void lineOrNull_isNullWhenNothingIsLeft() {
        assertNull(TextSanitizer.lineOrNull("  ​ ", 10));
        assertNull(TextSanitizer.lineOrNull(null, 10));
        assertEquals("A1", TextSanitizer.lineOrNull(" A1 ", 10));
    }

    // --- text ---

    @Test
    public void text_keepsLineBreaksButCleansEachLine() {
        assertEquals("bring laptop\nroom B2", TextSanitizer.text("  bring   laptop \r\n room\tB2  ", 100));
    }

    @Test
    public void text_allowsOnlyOneEmptyLineInARow() {
        assertEquals("a\n\nb\nc", TextSanitizer.text("a\n\n\n\n \nb\nc", 100));
    }

    @Test
    public void text_dropsEmptyLinesAtTheStartAndTheEnd() {
        assertEquals("a\nb", TextSanitizer.text("\n\n  a\nb\n \n\n", 100));
    }

    @Test
    public void text_isNullWhenNothingIsLeft() {
        assertNull(TextSanitizer.text(null, 100));
        assertNull(TextSanitizer.text(" \n ​\n", 100));
    }

    @Test
    public void text_treatsOtherLineSeparatorsAsLineBreaks() {
        assertEquals("a\nb\nc\nd", TextSanitizer.text("a\rb c\u0085d", 100));
    }

    @Test
    public void text_cutsAtTheLimit() {
        assertEquals(10, TextSanitizer.text("abcde\nfghij\nklmno", 10).length());
    }

    // --- email ---

    @Test
    public void email_hasNoWhitespaceAtAll() {
        assertEquals("anna.weber@uni.example", TextSanitizer.email(" anna.weber @uni.example\n"));
        assertEquals("a@b.example", TextSanitizer.email("a@b​.example"));
    }

    @Test
    public void email_isNullWhenBlank() {
        assertNull(TextSanitizer.email(null));
        assertNull(TextSanitizer.email("   "));
    }

    // --- phone ---

    @Test
    public void phone_keepsWhatIsDialedAndHowItIsWritten() {
        assertEquals("+49 (0) 30 / 123-45.6", TextSanitizer.phone("  +49  (0) 30 / 123-45.6 "));
        assertEquals("*#31#", TextSanitizer.phone("*#31#"));
    }

    @Test
    public void phone_dropsLettersAndOtherCharacters() {
        assertEquals("030 1234", TextSanitizer.phone("tel: 030​ 1234;"));
    }

    @Test
    public void phone_isNullWithoutADigit() {
        assertNull(TextSanitizer.phone(null));
        assertNull(TextSanitizer.phone("  "));
        assertNull(TextSanitizer.phone("abc +-"));
    }

    // --- web links ---

    @Test
    public void webLink_addsHttpsWhenTheSchemeIsMissing() {
        assertEquals("https://meet.example/abc", TextSanitizer.webLink("meet.example/abc"));
        assertEquals("https://localhost:3000/x", TextSanitizer.webLink("localhost:3000/x"));
    }

    @Test
    public void webLink_keepsHttpAndHttpsAndLowercasesTheScheme() {
        assertEquals("https://moodle.uni.example/course/view.php?id=7",
                TextSanitizer.webLink("https://moodle.uni.example/course/view.php?id=7"));
        assertEquals("http://intranet.example/x", TextSanitizer.webLink("HTTP://intranet.example/x"));
        assertEquals("https://Moodle.Example/Kurs", TextSanitizer.webLink("HTTPS://Moodle.Example/Kurs"));
    }

    @Test
    public void webLink_removesWhitespaceAndInvisibleCharacters() {
        assertEquals("https://meet.example/abc", TextSanitizer.webLink("  https://meet.example/\nabc ​"));
    }

    @Test
    public void webLink_isNullWhenBlank() {
        assertNull(TextSanitizer.webLink(null));
        assertNull(TextSanitizer.webLink("  \n "));
    }

    @Test
    public void webLink_refusesOtherSchemes() {
        assertThrows(IllegalArgumentException.class, () -> TextSanitizer.webLink("ftp://files.example/x"));
        assertThrows(IllegalArgumentException.class, () -> TextSanitizer.webLink("file:///sdcard/x"));
        assertThrows(IllegalArgumentException.class, () -> TextSanitizer.webLink("intent://x#Intent;end"));
        assertThrows(IllegalArgumentException.class, () -> TextSanitizer.webLink("javascript:alert(1)"));
        assertThrows(IllegalArgumentException.class, () -> TextSanitizer.webLink("mailto:a@b.example"));
    }

    @Test
    public void webLink_refusesALinkWithoutAHost() {
        assertThrows(IllegalArgumentException.class, () -> TextSanitizer.webLink("https://"));
        assertThrows(IllegalArgumentException.class, () -> TextSanitizer.webLink("https:///x"));
        assertThrows(IllegalArgumentException.class, () -> TextSanitizer.webLink("/just/a/path?x"));
    }

    @Test
    public void webLink_refusesAUserNameBeforeTheHost() {
        assertThrows(IllegalArgumentException.class, () -> TextSanitizer.webLink("https://uni.example@other.example/x"));
    }

    @Test
    public void webLink_refusesCharactersThatCannotBePartOfALink() {
        assertThrows(IllegalArgumentException.class, () -> TextSanitizer.webLink("exa<mple.com"));
        assertThrows(IllegalArgumentException.class, () -> TextSanitizer.webLink("https://exa mple.com/\"x\""));
        assertThrows(IllegalArgumentException.class, () -> TextSanitizer.webLink("https://example.com/a\\b"));
        assertThrows(IllegalArgumentException.class, () -> TextSanitizer.webLink("https://example.com:80x/"));
    }

    @Test
    public void webLink_acceptsInternationalHostsAndIpLiterals() {
        assertEquals("https://münchen.example/x", TextSanitizer.webLink("münchen.example/x"));
        assertEquals("https://192.168.0.5:8080/", TextSanitizer.webLink("192.168.0.5:8080/"));
        assertEquals("https://[::1]:8080/x", TextSanitizer.webLink("[::1]:8080/x"));
    }

    @Test
    public void webLink_aSchemeInsideTheQueryDoesNotCount() {
        assertEquals("https://meet.example/r?u=https://other.example",
                TextSanitizer.webLink("meet.example/r?u=https://other.example"));
    }

    @Test
    public void isValidWebLink_acceptsBlankAndWebLinksOnly() {
        assertTrue(TextSanitizer.isValidWebLink(null));
        assertTrue(TextSanitizer.isValidWebLink(""));
        assertTrue(TextSanitizer.isValidWebLink("meet.example/abc"));
        assertFalse(TextSanitizer.isValidWebLink("javascript:alert(1)"));
        assertFalse(TextSanitizer.isValidWebLink("ftp://x.example"));
    }

    // --- all of them ---

    @Test
    public void everyMethodIsIdempotent() {
        String messy = "  ‮Anna ​\t Weber\r\n\r\n\r\nroom B2  ";
        String line = TextSanitizer.line(messy, 100);
        assertEquals(line, TextSanitizer.line(line, 100));
        String text = TextSanitizer.text(messy, 100);
        assertEquals(text, TextSanitizer.text(text, 100));
        String phone = TextSanitizer.phone(" +49  30 12 ");
        assertEquals(phone, TextSanitizer.phone(phone));
        String email = TextSanitizer.email(" a@b.example ");
        assertEquals(email, TextSanitizer.email(email));
        String link = TextSanitizer.webLink("HTTPS://Meet.example/a b");
        assertEquals(link, TextSanitizer.webLink(link));
    }
}
