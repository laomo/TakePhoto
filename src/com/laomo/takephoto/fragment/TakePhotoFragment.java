package com.laomo.takephoto.fragment;

import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import com.laomo.takephoto.R;
import com.laomo.takephoto.TakePhotoActivity;
import com.laomo.takephoto.utils.BitmapUtils;
import com.laomo.takephoto.utils.LogUtils;

public class TakePhotoFragment extends Fragment implements OnClickListener, SurfaceHolder.Callback {

    private static final int GET_FORM_GALLERY = 1;
    private Camera mCamera;
    private SurfaceHolder mSurfaceHolder;
    private Button cancelBtn, takeBtn, albumBtn;
    private SurfaceView mSurfaceView;
    private TakePhotoActivity mActivity;
    private SurfaceHolder mPreviewDisplayHolder;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_take_ticket, container, false);
	mSurfaceView = (SurfaceView) rootView.findViewById(R.id.take_ticket_surfaceview);
	mSurfaceHolder = mSurfaceView.getHolder();
	mSurfaceHolder.addCallback(this);
	mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

	cancelBtn = (Button) rootView.findViewById(R.id.take_ticket_cancel_btn);
	cancelBtn.setOnClickListener(this);
	takeBtn = (Button) rootView.findViewById(R.id.take_ticket_btn);
	takeBtn.setOnClickListener(this);
	albumBtn = (Button) rootView.findViewById(R.id.take_ticket_album_btn);
	albumBtn.setOnClickListener(this);

	mActivity = (TakePhotoActivity) getActivity();
	return rootView;
    }

    @Override
    public void onPause() {
	super.onPause();
	LogUtils.log("onPause ");
	// Because the Camera object is a shared resource, it's very
	// important to release it when the activity is paused.
	if (mCamera != null) {
	    mCamera.stopPreview();
	    mCamera.release();
	    mCamera = null;
	}
    }

    @Override
    public void onResume() {
	super.onResume();
	LogUtils.log("onResume ");
	/*
	 * 从图库取回图片直接去预览
	 * 因为goTicketPreview调用mFragmentManager.beginTransaction()方法，
	 * 而beginTransaction()方法在Activity.onSaveInstanceState()方法执行后不可执行，
	 * 所以把goTicketPreview放在onResume中
	 */
	if (!mActivity.isFromCamera) {
	    mActivity.goTicketPreview(mActivity.data);
	    mActivity.isFromCamera = true;
	    return;
	}
	// Open the default i.e. the first rear facing camera.
	if (mCamera == null) {
	    mCamera = Camera.open();
	    //竖屏拍照支持
	    mCamera.setDisplayOrientation(90);
	    try {
		mCamera.setPreviewDisplay(mPreviewDisplayHolder);
	    } catch (IOException exception) {
		LogUtils.log("IOException caused by setPreviewDisplay()" + exception.getMessage());
	    }
	    mCamera.startPreview();
	}
    }

    //设置这个参数才会有拍照的声音
    private ShutterCallback shutterCallback = new ShutterCallback() {
	@Override
	public void onShutter() {

	}
    };

    private PictureCallback jpegCallback = new PictureCallback() {
	public void onPictureTaken(byte[] _data, Camera _camera) {
	    mActivity.goTicketPreview(_data);
	}
    };

    @Override
    public void onClick(View v) {
	switch (v.getId()) {
	    case R.id.take_ticket_cancel_btn:
		mActivity.finish();
		break;
	    case R.id.take_ticket_btn:
		mCamera.takePicture(shutterCallback, null, jpegCallback);
		break;
	    case R.id.take_ticket_album_btn:
		//打开图库
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("image/*");
		startActivityForResult(intent, GET_FORM_GALLERY);
		break;
	    default:
		break;
	}
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
	if (requestCode == GET_FORM_GALLERY) {
	    if (resultCode != Activity.RESULT_OK) {
		return;
	    }
	    if (data == null){
		return;
	    }
	    mActivity.data = BitmapUtils.getBytesFromBitmapUri(mActivity, data.getData());
	    mActivity.isFromCamera = false;
	}
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
	LogUtils.log("surfaceCreated ");
	mPreviewDisplayHolder = holder;
	try {
	    if (mCamera != null) {
		LogUtils.log("mCamera != null ");
		mCamera.setPreviewDisplay(mPreviewDisplayHolder);
	    }
	} catch (IOException exception) {
	    LogUtils.log("IOException caused by setPreviewDisplay()" + exception.getMessage());
	}
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
	LogUtils.log("surfaceChanged ");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
	LogUtils.log("surfaceDestroyed ");
    }

}
