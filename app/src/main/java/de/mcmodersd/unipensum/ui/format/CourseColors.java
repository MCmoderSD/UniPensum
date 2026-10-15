package de.mcmodersd.unipensum.ui.format;

import static de.mcmodersd.unipensum.R.color.*;

import android.content.Context;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.core.content.ContextCompat;

import de.mcmodersd.unipensum.domain.model.CourseColor;

/** Resolves the preset keys to the light or dark color of the current configuration. */
public final class CourseColors {

    private CourseColors() { }

    @ColorInt
    public static int resolve(Context context, CourseColor color) {
        return ContextCompat.getColor(context, resource(color));
    }

    @ColorRes
    private static int resource(CourseColor color) {
        return switch (color) {
            case RED -> course_red;
            case ORANGE -> course_orange;
            case YELLOW -> course_yellow;
            case GREEN -> course_green;
            case TEAL -> course_teal;
            case BLUE -> course_blue;
            case VIOLET -> course_violet;
            case PINK -> course_pink;
            case GRAPHITE -> course_graphite;
            case GRAY -> course_gray;
            case SILVER -> course_silver;
        };
    }
}