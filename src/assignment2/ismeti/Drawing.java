package assignment2.ismeti;

import assignment2.ismeti.R;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.View;

public class Drawing extends View {

	Paint paint = new Paint();
	Bitmap speedometer;

	public Drawing(Context context) {
		super(context);
		// TODO Auto-generated constructor stub

	}

	public Drawing(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	/*
	 * X and Y coordinates used when drawing into the canvas
	 */
	float startX;
	float startY;
	float stopX;
	float stopY;

	@SuppressLint("DrawAllocation")
	@Override
	protected void onDraw(Canvas canvas) {
		// TODO Auto-generated method stub
		super.onDraw(canvas);
		speedometer = BitmapFactory.decodeResource(getResources(),
				R.drawable.speedo);
		// Defining the start of X and Y coordinates
		startX = getWidth() / 2;
		startY = getHeight() - 4;

		// Defining the radius which is dependant on the starting X coordinate
		double R = 0;
		if (MainActivity.mRotation == Surface.ROTATION_0) {
			R = startX * 0.85;
		} else {
			R = startX * 0.7;
		}
		int canvasWidth = canvas.getWidth();
		int canvasHeight = canvas.getHeight();
		// The speed is taken from the static variable SPEED which is calculated
		// in the MainActivity class
		double Speed = MainActivity.SPEED;
		// Calculating the angle based on current speed
		double S = Speed / 60 * 180 + 180;
		// The max speed shown is 60km/h even if the actual speed is higher
		if (S > 360) {
			S = 360;
		}
		// Calculating the X and Y coordinates for the line's end point
		double X = startX + R * Math.cos(Math.toRadians(S));
		double Y = startY + R * Math.sin(Math.toRadians(S));

		// Adjusting the size of the rectangle where the speedometer will be
		// placed
		int squareWidth = canvasWidth - 1;
		int squareHeight = canvasHeight - 1;
		Rect destinationRect = new Rect();
		destinationRect.set(0, 0, squareWidth, squareHeight);

		// Defining paint attributes
		paint.setAntiAlias(true);
		paint.setStrokeWidth(6f);
		paint.setColor(Color.RED);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeJoin(Paint.Join.ROUND);

		try {
			canvas.drawBitmap(speedometer, null, destinationRect, null);
		} catch (Exception ex) {

		}
		canvas.drawLine(startX, startY, (float) X, (float) Y, paint);

	}
}
