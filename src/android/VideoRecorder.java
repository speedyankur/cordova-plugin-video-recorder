package com.homeinspectorpro.video.recorder;

import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Environment;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class VideoRecorder extends CordovaPlugin{
	private static final String LCAT = "VideoTest";
    private CameraPreviewer mPreview;
    final int MSG_START_TIMER = 0;
    final int MSG_STOP_TIMER = 1;
    final int MSG_UPDATE_TIMER = 2;
    Stopwatch timer = new Stopwatch();
    final int REFRESH_RATE = 100;
	InAppDialog dialog;
	private TextView tvTextView;
	private CallbackContext callbackContext;
	private Button close;
	
    Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
            case MSG_START_TIMER:
                timer.start(); //start timer
                mHandler.sendEmptyMessage(MSG_UPDATE_TIMER);
                break;

            case MSG_UPDATE_TIMER:
                runOnUiThread(new Runnable(){
                    @Override
                    public void run(){  
                        tvTextView.setText("0:"+ String.format("%02d",timer.getElapsedTimeSecs()));
                    }
                });                
                if(timer.getElapsedTimeSecs()>=15){
                	mHandler.removeMessages(MSG_UPDATE_TIMER); // no more updates.
                    timer.stop();//stop timer  
                    File file = mPreview.stopRecording();
                	dialog.dismiss();
                    callbackContext.success(file.toURI().toString());                    
                }
                else
                	mHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIMER,REFRESH_RATE); //text view is updated every second,                 
                break;                                  //though the timer is still running
            case MSG_STOP_TIMER:
                mHandler.removeMessages(MSG_UPDATE_TIMER); // no more updates.
                timer.stop();//stop timer
                runOnUiThread(new Runnable(){
                    @Override
                    public void run(){  
                        tvTextView.setText("0:"+ String.format("%02d",timer.getElapsedTimeSecs()));
                    }
                });                 
                
                break;
            default:
                break;
            }
        }
    }; 
	
	
	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		this.callbackContext = callbackContext;		
		cordova.getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		showCameraDialogBox(callbackContext);
		return true;
	}
	
    private void showCameraDialogBox(final CallbackContext callbackContext) {
        // Create dialog in new thread
        Runnable runnable = new Runnable() {
            /**
             * Convert our DIP units to Pixels
             *
             * @return int
             */
            private int dpToPixels(int dipValue) {
                int value = (int) TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_DIP,
                                                            (float) dipValue,
                                                            cordova.getActivity().getResources().getDisplayMetrics()
                );

                return value;
            }

            public void run() {
                // Let's create the main dialog
                dialog = new InAppDialog(cordova.getActivity(), android.R.style.Theme_NoTitleBar);
                dialog.getWindow().getAttributes().windowAnimations = android.R.style.Animation_Dialog;
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setCancelable(false);
                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {					
					@Override
					public void onDismiss(DialogInterface dialog) {
						// TODO Auto-generated method stub
						cordova.getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
					}
				});
                

                // Main container layout
                LinearLayout main = new LinearLayout(cordova.getActivity());
                main.setOrientation(LinearLayout.VERTICAL);
                main.setBackgroundColor(android.graphics.Color.BLACK);

                // Toolbar layout
                RelativeLayout toolbar = new RelativeLayout(cordova.getActivity());
                //Please, no more black! 
                toolbar.setBackgroundColor(android.graphics.Color.LTGRAY);
                toolbar.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, this.dpToPixels(44)));
                toolbar.setPadding(this.dpToPixels(2), this.dpToPixels(2), this.dpToPixels(2), this.dpToPixels(2));
                toolbar.setHorizontalGravity(Gravity.LEFT);
                toolbar.setVerticalGravity(Gravity.TOP);

                // Action Button Container layout
                RelativeLayout actionButtonContainer = new RelativeLayout(cordova.getActivity());
                actionButtonContainer.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
                actionButtonContainer.setHorizontalGravity(Gravity.LEFT);
                actionButtonContainer.setVerticalGravity(Gravity.CENTER_VERTICAL);
                actionButtonContainer.setId(1);
                
                Button recordButton = new Button(cordova.getActivity());
                recordButton.setId(2);
                recordButton.setText("Start");
                RelativeLayout.LayoutParams backLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT);
                backLayoutParams.addRule(RelativeLayout.ALIGN_LEFT);
                recordButton.setLayoutParams(backLayoutParams);
                recordButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                    	Button thisButton = (Button)v;
                    	if(thisButton.getText().equals("Start")){
                    		mHandler.sendEmptyMessage(MSG_START_TIMER);
                        	thisButton.setText("Stop");
                        	Log.i("MYAPP", "start recording now");
                        	mPreview.startRecording(); 
                        	close.setEnabled(false);
                    	}
                    	else{
                        	thisButton.setText("Start");
                        	mHandler.sendEmptyMessage(MSG_STOP_TIMER);
                        	Log.i("MYAPP", "stop recording now");
                        	File file = mPreview.stopRecording();
                        	dialog.dismiss();
                            callbackContext.success(file.toURI().toString());
                        	
                    	}

                    }
                });                
                
                actionButtonContainer.addView(recordButton);
                
                tvTextView =  new TextView(cordova.getActivity());
                tvTextView.setText("0:00");
                tvTextView.setTextSize(20);
                tvTextView.setTextColor(android.graphics.Color.RED);
                RelativeLayout.LayoutParams forwardLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
                forwardLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
                tvTextView.setLayoutParams(forwardLayoutParams);
                toolbar.addView(tvTextView);
                
               

                Resources activityRes = cordova.getActivity().getResources();

                // Close button
                close = new Button(cordova.getActivity());
                RelativeLayout.LayoutParams closeLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
                closeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                close.setLayoutParams(closeLayoutParams);
                close.setId(5);
                //close.setText(buttonLabel);
                int closeResId = activityRes.getIdentifier("ic_action_remove", "drawable", cordova.getActivity().getPackageName());
                Drawable closeIcon = activityRes.getDrawable(closeResId);
                if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN)
                {
                    close.setBackgroundDrawable(closeIcon);
                }
                else
                {
                    close.setBackground(closeIcon);
                }
                close.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        dialog.dismiss();
                        callbackContext.success();
                    }
                });



                // Add the views to our toolbar
                toolbar.addView(actionButtonContainer);

                toolbar.addView(close);


                main.addView(toolbar);       
                
                
                Log.d(LCAT, "createAdView()");	
                FrameLayout layout = new FrameLayout(cordova.getActivity());
        		//RelativeLayout layout = new RelativeLayout(cordova.getActivity());  
                layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));  
                layout.setBackgroundColor(android.graphics.Color.BLACK);
                           
                //mCamera = CameraPreviewer.getCameraInstance();
                mPreview = new CameraPreviewer(cordova.getActivity());   
                //mPreview.setZOrderOnTop(true);
        		RelativeLayout.LayoutParams mSurfaceViewParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT);  

        		mPreview.setLayoutParams(mSurfaceViewParams);  
                
                layout.addView(mPreview);  
        		
        		main.addView(layout); 


                WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
                lp.copyFrom(dialog.getWindow().getAttributes());
                lp.width = WindowManager.LayoutParams.MATCH_PARENT;
                lp.height = WindowManager.LayoutParams.MATCH_PARENT;

                dialog.setContentView(main);
                dialog.show();
                dialog.getWindow().setAttributes(lp);


            }
        };
        
        this.cordova.getActivity().runOnUiThread(runnable);
    }	
    
   
  
}
