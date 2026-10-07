package de.mcmodersd.unipensum.debug;

import android.content.Context;
import android.content.Intent;

/** Release counterpart of the debug seeder: does nothing, so no sample data ships. */
public final class DebugSeeder {

    public static final String EXTRA_SEED = "unipensum.debug.seed";

    private DebugSeeder() {
    }

    public static void seed(Context context, Intent intent) {
    }
}
