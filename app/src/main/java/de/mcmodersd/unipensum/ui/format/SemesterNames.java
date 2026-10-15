package de.mcmodersd.unipensum.ui.format;

import static de.mcmodersd.unipensum.R.string.*;
import static de.mcmodersd.unipensum.domain.logic.SemesterNamer.Season.WINTER;

import android.content.Context;

import de.mcmodersd.unipensum.domain.logic.SemesterNamer;
import de.mcmodersd.unipensum.domain.model.Semester;

public final class SemesterNames {

    private SemesterNames() { }

    /** The custom name if there is one, otherwise "WiSe 26/27" or "SoSe 27" in the app language. */
    public static String display(Context context, Semester semester) {
        if (semester.customName() != null) return semester.customName();
        var label = SemesterNamer.label(semester.start());
        if (label.season() == WINTER) {
            return context.getString(semester_winter, label.year() % 100, (label.year() + 1) % 100);
        }
        return context.getString(semester_summer, label.year() % 100);
    }
}