package com.laomo.takephoto;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.Window;
import android.view.WindowManager;

import com.laomo.takephoto.fragment.PhotoPreviewFragment;
import com.laomo.takephoto.fragment.TakePhotoFragment;

public class TakePhotoActivity extends FragmentActivity {
    
    static final String INTENT_KEY_BYTE_DATA = "data";
    
    private FragmentManager mFragmentManager;
    private TakePhotoFragment mTakePhotoFragment;
    private PhotoPreviewFragment mPhotoPreviewFragment;

    public byte[] data;
    /**
     * 默认拍照取小票上传，只有从图库取小票时为false，取到小票预览后置为true 
     */
    public boolean isFromCamera = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	requestWindowFeature(Window.FEATURE_NO_TITLE);
	getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
	mTakePhotoFragment = new TakePhotoFragment();
	mPhotoPreviewFragment = new PhotoPreviewFragment();
	mFragmentManager = getSupportFragmentManager();
	mFragmentManager.beginTransaction().add(android.R.id.content, mTakePhotoFragment).commit();
    }


    @Override
    public void onBackPressed() {
	if (mPhotoPreviewFragment.equals(mFragmentManager.findFragmentById(android.R.id.content))) {
	    backtoTakeTicket();
	} else {
	    super.onBackPressed();
	}
    }

    public void goTicketPreview(byte[] data) {
	this.data = data;
	mFragmentManager.beginTransaction().replace(android.R.id.content, mPhotoPreviewFragment).commit();
    }

    public void backtoTakeTicket() {
	mFragmentManager.beginTransaction().replace(android.R.id.content, mTakePhotoFragment).commit();
    }
    
    /**
     * 拍照预览完毕，返回之前页面处理上传逻辑
     */
    public void uploadTicket(){
	Intent intent = new Intent();
	intent.putExtra(INTENT_KEY_BYTE_DATA, data);
	setResult(RESULT_OK, intent);
	finish();
    }
}
