package de.mcmodersd.unipensum.ui;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import de.mcmodersd.unipensum.domain.text.TextSanitizer;

public final class Links {

    private Links() { }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean openWeb(Context context, String link) {
        String url;
        try {
            url = TextSanitizer.webLink(link);
        } catch (IllegalArgumentException notAWebLink) {
            return false;
        }
        if (url == null) return false;
        return start(context, new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    public static boolean mail(Context context, String address) {
        var clean = TextSanitizer.email(address);
        if (clean == null) return false;
        return start(context, new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", clean, null)));
    }

    public static boolean dial(Context context, String number) {
        var clean = TextSanitizer.phone(number);
        if (clean == null) return false;
        var digits = clean.replaceAll("[^0-9+*#]", "");
        return start(context, new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", digits, null)));
    }

    private static boolean start(Context context, Intent intent) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return true;
        } catch (ActivityNotFoundException | SecurityException unavailable) {
            return false;
        }
    }
}