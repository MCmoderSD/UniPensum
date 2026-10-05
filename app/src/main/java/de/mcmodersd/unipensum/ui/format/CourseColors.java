package de.mcmodersd.unipensum.ui.format;

import android.content.Context;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.core.content.ContextCompat;

import de.mcmodersd.unipensum.R;
import de.mcmodersd.unipensum.domain.model.CourseColor;

/** Resolves the preset keys to the light or dark color of the current configuration. */
public final class CourseColors {

    private CourseColors() {
    }

    @ColorInt
    public static int resolve(Context context, CourseColor color) {
        return ContextCompat.getColor(context, resource(color));
    }

    @ColorRes
    private static int resource(CourseColor color) {
        switch (color) {
            case RED:
                return R.color.course_red;
            case ORANGE:
                return R.color.course_orange;
            case YELLOW:
                return R.color.course_yellow;
            case GREEN:
                return R.color.course_green;
            case TEAL:
                return R.color.course_teal;
            case BLUE:
                return R.color.course_blue;
            case VIOLET:
                return R.color.course_violet;
            case PINK:
                return R.color.course_pink;
            case GRAPHITE:
                return R.color.course_graphite;
            case GRAY:
                return R.color.course_gray;
            case SILVER:
                return R.color.course_silver;
            default:
                throw new IllegalArgumentException("Unknown color: " + color);
        }
    }
}
