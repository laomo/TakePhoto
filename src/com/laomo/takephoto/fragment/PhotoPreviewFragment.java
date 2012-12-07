package com.laomo.takephoto.fragment;


import java.io.ByteArrayOutputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.laomo.takephoto.R;
import com.laomo.takephoto.TakePhotoActivity;

public class PhotoPreviewFragment extends Fragment implements OnClickListener {

    private ImageView mPreviewView;
    private Button reworkBtn;
    private Button uploadBtn;

    private TakePhotoActivity mActivity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_ticket_preview, container, false);
	mPreviewView = (ImageView) rootView.findViewById(R.id.ticket_preview);
	reworkBtn = (Button) rootView.findViewById(R.id.ticket_rework_btn);
	reworkBtn.setOnClickListener(this);
	uploadBtn = (Button) rootView.findViewById(R.id.ticket_upload_btn);
	uploadBtn.setOnClickListener(this);

	mActivity = (TakePhotoActivity) getActivity();

	Bitmap bMap = BitmapFactory.decodeByteArray(mActivity.data, 0, mActivity.data.length);
	//自定义相机拍照需要旋转90预览支持竖屏
	if (mActivity.isFromCamera) {
	    Matrix matrix = new Matrix();
	    matrix.reset();
	    //竖屏拍照支持
	    matrix.postRotate(90);
	    Bitmap bMapRotate = Bitmap.createBitmap(bMap, 0, 0, bMap.getWidth(), bMap.getHeight(), matrix, true);
	    bMap = bMapRotate;
	}
	mPreviewView.setImageBitmap(bMap);
	new MyThread(bMap).start();
	return rootView;
    }

    @Override
    public void onClick(View v) {
	switch (v.getId()) {
	    case R.id.ticket_rework_btn:
		mActivity.backtoTakeTicket();
		break;
	    case R.id.ticket_upload_btn:
		//上传
		if(isCompressing){//用户是个猴子，等到数据压缩完成后上传
		    isUserAMonkey = true;
		    Toast.makeText(mActivity, "正在压缩数据，请稍后...", Toast.LENGTH_SHORT).show();
		}else{
		    mActivity.uploadTicket();
		}
	    default:
		break;
	}
    }
    
    /**
     * 用户在压缩数据(100毫秒内)完成前点击上传按钮，他就是猴子
     */
    private boolean isUserAMonkey = false;
    private boolean isCompressing = true;
    
    /**
     * 图片byte数据压缩线程
     */
    class MyThread extends Thread {
	private Bitmap bitmap;

	public MyThread(Bitmap bitmap) {
	    this.bitmap = bitmap;
	}

	@Override
	public void run() {
	    isCompressing = true;
	    //80%质量压缩图片
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
	    mActivity.data = baos.toByteArray();
	    isCompressing = false;
	    if(isUserAMonkey){
		mActivity.uploadTicket();
	    }
	}
    }
}
