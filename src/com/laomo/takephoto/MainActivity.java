package com.laomo.takephoto;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;

public class MainActivity extends Activity {

    private static final int CODE_TAKE_PHOTO = 1;
    
    private ImageView mImageView;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent intent = new Intent(this,TakePhotoActivity.class);
        startActivityForResult(intent, CODE_TAKE_PHOTO);
        mImageView = (ImageView) findViewById(R.id.imageview);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	//取得拍照所得图片的byte数组
	if(resultCode == RESULT_OK&&requestCode == CODE_TAKE_PHOTO){
	   byte[] _data=  data.getByteArrayExtra(TakePhotoActivity.INTENT_KEY_BYTE_DATA);
	   Bitmap bitmap = BitmapFactory.decodeByteArray(_data, 0, _data.length);
	   mImageView.setImageBitmap(bitmap);
	   //接下来就是你的上传处理
	}
	super.onActivityResult(requestCode, resultCode, data);
    }
}
