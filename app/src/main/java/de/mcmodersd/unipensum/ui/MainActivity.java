package de.mcmodersd.unipensum.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import de.mcmodersd.unipensum.R;
import de.mcmodersd.unipensum.debug.DebugSeeder;
import de.mcmodersd.unipensum.reminder.ReminderScheduler;
import de.mcmodersd.unipensum.ui.session.SessionDetailSheet;
import de.mcmodersd.unipensum.ui.week.WeekFragment;
import de.mcmodersd.unipensum.ui.widget.PaneLayout;

/**
 * The one activity. The week calendar lives in its own container and stays there; the pages that open
 * from it (settings, courses, editors) stack in the side pane, see {@link PaneLayout}. The back stack
 * only ever holds those pages, so "the pane is open" is the same as "the back stack is not empty".
 */
public class MainActivity extends AppCompatActivity implements Navigator {

    /** Phones stay upright; from this smallest width on a device is a tablet and may turn. */
    private static final int TABLET_MIN_SMALLEST_WIDTH_DP = 600;
    /**
     * The fragment tag of the first page of the pane. It stays with the page when its view is rebuilt, which
     * is more reliable than counting back stack entries while a page is leaving.
     */
    private static final String FIRST_PAGE = "first-page";
    /** Set by a reminder: the session the notification is about, which opens as its sheet. */
    public static final String EXTRA_SESSION_ID = "unipensum.session_id";

    private PaneLayout pane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        lockPhonesToPortrait();
        setContentView(R.layout.activity_main);

        pane = findViewById(R.id.pane);
        View side = findViewById(R.id.side);
        ViewCompat.setOnApplyWindowInsetsListener(pane, (view, windowInsets) -> {
            var bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
            );
            var ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            // Only the pane holds text fields, so only it makes room for the keyboard.
            side.setPadding(0, 0, 0, Math.max(0, ime.bottom - bars.bottom));
            return windowInsets;
        });

        var manager = getSupportFragmentManager();
        manager.registerFragmentLifecycleCallbacks(new CloseIcon(), false);
        manager.addOnBackStackChangedListener(() -> pane.setSideOpen(paneHasPage(), true));
        // After the screen was recreated, the pane is open again right away, without sliding in. Nothing is
        // changing at this point, so the count of the back stack can be trusted here, unlike in the listener.
        pane.setSideOpen(manager.getBackStackEntryCount() > 0, false);

        if (savedInstanceState == null) {
            seedIfAsked(getIntent());
            manager.beginTransaction()
                    .replace(R.id.container, new WeekFragment())
                    .commit();
            openSessionOfIntent(getIntent());
        }
    }

    /** The app is already open when a reminder is tapped: the same activity gets the new intent. */
    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        seedIfAsked(intent);
        openSessionOfIntent(intent);
    }

    /**
     * Only does something in debug builds: adb shell am start ... --ez unipensum.debug.seed true. The extra is
     * taken from the intent, so it applies once, also when the app was already open (a new intent then).
     */
    private void seedIfAsked(Intent intent) {
        if (!intent.getBooleanExtra(DebugSeeder.EXTRA_SEED, false)) return;
        intent.removeExtra(DebugSeeder.EXTRA_SEED);
        DebugSeeder.seed(this, intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Reminders that came due while the app was not running, and the alarm for the next one.
        ReminderScheduler.update(this, true, null);
    }

    private void openSessionOfIntent(Intent intent) {
        var sessionId = intent.getLongExtra(EXTRA_SESSION_ID, 0);
        if (sessionId == 0) return;
        // Taken from the intent, so turning the screen or coming back does not open it again.
        intent.removeExtra(EXTRA_SESSION_ID);
        var manager = getSupportFragmentManager();
        var shown = manager.findFragmentByTag(SessionDetailSheet.TAG);
        if (shown instanceof DialogFragment) ((DialogFragment) shown).dismissAllowingStateLoss();
        SessionDetailSheet.show(manager, sessionId);
    }

    /**
     * Asked for in code, not in the manifest: a manifest value would also lock tablets on Android 12 to 15,
     * while a tablet is the one device where turning is wanted.
     */
    @SuppressLint("SourceLockedOrientationActivity")
    private void lockPhonesToPortrait() {
        var tablet = getResources().getConfiguration().smallestScreenWidthDp >= TABLET_MIN_SMALLEST_WIDTH_DP;
        setRequestedOrientation(
                tablet
                        ? ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        );
    }

    @Override
    public void push(Fragment fragment) {
        show(fragment, false);
    }

    @Override
    public void pop() {
        getSupportFragmentManager().popBackStack();
    }

    @Override
    public void open(Fragment fragment) {
        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        show(fragment, true);
    }

    @Override
    public void toggle(Fragment fragment) {
        var current = getSupportFragmentManager().findFragmentById(R.id.side);
        var onlyThisPage = current != null && FIRST_PAGE.equals(current.getTag())
                && current.getClass() == fragment.getClass();
        if (onlyThisPage) pop();
        else open(fragment);
    }

    /**
     * Whether the pane holds a page. Asked of the fragment itself: while a page is leaving with the back
     * gesture, the manager still counts its back stack entry, and a page that is animating out is still known
     * to it but no longer added.
     */
    private boolean paneHasPage() {
        var page = getSupportFragmentManager().findFragmentById(R.id.side);
        return page != null && page.isAdded();
    }

    private void show(Fragment fragment, boolean firstPage) {
        var transaction = getSupportFragmentManager().beginTransaction().setReorderingAllowed(true);
        if (firstPage && pane.isSplit()) {
            // The pane slides in and out by itself; the page only has to stay put while it does.
            transaction.setCustomAnimations(0, 0, 0, R.animator.pane_hold);
        } else {
            transaction.setCustomAnimations(
                    R.animator.screen_enter, R.animator.screen_exit,
                    R.animator.screen_pop_enter, R.animator.screen_pop_exit
            );
        }
        transaction.replace(R.id.side, fragment, firstPage ? FIRST_PAGE : null).addToBackStack(null).commit();
    }

    /**
     * The first page of a wide pane has nothing to go back to, so its button closes the pane and says so.
     * Done here once, so the pages keep a plain back button that always calls {@link #pop}.
     */
    private void updateBackIcon(@Nullable Fragment page) {
        View view = page == null ? null : page.getView();
        ImageView back = view == null ? null : view.findViewById(R.id.back);
        if (back == null) return;
        var closes = pane.isSplit() && page != null && FIRST_PAGE.equals(page.getTag());
        back.setImageResource(closes ? R.drawable.ic_close : R.drawable.ic_chevron_left);
        back.setContentDescription(getString(closes ? R.string.action_close : R.string.action_back));
    }

    private final class CloseIcon extends FragmentManager.FragmentLifecycleCallbacks {
        @Override
        public void onFragmentViewCreated(@NonNull FragmentManager manager, @NonNull Fragment fragment,
                                          @NonNull View view, @Nullable Bundle savedInstanceState) {
            if (fragment.getId() == R.id.side) updateBackIcon(fragment);
        }
    }
}