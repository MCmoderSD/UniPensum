package de.mcmodersd.unipensum.ui.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import de.mcmodersd.unipensum.R;

/**
 * A vertical wheel that snaps to one of its values; values far from the center fade out.
 * Tapping a value scrolls it into the center. The wheel keeps every vertical gesture that starts
 * on it, so scrolling it never drags or closes the sheet it sits in.
 */
// The labels are replaced as a whole.
@SuppressLint("NotifyDataSetChanged")
public class WheelPicker extends RecyclerView {

    public interface OnSelectedListener {
        void onSelected(int index);
    }

    private static final int VISIBLE_ITEMS = 5;

    private final int itemHeight;
    private final LinearSnapHelper snapHelper = new LinearSnapHelper();
    private final LinearLayoutManager layoutManager;
    private String[] labels = new String[0];
    private int selected;
    /** The value in the center row the last time the wheel moved, to notice when the next one arrives. */
    private int centered = RecyclerView.NO_POSITION;
    private OnSelectedListener listener;

    public WheelPicker(Context context) {
        this(context, null);
    }

    public WheelPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        itemHeight = Math.round(44 * getResources().getDisplayMetrics().density);
        layoutManager = new LinearLayoutManager(context);
        setLayoutManager(layoutManager);
        setOverScrollMode(OVER_SCROLL_NEVER);
        setClipToPadding(false);
        setPadding(0, itemHeight * (VISIBLE_ITEMS / 2), 0, itemHeight * (VISIBLE_ITEMS / 2));
        // Scrolling must not be offered to the surrounding sheet, which would move instead of the wheel.
        setNestedScrollingEnabled(false);
        snapHelper.attachToRecyclerView(this);
        setAdapter(new LabelAdapter());
        addOnChildAttachStateChangeListener(new OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(@NonNull View view) {
                post(WheelPicker.this::updateAlphas);
            }

            @Override
            public void onChildViewDetachedFromWindow(@NonNull View view) { }
        });
    }

    public void setLabels(String... labels) {
        this.labels = labels;
        getAdapter().notifyDataSetChanged();
    }

    /** The value in the center row; while the wheel is still settling, the one it is settling on. */
    public int getSelectedIndex() {
        var snapView = snapHelper.findSnapView(layoutManager);
        if (snapView != null && !isLayoutRequested()) {
            var index = getChildAdapterPosition(snapView);
            if (index != RecyclerView.NO_POSITION) return index;
        }
        return selected;
    }

    /** Jumps to the value without notifying the listener. */
    public void setSelectedIndex(int index) {
        selected = Math.max(0, Math.min(labels.length - 1, index));
        layoutManager.scrollToPositionWithOffset(selected, 0);
        post(this::updateAlphas);
    }

    public void setOnSelectedListener(OnSelectedListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, MeasureSpec.makeMeasureSpec(itemHeight * VISIBLE_ITEMS, MeasureSpec.EXACTLY));
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        claimGesture(event);
        return super.onInterceptTouchEvent(event);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        claimGesture(event);
        return super.onTouchEvent(event);
    }

    /** From the first touch on, no ancestor may take the gesture away, in either direction. */
    private void claimGesture(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN && getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
    }

    @Override
    public void onScrolled(int dx, int dy) {
        super.onScrolled(dx, dy);
        updateAlphas();
        tickOnNewValue();
    }

    /** Every value that reaches the center row while the wheel turns is felt, also while it runs out. */
    private void tickOnNewValue() {
        var snapView = snapHelper.findSnapView(layoutManager);
        if (snapView == null) return;
        var index = getChildAdapterPosition(snapView);
        if (index == RecyclerView.NO_POSITION || index == centered) return;
        centered = index;
        // A jump to a value (setSelectedIndex) moves the center without the wheel turning, which is silent.
        if (getScrollState() != SCROLL_STATE_IDLE) Haptics.segmentTick(this);
    }

    @Override
    public void onScrollStateChanged(int state) {
        super.onScrollStateChanged(state);
        if (state != SCROLL_STATE_IDLE) return;
        var snapView = snapHelper.findSnapView(layoutManager);
        if (snapView == null) return;
        var index = getChildAdapterPosition(snapView);
        if (index != RecyclerView.NO_POSITION && index != selected) {
            selected = index;
            if (listener != null) listener.onSelected(index);
        }
    }

    /** Animates the value into the center row. */
    private void scrollToIndex(int index) {
        LinearSmoothScroller scroller = new LinearSmoothScroller(getContext()) {
            @Override
            protected int getVerticalSnapPreference() {
                // The top padding is two rows high, so "start" is the center row.
                return SNAP_TO_START;
            }
        };
        scroller.setTargetPosition(index);
        layoutManager.startSmoothScroll(scroller);
    }

    private void updateAlphas() {
        var center = getPaddingTop() + itemHeight / 2f;
        for (var i = 0; i < getChildCount(); i++) {
            var child = getChildAt(i);
            var distance = Math.abs((child.getTop() + child.getBottom()) / 2f - center);
            child.setAlpha(Math.max(0.2f, 1f - distance / (itemHeight * 2.4f)));
        }
    }

    private final class LabelAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            var view = new TextView(parent.getContext());
            view.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, itemHeight));
            view.setGravity(Gravity.CENTER);
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
            view.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            view.setTextColor(ContextCompat.getColor(parent.getContext(), R.color.text_primary));
            RecyclerView.ViewHolder holder = new RecyclerView.ViewHolder(view) {
            };
            view.setOnClickListener(v -> {
                var position = holder.getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION && position != selected) scrollToIndex(position);
            });
            return holder;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ((TextView) holder.itemView).setText(labels[position]);
        }

        @Override
        public int getItemCount() {
            return labels.length;
        }
    }
}