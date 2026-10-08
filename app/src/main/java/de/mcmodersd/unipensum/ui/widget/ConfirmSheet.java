package de.mcmodersd.unipensum.ui.widget;

import android.content.Context;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import de.mcmodersd.unipensum.R;

/**
 * Asks before something irreversible. Delivers {@link #RESULT_CONFIRMED} under the request key
 * only when the user confirms; cancelling just closes the sheet.
 */
public final class ConfirmSheet extends UpSheet {

    public static final String RESULT_CONFIRMED = "confirmed";

    private static final String ARG_KEY = "key";
    private static final String ARG_TITLE = "title";
    private static final String ARG_MESSAGE = "message";
    private static final String ARG_CONFIRM = "confirm";
    private static final String ARG_DESTRUCTIVE = "destructive";

    public static void show(FragmentManager manager, String requestKey, CharSequence title, CharSequence message,
                            CharSequence confirmLabel, boolean destructive) {
        var args = new Bundle();
        args.putString(ARG_KEY, requestKey);
        args.putCharSequence(ARG_TITLE, title);
        args.putCharSequence(ARG_MESSAGE, message);
        args.putCharSequence(ARG_CONFIRM, confirmLabel);
        args.putBoolean(ARG_DESTRUCTIVE, destructive);
        var sheet = new ConfirmSheet();
        sheet.setArguments(args);
        sheet.show(manager, "confirm:" + requestKey);
    }

    @Nullable
    @Override
    protected CharSequence title(@NonNull Context context) {
        return requireArguments().getCharSequence(ARG_TITLE);
    }

    @Override
    protected View createContent(@NonNull LayoutInflater inflater, @NonNull ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
        var args = requireArguments();
        var context = requireContext();
        var dp = getResources().getDisplayMetrics().density;

        var column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);

        var message = new TextView(context);
        message.setText(args.getCharSequence(ARG_MESSAGE));
        message.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        message.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
        message.setPadding(Math.round(8 * dp), 0, Math.round(8 * dp), Math.round(20 * dp));
        column.addView(message);

        var confirm = new UpButton(context);
        confirm.setText(args.getCharSequence(ARG_CONFIRM));
        confirm.setVariant(args.getBoolean(ARG_DESTRUCTIVE) ? UpButton.Variant.DESTRUCTIVE : UpButton.Variant.PRIMARY);
        confirm.setOnClickListener(v -> {
            Haptics.confirm(v);
            var result = new Bundle();
            result.putBoolean(RESULT_CONFIRMED, true);
            getParentFragmentManager().setFragmentResult(args.getString(ARG_KEY), result);
            dismiss();
        });
        column.addView(
                confirm, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        var cancel = new UpButton(context);
        cancel.setText(R.string.action_cancel);
        cancel.setVariant(UpButton.Variant.SECONDARY);
        cancel.setOnClickListener(v -> dismiss());
        var cancelParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cancelParams.topMargin = Math.round(8 * dp);
        column.addView(cancel, cancelParams);
        return column;
    }
}
