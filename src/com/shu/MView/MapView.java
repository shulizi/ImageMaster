package com.shu.MView;

import java.io.IOException;
import java.io.InputStream;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;

public class MapView extends View {
	private GestureDetector gestureDetector;
	private PointF fingerOriginP;
	private PointF fingerDefaultP;
	private PointF fingerTranslateP;
	private PointF fingerScaleP;
	private PointF screenP;
	private float fingerOriginDistanceF = -1;
	private Matrix matrix;
	private Matrix preM;
	private Matrix scaleM;
	private Matrix globalMatrix;
	private Bitmap bitmap;
	private Bitmap bitmap_pre;
	private boolean isDrawPre;
	private int drawingIndex;
	private float scaleRate = 1;
	private float maxScale;
	private float rightBorder;
	private float downBorder;
	private float[] globalValues;
	private boolean globalFlingXFlag;
	private boolean globalFlingYFlag;
	private Context context;
	private Handler handler = new Handler(new Handler.Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			bitmap = (Bitmap) msg.obj;
			int tempIndex = msg.arg1;
			matrix.getValues(globalValues);
			globalValues[2] = 0;
			globalValues[5] = 0;
			matrix.setValues(globalValues);
			if (tempIndex == drawingIndex && scaleRate >= 1) {
				isDrawPre = false;
				invalidate();
			}
			return false;
		}
	});
	private Handler preHandler = new Handler(new Handler.Callback() {

		@Override
		public boolean handleMessage(Message msg) {
			bitmap_pre = (Bitmap) msg.obj;
			preM.postScale(2, 2);
			invalidate();
			return false;
		}
	});

	public MapView(Context context) {
		super(context);
		this.context = context;
		init();
	}

	public MapView(Context context, AttributeSet a) {
		super(context, a);
		this.context = context;
		init();
	}

	private void init() {
		globalValues = new float[9];
		drawingIndex = 0;
		maxScale = 5;
		gestureDetector = new GestureDetector(context, new MGestureListener());
		fingerOriginP = new PointF();
		screenP = new PointF();
		fingerDefaultP = new PointF();
		fingerTranslateP = new PointF();
		fingerScaleP = new PointF();
		DisplayMetrics dm = context.getResources().getDisplayMetrics();
		screenP.x = dm.widthPixels;
		screenP.y = dm.heightPixels;
		matrix = new Matrix();
		scaleM = new Matrix();
		preM = new Matrix();
		globalMatrix = new Matrix();
		initPreBitmap();
		initBitmap();
	}

	private void initPreBitmap() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				Bitmap bitmap = null;

				try {
					BitmapFactory.Options options = new BitmapFactory.Options();
					options.inSampleSize = 2;
					InputStream is = context.getAssets().open("map.jpg");
					bitmap = BitmapFactory.decodeStream(is, null, options);
				} catch (IOException e) {
					e.printStackTrace();
				}

				rightBorder = bitmap.getWidth() * 2;
				downBorder = bitmap.getHeight() * 2;
				Message msg = new Message();
				msg.obj = bitmap;
				preHandler.sendMessage(msg);
			}
		}).start();
	}

	@SuppressLint("NewApi")
	private void initBitmap() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				int tempIndex = drawingIndex;
				Bitmap bitmap = null;
				try {
					InputStream inputStream = context.getAssets().open(
							"map.jpg");
					BitmapFactory.Options options = new BitmapFactory.Options();
					BitmapRegionDecoder bitmapRegionDecoder = BitmapRegionDecoder
							.newInstance(inputStream, false);
					options.inPreferredConfig = Bitmap.Config.RGB_565;
					bitmap = bitmapRegionDecoder.decodeRegion(new Rect(
							(int) (fingerDefaultP.x), (int) (fingerDefaultP.y),
							(int) (fingerDefaultP.x + screenP.x / scaleRate),
							(int) (fingerDefaultP.y + screenP.y / scaleRate)),
							options);
					inputStream.close();
				} catch (IOException e) {
					Log.d("test", "Error");
					e.printStackTrace();
				}
				Message msg = new Message();
				msg.arg1 = tempIndex;
				msg.obj = bitmap;
				handler.sendMessage(msg);
			}
		}).start();
	}

	public void showMatrix(Matrix matrix) {
		float[] values = new float[9];
		matrix.getValues(values);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				sb.append(values[i * 3 + j] + " ");
			}
			sb.append("-----" + i + "\n");
		}
		Log.d("test", sb.toString());
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		drawingIndex = (drawingIndex + 1) % 10000;
		gestureDetector.onTouchEvent(event);
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			fingerOriginP.x = event.getX();
			fingerOriginP.y = event.getY();
			break;
		case MotionEvent.ACTION_MOVE:
			globalFlingXFlag = false;
			globalFlingYFlag = false;
			isDrawPre = true;
			if (event.getPointerCount() >= 2) {
				if (fingerOriginDistanceF != -1) {
					float currentDistanceF = (float) Math.sqrt((Math.pow(
							event.getX(0) - event.getX(1), 2) + Math.pow(
							event.getY(0) - event.getY(1), 2)));
					globalMatrix.set(preM);
					globalMatrix.postScale(currentDistanceF
							/ fingerOriginDistanceF, currentDistanceF
							/ fingerOriginDistanceF, screenP.x / 2,
							screenP.y / 2);
					globalMatrix.getValues(globalValues);
					scaleRate *= currentDistanceF / fingerOriginDistanceF;
					if (scaleRate > maxScale
							|| globalValues[2] > 0
							|| globalValues[5] > 0
							|| globalValues[2] - screenP.x < -rightBorder
									* scaleRate
							|| globalValues[5] - screenP.y < -downBorder
									* scaleRate) {
						scaleRate /= currentDistanceF / fingerOriginDistanceF;
						break;
					}

					matrix.postScale(currentDistanceF / fingerOriginDistanceF,
							currentDistanceF / fingerOriginDistanceF,
							screenP.x / 2, screenP.y / 2);
					scaleM.postScale(currentDistanceF / fingerOriginDistanceF,
							currentDistanceF / fingerOriginDistanceF,
							screenP.x / 2, screenP.y / 2);
					preM.postScale(currentDistanceF / fingerOriginDistanceF,
							currentDistanceF / fingerOriginDistanceF,
							screenP.x / 2, screenP.y / 2);

					fingerOriginDistanceF = currentDistanceF;

				} else {
					fingerOriginDistanceF = (float) Math.sqrt((Math.pow(
							event.getX(0) - event.getX(1), 2) + Math.pow(
							event.getY(0) - event.getY(1), 2)));
				}
			} else if (fingerOriginDistanceF == -1) {

				float currentX = event.getX();
				float currentY = event.getY();
				preM.getValues(globalValues);
				if (globalValues[2] + (currentX - fingerOriginP.x) > 0
						|| globalValues[5] + (currentY - fingerOriginP.y) > 0
						|| globalValues[2] + (currentX - fingerOriginP.x)
								- screenP.x < -rightBorder * scaleRate
						|| globalValues[5] + (currentY - fingerOriginP.y)
								- screenP.y < -downBorder * scaleRate)
					break;
				fingerTranslateP.x -= (currentX - fingerOriginP.x) / scaleRate;
				fingerTranslateP.y -= (currentY - fingerOriginP.y) / scaleRate;
				matrix.postTranslate(currentX - fingerOriginP.x, currentY
						- fingerOriginP.y);
				preM.postTranslate(currentX - fingerOriginP.x, currentY
						- fingerOriginP.y);

				fingerOriginP.x = currentX;
				fingerOriginP.y = currentY;
			}
			invalidate();
			break;
		case MotionEvent.ACTION_UP:

			setMatrix();

		}
		return true;
	};

	public Matrix getPreMatrix() {
		return preM;
	}

	private void setMatrix() {
		if (fingerOriginDistanceF != -1) {
			fingerOriginDistanceF = -1;
			scaleM.getValues(globalValues);
			fingerScaleP.x = -globalValues[2] / scaleRate;
			fingerScaleP.y = -globalValues[5] / scaleRate;
		}
		fingerDefaultP.x = fingerScaleP.x + fingerTranslateP.x;
		fingerDefaultP.y = fingerScaleP.y + fingerTranslateP.y;
		initBitmap();

	}

	private void move(float x, float y) {
		globalMatrix.set(preM);
		globalMatrix.postTranslate(x, y);
		globalMatrix.getValues(globalValues);
		if (globalValues[2] > 0
				|| globalValues[2] - screenP.x < -rightBorder * scaleRate) {
			x = 0;
			globalFlingXFlag = false;
		}
		if (globalValues[5] > 0
				|| globalValues[5] - screenP.y < -downBorder * scaleRate) {
			y = 0;
			globalFlingYFlag = false;
		}
		preM.postTranslate(x, y);
		matrix.postTranslate(x, y);
		fingerTranslateP.x = fingerTranslateP.x - x / scaleRate;
		fingerTranslateP.y = fingerTranslateP.y - y / scaleRate;
		invalidate();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (bitmap != null) {

			if (isDrawPre)
				canvas.drawBitmap(bitmap_pre, preM, null);
			else
				canvas.drawBitmap(bitmap, matrix, null);
		}

	}

	private class MGestureListener extends SimpleOnGestureListener {

		private Handler flingH = new Handler(new Handler.Callback() {
			@Override
			public boolean handleMessage(Message msg) {
				float x = msg.arg1;
				float y = msg.arg2;
				move(x, y);
				return false;
			}
		});

		private void startAnimation(MotionEvent e1, MotionEvent e2,
				final float velocityX, final float velocityY) {
			// Log.d("test","move"+velocityX+" "+velocityY);
			globalFlingXFlag = true;
			globalFlingYFlag = true;
			new Thread(new Runnable() {
				float speedX = velocityX;
				float speedY = velocityY;

				@Override
				public void run() {
					while (Math.abs(speedX / scaleRate) > 50
							|| Math.abs(speedY / scaleRate) > 50) {
						if (!globalFlingXFlag)
							speedX = 0;
						if (!globalFlingYFlag)
							speedY = 0;

						drawingIndex = (drawingIndex + 1) % 10000;
						speedX = (float) (speedX / (1 + 0.1 / scaleRate));
						speedY = (float) (speedY / (1 + 0.1 / scaleRate));
						try {
							Thread.sleep(10);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						preM.getValues(globalValues);

						Message msg = new Message();
						msg.arg1 = (int) (speedX / 100);
						msg.arg2 = (int) (speedY / 100);
						flingH.sendMessage(msg);
					}

					setMatrix();

				}
			}).start();

		}

		@Override
		public boolean onDoubleTap(MotionEvent e) {
			return super.onDoubleTap(e);
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			// Log.i("test", "onFling:velocityX = " + velocityX + " velocityY"
			// + velocityY);
			startAnimation(e1, e2, velocityX, velocityY);
			return super.onFling(e1, e2, velocityX, velocityY);
		}
	}
};
