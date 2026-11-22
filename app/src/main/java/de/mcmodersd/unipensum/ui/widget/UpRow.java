package de.mcmodersd.unipensum.ui.widget;

import static android.util.TypedValue.COMPLEX_UNIT_SP;
import static android.view.Gravity.CENTER_VERTICAL;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static de.mcmodersd.unipensum.R.color.*;
import static de.mcmodersd.unipensum.R.drawable.ic_chevron_right;
import static de.mcmodersd.unipensum.R.styleable.*;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.widget.ImageViewCompat;

import de.mcmodersd.unipensum.R;

/** A card-like row: title (and optional subtitle) on the left, a value and an optional chevron on the right. */
public class UpRow extends LinearLayout {

    private static final int VALUE_GAP_DP = 12;

    private final LinearLayout texts;
    private final TextView title;
    private final TextView subtitle;
    private final TextView value;
    private final ImageView chevron;
    private UpIconButton action;
    private View dot;
    private ImageView leadingIcon;
    /** Whether the value stands under the texts because there is no room for it beside them. */
    private boolean valueBelow;
    /** The widths the decision about the value needs, measured once per text and not on every measure; -1 for not known. */
    private float wordsWidth = -1;
    private float valueWidth = -1;

    public UpRow(Context context) {
        this(context, null);
    }

    public UpRow(Context context, AttributeSet attrs) {
        super(context, attrs);
        var dp = getResources().getDisplayMetrics().density;
        setOrientation(HORIZONTAL);
        setGravity(CENTER_VERTICAL);
        setMinimumHeight(Math.round(56 * dp));
        setPaddingRelative(Math.round(16 * dp), Math.round(8 * dp), Math.round(12 * dp), Math.round(8 * dp));
        var background = new GradientDrawable();
        background.setCornerRadius(14 * dp);
        background.setColor(ContextCompat.getColor(context, surface));
        setBackground(background);
        FocusRing.install(this, 14);
        // A row is one thing to a screen reader, read as its title and its subtitle, also where it cannot be tapped.
        ViewCompat.setScreenReaderFocusable(this, true);

        texts = new LinearLayout(context);
        texts.setOrientation(VERTICAL);
        title = text(context, 16, text_primary);
        subtitle = text(context, 13, text_secondary);
        subtitle.setVisibility(GONE);
        texts.addView(title);
        texts.addView(subtitle);
        addView(texts, new LayoutParams(0, WRAP_CONTENT, 1f));

        value = text(context, 16, text_secondary);
        value.setVisibility(GONE);
        value.setPaddingRelative(Math.round(VALUE_GAP_DP * dp), 0, 0, 0);
        addView(value);

        chevron = new ImageView(context);
        chevron.setImageResource(ic_chevron_right);
        ImageViewCompat.setImageTintList(
                chevron,
                ColorStateList.valueOf(ContextCompat.getColor(context, text_secondary))
        );
        chevron.setVisibility(GONE);
        addView(chevron, new LayoutParams(Math.round(20 * dp), Math.round(20 * dp)));

        if (attrs != null) {
            try (var array = context.obtainStyledAttributes(attrs, R.styleable.UpRow)) {
                setTitle(array.getText(UpRow_upTitle));
                setSubtitle(array.getText(UpRow_upSubtitle));
                setValue(array.getText(UpRow_upValue));
                setChevronVisible(array.getBoolean(UpRow_upChevron, false));
            }
        }
    }

    /**
     * With a large system font the value can take so much of the row that the title is left with a letter or two per
     * line, as "UniPensum" beside "Version 1.0". Then the value moves under the texts, where it has the whole width.
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED) {
            var below = valueNeedsOwnLine(MeasureSpec.getSize(widthMeasureSpec));
            if (below != valueBelow) placeValue(below);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    /** @return whether the longest word of the texts and the value together are wider than the row */
    private boolean valueNeedsOwnLine(int width) {
        if (value.getVisibility() != VISIBLE) return false;
        var dp = getResources().getDisplayMetrics().density;
        var fixed = getPaddingStart() + getPaddingEnd() + VALUE_GAP_DP * dp;
        for (var i = 0; i < getChildCount(); i++) {
            var child = getChildAt(i);
            if (child == texts || child == value || child.getVisibility() == GONE) continue;
            var params = (LayoutParams) child.getLayoutParams();
            // An icon button measures itself.
            fixed += (params.width > 0 ? params.width : UpIconButton.SIZE_DP * dp) + params.getMarginStart() + params.getMarginEnd();
        }
        if (wordsWidth < 0) wordsWidth = Math.max(longestWord(title), longestWord(subtitle));
        if (valueWidth < 0) valueWidth = value.getPaint().measureText(value.getText().toString());
        return wordsWidth + valueWidth + fixed > width;
    }

    private static float longestWord(TextView view) {
        var longest = 0f;
        for (var word : view.getText().toString().split("\\s+")) {
            longest = Math.max(longest, view.getPaint().measureText(word));
        }
        return longest;
    }

    private void placeValue(boolean below) {
        valueBelow = below;
        var dp = getResources().getDisplayMetrics().density;
        if (below) {
            removeView(value);
            value.setPaddingRelative(0, 0, 0, 0);
            texts.addView(value);
        } else {
            texts.removeView(value);
            value.setPaddingRelative(Math.round(VALUE_GAP_DP * dp), 0, 0, 0);
            addView(value, indexOfChild(texts) + 1);
        }
    }

    public void setTitle(CharSequence text) {
        title.setText(text);
        wordsWidth = -1;
    }

    /**
     * A phone number, an address or a link has no language of its own: left to the direction of the layout, the groups
     * of digits of a number come out in reverse order. It reads from left to right whatever the layout.
     */
    public void keepTitleLeftToRight() {
        title.setTextDirection(TEXT_DIRECTION_LTR);
    }

    public void setSubtitle(CharSequence text) {
        subtitle.setText(text);
        subtitle.setVisibility(TextUtils.isEmpty(text) ? GONE : VISIBLE);
        wordsWidth = -1;
    }

    public void setValue(CharSequence text) {
        value.setText(text);
        value.setVisibility(TextUtils.isEmpty(text) ? GONE : VISIBLE);
        valueWidth = -1;
    }

    public void setChevronVisible(boolean visible) {
        chevron.setVisibility(visible ? VISIBLE : GONE);
    }

    /** Shows a colored dot before the title, used for courses. */
    public void setLeadingColor(int color) {
        if (dot == null) {
            var dp = getResources().getDisplayMetrics().density;
            dot = new View(getContext());
            var params = new LayoutParams(Math.round(14 * dp), Math.round(14 * dp));
            params.setMarginEnd(Math.round(14 * dp));
            addView(dot, 0, params);
        }
        var oval = new GradientDrawable();
        oval.setShape(GradientDrawable.OVAL);
        oval.setColor(color);
        dot.setBackground(oval);
    }

    /** Shows an icon before the title, in the secondary text color. */
    public void setLeadingIcon(@DrawableRes int iconRes) {
        if (leadingIcon == null) {
            var dp = getResources().getDisplayMetrics().density;
            leadingIcon = new ImageView(getContext());
            ImageViewCompat.setImageTintList(
                    leadingIcon,
                    ColorStateList.valueOf(ContextCompat.getColor(getContext(), text_secondary))
            );
            var params = new LayoutParams(Math.round(22 * dp), Math.round(22 * dp));
            params.setMarginEnd(Math.round(14 * dp));
            addView(leadingIcon, 0, params);
        }
        leadingIcon.setImageResource(iconRes);
    }

    /** Replaces the chevron at the end of the row with another icon, such as a check mark. */
    public void setTrailingIcon(@DrawableRes int iconRes) {
        chevron.setImageResource(iconRes);
        chevron.setVisibility(VISIBLE);
    }

    /** Adds a separate icon button at the end of the row, next to the row's own click action. */
    public void setAction(int iconRes, CharSequence description, OnClickListener listener) {
        if (action == null) {
            action = new UpIconButton(getContext());
            var params = new LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            params.setMarginStart(Math.round(4 * getResources().getDisplayMetrics().density));
            addView(action, params);
        }
        action.setImageResource(iconRes);
        action.setContentDescription(description);
        action.setOnClickListener(listener);
    }

    @Override
    public boolean performClick() {
        Haptics.tap(this);
        return super.performClick();
    }

    /** A row that can be tapped is a button to a screen reader; one that only shows a fact is not. */
    @Override
    public CharSequence getAccessibilityClassName() {
        return hasOnClickListeners() ? Button.class.getName() : super.getAccessibilityClassName();
    }

    @Override
    public void setPressed(boolean pressed) {
        super.setPressed(pressed);
        setAlpha(pressed ? 0.6f : 1f);
    }

    private static TextView text(Context context, float sp, int colorRes) {
        var view = new TextView(context);
        view.setTextSize(COMPLEX_UNIT_SP, sp);
        view.setTextColor(ContextCompat.getColor(context, colorRes));
        return view;
    }
}