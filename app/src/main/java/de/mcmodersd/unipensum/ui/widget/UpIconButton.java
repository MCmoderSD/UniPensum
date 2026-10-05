package de.mcmodersd.unipensum.ui.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.widget.ImageView;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import de.mcmodersd.unipensum.R;

/** A tappable icon with a 44dp touch target. Set a content description wherever it is used. */
public class UpIconButton extends AppCompatImageView {

    public UpIconButton(Context context) {
        this(context, null);
    }

    public UpIconButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        float dp = getResources().getDisplayMetrics().density;
        int padding = Math.round(10 * dp);
        setPadding(padding, padding, padding, padding);
        setScaleType(ImageView.ScaleType.FIT_CENTER);
        setClickable(true);
        setFocusable(true);
        ImageViewCompat.setImageTintList(this,
                ColorStateList.valueOf(ContextCompat.getColor(context, R.color.text_primary)));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = Math.round(44 * getResources().getDisplayMetrics().density);
        setMeasuredDimension(resolveSize(size, widthMeasureSpec), resolveSize(size, heightMeasureSpec));
    }

    @Override
    public boolean performClick() {
        Haptics.tap(this);
        return super.performClick();
    }

    @Override
    public void setPressed(boolean pressed) {
        super.setPressed(pressed);
        setAlpha(pressed ? 0.5f : 1f);
    }
}
