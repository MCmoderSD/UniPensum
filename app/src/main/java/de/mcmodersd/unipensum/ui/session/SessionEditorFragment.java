package de.mcmodersd.unipensum.ui.session;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;

import de.mcmodersd.unipensum.R;
import de.mcmodersd.unipensum.UniPensumApp;
import de.mcmodersd.unipensum.data.SessionContext;
import de.mcmodersd.unipensum.data.db.Database;
import de.mcmodersd.unipensum.domain.logic.Recurrence;
import de.mcmodersd.unipensum.domain.logic.Reminders;
import de.mcmodersd.unipensum.domain.logic.SemesterDefaults;
import de.mcmodersd.unipensum.domain.logic.SemesterRules;
import de.mcmodersd.unipensum.domain.model.EditScope;
import de.mcmodersd.unipensum.domain.model.Lecturer;
import de.mcmodersd.unipensum.domain.model.Mode;
import de.mcmodersd.unipensum.domain.model.NameStyle;
import de.mcmodersd.unipensum.domain.model.Schedule;
import de.mcmodersd.unipensum.domain.model.Semester;
import de.mcmodersd.unipensum.domain.model.Series;
import de.mcmodersd.unipensum.domain.model.SessionDetails;
import de.mcmodersd.unipensum.domain.model.SessionType;
import de.mcmodersd.unipensum.domain.text.TextSanitizer;
import de.mcmodersd.unipensum.reminder.NotificationAccess;
import de.mcmodersd.unipensum.ui.Navigator;
import de.mcmodersd.unipensum.ui.course.CourseDraftViewModel;
import de.mcmodersd.unipensum.ui.format.ReminderFormat;
import de.mcmodersd.unipensum.ui.format.SeriesFormat;
import de.mcmodersd.unipensum.ui.format.TimeFormat;
import de.mcmodersd.unipensum.ui.lecturer.LecturerSheet;
import de.mcmodersd.unipensum.ui.widget.DatePickerSheet;
import de.mcmodersd.unipensum.ui.widget.Haptics;
import de.mcmodersd.unipensum.ui.widget.TimePickerSheet;
import de.mcmodersd.unipensum.ui.widget.UpButton;
import de.mcmodersd.unipensum.ui.widget.UpRow;
import de.mcmodersd.unipensum.ui.widget.UpSegmentedControl;
import de.mcmodersd.unipensum.ui.widget.UpStepper;
import de.mcmodersd.unipensum.ui.widget.UpSwitch;
import de.mcmodersd.unipensum.ui.widget.UpTextField;

/**
 * The form for one event. It runs in two modes:
 * <ul>
 *   <li><b>series</b> (from the course editor): edits an entry of the course draft, nothing is stored yet;</li>
 *   <li><b>session</b> (from a session's sheet): edits that session within an {@link EditScope} and stores it.</li>
 * </ul>
 * For a single session the form shows its date (which is how it is moved); for the series scopes it
 * shows weekday, period and repetition instead.
 */
public class SessionEditorFragment extends Fragment {

    private static final String ARG_SERIES_INDEX = "series_index";
    private static final String ARG_SESSION_ID = "session_id";
    private static final String ARG_SCOPE = "scope";

    private static final String KEY_START = "editor_start";
    private static final String KEY_END = "editor_end";
    private static final String KEY_DAY = "editor_day";
    private static final String KEY_FROM = "editor_from";
    private static final String KEY_UNTIL = "editor_until";
    private static final String KEY_LECTURER = "editor_lecturer";

    private static final int REPEAT_WEEKLY = 0;
    private static final int REPEAT_BIWEEKLY = 1;
    private static final int REPEAT_CUSTOM = 2;
    private static final int DEFAULT_CUSTOM_INTERVAL = 3;
    /** A new event starts at 08:00 and lasts as long as a typical lecture block: 3 h 15 min. */
    private static final int DEFAULT_START_MIN = 8 * 60;
    private static final int DEFAULT_DURATION_MIN = 3 * 60 + 15;

    /** @param index position in the course draft, or -1 for a new event */
    public static SessionEditorFragment forSeries(int index) {
        var args = new Bundle();
        args.putInt(ARG_SERIES_INDEX, index);
        var fragment = new SessionEditorFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public static SessionEditorFragment forSession(long sessionId, EditScope scope) {
        var args = new Bundle();
        args.putLong(ARG_SESSION_ID, sessionId);
        args.putString(ARG_SCOPE, scope.name());
        var fragment = new SessionEditorFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /** Everything the form edits; lives in a ViewModel so it survives configuration changes. */
    static final class Form {
        SessionType type = SessionType.LECTURE;
        DayOfWeek weekday = DayOfWeek.MONDAY;
        int startMin = DEFAULT_START_MIN;
        int endMin = DEFAULT_START_MIN + DEFAULT_DURATION_MIN;
        Mode mode = Mode.IN_PERSON;
        boolean hybrid;
        String room = "";
        String link = "";
        long lecturerId = SessionDetails.NO_LECTURER;
        String note = "";
        int reminderMin = Reminders.defaultFor(Mode.IN_PERSON);
        LocalDate first;
        LocalDate last;
        /**
         * Whether "Until" was chosen by hand. Until then, a new event runs 16 weeks from the "From" the user
         * picks (but not past the semester); an existing event never moves its end by itself.
         */
        boolean lastTouched;
        int interval = 1;
        LocalDate day;
        Semester semester;
        /** Session mode only: what the series looked like when the form opened. */
        Schedule originalSchedule;
        /** Series mode only: the entry being edited, {@code null} for a new one. */
        Series source;
    }

    public static class FormViewModel extends ViewModel {
        Form form;
    }

    private Form form;
    private boolean seriesMode;
    private EditScope scope = EditScope.ALL;
    private long sessionId;
    private int seriesIndex;
    private CourseDraftViewModel draft;
    /** All lecturers by id, to put a name on the chosen one; {@code null} until they have loaded. */
    private HashMap<Long, Lecturer> lecturers;

    private View formScroll;
    private UpSegmentedControl typeControl;
    private UpSegmentedControl weekdayControl;
    private UpSegmentedControl modeControl;
    private UpSegmentedControl repeatControl;
    private UpStepper repeatStepper;
    private UpRow dateRow;
    private UpRow startRow;
    private UpRow endRow;
    private UpRow fromRow;
    private UpRow untilRow;
    private UpSwitch hybridSwitch;
    private View hybridRow;
    private UpTextField roomField;
    private UpTextField linkField;
    private UpRow lecturerRow;
    private UpSwitch reminderSwitch;
    private UpStepper reminderStepper;
    private UpRow notificationsOffRow;
    private UpTextField noteField;
    private TextView timeError;
    private TextView scheduleError;
    private TextView scheduleNote;
    private UpButton saveButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_session_editor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        var args = requireArguments();
        seriesMode = !args.containsKey(ARG_SESSION_ID);
        seriesIndex = args.getInt(ARG_SERIES_INDEX, -1);
        sessionId = args.getLong(ARG_SESSION_ID);
        if (!seriesMode) scope = EditScope.valueOf(args.getString(ARG_SCOPE));
        draft = new ViewModelProvider(requireActivity()).get(CourseDraftViewModel.class);
        if (seriesMode && !draft.isReady()) {
            view.post(() -> Navigator.of(this).pop());
            return;
        }

        findViews(view);
        view.findViewById(R.id.back).setOnClickListener(v -> Navigator.of(this).pop());
        ((TextView) view.findViewById(R.id.bar_title)).setText(
                seriesMode
                        ? (seriesIndex < 0 ? R.string.event_new : R.string.event_edit) : R.string.session_edit
        );
        saveButton.setOnClickListener(v -> save());

        FormViewModel holder = new ViewModelProvider(this).get(FormViewModel.class);
        if (holder.form != null) {
            form = holder.form;
            bind();
        } else if (seriesMode) {
            form = holder.form = buildSeriesForm();
            bind();
        } else {
            UniPensumApp.from(requireContext()).repository().loadSessionContext(
                    sessionId,
                    new Database.Callback<>() {
                        @Override
                        public void onSuccess(SessionContext context) {
                            if (getView() == null) return;
                            form = holder.form = buildSessionForm(context);
                            bind();
                        }

                        @Override
                        public void onError(Exception error) {
                            if (isAdded()) Navigator.of(SessionEditorFragment.this).pop();
                        }
                    }
            );
        }
    }

    // --- building the form ---

    private Form buildSeriesForm() {
        var result = new Form();
        var semester = draft.semester();
        result.semester = semester;
        Series source = seriesIndex >= 0 ? draft.series().get(seriesIndex) : null;
        result.source = source;
        if (source != null) {
            fill(result, source.details());
            result.weekday = source.schedule().weekday();
            result.first = source.schedule().first();
            result.last = source.schedule().last();
            result.interval = source.schedule().intervalWeeks();
        } else {
            result.first = semester.start();
            result.last = lectureEnd(semester, result.first);
            // Further events start from the lecturer of the first one.
            if (!draft.series().isEmpty()) {
                result.lecturerId = draft.series().get(0).details().lecturerId();
            }
        }
        return result;
    }

    private Form buildSessionForm(SessionContext context) {
        var result = new Form();
        result.semester = context.semester();
        fill(result, context.session().details());
        var series = context.series().schedule();
        result.weekday = series.weekday();
        result.interval = series.intervalWeeks();
        result.last = series.last();
        result.first = scope == EditScope.THIS_AND_FOLLOWING ? context.session().day() : series.first();
        result.day = context.session().day();
        result.originalSchedule = new Schedule(result.weekday, result.first, result.last, result.interval);
        return result;
    }

    /** The end of a 16-week run from {@code first}, cut off at the end of the semester. */
    private static LocalDate lectureEnd(Semester semester, LocalDate first) {
        var end = SemesterDefaults.lectureEnd(first);
        return end.isAfter(semester.end()) ? semester.end() : end;
    }

    private static void fill(Form target, SessionDetails details) {
        target.type = details.type();
        target.startMin = details.startMin();
        target.endMin = details.endMin();
        target.mode = details.mode();
        target.hybrid = details.hybrid();
        target.room = orEmpty(details.room());
        target.link = orEmpty(details.link());
        target.lecturerId = details.lecturerId();
        target.note = orEmpty(details.note());
        target.reminderMin = details.reminderMin();
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }

    // --- views ---

    private void findViews(View view) {
        formScroll = view.findViewById(R.id.form_scroll);
        typeControl = view.findViewById(R.id.type_control);
        weekdayControl = view.findViewById(R.id.weekday_control);
        modeControl = view.findViewById(R.id.mode_control);
        repeatControl = view.findViewById(R.id.repeat_control);
        repeatStepper = view.findViewById(R.id.repeat_stepper);
        dateRow = view.findViewById(R.id.date_row);
        startRow = view.findViewById(R.id.start_row);
        endRow = view.findViewById(R.id.end_row);
        fromRow = view.findViewById(R.id.from_row);
        untilRow = view.findViewById(R.id.until_row);
        hybridSwitch = view.findViewById(R.id.hybrid_switch);
        hybridRow = view.findViewById(R.id.hybrid_row);
        roomField = view.findViewById(R.id.room_field);
        linkField = view.findViewById(R.id.link_field);
        lecturerRow = view.findViewById(R.id.lecturer_row);
        reminderSwitch = view.findViewById(R.id.reminder_switch);
        reminderStepper = view.findViewById(R.id.reminder_stepper);
        notificationsOffRow = view.findViewById(R.id.notifications_off_row);
        noteField = view.findViewById(R.id.note_field);
        timeError = view.findViewById(R.id.time_error);
        scheduleError = view.findViewById(R.id.schedule_error);
        scheduleNote = view.findViewById(R.id.schedule_note);
        saveButton = view.findViewById(R.id.save);
    }

    private boolean thisOnly() {
        return !seriesMode && scope == EditScope.THIS_ONLY;
    }

    /** Fills the views from the form and wires the listeners; runs once the form exists. */
    private void bind() {
        var root = requireView();
        var manager = getParentFragmentManager();

        TextView scopeNote = root.findViewById(R.id.scope_note);
        if (seriesMode) {
            scopeNote.setVisibility(View.GONE);
        } else {
            scopeNote.setVisibility(View.VISIBLE);
            scopeNote.setText(scopeText());
        }
        root.findViewById(R.id.weekday_block).setVisibility(thisOnly() ? View.GONE : View.VISIBLE);
        root.findViewById(R.id.period_block).setVisibility(thisOnly() ? View.GONE : View.VISIBLE);
        dateRow.setVisibility(thisOnly() ? View.VISIBLE : View.GONE);

        typeControl.setOptions(
                getString(R.string.type_lecture), getString(R.string.type_exercise),
                getString(R.string.type_lab), getString(R.string.type_tutorial)
        );
        typeControl.setSelectedIndex(form.type.ordinal());
        typeControl.setOnSelectionChangedListener(index -> form.type = SessionType.values()[index]);

        var weekdays = new CharSequence[5];
        for (var i = 0; i < 5; i++) weekdays[i] = SeriesFormat.weekdayShort(requireContext(), DayOfWeek.of(i + 1));
        weekdayControl.setOptions(weekdays);
        weekdayControl.setSelectedIndex(form.weekday.getValue() - 1);
        weekdayControl.setOnSelectionChangedListener(index -> {
            form.weekday = DayOfWeek.of(index + 1);
            updateScheduleNote();
        });

        modeControl.setOptions(getString(R.string.mode_in_person), getString(R.string.mode_online));
        modeControl.setSelectedIndex(form.mode.ordinal());
        modeControl.setOnSelectionChangedListener(index -> {
            var previous = form.mode;
            form.mode = Mode.values()[index];
            // A reminder that is still the standard time of the old format becomes that of the new one.
            form.reminderMin = Reminders.afterModeChange(form.reminderMin, previous, form.mode);
            reminderStepper.setValue(Reminders.afterModeChange(reminderStepper.getValue(), previous, form.mode));
            updateModeVisibility();
        });
        hybridSwitch.setChecked(form.hybrid);
        hybridSwitch.setOnCheckedChangeListener(checked -> {
            form.hybrid = checked;
            updateModeVisibility();
        });

        roomField.setMaxLength(TextSanitizer.MAX_ROOM);
        roomField.setText(form.room);
        roomField.addTextWatcher(text -> form.room = text);
        linkField.setMaxLength(TextSanitizer.MAX_LINK);
        linkField.setText(form.link);
        linkField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        linkField.addTextWatcher(text -> {
            form.link = text;
            linkField.setError(null);
        });
        lecturerRow.setOnClickListener(v -> LecturerSheet.showPicker(manager, KEY_LECTURER, form.lecturerId));
        manager.setFragmentResultListener(KEY_LECTURER, getViewLifecycleOwner(), (key, result) -> {
            form.lecturerId = result.getLong(LecturerSheet.RESULT_LECTURER_ID);
            renderLecturer();
        });
        UniPensumApp.from(requireContext()).repository().lecturers().observe(getViewLifecycleOwner(), list -> {
            lecturers = new HashMap<>();
            for (var lecturer : list) lecturers.put(lecturer.id(), lecturer);
            renderLecturer();
        });
        reminderStepper.setRange(0, SessionDetails.MAX_REMINDER_MIN);
        reminderStepper.setStep(Reminders.STEP_MIN);
        reminderStepper.setFormatter(minutes -> ReminderFormat.text(requireContext(), minutes));
        var reminderOn = form.reminderMin != SessionDetails.NO_REMINDER;
        reminderStepper.setValue(reminderOn ? form.reminderMin : Reminders.defaultFor(form.mode));
        reminderSwitch.setChecked(reminderOn);
        reminderSwitch.setOnCheckedChangeListener(checked -> {
            form.reminderMin = checked ? reminderStepper.getValue() : SessionDetails.NO_REMINDER;
            updateReminderVisibility();
        });
        reminderStepper.setOnValueChangedListener(minutes -> form.reminderMin = minutes);
        notificationsOffRow.setOnClickListener(v -> NotificationAccess.openSettings(requireContext()));

        noteField.setMultiLine(3);
        noteField.setMaxLength(TextSanitizer.MAX_NOTE);
        noteField.setText(form.note);
        noteField.addTextWatcher(text -> form.note = text);

        repeatControl.setOptions(
                getString(R.string.repeat_weekly), getString(R.string.repeat_biweekly),
                getString(R.string.repeat_custom)
        );
        repeatControl.setSelectedIndex(repeatIndex(form.interval));
        repeatStepper.setRange(1, 12);
        repeatStepper.setFormatter(value -> SeriesFormat.repeat(requireContext(), value));
        repeatStepper.setValue(form.interval > 2 ? form.interval : DEFAULT_CUSTOM_INTERVAL);
        repeatControl.setOnSelectionChangedListener(index -> {
            form.interval = index == REPEAT_WEEKLY ? 1 : (index == REPEAT_BIWEEKLY ? 2 : repeatStepper.getValue());
            updateRepeatVisibility();
            updateScheduleNote();
        });
        repeatStepper.setOnValueChangedListener(value -> {
            form.interval = value;
            updateScheduleNote();
        });

        startRow.setOnClickListener(v -> TimePickerSheet.show(
                manager, KEY_START,
                getString(R.string.field_start), form.startMin
        ));
        endRow.setOnClickListener(v -> TimePickerSheet.show(
                manager, KEY_END,
                getString(R.string.field_end), form.endMin
        ));
        dateRow.setOnClickListener(v -> DatePickerSheet.show(
                manager, KEY_DAY, getString(R.string.field_date),
                form.day, form.semester.start(), form.semester.end(), true
        ));
        fromRow.setOnClickListener(v -> DatePickerSheet.show(
                manager, KEY_FROM, getString(R.string.field_from),
                form.first, form.semester.start(), form.semester.end(), false
        ));
        untilRow.setOnClickListener(v -> DatePickerSheet.show(
                manager, KEY_UNTIL, getString(R.string.field_until),
                form.last, form.semester.start(), form.semester.end(), false
        ));

        manager.setFragmentResultListener(KEY_START, getViewLifecycleOwner(), (key, result) -> {
            var duration = form.endMin > form.startMin ? form.endMin - form.startMin : DEFAULT_DURATION_MIN;
            form.startMin = result.getInt(TimePickerSheet.RESULT_MINUTES);
            // Moving the start keeps the length, which is what one usually means.
            form.endMin = Math.min(SessionDetails.MINUTES_PER_DAY - 5, form.startMin + duration);
            renderValues();
        });
        manager.setFragmentResultListener(KEY_END, getViewLifecycleOwner(), (key, result) -> {
            form.endMin = result.getInt(TimePickerSheet.RESULT_MINUTES);
            renderValues();
        });
        manager.setFragmentResultListener(KEY_DAY, getViewLifecycleOwner(), (key, result) -> {
            form.day = LocalDate.ofEpochDay(result.getLong(DatePickerSheet.RESULT_EPOCH_DAY));
            renderValues();
        });
        manager.setFragmentResultListener(KEY_FROM, getViewLifecycleOwner(), (key, result) -> {
            form.first = LocalDate.ofEpochDay(result.getLong(DatePickerSheet.RESULT_EPOCH_DAY));
            if (seriesMode && form.source == null && !form.lastTouched) {
                form.last = lectureEnd(form.semester, form.first);
            }
            renderValues();
        });
        manager.setFragmentResultListener(KEY_UNTIL, getViewLifecycleOwner(), (key, result) -> {
            form.last = LocalDate.ofEpochDay(result.getLong(DatePickerSheet.RESULT_EPOCH_DAY));
            form.lastTouched = true;
            renderValues();
        });

        renderValues();
        updateModeVisibility();
        updateRepeatVisibility();
        updateReminderVisibility();
        formScroll.setVisibility(View.VISIBLE);
    }

    /** The user may come back from the system settings where the notifications were turned on. */
    @Override
    public void onResume() {
        super.onResume();
        if (form != null && getView() != null) updateReminderVisibility();
    }

    /** The time is only there while the reminder is on, and so is the hint that notifications are off. */
    private void updateReminderVisibility() {
        var on = form.reminderMin != SessionDetails.NO_REMINDER;
        reminderStepper.setVisibility(on ? View.VISIBLE : View.GONE);
        notificationsOffRow.setVisibility(
                on && !NotificationAccess.allowed(requireContext())
                        ? View.VISIBLE : View.GONE
        );
    }

    private static int repeatIndex(int interval) {
        return interval == 1 ? REPEAT_WEEKLY : (interval == 2 ? REPEAT_BIWEEKLY : REPEAT_CUSTOM);
    }

    private String scopeText() {
        return switch (scope) {
            case THIS_ONLY -> getString(R.string.scope_this_only);
            case THIS_AND_FOLLOWING -> getString(R.string.scope_this_and_following);
            default -> getString(R.string.scope_all);
        };
    }

    private void renderLecturer() {
        if (lecturers == null) return;
        var lecturer = lecturers.get(form.lecturerId);
        lecturerRow.setValue(lecturer == null ? getString(R.string.lecturer_none) : lecturer.name(NameStyle.FULL_NAME));
    }

    /** Refreshes the rows that show picked values. */
    private void renderValues() {
        startRow.setValue(TimeFormat.time(requireContext(), form.startMin));
        endRow.setValue(TimeFormat.time(requireContext(), form.endMin));
        timeError.setVisibility(View.GONE);
        if (form.day != null) dateRow.setValue(TimeFormat.dateMedium(requireContext(), form.day));
        fromRow.setValue(TimeFormat.dateMedium(requireContext(), form.first));
        untilRow.setValue(TimeFormat.dateMedium(requireContext(), form.last));
        scheduleError.setVisibility(View.GONE);
        updateScheduleNote();
    }

    private void updateModeVisibility() {
        var inPerson = form.mode == Mode.IN_PERSON;
        hybridRow.setVisibility(inPerson ? View.VISIBLE : View.GONE);
        roomField.setVisibility(inPerson ? View.VISIBLE : View.GONE);
        linkField.setVisibility(!inPerson || form.hybrid ? View.VISIBLE : View.GONE);
    }

    private void updateRepeatVisibility() {
        repeatStepper.setVisibility(repeatControl.getSelectedIndex() == REPEAT_CUSTOM ? View.VISIBLE : View.GONE);
    }

    /** Warns that regenerated sessions lose their individual edits. */
    private void updateScheduleNote() {
        var changed = !seriesMode && !thisOnly() && form.originalSchedule != null
                && !form.originalSchedule.equals(currentSchedule());
        scheduleNote.setVisibility(changed ? View.VISIBLE : View.GONE);
    }

    private Schedule currentSchedule() {
        return new Schedule(form.weekday, form.first, form.last, form.interval);
    }

    // --- saving ---

    private void save() {
        timeError.setVisibility(View.GONE);
        scheduleError.setVisibility(View.GONE);
        var valid = true;

        if (form.endMin <= form.startMin) {
            timeError.setText(R.string.error_time);
            timeError.setVisibility(View.VISIBLE);
            valid = false;
        }
        // The link only counts for online and hybrid events; the field is hidden otherwise.
        var linkUsed = form.mode == Mode.ONLINE || form.hybrid;
        if (linkUsed && !TextSanitizer.isValidWebLink(form.link)) {
            linkField.setError(getString(R.string.error_link));
            valid = false;
        }
        Schedule schedule = null;
        if (!thisOnly()) {
            schedule = currentSchedule();
            var problem = scheduleProblem(schedule);
            if (problem != null) {
                scheduleError.setText(problem);
                scheduleError.setVisibility(View.VISIBLE);
                valid = false;
            }
        }
        if (!valid) {
            Haptics.reject(saveButton);
            return;
        }

        var details = new SessionDetails(
                form.type, form.startMin, form.endMin, form.mode, form.hybrid,
                form.room, form.link, form.lecturerId, form.note, form.reminderMin
        ).normalized();

        if (seriesMode) {
            var source = form.source;
            var result = new Series(
                    source == null ? 0 : source.id(), source == null ? 0 : source.courseId(),
                    details, schedule
            );
            var list = draft.series();
            if (seriesIndex >= 0) list.set(seriesIndex, result);
            else list.add(result);
            Haptics.confirm(saveButton);
            Navigator.of(this).pop();
            return;
        }

        saveButton.setEnabled(false);
        UniPensumApp.from(requireContext()).repository().editSession(
                sessionId, scope, details,
                thisOnly() ? form.day : null, schedule, new Database.Callback<>() {
                    @Override
                    public void onSuccess(Void result) {
                        if (!isAdded()) return;
                        Haptics.confirm(saveButton);
                        Navigator.of(SessionEditorFragment.this).pop();
                    }

                    @Override
                    public void onError(Exception error) {
                        if (!isAdded() || getView() == null) return;
                        saveButton.setEnabled(true);
                        scheduleError.setText(R.string.error_save);
                        scheduleError.setVisibility(View.VISIBLE);
                        Haptics.reject(saveButton);
                    }
                }
        );
    }

    /** @return a message if the schedule cannot be saved, otherwise {@code null} */
    @Nullable
    private String scheduleProblem(Schedule schedule) {
        var semester = form.semester;
        return switch (SemesterRules.checkSchedule(semester, schedule)) {
            case EMPTY_RANGE -> getString(R.string.error_period_empty);
            case OUTSIDE_SEMESTER -> getString(
                    R.string.error_period_outside,
                    TimeFormat.dateMedium(requireContext(), semester.start()),
                    TimeFormat.dateMedium(requireContext(), semester.end())
            );
            default ->
                    Recurrence.occurrences(schedule).isEmpty() ? getString(R.string.error_period_no_dates) : null;
        };
    }
}