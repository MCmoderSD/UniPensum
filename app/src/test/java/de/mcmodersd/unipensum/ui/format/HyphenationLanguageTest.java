package de.mcmodersd.unipensum.ui.format;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.Locale;

public class HyphenationLanguageTest {

    private static Locale tag(String languageTag) {
        return Locale.forLanguageTag(languageTag);
    }

    @Test
    public void aNameInLatinLettersOnADeviceInAnotherScript_isHyphenatedInEnglish() {
        for (var device : new String[]{"ar", "ar-SA", "he", "fa", "ur", "ja", "zh", "ko", "ru", "hi", "th"}) {
            assertEquals(device, Locale.ENGLISH, HyphenationLanguage.of("Software Engineering", tag(device)));
        }
    }

    @Test
    public void aNameInLatinLettersOnADeviceInLatinLetters_followsTheDevice() {
        for (var device : new String[]{"en", "de", "fr", "es", "pl", "tr", "vi"}) {
            assertNull(device, HyphenationLanguage.of("Softwaretechnik", tag(device)));
        }
    }

    @Test
    public void aNameInAnotherScript_followsTheDevice() {
        assertNull(HyphenationLanguage.of("هندسة البرمجيات", tag("ar")));
        assertNull(HyphenationLanguage.of("هندسة البرمجيات", tag("en")));
        assertNull(HyphenationLanguage.of("Программная инженерия", tag("ar")));
    }

    @Test
    public void digitsAndMarksBeforeTheFirstLetterAreSkipped() {
        assertEquals(Locale.ENGLISH, HyphenationLanguage.of("2D Graphics", tag("ar")));
        assertEquals(Locale.ENGLISH, HyphenationLanguage.of("(1) Algorithms", tag("he")));
        assertNull(HyphenationLanguage.of("1. الجبر", tag("ar")));
    }

    @Test
    public void aTextWithoutLettersFollowsTheDevice() {
        assertNull(HyphenationLanguage.of("", tag("ar")));
        assertNull(HyphenationLanguage.of("2026", tag("ar")));
        assertNull(HyphenationLanguage.of("Algorithms", Locale.ROOT));
    }
}