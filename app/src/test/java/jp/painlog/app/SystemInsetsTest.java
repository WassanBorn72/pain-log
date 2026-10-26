package jp.painlog.app;

import android.graphics.Insets;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {30, 35})
public class SystemInsetsTest {
    private WindowInsets insets(int left, int top, int right, int bottom, int keyboard) {
        int safe = WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout();
        return new WindowInsets.Builder()
            .setInsets(safe, Insets.of(left, top, right, bottom))
            .setInsetsIgnoringVisibility(safe, Insets.of(left, top, right, bottom))
            .setInsets(WindowInsets.Type.ime(), Insets.of(0, 0, 0, keyboard))
            .setVisible(WindowInsets.Type.ime(), keyboard > 0).build();
    }
    @Test public void activityAppliesInsetsWithoutAccumulatingPadding() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            View root = ((ViewGroup)controller.get().findViewById(android.R.id.content)).getChildAt(0);
            float density = root.getResources().getDisplayMetrics().density;
            int h = Math.round(20 * density), v = Math.round(12 * density);
            // Three-button navigation, followed by gesture navigation after rotation.
            root.dispatchApplyWindowInsets(insets(0, 32, 0, 72, 0));
            assertEquals(h, root.getPaddingLeft()); assertEquals(v + 32, root.getPaddingTop());
            assertEquals(v + 72, root.getPaddingBottom());
            root.dispatchApplyWindowInsets(insets(44, 0, 24, 18, 0));
            root.dispatchApplyWindowInsets(insets(44, 0, 24, 18, 0));
            assertEquals(h + 44, root.getPaddingLeft()); assertEquals(h + 24, root.getPaddingRight());
            assertEquals(v, root.getPaddingTop()); assertEquals(v + 18, root.getPaddingBottom());
        }
    }
    @Test public void keyboardUsesMaximumInsetAndRestoresAfterDismissal() {
        View view = new View(RuntimeEnvironment.getApplication());
        MainActivity.applySafeInsets(view, insets(0, 30, 0, 48, 300), 20, 12);
        assertEquals(312, view.getPaddingBottom()); // Not 300 + 48 + 12.
        MainActivity.applySafeInsets(view, insets(0, 30, 0, 48, 0), 20, 12);
        assertEquals(60, view.getPaddingBottom()); assertEquals(42, view.getPaddingTop());
    }
}
