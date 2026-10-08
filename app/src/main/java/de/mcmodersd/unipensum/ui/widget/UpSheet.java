package de.mcmodersd.unipensum.ui.widget;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import de.mcmodersd.unipensum.R;

/**
 * Base of every sheet in the app. Material only supplies the drag and back-gesture mechanics;
 * the look (surface, corners, handle, title) comes from the app theme and {@code sheet_base.xml}.
 * Subclasses provide the title and the content. Results go back through the fragment result API.
 * <p>
 * A sheet is closed by dragging its handle or title down, by tapping outside, or with the back
 * gesture. Gestures on the content never move it, so scrolling a picker cannot dismiss it by accident.
 */
public abstract class UpSheet extends BottomSheetDialogFragment {

    /** @return the title, or {@code null} for a sheet without one */
    @Nullable
    protected abstract CharSequence title(@NonNull Context context);

    protected abstract View createContent(@NonNull LayoutInflater inflater, @NonNull ViewGroup container,
                                          @Nullable Bundle savedInstanceState);

    /**
     * How much of the screen height the sheet takes at least, from 0 to 1. The default is 0: as tall as
     * its content. A sheet with more content than fits scrolls it, whatever this returns.
     */
    protected float minHeightFraction() {
        return 0f;
    }

    /** Gap kept between a tall sheet and the top of the screen (below the status bar). */
    private static final int TOP_GAP_DP = 8;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        var root = inflater.inflate(R.layout.sheet_base, container, false);

        TextView title = root.findViewById(R.id.sheet_title);
        var text = title(requireContext());
        title.setText(text);
        title.setVisibility(text == null ? View.GONE : View.VISIBLE);

        ViewGroup content = root.findViewById(R.id.sheet_content);
        content.addView(createContent(inflater, content, savedInstanceState));

        var fraction = minHeightFraction();
        if (fraction > 0f) {
            root.setMinimumHeight(Math.round(fraction * getResources().getDisplayMetrics().heightPixels));
        }

        // Keeps the content above the navigation bar and, while typing, above the keyboard, and a tall
        // sheet below the status bar, so that it keeps its rounded top corners.
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            var bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            view.setPadding(0, 0, 0, bottom.bottom);
            limitHeight(insets.getInsets(WindowInsetsCompat.Type.systemBars()).top);
            return insets;
        });
        return root;
    }

    private void limitHeight(int statusBarTop) {
        if (!(getDialog() instanceof BottomSheetDialog)) return;
        var dp = getResources().getDisplayMetrics().density;
        var screen = getResources().getDisplayMetrics().heightPixels;
        ((BottomSheetDialog) getDialog()).getBehavior().setMaxHeight(screen - statusBarTop - Math.round(TOP_GAP_DP * dp));
    }

    /**
     * Puts a view (usually the action buttons) below the content. It stays in place while the content
     * scrolls. Pass {@code null} to remove it. Call once the view exists.
     */
    protected final void setFooter(@Nullable View view) {
        ViewGroup footer = requireView().findViewById(R.id.sheet_footer);
        footer.removeAllViews();
        if (view != null) footer.addView(view);
        footer.setVisibility(view == null ? View.GONE : View.VISIBLE);
    }

    /** For sheets whose title is only known after their data has loaded. */
    protected final void setSheetTitle(@Nullable CharSequence text) {
        var root = getView();
        if (root == null) return;
        TextView title = root.findViewById(R.id.sheet_title);
        title.setText(text);
        title.setVisibility(text == null ? View.GONE : View.VISIBLE);
    }

    /** Shows an icon button next to the title, or replaces the one already there. Call once the view exists. */
    protected final void setHeaderAction(@DrawableRes int icon, CharSequence description, View.OnClickListener listener) {
        UpIconButton action = requireView().findViewById(R.id.sheet_action);
        action.setImageResource(icon);
        action.setContentDescription(description);
        action.setOnClickListener(listener);
        action.setVisibility(View.VISIBLE);
    }

    /**
     * Makes the sheet draggable by its header only. Material would let any gesture on the content
     * drag the sheet as soon as that content cannot scroll further, which closes pickers by accident.
     * The decision is taken for each gesture on its first touch, before any view sees the event.
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new BottomSheetDialog(requireContext(), getTheme()) {
            @Override
            public boolean dispatchTouchEvent(@NonNull MotionEvent event) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    getBehavior().setDraggable(isOnHeader(event.getRawX(), event.getRawY()));
                }
                return super.dispatchTouchEvent(event);
            }
        };
    }

    private boolean isOnHeader(float rawX, float rawY) {
        var root = getView();
        if (root == null) return false;
        View header = root.findViewById(R.id.sheet_header);
        var location = new int[2];
        header.getLocationOnScreen(location);
        return rawX >= location[0] && rawX < location[0] + header.getWidth()
                && rawY >= location[1] && rawY < location[1] + header.getHeight();
    }

    @Override
    public void onStart() {
        super.onStart();
        var dialog = (BottomSheetDialog) requireDialog();
        BottomSheetBehavior<?> behavior = dialog.getBehavior();
        behavior.setSkipCollapsed(true);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }
}