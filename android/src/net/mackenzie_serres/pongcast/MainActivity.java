/*
 * Copyright (C) 2014 Google Inc. All Rights Reserved. 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

package net.mackenzie_serres.pongcast;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.Cast.ApplicationConnectionResult;
import com.google.android.gms.cast.Cast.MessageReceivedCallback;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Main activity to send messages to the receiver.
 */
public class MainActivity extends ActionBarActivity {
	private static final String TAG = MainActivity.class.getSimpleName();
	private static final int REQUEST_CODE = 1;
	private MediaRouter mMediaRouter;
	private MediaRouteSelector mMediaRouteSelector;
	private MediaRouter.Callback mMediaRouterCallback;
	private CastDevice mSelectedDevice;
	private GoogleApiClient mApiClient;
	private HelloWorldChannelCallbacks mHelloWorldChannelCallbacks;
	private boolean mWaitingForReconnect;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		ActionBar actionBar = getSupportActionBar();
		actionBar.setBackgroundDrawable(new ColorDrawable(
				android.R.color.transparent));

		// When the user clicks on the button, use Android voice recognition to
		// get text
		Button voiceButton = (Button) findViewById(R.id.voiceButton);
		voiceButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startVoiceRecognitionActivity();
			}
		});

		// Configure Cast device discovery
		mMediaRouter = MediaRouter.getInstance(getApplicationContext());
		mMediaRouteSelector = new MediaRouteSelector.Builder()
				.addControlCategory(
						CastMediaControlIntent.categoryForCast(getResources()
								.getString(R.string.app_id))).build();

		// Only create the callback once, on creation
		mMediaRouterCallback = new MyMediaRouterCallback();
	}

	/**
	 * Android voice recognition
	 */
	private void startVoiceRecognitionActivity() {
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
				RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
				getString(R.string.message_to_cast));
		startActivityForResult(intent, REQUEST_CODE);
	}

	/*
	 * Handle the voice recognition response
	 * @see android.support.v4.app.FragmentActivity#onActivityResult(int, int,
	 * android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
			ArrayList<String> matches = data
					.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
			if (matches.size() > 0) {
				Log.d(TAG, matches.get(0));
				sendMessage(matches.get(0));
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onDestroy() {
		teardown();
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		if (isFinishing()) {
			// End media router discovery
			mMediaRouter.removeCallback(mMediaRouterCallback);
		}
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		// Start media router discovery
		mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback,
				MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.main, menu);
		MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
		MediaRouteActionProvider mediaRouteActionProvider =
				(MediaRouteActionProvider) MenuItemCompat.getActionProvider(mediaRouteMenuItem);
		// Set the MediaRouteActionProvider selector for device discovery.
		mediaRouteActionProvider.setRouteSelector(mMediaRouteSelector);
		return true;
	}

	/**
	 * Start the receiver app
	 */
	private void launchReceiver() {
		try {
			Cast.Listener mCastListener = new Cast.Listener() {
				@Override
				public void onApplicationStatusChanged() {
					Log.d(TAG, "application status has changed");
					// TODO need to request the status? Not passed in here :-(
				}

				@Override
				public void onApplicationDisconnected(int errorCode) {
					Log.d(TAG, "application has stopped");
					teardown();
				}
			};

			// Connect to Google Play services
			ConnectionCallbacks mConnectionCallbacks = new ConnectionCallbacks();
			ConnectionFailedListener mConnectionFailedListener = new ConnectionFailedListener();
			Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions
					.builder(mSelectedDevice, mCastListener);

			mApiClient = new GoogleApiClient.Builder(this)
					.addApi(Cast.API, apiOptionsBuilder.build())
					.addConnectionCallbacks(mConnectionCallbacks)
					.addOnConnectionFailedListener(mConnectionFailedListener)
					.build();

			mApiClient.connect();
		} catch (Exception e) {
			Log.e(TAG, "Failed launchReceiver", e);
		}
	}

	/**
	 * Tear down the connection to the receiver
	 */
	private void teardown() {
		if (mApiClient != null) {
			try {
					/*
					Cast.CastApi.stopApplication(mApiClient);
					*/
				if (mHelloWorldChannelCallbacks != null) {
					Cast.CastApi.removeMessageReceivedCallbacks(mApiClient,
							mHelloWorldChannelCallbacks.getNamespace());
					mHelloWorldChannelCallbacks = null;
				}
			} catch (IOException e) {
				Log.e(TAG, "Exception while removing channel", e);
			}

			if (mApiClient.isConnected()) {
				mApiClient.disconnect();
			}
			mApiClient = null;
		}

		mSelectedDevice = null;
		mWaitingForReconnect = false;
	}

	/**
	 * Send a text message to the receiver
	 *
	 * @param message String to send to the cast device
	 */
	private void sendMessage(String message) {
		if (mApiClient != null) {
			if (mHelloWorldChannelCallbacks != null) {
				try {
					Cast.CastApi.sendMessage(mApiClient,
							mHelloWorldChannelCallbacks.getNamespace(), message)
							.setResultCallback(new ResultCallback<Status>() {
								@Override
								public void onResult(Status result) {
									if (!result.isSuccess()) {
										Log.e(TAG, "Sending message failed");
									}
								}
							});
				} catch (Exception e) {
					Log.e(TAG, "Exception while sending message", e);
					Toast.makeText(MainActivity.this, "Exception while sending message", Toast.LENGTH_SHORT).show();
				}
			} else {
				Toast.makeText(MainActivity.this, "No Cast HelloWorld Channel", Toast.LENGTH_SHORT).show();
			}
		} else {
			Toast.makeText(MainActivity.this, "No Cast Client API", Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * Callback for MediaRouter events
	 */
	private class MyMediaRouterCallback extends MediaRouter.Callback {
		@Override
		public void onRouteSelected(MediaRouter router, RouteInfo info) {
			Log.d(TAG, "onRouteSelected");
			// Handle the user route selection.
			mSelectedDevice = CastDevice.getFromBundle(info.getExtras());

			launchReceiver();
		}

		@Override
		public void onRouteUnselected(MediaRouter router, RouteInfo info) {
			Log.d(TAG, "onRouteUnselected: info=" + info);
			teardown();
			mSelectedDevice = null;
		}
	}

	/**
	 * Google Play services callbacks
	 */
	private class ConnectionCallbacks implements GoogleApiClient.ConnectionCallbacks {
		@Override
		public void onConnected(Bundle connectionHint) {
			Log.d(TAG, "onConnected");

			if (mApiClient == null) {
				// We got disconnected while this runnable was pending
				// execution.
				return;
			}

			try {
				if (mWaitingForReconnect) {
					mWaitingForReconnect = false;

					// Check if the receiver app is still running
					if ((connectionHint != null)
							&& connectionHint.getBoolean(Cast.EXTRA_APP_NO_LONGER_RUNNING)) {
						Log.d(TAG, "App  is no longer running");
						teardown();
					} else {
						// Re-create the custom message channel
						try {
							Cast.CastApi.setMessageReceivedCallbacks(mApiClient,
									mHelloWorldChannelCallbacks.getNamespace(),
									mHelloWorldChannelCallbacks);
						} catch (IOException e) {
							Log.e(TAG, "Exception while setting message received callbacks", e);
						}
					}
				} else {
					// Launch the receiver app
					Cast.CastApi
							.launchApplication(mApiClient,
									getString(R.string.app_id), false)
							.setResultCallback(
									new ResultCallback<Cast.ApplicationConnectionResult>() {
										@Override
										public void onResult(
												ApplicationConnectionResult result) {
											Status status = result.getStatus();
											Log.d(TAG,
													"ApplicationConnectionResultCallback.onResult: statusCode"
															+ status.getStatusCode());
											if (status.isSuccess()) {
												ApplicationMetadata applicationMetadata = result
														.getApplicationMetadata();
												String sessionId = result
														.getSessionId();
												String applicationStatus = result
														.getApplicationStatus();
												boolean wasLaunched = result
														.getWasLaunched();
												Log.d(TAG,
														"application name: "
																+ applicationMetadata
																.getName()
																+ ", status: "
																+ applicationStatus
																+ ", sessionId: "
																+ sessionId
																+ ", wasLaunched: "
																+ wasLaunched);

												// Create the custom message channel
												mHelloWorldChannelCallbacks = new HelloWorldChannelCallbacks();
												try {
													Cast.CastApi
															.setMessageReceivedCallbacks(
																	mApiClient,
																	mHelloWorldChannelCallbacks
																			.getNamespace(),
																	mHelloWorldChannelCallbacks);
												} catch (IOException e) {
													Log.e(TAG,
															"Exception while creating channel",
															e);
												}

												// set the initial instructions
												// on the receiver
												/*
												sendMessage(getString(R.string.instructions));
												*/
											} else {
												Log.e(TAG,
														"application could not launch");
												teardown();
											}
										}
									});
				}
			} catch (Exception e) {
				Log.e(TAG, "Failed to launch application", e);
			}
		}

		@Override
		public void onConnectionSuspended(int cause) {
			Log.d(TAG, "onConnectionSuspended");
			mWaitingForReconnect = true;
		}
	}

	/**
	 * Google Play services callbacks
	 */
	private class ConnectionFailedListener implements
			GoogleApiClient.OnConnectionFailedListener {
		@Override
		public void onConnectionFailed(ConnectionResult result) {
			Log.e(TAG, "onConnectionFailed ");

			teardown();
		}
	}

	/**
	 * Custom message channel
	 */
	class HelloWorldChannelCallbacks implements MessageReceivedCallback {
		/**
		 * @return custom namespace
		 */
		public String getNamespace() {
			return getString(R.string.namespace);
		}

		/**
		 * Receive message from the receiver app
		 */
		@Override
		public void onMessageReceived(CastDevice castDevice, String namespace, String message) {
			Log.d(TAG, "onMessageReceived: " + message);
			Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
		}

	}

}
