package com.shu.imagemaster;

import android.app.Activity;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Toast;
import com.shu.MView.MapView;

public class MainActivity extends Activity {
	private float originX;
	private float originY;
	private float currentX;
	private float currentY;
	private float screenX;
	private float screenY;
	
	private Matrix matrix;
	private float[] values;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		final MapView mapView = (MapView) findViewById(R.id.mapView);
		matrix=new Matrix();
		values=new float[9];
		DisplayMetrics dm = getResources().getDisplayMetrics();
		screenX = dm.widthPixels;
		screenY = dm.heightPixels;
		mapView.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View arg0, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					originX = event.getX();
					originY = event.getY();

					break;
				case MotionEvent.ACTION_MOVE:

					break;
				case MotionEvent.ACTION_UP:
					currentX=event.getX();
					currentY=event.getY();
					if (Math.abs((currentX - originX)) < 5
							&& Math.abs((currentY - originY)) < 5){
						
						matrix=mapView.getPreMatrix();
						matrix.getValues(values);
						Toast.makeText(MainActivity.this, (values[2]-currentX)/(values[0]/2)+" "+(values[5]-currentY)/(values[0]/2), Toast.LENGTH_SHORT).show();
					}
					break;

				}
				return false;
			}
		});
	}

}
