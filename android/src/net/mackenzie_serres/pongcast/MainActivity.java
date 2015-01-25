package net.mackenzie_serres.pongcast;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouteSelector;
import android.view.Menu;
import android.view.MenuItem;
import com.google.android.gms.cast.CastMediaControlIntent;
import net.mackenzie_serres.chromecast.ChromecastInteractor;

/**
 * Main activity of the application
 */
public class MainActivity extends ActionBarActivity {
    private MediaRouteSelector mMediaRouteSelector;
    private PongController pongController;
    private ChromecastInteractor chromecast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pongController = new PongController();
        mMediaRouteSelector = new MediaRouteSelector.Builder().addControlCategory(
                CastMediaControlIntent.categoryForCast(getString(R.string.app_id))).build();
        chromecast = new ChromecastInteractor(this, getString(R.string.app_id), getString(R.string.namespace),
                mMediaRouteSelector, pongController);
        new GameControllerView(this, pongController);

    }

    @Override
    protected void onPause() {
        pongController.pause();

        // TODO try doing this always to make synetrical with onResume()
        if (isFinishing()) {
            chromecast.pause();
        }
        super.onPause();
    }

    // TODO Avoid Pause/Resume on first rotation
    @Override
    protected void onResume() {
        super.onResume();
        chromecast.resume();
    }

    @Override
    public void onDestroy() {
        chromecast.disconnect();
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    }

    /**
     * Add the MediaRoute ("Chromecast") button to the ActionBar at the top of the app
     *
     * @param menu - menu to add the menu items to
     * @return true if added an item
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);
        setMediaRouteSelector(menu);
        return true;
    }

    /**
     * Sets the selector for the chromecast device into an action in a Menu
     *
     * @param menu to add the action to
     */
    private void setMediaRouteSelector(Menu menu) {
        MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
        MediaRouteActionProvider mediaRouteActionProvider =
                (MediaRouteActionProvider) MenuItemCompat.getActionProvider(mediaRouteMenuItem);
        // Set the MediaRouteActionProvider selector for device discovery.
        mediaRouteActionProvider.setRouteSelector(mMediaRouteSelector);
    }

}
