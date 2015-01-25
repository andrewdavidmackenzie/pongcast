package net.mackenzie_serres.pongcast;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import net.mackenzie_serres.chromecast.ChromecastInteractor;

/**
 * Main activity of the application
 */
public class MainActivity extends ActionBarActivity {
    private PongController pongController;
    private ChromecastInteractor chromecast;
    private PongControllerView pongControllerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pongController = new PongController();
        pongControllerView = new PongControllerView(this, getString(R.string.app_id), pongController);
        chromecast = new ChromecastInteractor(this, getString(R.string.app_id), getString(R.string.namespace),
                pongControllerView.getMediaSelector(), pongController);
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
        pongControllerView.setMediaRouteSelector(menu);
        return true;
    }
}
