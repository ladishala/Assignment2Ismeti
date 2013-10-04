package assignment2.ismeti;

import assignment2.ismeti.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnLongClickListener;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class MainActivity extends Activity implements GPSCallBack,
		SensorEventListener {
	public static double SPEED = 0.0;
	private GPSManager gpsManager = null;
	private double speed = 0.0;
	private SensorManager mSensorManager;
	private Sensor mOrientation;
	private Sensor mAccelerometer;
	private Sensor mCompass;
	private Sensor lightSensor;
	private ImageView img_side;
	private ImageView img_front;
	int calPitch = 0; // Current value of calibrated pitch and roll angle.
	int calRoll = 0;
	int k = 0; // Key which is used to save the current state of the light sensor.
	private SeekBar seekbar;
	Camera cam;

	// Declaring the values taken from the sensors.
	private float[] accelValues = new float[3];
	private float[] compassValues = new float[3];
	private float[] inR = new float[9];
	private float[] inclineMatrix = new float[9];
	private float[] orientationValues = new float[3];
	private float[] prefValues = new float[3];
	public static int mRotation = 0;
	private boolean ready = false;

	// Object of the Drawing class.
	Drawing d;

	// Used to play the honk sound.
	MediaPlayer myMediaPlayer;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Keeps the screen always on.
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		// Initializes GPSManager.
		gpsManager = new GPSManager();
		gpsManager.startListening(getApplicationContext());
		gpsManager.setGPSCallback(this);

		// Initializes objects for the bike ImageViews from UI and sets their longClickListener.
		img_side = (ImageView) findViewById(R.id.imgBikeSide);
		img_side.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				calibrate();
				return true;
			}
		});

		img_front = (ImageView) findViewById(R.id.imgBikeFront);
		img_front.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				calibrate();
				return true;
			}
		});

		// Saves the orientation of the bicycles.
		mRotation = this.getWindowManager().getDefaultDisplay().getRotation();

		//Initialises sensor manager.
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		
		// There were problems on portrait-view with accelerometer sensor for
		// orientation, so we used one which was deprecated and worked properly.
		mOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);

		// Sets the sensor objects.
		mAccelerometer = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mCompass = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		lightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

		// Initialises objects declared above
		myMediaPlayer = MediaPlayer.create(MainActivity.this, R.raw.honkhonk);
		d = (Drawing) findViewById(R.id.view);

		// Initialises SeekBar object and sets it's onchangelistener, if device
		// has no lightsensor or flash it disables the seekbar on UI.
		seekbar = (SeekBar) findViewById(R.id.seekBar);
		if (lightSensor == null
				|| !getPackageManager().hasSystemFeature(
						PackageManager.FEATURE_CAMERA_FLASH)) {
			seekbar.setEnabled(false);
		} else {
			seekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

				@Override
				public void onProgressChanged(SeekBar seekBar, int progress,
						boolean fromUser) {
					if (seekbar.getProgress() == 0) {
						lightlistener(0);
						try {
							ledoff();
						} catch (Exception ex) {

						}
						k = 0;
					} else if (seekbar.getProgress() == 1) {
						k = 1;
						lightlistener(1);

					} else {
						k = 2;
						lightlistener(0);
						try {
							ledon();
						} catch (Exception ex) {

						}
					}

				}

				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
					// TODO Auto-generated method stub

				}

				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
					// TODO Auto-generated method stub

				}

			});
		}

	}

	private void getpreferences() {
		// Read preferences from shared preferences.
		k = getPreferences(MODE_PRIVATE).getInt("KEY", 0);
		calPitch = getPreferences(MODE_PRIVATE).getInt("calpitch", 0);
		calRoll = getPreferences(MODE_PRIVATE).getInt("calroll", 0);
		
		// If device orientation differs from actual saved in shared preferences
		// the calPitch and calRoll switch places.
		if (getPreferences(MODE_PRIVATE).getInt("Orientation", 0) != mRotation) {
			int a = calPitch;
			calPitch = calRoll;
			calRoll = a;
		}

	}

	private void savepreferences() {
		// Save values of k, calpitch, calrol and current orientation on shared preferences.
		getPreferences(MODE_PRIVATE).edit().putInt("KEY", k).commit();
		getPreferences(MODE_PRIVATE).edit().putInt("calpitch", calPitch)
				.commit();
		getPreferences(MODE_PRIVATE).edit().putInt("calroll", calRoll).commit();
		getPreferences(MODE_PRIVATE).edit().putInt("Orientation", mRotation)
				.commit();
	}

	@Override
	public void onGPSUpdate(Location location) {
		// Change the speed value when new location read from GPS, also
		// invalidate the drawing object.
		location.getLatitude();
		location.getLongitude();
		speed = location.getSpeed();
		SPEED = speed;
		d.invalidate();

	}

	@Override
	protected void onResume() {

		super.onResume();
		// Read preferences and registers sensors.
		getpreferences();
		mSensorManager.registerListener(this, mOrientation,
				SensorManager.SENSOR_DELAY_UI);
		mSensorManager.registerListener(this, mAccelerometer,
				SensorManager.SENSOR_DELAY_UI);
		mSensorManager.registerListener(this, mCompass,
				SensorManager.SENSOR_DELAY_UI);

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// Stops gpsmanager listening and saves values on sharedpreferences
		gpsManager.stopListening();
		gpsManager.setGPSCallback(null);
		gpsManager = null;
		savepreferences();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean result = true;
		return result;
	}

	@Override
	protected void onPause() {

		super.onPause();
		// Unregisters all listeners and saves values to shared preferences
		mSensorManager.unregisterListener(this);
		savepreferences();

	}

	@Override
	public void onSensorChanged(SensorEvent event) {

		/* This method is called when new data are read from any sensor this
		   switch orientates the actions to be done
		   based on which sensor is giving data*/
		
		switch (event.sensor.getType()) {
		
		/* If sensor type is acceleremoter or orientation it saves the data of
		 pitch roll and azimuth on responding lists and if both of these
		 sensors are triggered at least one in current session the boolean value ready becomes true
		 and now we can have a rotation matrix from which we manipulate the
		 imageviews for inclinometer.*/
		
		case Sensor.TYPE_ACCELEROMETER:
			for (int i = 0; i < 3; i++) {
				accelValues[i] = event.values[i];
			}
			if (compassValues[0] != 0)
				ready = true;
			break;
		case Sensor.TYPE_MAGNETIC_FIELD:
			for (int i = 0; i < 3; i++) {
				compassValues[i] = event.values[i];
			}
			if (accelValues[2] != 0)
				ready = true;
			break;
		case Sensor.TYPE_ORIENTATION:
			for (int i = 0; i < 3; i++) {
				orientationValues[i] = event.values[i];
			}
			break;

		case Sensor.TYPE_LIGHT:
			
			/* If sensor type is light and if auto mode is activated(k=1) it checks if current light is less than 30 lux
			 and more than 1 lux(this was added to avoid led on when user puts his phone into 
			 pocket in a hurry) if so it turns on the flashlight else it turns it off.*/
			
			if (k == 1) {
				float cl = event.values[0];

				if (cl <= (float) 30 && cl > 1) {
					try {
						ledon();
					} catch (Exception ex) {

					}
				} else {
					try {
						ledoff();
					} catch (Exception ex) {

					}

				}
			}
			break;
		}

		if (!ready)
			return;

		if (SensorManager.getRotationMatrix(inR, inclineMatrix, accelValues,
				compassValues)) {
			
			SensorManager.getOrientation(inR, prefValues);
			
			/* When we have the Rotation matrix we manipulate with imageviews of
			 bikes based on screen orientation if screen orientation is portrait 
			 roll is used for bike side and pitch for bike front, otherwise roll 
			 and pitch change sides.*/
			
			if (mRotation == Surface.ROTATION_90) {
				img_front.setRotation((float) (360 - Math
						.toDegrees(prefValues[1]) - calPitch));
				img_side.setRotation((float) (Math.toDegrees(prefValues[2]) - calRoll));
			} else if (mRotation == Surface.ROTATION_0) {
				img_side.setRotation((float) (orientationValues[1] - calPitch));
				img_front
						.setRotation((float) (360 - orientationValues[2] + calRoll));
			} else if (mRotation == Surface.ROTATION_270) {
				img_front
						.setRotation((float) (Math.toDegrees(prefValues[1]) - calPitch));
				img_side.setRotation((float) (0 - Math.toDegrees(prefValues[2]) + calRoll));
			}
		}
	}

	void ledon() {
		// Turn on flashlight.
		cam = Camera.open();
		Parameters params = cam.getParameters();
		params.setFlashMode(Parameters.FLASH_MODE_TORCH);
		cam.setParameters(params);
		cam.startPreview();
		cam.autoFocus(new AutoFocusCallback() {
			public void onAutoFocus(boolean success, Camera camera) {
			}
		});
	}

	void ledoff() {
		// Turn off flashlight
		cam.stopPreview();
		cam.release();
	}

	public void calibrate() {

		// Calibrate inclinometer set calroll and calpitch based on screen
		// orientation similar to imageview manipulation described above.
		if (mRotation == Surface.ROTATION_90) {
			calPitch = (int) Math.toDegrees(prefValues[1]);
			calRoll = (int) Math.toDegrees(prefValues[2]);
		} else if (mRotation == Surface.ROTATION_0) {
			calPitch = (int) orientationValues[1];
			calRoll = (int) orientationValues[2];
		} else if (mRotation == Surface.ROTATION_270) {
			calPitch = (int) Math.toDegrees(prefValues[1]);
			calRoll = (int) Math.toDegrees(prefValues[2]);
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	// Sets the listener for the DoubleTapDetector class.
	DoubleTapDetector doubleTapListener = new DoubleTapDetector() {
		@Override
		//Plays the sound when the DoubleTap gesture is detected.
		public void onDoubleTap() {
			myMediaPlayer.start();
		}
	};

	@Override
	//Listens for the double tap event and returns true when fired.
	public boolean onTouchEvent(MotionEvent event) {
		if (doubleTapListener.onDoubleTapEvent(event))
			return true;
		return super.onTouchEvent(event);
	}

	//Opens the next activity (Map Activity)
	public void btnRecTrackonClick(View v) {
		Intent i = new Intent(MainActivity.this, MapActivity.class);
		startActivity(i);
	}

	//Registers and unregisters the light listener based on parameter a
	private void lightlistener(int a) {
		if (a == 1) {
			mSensorManager.registerListener(this, lightSensor,
					SensorManager.SENSOR_DELAY_UI);
		} else {
			mSensorManager.unregisterListener(this, lightSensor);
		}

	}

}