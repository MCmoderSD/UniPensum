package de.mcmodersd.unipensum.ui.format;

import static java.lang.Character.UnicodeScript.LATIN;

import androidx.annotation.Nullable;

import java.util.Locale;

/**
 * The language to hyphenate a name in. A text is hyphenated by the patterns of its language, which is the language of
 * the device, and a device in Arabic, Hebrew or Japanese has none for a course in Latin letters: "Software Engineering"
 * broke as "Software E" and "ngineering" in a narrow block, and as "Engineer-" and "ing" on an English device.
 */
public final class HyphenationLanguage {

    private HyphenationLanguage() { }

    /**
     * Names in Latin letters are the ones that are told apart, because the patterns for English are a good guess for
     * them in a language written in another script, and for the other scripts the language of the text cannot be told.
     *
     * @return English for a text in Latin letters on a device whose language is written in another script, and
     * {@code null}, the language of the device, otherwise
     */
    @Nullable
    public static Locale of(CharSequence text, Locale device) {
        var letter = firstLetter(text);
        if (letter == 0 || script(letter) != LATIN) return null;
        // The name of the language of the device, in that language, is written in its script.
        var deviceLetter = firstLetter(device.getDisplayLanguage(device));
        return deviceLetter == 0 || script(deviceLetter) == LATIN ? null : Locale.ENGLISH;
    }

    /** @return the first letter, or 0 if there is none */
    private static int firstLetter(CharSequence text) {
        for (var i = 0; i < text.length(); ) {
            var codePoint = Character.codePointAt(text, i);
            if (Character.isLetter(codePoint)) return codePoint;
            i += Character.charCount(codePoint);
        }
        return 0;
    }

    private static Character.UnicodeScript script(int codePoint) {
        return Character.UnicodeScript.of(codePoint);
    }
}