
 /* Written by:
 * Gentiana Desipojci
 * Lavderim Shala
 * Isuf Deliu
 * Agnesa Belegu
 */

package assignment2.ismeti;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import assignment2.ismeti.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

public class MapActivity extends FragmentActivity implements
		OnMyLocationButtonClickListener, ConnectionCallbacks,
		OnConnectionFailedListener,
		com.google.android.gms.location.LocationListener {

	//Declaring variables and objects needed for acitivity.
	private GoogleMap mMap;
	private LocationClient mLocationClient;
	private ArrayList<LatLng> latLngList = new ArrayList<LatLng>();
	private float distance;
	private Timer timer;
	private int time;
	private int key; // Key for switching button text.
	private TextView txtDistance;
	private TextView txtTime;
	private TextView txtSpeed;
	private Button btnStart;

	//Declares and implements LocationRequest.
	private static final LocationRequest REQUEST = LocationRequest.create()
			.setInterval(5000).setFastestInterval(16)
			.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_aktivitetikryesore);
		
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setUpMapIfNeeded();
		setUpLocationClientIfNeeded();
		
		txtTime = (TextView) findViewById(R.id.txtTimeRes);
		txtSpeed = (TextView) findViewById(R.id.txtSpeedRes);
		txtDistance = (TextView) findViewById(R.id.txtDistanceRes);
		btnStart = (Button) findViewById(R.id.btnStartStop);
		distance = 0;
		time = 0;
	}

	@SuppressLint("NewApi")
	@Override
	protected void onResume() {
		super.onResume();
		
		setUpMapIfNeeded();
		setUpLocationClientIfNeeded();
		//Loading data and recovering state from shared preferences.
		mLocationClient.connect();
		key = getPreferences(MODE_PRIVATE).getInt("K", 0);
		
		if (key == 1) {
			startTimer();
			btnStart.setText("Stop");
			btnStart.setBackground(getResources().getDrawable(R.drawable.btnstop));
			btnStart.invalidate();
		}
		
		loadArray();
		
		mMap.addPolyline(new PolylineOptions().addAll(latLngList).width(5)
				.color(-16776961));
		
		doTime();
		showSpeedDistance();
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mLocationClient != null) {
			mLocationClient.disconnect();
		}
		//Cancel timer if it is activated and save state on shared preferences.
		if (key == 1) {
			timer.cancel();
		}
		saveArray();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		//Cancel timer if it is activated and save state on shared preferences.
		if (key == 1) {
			timer.cancel();
		}
		saveArray();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private void setUpMapIfNeeded() {
		//Taken from google mas examples at google developer site.
		// Do a null check to confirm that we have not already instantiated the
		// map.
		if (mMap == null) {
			// Try to obtain the map from the SupportMapFragment.
			mMap = ((SupportMapFragment) getSupportFragmentManager()
					.findFragmentById(R.id.map)).getMap();
			// Check if we were successful in obtaining the map.
			if (mMap != null) {
				mMap.setMyLocationEnabled(true);
				mMap.setOnMyLocationButtonClickListener(this);
			}
		}
	}

	public boolean onMyLocationButtonClick() {
		return false;
		//My location button clicker to be implemented later, returned false to use default implementation.
	}

	@Override
	public void onLocationChanged(Location loc) {
		//Getting current location from LocationManager, animate camera to current location with zoom level 15
		LatLng latLng = new LatLng(loc.getLatitude(), loc.getLongitude());
		CameraUpdate camUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15);
		mMap.animateCamera(camUpdate);
		
		//If recording has started(k=1) add current location to latlnglist which is the list of all positions recorded for current sesion.
		try {
			if (key == 1) {
				latLngList.add(latLng);
				calculatedistance();
				showSpeedDistance();

				if (!latLngList.isEmpty()) {
					//Writing poyline with latlng registered so far.
					mMap.addPolyline(new PolylineOptions().addAll(latLngList)
							.width(5).color(-16776961));
				}

			}
		} catch (Exception ex) {

		}
	}

	@Override
	public void onConnectionFailed(ConnectionResult result) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onDisconnected() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onConnected(Bundle connectionHint) {
		mLocationClient.requestLocationUpdates(REQUEST, this);
	}

	private void setUpLocationClientIfNeeded() {
		//Taken from google maps exaple at google android developer site.
		if (mLocationClient == null) {
			mLocationClient = new LocationClient(getApplicationContext(), this, // ConnectionCallbacks
					this); // OnConnectionFailedListener
		}
	}

	@SuppressLint("NewApi")
	// min API req. 16
	public void buttonClick(View v) {
		//Start/Stop recording track and other values based on key value if k=1 stop else start
		if (key == 1) {
			key = 0;
			timer.cancel();
			btnStart.setText("Start");
			btnStart.setBackground(getResources().getDrawable(R.drawable.btnstart));
			btnStart.invalidate();

			if (Locale.getDefault().getLanguage().equals("de")) {
				showAlertMessage("Verfolgen abgeschlossen!\n\nDistanz: "
						+ txtDistance.getText() + "\n\nZeit: "
						+ txtTime.getText() + "\n\nTempo: " + txtSpeed.getText());
			} else {
				showAlertMessage("Track Completed!\n\nDistance: "
						+ txtDistance.getText() + "\n\nTime: "
						+ txtTime.getText() + "\n\nSpeed: " + txtSpeed.getText());
			}

		} else {
			startTimer();
			latLngList.clear();
			time = 0;
			distance = 0;
			key = 1;
			latLngList.clear();
			mMap.clear();
			btnStart.setText("Stop");
			btnStart.setBackground(getResources().getDrawable(R.drawable.btnstop));
			btnStart.invalidate();
		}

	}

	private void showSpeedDistance() {

		//Show current distance and speed in a proper format to related textviews on UI.
		String strDistance = new DecimalFormat("#.##").format(
				(double) (distance)).toString();
		strDistance += " m";
		String strSpeed = "0 Km/h";
		if (time != 0) {
			strSpeed = new DecimalFormat("#.##").format(
					(double) (0.27 * distance / time)).toString();
			strSpeed += " Km/h";
		}
		txtDistance.setText(strDistance);
		txtSpeed.setText(strSpeed);
	}

	private void doTime() {
		//This method is called from timer when activated and it deals with timing, increases the value and shows on UI a proper format of time.
		if (key == 1) {
			time += 1;
		}
		String result = "";
		if (Locale.getDefault().getLanguage().equals("de")) {
			result = Integer.toString(time / 3600) + " Stunde(n) "
					+ Integer.toString(time / 60) + " Minute(n) "
					+ Integer.toString(time % 60) + " Sekunde(n)";
		} else {
			result = Integer.toString(time / 3600) + " Hour(s) "
					+ Integer.toString(time / 60) + " Minutes(s) "
					+ Integer.toString(time % 60) + " Second(s)";
		}

		if (time > 3600 * 24) {
			time = 0;
		}
		txtTime.setText(result);

	}

	private void startTimer() {
		//This method starts timer when in record mode this timer calls method dotime every second.
		timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {

			public void run() {
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						doTime();
					}

				});
			}

		}, 0, 1000);
	}

	private void saveArray() {
		
		//Save latlnglist, key, time and distance to sharedpreferences
		getPreferences(MODE_PRIVATE).edit().putInt("Size", latLngList.size())
				.commit();
		getPreferences(MODE_PRIVATE).edit().putFloat("Distance", distance)
				.commit();
		getPreferences(MODE_PRIVATE).edit().putInt("K", key).commit();
		getPreferences(MODE_PRIVATE).edit().putInt("Time", time).commit();
		for (int i = 0; i < latLngList.size(); i++) {
			getPreferences(MODE_PRIVATE).edit().remove("Cord_Lat_" + i)
					.commit();
			getPreferences(MODE_PRIVATE).edit().remove("Cord_Long_" + i)
					.commit();
			getPreferences(MODE_PRIVATE)
					.edit()
					.putFloat("Cord_Lat_" + i,
							(float) latLngList.get(i).latitude).commit();
			getPreferences(MODE_PRIVATE)
					.edit()
					.putFloat("Cord_Long_" + i,
							(float) latLngList.get(i).longitude).commit();
		}

	}

	private void loadArray() {

		//Get latlnglist, key, time and distance to sharedpreferences
		latLngList.clear();
		int size = getPreferences(MODE_PRIVATE).getInt("Size", 0);
		distance = getPreferences(MODE_PRIVATE).getFloat("Distance", 0);
		key = getPreferences(MODE_PRIVATE).getInt("K", 0);
		time = getPreferences(MODE_PRIVATE).getInt("Time", 0);

		for (int i = 0; i < size; i++) {
			double lat = (double) getPreferences(MODE_PRIVATE).getFloat(
					"Cord_Lat_" + i, (float) 5.0);
			double lng = (double) getPreferences(MODE_PRIVATE).getFloat(
					"Cord_Long_" + i, (float) 5.0);
			latLngList.add(new LatLng(lat, lng));
		}
	}

	private void calculatedistance() {
		//Calculates distance between last and second-last member of latlnglist if this list has more than 2 members.
		// and saves this value to variable distance which reprsents the total distance for the sesion.
		int size = latLngList.size();
		if (size > 1) {
			distance += Distance(latLngList.get(size - 1).latitude,
					latLngList.get(size - 1).longitude,
					latLngList.get(size - 2).latitude,
					latLngList.get(size - 2).longitude);
		}
	}

	private float Distance(double nLat1, double nLon1, double nLat2,
			double nLon2) {
		/*
		 * Taken From Jaimerios.com it uses haversine formula to calculate
		 * distance.
		 */

		double nRadius = 6371; // Earth's radius in Kilometers
		/*
		 * Get the difference between our two points then convert the difference
		 * into radians
		 */

		double nDLat = Math.toRadians(nLat2 - nLat1);
		double nDLon = Math.toRadians(nLon2 - nLon1);

		// Here is the new line
		nLat1 = Math.toRadians(nLat1);
		nLat2 = Math.toRadians(nLat2);

		double nA = Math.pow(Math.sin(nDLat / 2), 2) + Math.cos(nLat1)
				* Math.cos(nLat2) * Math.pow(Math.sin(nDLon / 2), 2);
		double nC = 2 * Math.atan2(Math.sqrt(nA), Math.sqrt(1 - nA));
		double nD = nRadius * nC;

		return (float) nD; // Return our calculated distance
	}

	// Shows a generic MessageBox.
	public void showAlertMessage(String msg) {
		AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
		dlgAlert.setMessage(msg);
		dlgAlert.setPositiveButton("OK", null);
		dlgAlert.setCancelable(true);
		dlgAlert.create().show();
	}
}
