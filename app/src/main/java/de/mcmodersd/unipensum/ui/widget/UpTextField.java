package de.mcmodersd.unipensum.ui.widget;

import static android.text.InputType.*;
import static android.util.TypedValue.COMPLEX_UNIT_SP;
import static android.view.Gravity.*;
import static android.view.ViewGroup.LayoutParams.*;
import static de.mcmodersd.unipensum.R.color.*;
import static de.mcmodersd.unipensum.R.styleable.*;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.content.ContextCompat;

import java.util.function.Consumer;

import de.mcmodersd.unipensum.R;

/** A labeled single-line text input with an inline error line. */
public class UpTextField extends LinearLayout {

    private final TextView label;
    private final AppCompatEditText input;
    private final TextView error;
    private final GradientDrawable background = new GradientDrawable();
    private final float dp;

    public UpTextField(Context context) {
        this(context, null);
    }

    public UpTextField(Context context, AttributeSet attrs) {
        super(context, attrs);
        dp = getResources().getDisplayMetrics().density;
        setOrientation(VERTICAL);

        label = new TextView(context);
        label.setTextSize(COMPLEX_UNIT_SP, 13);
        label.setTextColor(ContextCompat.getColor(context, text_secondary));
        label.setPadding(Math.round(4 * dp), 0, 0, Math.round(6 * dp));
        addView(label);

        input = new AppCompatEditText(context);
        input.setSingleLine(true);
        input.setInputType(TYPE_CLASS_TEXT | TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setTextSize(COMPLEX_UNIT_SP, 16);
        input.setTextColor(ContextCompat.getColor(context, text_primary));
        input.setHintTextColor(ContextCompat.getColor(context, text_secondary));
        input.setGravity(CENTER_VERTICAL);
        input.setMinHeight(Math.round(52 * dp));
        input.setPadding(Math.round(16 * dp), 0, Math.round(16 * dp), 0);
        background.setCornerRadius(14 * dp);
        background.setColor(ContextCompat.getColor(context, surface));
        input.setBackground(background);
        input.setOnFocusChangeListener((v, focused) -> updateStroke(focused));
        addView(input, new LayoutParams(MATCH_PARENT, WRAP_CONTENT));

        error = new TextView(context);
        error.setTextSize(COMPLEX_UNIT_SP, 13);
        error.setTextColor(ContextCompat.getColor(context, danger));
        error.setPadding(Math.round(4 * dp), Math.round(6 * dp), 0, 0);
        error.setVisibility(GONE);
        addView(error);

        if (attrs != null) {
            try (var array = context.obtainStyledAttributes(attrs, R.styleable.UpTextField)) {
                label.setText(array.getText(UpTextField_upLabel));
                input.setHint(array.getText(UpTextField_upHint));
            }
        }
        label.setVisibility(label.length() == 0 ? GONE : VISIBLE);
    }

    public void setLabel(CharSequence text) {
        label.setText(text);
        label.setVisibility(TextUtils.isEmpty(text) ? GONE : VISIBLE);
    }

    public void setHint(CharSequence hint) {
        input.setHint(hint);
    }

    public String getText() {
        return input.getText() == null ? "" : input.getText().toString();
    }

    /** A copy of the text as characters, for passwords: unlike a String it can be wiped after use. */
    public char[] getTextChars() {
        var text = input.getText();
        if (text == null) return new char[0];
        var chars = new char[text.length()];
        text.getChars(0, chars.length, chars, 0);
        return chars;
    }

    /**
     * Turns the field into a password field: the text is hidden, the keyboard is told not to learn or
     * suggest it, and the autofill service leaves it alone.
     */
    public void setPassword() {
        input.setInputType(TYPE_CLASS_TEXT | TYPE_TEXT_VARIATION_PASSWORD);
        input.setImportantForAutofill(IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
    }

    public void setText(CharSequence text) {
        input.setText(text);
    }

    /** Shows the message below the field, or clears it for {@code null}. */
    public void setError(CharSequence message) {
        error.setText(message);
        error.setVisibility(message == null ? GONE : VISIBLE);
        updateStroke(input.hasFocus());
    }

    /** Reports every edit of the text. */
    public void addTextWatcher(Consumer<String> onChange) {
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                onChange.accept(s.toString());
            }
        });
    }

    /** Lets the field grow over several lines, for notes. */
    public void setMultiLine(int minLines) {
        input.setSingleLine(false);
        input.setMinLines(minLines);
        input.setGravity(TOP | START);
        input.setInputType(TYPE_CLASS_TEXT | TYPE_TEXT_FLAG_MULTI_LINE | TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setPadding(input.getPaddingLeft(), Math.round(14 * dp), input.getPaddingRight(), Math.round(14 * dp));
    }

    /** Stops typing and pasting at {@code maxLength} characters, the limits of {@code TextSanitizer}. */
    public void setMaxLength(int maxLength) {
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLength)});
    }

    /** One of the {@link InputType} class constants, for example {@link InputType#TYPE_TEXT_VARIATION_URI}. */
    public void setInputType(int inputType) {
        input.setInputType(inputType);
    }

    private void updateStroke(boolean focused) {
        var hasError = error.getVisibility() == VISIBLE;
        if (hasError) {
            background.setStroke(Math.round(1.5f * dp), ContextCompat.getColor(getContext(), danger));
        } else if (focused) {
            background.setStroke(Math.round(1.5f * dp), ContextCompat.getColor(getContext(), accent));
        } else {
            background.setStroke(0, 0);
        }
    }
}