package de.mcmodersd.unipensum.ui.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import de.mcmodersd.unipensum.R;

/** A card-like row: title (and optional subtitle) on the left, a value and an optional chevron on the right. */
public class UpRow extends LinearLayout {

    private final TextView title;
    private final TextView subtitle;
    private final TextView value;
    private final ImageView chevron;
    private UpIconButton action;
    private View dot;
    private ImageView leadingIcon;

    public UpRow(Context context) {
        this(context, null);
    }

    public UpRow(Context context, AttributeSet attrs) {
        super(context, attrs);
        float dp = getResources().getDisplayMetrics().density;
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setMinimumHeight(Math.round(56 * dp));
        setPadding(Math.round(16 * dp), Math.round(8 * dp), Math.round(12 * dp), Math.round(8 * dp));
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(14 * dp);
        background.setColor(ContextCompat.getColor(context, R.color.surface));
        setBackground(background);

        LinearLayout texts = new LinearLayout(context);
        texts.setOrientation(VERTICAL);
        title = text(context, 16, R.color.text_primary);
        subtitle = text(context, 13, R.color.text_secondary);
        subtitle.setVisibility(GONE);
        texts.addView(title);
        texts.addView(subtitle);
        addView(texts, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

        value = text(context, 16, R.color.text_secondary);
        value.setVisibility(GONE);
        value.setPadding(Math.round(12 * dp), 0, 0, 0);
        addView(value);

        chevron = new ImageView(context);
        chevron.setImageResource(R.drawable.ic_chevron_right);
        ImageViewCompat.setImageTintList(chevron,
                ColorStateList.valueOf(ContextCompat.getColor(context, R.color.text_secondary)));
        chevron.setVisibility(GONE);
        addView(chevron, new LayoutParams(Math.round(20 * dp), Math.round(20 * dp)));

        if (attrs != null) {
            TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.UpRow);
            setTitle(array.getText(R.styleable.UpRow_upTitle));
            setSubtitle(array.getText(R.styleable.UpRow_upSubtitle));
            setValue(array.getText(R.styleable.UpRow_upValue));
            setChevronVisible(array.getBoolean(R.styleable.UpRow_upChevron, false));
            array.recycle();
        }
    }

    public void setTitle(CharSequence text) {
        title.setText(text);
    }

    public void setSubtitle(CharSequence text) {
        subtitle.setText(text);
        subtitle.setVisibility(text == null || text.length() == 0 ? GONE : VISIBLE);
    }

    public void setValue(CharSequence text) {
        value.setText(text);
        value.setVisibility(text == null || text.length() == 0 ? GONE : VISIBLE);
    }

    public void setChevronVisible(boolean visible) {
        chevron.setVisibility(visible ? VISIBLE : GONE);
    }

    /** Shows a colored dot before the title, used for courses. */
    public void setLeadingColor(int color) {
        if (dot == null) {
            float dp = getResources().getDisplayMetrics().density;
            dot = new View(getContext());
            LayoutParams params = new LayoutParams(Math.round(14 * dp), Math.round(14 * dp));
            params.setMarginEnd(Math.round(14 * dp));
            addView(dot, 0, params);
        }
        GradientDrawable oval = new GradientDrawable();
        oval.setShape(GradientDrawable.OVAL);
        oval.setColor(color);
        dot.setBackground(oval);
    }

    /** Shows an icon before the title, in the secondary text color. */
    public void setLeadingIcon(@DrawableRes int iconRes) {
        if (leadingIcon == null) {
            float dp = getResources().getDisplayMetrics().density;
            leadingIcon = new ImageView(getContext());
            ImageViewCompat.setImageTintList(leadingIcon,
                    ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.text_secondary)));
            LayoutParams params = new LayoutParams(Math.round(22 * dp), Math.round(22 * dp));
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
            LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
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

    @Override
    public void setPressed(boolean pressed) {
        super.setPressed(pressed);
        setAlpha(pressed ? 0.6f : 1f);
    }

    private static TextView text(Context context, float sp, int colorRes) {
        TextView view = new TextView(context);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        view.setTextColor(ContextCompat.getColor(context, colorRes));
        return view;
    }
}
