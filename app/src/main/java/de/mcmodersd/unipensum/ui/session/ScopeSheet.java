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

import java.time.LocalDate;

import de.mcmodersd.unipensum.R;
import de.mcmodersd.unipensum.domain.model.EditScope;
import de.mcmodersd.unipensum.ui.format.TimeFormat;
import de.mcmodersd.unipensum.ui.widget.UpRow;
import de.mcmodersd.unipensum.ui.widget.UpSheet;

/**
 * Asks how far an edit or deletion reaches: this session, this and all following, or the whole series.
 * Delivers {@link #RESULT_SCOPE} (an {@link EditScope} name) under the request key.
 */
public final class ScopeSheet extends UpSheet {

    public static final String RESULT_SCOPE = "scope";

    private static final String ARG_KEY = "key";
    private static final String ARG_TITLE = "title";
    private static final String ARG_DAY = "day";

    /** @param day the date of the session in question, shown to make the options concrete */
    public static void show(FragmentManager manager, String requestKey, CharSequence title, LocalDate day) {
        var args = new Bundle();
        args.putString(ARG_KEY, requestKey);
        args.putCharSequence(ARG_TITLE, title);
        args.putLong(ARG_DAY, day.toEpochDay());
        var sheet = new ScopeSheet();
        sheet.setArguments(args);
        sheet.show(manager, "scope:" + requestKey);
    }

    @Nullable
    @Override
    protected CharSequence title(@NonNull Context context) {
        return requireArguments().getCharSequence(ARG_TITLE);
    }

    @Override
    protected View createContent(@NonNull LayoutInflater inflater, @NonNull ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
        var context = requireContext();
        var dp = getResources().getDisplayMetrics().density;
        var day = LocalDate.ofEpochDay(requireArguments().getLong(ARG_DAY));
        var date = TimeFormat.dateMedium(context, day);

        var column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.addView(row(context, dp, EditScope.THIS_ONLY, R.string.scope_this_only, date));
        column.addView(row(context, dp, EditScope.THIS_AND_FOLLOWING, R.string.scope_this_and_following,
                getString(R.string.scope_from, date)));
        column.addView(row(context, dp, EditScope.ALL, R.string.scope_all, null));
        return column;
    }

    private UpRow row(Context context, float dp, EditScope scope, int titleRes, @Nullable String subtitle) {
        var row = new UpRow(context);
        row.setTitle(getString(titleRes));
        row.setSubtitle(subtitle);
        row.setChevronVisible(true);
        row.setOnClickListener(v -> {
            var result = new Bundle();
            result.putString(RESULT_SCOPE, scope.name());
            getParentFragmentManager().setFragmentResult(requireArguments().getString(ARG_KEY), result);
            dismiss();
        });
        var params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = Math.round(8 * dp);
        row.setLayoutParams(params);
        return row;
    }
}
