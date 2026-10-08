package de.mcmodersd.unipensum.ui.session;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import de.mcmodersd.unipensum.R;
import de.mcmodersd.unipensum.UniPensumApp;
import de.mcmodersd.unipensum.data.SessionContext;
import de.mcmodersd.unipensum.data.TimetableRepository;
import de.mcmodersd.unipensum.data.db.Database;
import de.mcmodersd.unipensum.domain.model.EditScope;
import de.mcmodersd.unipensum.domain.model.Lecturer;
import de.mcmodersd.unipensum.domain.model.Mode;
import de.mcmodersd.unipensum.domain.model.NameStyle;
import de.mcmodersd.unipensum.domain.model.SessionDetails;
import de.mcmodersd.unipensum.ui.Links;
import de.mcmodersd.unipensum.ui.Navigator;
import de.mcmodersd.unipensum.ui.format.ReminderFormat;
import de.mcmodersd.unipensum.ui.format.SeriesFormat;
import de.mcmodersd.unipensum.ui.format.TimeFormat;
import de.mcmodersd.unipensum.ui.widget.Haptics;
import de.mcmodersd.unipensum.ui.widget.UpButton;
import de.mcmodersd.unipensum.ui.widget.UpRow;
import de.mcmodersd.unipensum.ui.widget.UpSheet;

/**
 * Everything about one session. The information scrolls; the buttons to join the meeting, open Moodle,
 * edit or delete stay at the bottom. Tapping the lecturer's e-mail address or phone number opens the
 * mail or phone app.
 */
public final class SessionDetailSheet extends UpSheet {

    /** The fragment tag, which lets the app find the sheet that is open. */
    public static final String TAG = "session-detail";

    private static final String ARG_SESSION_ID = "session_id";
    private static final String KEY_EDIT_SCOPE = "detail_edit_scope";
    private static final String KEY_DELETE_SCOPE = "detail_delete_scope";

    /** Takes most of the screen, so the information has room and the buttons sit low. */
    private static final float MIN_HEIGHT_FRACTION = 0.72f;

    private LinearLayout column;

    public static void show(FragmentManager manager, long sessionId) {
        var args = new Bundle();
        args.putLong(ARG_SESSION_ID, sessionId);
        var sheet = new SessionDetailSheet();
        sheet.setArguments(args);
        sheet.show(manager, TAG);
    }

    @Nullable
    @Override
    protected CharSequence title(@NonNull Context context) {
        return null;
    }

    @Override
    protected float minHeightFraction() {
        return MIN_HEIGHT_FRACTION;
    }

    @Override
    protected View createContent(@NonNull LayoutInflater inflater, @NonNull ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
        column = new LinearLayout(requireContext());
        column.setOrientation(LinearLayout.VERTICAL);
        return column;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        var manager = getParentFragmentManager();
        manager.setFragmentResultListener(KEY_EDIT_SCOPE, getViewLifecycleOwner(), (key, result) -> {
            var scope = EditScope.valueOf(result.getString(ScopeSheet.RESULT_SCOPE));
            var navigator = (Navigator) requireActivity();
            var sessionId = requireArguments().getLong(ARG_SESSION_ID);
            dismiss();
            navigator.open(SessionEditorFragment.forSession(sessionId, scope));
        });
        manager.setFragmentResultListener(KEY_DELETE_SCOPE, getViewLifecycleOwner(), (key, result) ->
                delete(EditScope.valueOf(result.getString(ScopeSheet.RESULT_SCOPE))));

        UniPensumApp.from(requireContext()).repository().loadSessionContext(
                requireArguments().getLong(ARG_SESSION_ID), new Database.Callback<SessionContext>() {
                    @Override
                    public void onSuccess(SessionContext context) {
                        if (getView() == null) return;
                        render(context);
                    }

                    @Override
                    public void onError(Exception error) {
                        // The session is gone, for example deleted from another sheet.
                        if (isAdded()) dismiss();
                    }
                });
    }

    private void render(SessionContext context) {
        var app = requireContext();
        var dp = getResources().getDisplayMetrics().density;
        var details = context.session().details();
        column.removeAllViews();

        setSheetTitle(context.course().name());

        column.addView(info(app, dp, SeriesFormat.timeRange(app, details),
                SeriesFormat.weekdayFull(app, context.session().day().getDayOfWeek()) + ", "
                        + TimeFormat.dateMedium(app, context.session().day())
                        + " · " + TimeFormat.typeName(app, details.type())));

        // "Online" and "A2 | Hybrid" already name the format; a room on its own gets "In person" below it.
        var place = SeriesFormat.place(app, details);
        var plainInPerson = details.mode() == Mode.IN_PERSON && !details.hybrid();
        if (place.isEmpty()) {
            column.addView(info(app, dp, getString(R.string.mode_in_person), null));
        } else {
            column.addView(info(app, dp, place, plainInPerson ? getString(R.string.mode_in_person) : null));
        }

        var lecturer = context.lecturer();
        if (lecturer != null) {
            // The large view always spells the name out; the setting only shortens it in the grid and lists.
            column.addView(info(app, dp, lecturer.name(NameStyle.FULL_NAME), getString(R.string.field_lecturer)));
            if (lecturer.email() != null) {
                column.addView(contact(app, dp, R.drawable.ic_mail, lecturer.email(),
                        getString(R.string.lecturer_email), Links::mail));
            }
            if (lecturer.phone() != null) {
                column.addView(contact(app, dp, R.drawable.ic_phone, lecturer.phone(),
                        getString(R.string.lecturer_phone), Links::dial));
            }
        }
        if (details.hasReminder()) {
            column.addView(info(app, dp, ReminderFormat.text(app, details.reminderMin()),
                    getString(R.string.field_reminder)));
        }
        if (details.note() != null) {
            column.addView(info(app, dp, details.note(), getString(R.string.field_note)));
        }

        setFooter(actions(app, dp, context));
    }

    /** Meeting and Moodle on one row (whichever exist), edit and delete on the next. */
    private View actions(Context app, float dp, SessionContext context) {
        var meeting = context.session().details().link();
        var moodle = context.course().moodleLink();

        var actions = new LinearLayout(app);
        actions.setOrientation(LinearLayout.VERTICAL);

        if (meeting != null || moodle != null) {
            var links = new LinearLayout(app);
            links.setBaselineAligned(false);
            if (meeting != null) {
                links.addView(button(app, R.string.action_open_meeting, UpButton.Variant.PRIMARY,
                        v -> open(v, meeting)), weighted(dp, links.getChildCount() > 0));
            }
            if (moodle != null) {
                links.addView(button(app, R.string.action_open_moodle, UpButton.Variant.SECONDARY,
                        v -> open(v, moodle)), weighted(dp, links.getChildCount() > 0));
            }
            actions.addView(links, rowParams(dp, 0, 8));
        }

        var manage = new LinearLayout(app);
        manage.setBaselineAligned(false);
        // The first button of the sheet is the filled one: the meeting if there is one, else Edit.
        manage.addView(button(app, R.string.action_edit,
                meeting != null ? UpButton.Variant.SECONDARY : UpButton.Variant.PRIMARY,
                v -> ScopeSheet.show(getParentFragmentManager(), KEY_EDIT_SCOPE,
                        getString(R.string.scope_edit_title), context.session().day())), weighted(dp, false));
        manage.addView(button(app, R.string.action_delete, UpButton.Variant.DESTRUCTIVE,
                v -> ScopeSheet.show(getParentFragmentManager(), KEY_DELETE_SCOPE,
                        getString(R.string.scope_delete_title), context.session().day())), weighted(dp, true));
        actions.addView(manage, rowParams(dp, 0, 0));
        return actions;
    }

    private void open(View source, String link) {
        if (!Links.openWeb(requireContext(), link)) Haptics.reject(source);
    }

    private void delete(EditScope scope) {
        var repository = UniPensumApp.from(requireContext()).repository();
        repository.deleteSession(requireArguments().getLong(ARG_SESSION_ID), scope, new Database.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (isAdded()) dismiss();
            }

            @Override
            public void onError(Exception error) {
                if (isAdded() && getView() != null) Haptics.reject(column);
            }
        });
    }

    private interface Opener {
        boolean open(Context context, String value);
    }

    /** A row with an icon that hands its value to another app when tapped. */
    private UpRow contact(Context context, float dp, int icon, String value, String label, Opener opener) {
        var row = info(context, dp, value, label);
        row.setLeadingIcon(icon);
        row.setOnClickListener(v -> {
            if (!opener.open(context, value)) Haptics.reject(v);
        });
        return row;
    }

    private UpRow info(Context context, float dp, String title, @Nullable String subtitle) {
        var row = new UpRow(context);
        row.setTitle(title);
        row.setSubtitle(subtitle);
        row.setLayoutParams(rowParams(dp, 0, 8));
        return row;
    }

    private static UpButton button(Context context, int text, UpButton.Variant variant, View.OnClickListener listener) {
        var button = new UpButton(context);
        button.setText(text);
        button.setVariant(variant);
        button.setOnClickListener(listener);
        return button;
    }

    /**
     * Two buttons share a row evenly, with a gap before the second. Both take the height of the taller one,
     * which matters when a label wraps (large font sizes).
     */
    private static LinearLayout.LayoutParams weighted(float dp, boolean gapBefore) {
        var params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        if (gapBefore) params.setMarginStart(Math.round(8 * dp));
        return params;
    }

    private static LinearLayout.LayoutParams rowParams(float dp, int topDp, int bottomDp) {
        var params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = Math.round(topDp * dp);
        params.bottomMargin = Math.round(bottomDp * dp);
        return params;
    }
}
