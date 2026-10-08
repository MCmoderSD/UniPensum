package de.mcmodersd.unipensum.ui.format;

import android.content.Context;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.core.content.ContextCompat;

import de.mcmodersd.unipensum.R;
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
            case RED -> R.color.course_red;
            case ORANGE -> R.color.course_orange;
            case YELLOW -> R.color.course_yellow;
            case GREEN -> R.color.course_green;
            case TEAL -> R.color.course_teal;
            case BLUE -> R.color.course_blue;
            case VIOLET -> R.color.course_violet;
            case PINK -> R.color.course_pink;
            case GRAPHITE -> R.color.course_graphite;
            case GRAY -> R.color.course_gray;
            case SILVER -> R.color.course_silver;
        };
    }
}