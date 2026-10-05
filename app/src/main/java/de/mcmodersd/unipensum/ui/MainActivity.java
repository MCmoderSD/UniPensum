package de.mcmodersd.unipensum.ui;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import de.mcmodersd.unipensum.R;
import de.mcmodersd.unipensum.debug.DebugSeeder;
import de.mcmodersd.unipensum.ui.week.WeekFragment;

public class MainActivity extends AppCompatActivity implements Navigator {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.container), (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return windowInsets;
        });

        if (savedInstanceState == null) {
            // Only does something in debug builds: adb shell am start ... --ez unipensum.debug.seed true
            if (getIntent().getBooleanExtra(DebugSeeder.EXTRA_SEED, false)) {
                DebugSeeder.seed(this);
            }
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, new WeekFragment())
                    .commit();
        }
    }

    @Override
    public void push(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .setCustomAnimations(R.animator.screen_enter, R.animator.screen_exit,
                        R.animator.screen_pop_enter, R.animator.screen_pop_exit)
                .replace(R.id.container, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void pop() {
        getSupportFragmentManager().popBackStack();
    }
}
