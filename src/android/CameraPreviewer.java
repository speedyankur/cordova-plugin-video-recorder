package com.homeinspectorpro.video.recorder;

import java.io.IOException;
import java.io.File;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Environment;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;


public class CameraPreviewer extends SurfaceView implements
		SurfaceHolder.Callback {
	private final String LCAT = "VideoTest";

	private SurfaceHolder mHolder;
	private Camera mCamera;

	private File videoFile;
	private MediaRecorder mMediaRecorder;
	public List<Camera.Size> mSupportedPreviewSizes;
	public int preferredWidth, preferredHeight; 

	private Camera getCameraInstance() {

		if (mCamera == null) {
			try {
				mCamera = Camera.open();
			} catch (Exception e) {
				Log.i("MYAPP", "Camera not available: " + e.getMessage());
				// Camera is not available (in use or does not exist)
			}

		}

		return mCamera; // returns null if camera is unavailable
	}

	public void startRecording() {
		// This method is an example of exposing a native method to JavaScript.
		// The method signature does not declare any parameters.

		Log.d(LCAT, "[METHODSDEMO] start recording");
		try {
			//this.setZOrderOnTop(false);
			try {
				initRecorder(mHolder.getSurface());
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			mMediaRecorder.start();
			Log.d(LCAT, "start recording");
		} catch (Exception ee) {
			Log.e(LCAT, "Caught io exception " + ee.getMessage());
			ee.printStackTrace();
		}
	}
	
	public File stopRecording() {
		try {
			mMediaRecorder.stop();
			mMediaRecorder.reset();
			mMediaRecorder.release();
			Log.d(LCAT, "stop recording");

		} catch (Exception ee) {
			Log.e(LCAT, "Caught io exception " + ee.toString());
		} finally{
			mCamera.release();
		}
		// once the objects have been released they can't be reused
		mMediaRecorder = null;
		mCamera = null;
		return videoFile;
	}

	/*
	 * Init the MediaRecorder, the order the methods are called is vital to its
	 * correct functioning.
	 */

	private void initRecorder(Surface surface) throws IOException {
		// It is very important to unlock the camera before doing setCamera
		// or it will results in a black preview
		mCamera = getCameraInstance();
		try{
			//mCamera.setPreviewDisplay(null);
		}
		catch(Exception e){
			Log.e(LCAT, "Caught io exception " + e.toString());
		}
		//mCamera.setPreviewDisplay(mHolder);
		mCamera.stopPreview();		
		mCamera.unlock();


		mMediaRecorder = new MediaRecorder();

		mMediaRecorder.setCamera(mCamera);

		mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mMediaRecorder.setPreviewDisplay(surface);
		mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
		mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
		mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
		
		mMediaRecorder.setVideoSize(preferredWidth, preferredHeight);
		

		// No limit. Don't forget to check the space on disk.
		mMediaRecorder.setMaxDuration(15000);
		//mMediaRecorder.setVideoFrameRate(15);

		File sampleDir = new File(Environment.getExternalStorageDirectory()
				.getAbsolutePath() + "/Download/");
		videoFile = File.createTempFile("test", ".3gp", sampleDir);
		Log.i(LCAT, "path--" + videoFile.getAbsolutePath());
		mMediaRecorder.setOutputFile(videoFile.getAbsolutePath());

		try {
			mMediaRecorder.prepare();
		} catch (IllegalStateException e) {
			// This is thrown if the previous calls are not called with the
			// proper order
			e.printStackTrace();
		}

	}

	@SuppressWarnings("deprecation")
	public CameraPreviewer(Context context) {
		super(context);

		// Install a SurfaceHolder.Callback so we get notified when the
		// underlying surface is created and destroyed.
		mCamera = getCameraInstance();
		mHolder = getHolder();
		mHolder.addCallback(this);
		
		

		// deprecated setting, but required on Android versions prior to 3.0
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	public void surfaceCreated(SurfaceHolder holder) {
		Log.i("MYAPP", "surface created");
		if (mCamera != null) {
			// The Surface has been created, now tell the camera where to draw
			// the preview.
			try {

				mCamera.setPreviewDisplay(holder);
				mCamera.startPreview();
				mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
				Collections.sort(mSupportedPreviewSizes, new Comparator<Camera.Size>() {
				  public int compare(final Camera.Size a, final Camera.Size b) {
				    return a.width * a.height - b.width * b.height;
				  }
				});
				Log.i("MYAPP", "total previews : "+mSupportedPreviewSizes.size());
				for (Camera.Size size : mSupportedPreviewSizes) {
					Log.i("MYAPP", size.width + "x" + size.height);
					if(size.width>300 && size.width<400){
						preferredWidth = size.width;
						preferredHeight = size.height;
						break;
					}
				}
				Log.i("MYAPP", "preferred size");
				Log.i("MYAPP", preferredWidth + "x" + preferredHeight);

			} catch (IOException e) {
				Log.i("MYAPP",
						"Error setting camera preview: " + e.getMessage());
				e.printStackTrace();
			}
		}

	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.i("MYAPP", "surface destroyed");
		// empty. Take care of releasing the Camera preview in your activity.
		if (mCamera != null) {
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
		}
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		Log.i("MYAPP", "surface changed");
	}
}