package de.mcmodersd.unipensum.ui.course;

import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

import de.mcmodersd.unipensum.data.CourseContext;
import de.mcmodersd.unipensum.domain.model.CourseColor;
import de.mcmodersd.unipensum.domain.model.Semester;
import de.mcmodersd.unipensum.domain.model.Series;

/**
 * The course being edited, shared between the course editor and the event editor above it
 * (scoped to the activity). Nothing is written to the database until the course is saved.
 * Call {@link #startNew} or {@link #startEdit} before opening the editor.
 */
public class CourseDraftViewModel extends ViewModel {

    private long courseId;
    private Semester semester;
    private String name = "";
    private CourseColor color = CourseColor.BLUE;
    private String moodleLink = "";
    private final List<Series> series = new ArrayList<>();

    public void startNew(Semester semester) {
        this.courseId = 0;
        this.semester = semester;
        this.name = "";
        this.color = CourseColor.BLUE;
        this.moodleLink = "";
        this.series.clear();
    }

    public void startEdit(CourseContext context) {
        this.courseId = context.course().id();
        this.semester = context.semester();
        this.name = context.course().name();
        this.color = context.course().color();
        this.moodleLink = context.course().moodleLink() == null ? "" : context.course().moodleLink();
        this.series.clear();
        this.series.addAll(context.series());
    }

    /** {@code false} if no editor was prepared, which happens when the process was restored. */
    public boolean isReady() {
        return semester != null;
    }

    public boolean isNew() {
        return courseId == 0;
    }

    public long courseId() {
        return courseId;
    }

    public Semester semester() {
        return semester;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CourseColor color() {
        return color;
    }

    public void setColor(CourseColor color) {
        this.color = color;
    }

    /** The Moodle link as typed; empty if there is none. */
    public String moodleLink() {
        return moodleLink;
    }

    public void setMoodleLink(String moodleLink) {
        this.moodleLink = moodleLink;
    }

    /** Mutable on purpose: the editors add, replace and remove entries. */
    public List<Series> series() {
        return series;
    }
}