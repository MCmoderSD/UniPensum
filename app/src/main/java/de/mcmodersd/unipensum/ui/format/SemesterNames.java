package de.mcmodersd.unipensum.ui.format;

import android.content.Context;

import de.mcmodersd.unipensum.R;
import de.mcmodersd.unipensum.domain.logic.SemesterNamer;
import de.mcmodersd.unipensum.domain.model.Semester;

public final class SemesterNames {

    private SemesterNames() {
    }

    /** The custom name if there is one, otherwise "WiSe 26/27" or "SoSe 27" in the app language. */
    public static String display(Context context, Semester semester) {
        if (semester.customName() != null) return semester.customName();
        var label = SemesterNamer.label(semester.start());
        if (label.season() == SemesterNamer.Season.WINTER) {
            return context.getString(R.string.semester_winter, label.year() % 100, (label.year() + 1) % 100);
        }
        return context.getString(R.string.semester_summer, label.year() % 100);
    }
}
