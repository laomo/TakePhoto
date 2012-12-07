package com.laomo.takephoto.utils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import android.provider.MediaStore.MediaColumns;
import android.widget.Toast;

/**
 * 处理图片的工具类
 */
public class BitmapUtils {
    /** Options used internally. */
    public static final int IO_BUFFER_SIZE = 8 * 1024;
    private static final int OPTIONS_NONE = 0x0;
    private static final int OPTIONS_SCALE_UP = 0x1;

    /***
     * Constant used to indicate we should recycle the input in {@link #extractThumbnail(Bitmap, int, int, int)} unless
     * the output is the input.
     */
    public static final int OPTIONS_RECYCLE_INPUT = 0x2;

    /***
     * Constant used to indicate the dimension of mini thumbnail.
     * @hide Only used by media framework and media provider internally.
     */
    public static final int TARGET_SIZE_MINI_THUMBNAIL = 320;

    /***
     * Constant used to indicate the dimension of micro thumbnail.
     * @hide Only used by media framework and media provider internally.
     */
    public static final int TARGET_SIZE_MICRO_THUMBNAIL = 96;
    /**
     * Constant used to indicate the SD Card is necessary.
     */
    public static final String NO_SDCARD = "无可用SDCARD";

    /***
     * Creates a centered bitmap of the desired size.
     * @param source original bitmap source
     * @param width targeted width
     * @param height targeted height
     */
    public static Bitmap extractThumbnail(Bitmap source, int width, int height) {
	return extractThumbnail(source, width, height, OPTIONS_NONE);
    }

    /***
     * Creates a centered bitmap of the desired size.
     * @param source original bitmap source
     * @param width targeted width
     * @param height targeted height
     * @param options options used during thumbnail extraction
     */
    public static Bitmap extractThumbnail(Bitmap source, int width, int height, int options) {
	if (source == null) {
	    return null;
	}

	float scale;
	if (source.getWidth() < source.getHeight()) {
	    scale = width / (float) source.getWidth();
	} else {
	    scale = height / (float) source.getHeight();
	}
	Matrix matrix = new Matrix();
	matrix.setScale(scale, scale);
	Bitmap thumbnail = transform(matrix, source, width, height, OPTIONS_SCALE_UP | options);
	return thumbnail;
    }

    /**
     * Creates a bitmap of the desired size.
     * @param drawable the src to be resized
     * @param width targeted width
     * @param height targeted height
     * @return
     */
    public static Bitmap resizeDrawable(Drawable drawable, int width, int height) {

	Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
	Canvas cas = new Canvas(bitmap);
	drawable.setBounds(0, 0, width, height);
	drawable.draw(cas);

	return bitmap;
    }

    /**
     * Creates a bitmap of the desired size.
     * @param drawable the src to be resized
     * @param width targeted width
     * @param height targeted height
     * @return
     */
    public static Bitmap resizeDrawableByWidth(Bitmap bitmap, int width) {
	int a = bitmap.getWidth();
	int b = bitmap.getHeight();

	Drawable drawable = new BitmapDrawable(bitmap);
	Bitmap bitmapResult = Bitmap.createBitmap(width, width * b / a, Bitmap.Config.RGB_565);
	Canvas cas = new Canvas(bitmapResult);
	drawable.setBounds(0, 0, width, width * b / a);
	drawable.draw(cas);

	return bitmapResult;
    }

    public static Bitmap resizeDrawableByHeight(Bitmap bitmap, int height) {
	int a = bitmap.getWidth();
	int b = bitmap.getHeight();

	Drawable drawable = new BitmapDrawable(bitmap);
	Bitmap bitmapResult = Bitmap.createBitmap(a * height / b, height, Bitmap.Config.ARGB_8888);
	Canvas cas = new Canvas(bitmapResult);
	drawable.setBounds(0, 0, a * height / b, height);
	drawable.draw(cas);

	return bitmapResult;
    }

    /**
     * Transform source Bitmap to targeted width and height.
     * @param scaler
     * @param source
     * @param targetWidth
     * @param targetHeight
     * @param options
     * @return
     */
    private static Bitmap transform(Matrix scaler, Bitmap source, int targetWidth, int targetHeight, int options) {
	boolean scaleUp = (options & OPTIONS_SCALE_UP) != 0;
	boolean recycle = (options & OPTIONS_RECYCLE_INPUT) != 0;

	int deltaX = source.getWidth() - targetWidth;
	int deltaY = source.getHeight() - targetHeight;
	if (!scaleUp && (deltaX < 0 || deltaY < 0)) {
	    /**
	     * In this case the bitmap is smaller, at least in one dimension, than the target. Transform it by placing
	     * as much of the image as possible into the target and leaving the top/bottom or left/right (or both)
	     * black.
	     */
	    Bitmap b2 = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
	    Canvas c = new Canvas(b2);

	    int deltaXHalf = Math.max(0, deltaX / 2);
	    int deltaYHalf = Math.max(0, deltaY / 2);
	    Rect src = new Rect(deltaXHalf, deltaYHalf, deltaXHalf + Math.min(targetWidth, source.getWidth()),
		deltaYHalf + Math.min(targetHeight, source.getHeight()));
	    int dstX = (targetWidth - src.width()) / 2;
	    int dstY = (targetHeight - src.height()) / 2;
	    Rect dst = new Rect(dstX, dstY, targetWidth - dstX, targetHeight - dstY);
	    c.drawBitmap(source, src, dst, null);
	    if (recycle) {
		source.recycle();
	    }
	    return b2;
	}
	float bitmapWidthF = source.getWidth();
	float bitmapHeightF = source.getHeight();

	float bitmapAspect = bitmapWidthF / bitmapHeightF;
	float viewAspect = (float) targetWidth / targetHeight;

	if (bitmapAspect > viewAspect) {
	    float scale = targetHeight / bitmapHeightF;
	    if (scale < .9F || scale > 1F) {
		scaler.setScale(scale, scale);
	    } else {
		scaler = null;
	    }
	} else {
	    float scale = targetWidth / bitmapWidthF;
	    if (scale < .9F || scale > 1F) {
		scaler.setScale(scale, scale);
	    } else {
		scaler = null;
	    }
	}

	Bitmap b1;
	if (scaler != null) {
	    // this is used for minithumb and crop, so we want to filter here.
	    b1 = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), scaler, true);
	} else {
	    b1 = source;
	}

	if (recycle && b1 != source) {
	    source.recycle();
	}

	int dx1 = Math.max(0, b1.getWidth() - targetWidth);
	int dy1 = Math.max(0, b1.getHeight() - targetHeight);

	Bitmap b2 = Bitmap.createBitmap(b1, dx1 / 2, dy1 / 2, targetWidth, targetHeight);

	if (b2 != b1) {
	    if (recycle || b1 != source) {
		b1.recycle();
	    }
	}

	return b2;
    }

    /**
     * Get bitmap by Uri
     * @param context {@link Context}
     * @param bmUri
     * @return
     */
    public static Bitmap getBitmapByUri(Context context, Uri bmUri) {
	ContentResolver resolver = context.getContentResolver();
	Bitmap bMap = null;
	byte[] mContent = null;
	try {
	    mContent = getBytesFromInputStream(resolver.openInputStream(Uri.parse(bmUri.toString())), 3500000);
	} catch (IOException e) {
	    e.printStackTrace();
	}
	bMap = getPicFromBytes(mContent, null);
	return bMap;
    }

    /**
     * @param bytes
     * @param opts
     * @return
     */
    public static Bitmap getPicFromBytes(byte[] bytes, BitmapFactory.Options opts) {
	if (bytes != null)
	    if (opts != null) {
		return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);
	    } else {
		return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
	    }
	return null;
    }

    public static byte[] getBytesFromBitmapUri(Context context, Uri uri) {
	byte[] data = null;
	ContentResolver resolver = context.getContentResolver();
	try {
	    data = getBytesFromInputStream(resolver.openInputStream(Uri.parse(uri.toString())), 3500000);
	} catch (IOException e) {
	    e.printStackTrace();
	}
	return data;
    }

    /**
     * @param is
     * @param bufsiz
     * @return
     * @throws IOException
     */
    public static byte[] getBytesFromInputStream(InputStream is, int bufsiz) throws IOException {
	int total = 0;
	byte[] bytes = new byte[4096];
	ByteBuffer bb = ByteBuffer.allocate(bufsiz);
	while (true) {
	    int read = is.read(bytes);
	    if (read == -1)
		break;
	    bb.put(bytes, 0, read);
	    total += read;
	}
	byte[] content = new byte[total];
	bb.flip();
	bb.get(content, 0, total);
	return content;
    }

    public static Bitmap getBitmapAutoResize(String imagePath) {
	if (imagePath == null || imagePath.equals("")) {
	    return null;
	}
	Bitmap bm = null;
	BitmapFactory.Options opts = new BitmapFactory.Options();
	opts.inJustDecodeBounds = true;
	BitmapFactory.decodeFile(imagePath, opts);
	opts.inSampleSize = calculateInSampleSize(opts, 1281, 901);
	opts.inJustDecodeBounds = false;
	try {

	    bm = BitmapFactory.decodeFile(imagePath, opts);

	} catch (Exception ex) {
	    ex.printStackTrace();
	    return null;
	}
	return bm;
    }

    /**
     * Get the real path by Uri
     * @param contentUri
     * @param context
     * @return
     */
    public static String getRealPathFromUri(Uri contentUri, Activity context) {
	String[] proj = { MediaColumns.DATA };
	Cursor cursor = context.managedQuery(contentUri, proj, null, null, null);
	if (cursor == null || cursor.getCount() == 0) {
	    return null;
	}
	int column_index = cursor.getColumnIndexOrThrow(MediaColumns.DATA);
	cursor.moveToFirst();
	return cursor.getString(column_index);
    }

    /**
     * @param file targeted file
     * @param bitmap the bitmap need to be saved
     */
    public static void saveBitmapToFile(File file, Bitmap bitmap) {
	file.delete();
	BufferedOutputStream outStream = null;
	try {
	    if (!file.exists()) {
		file.createNewFile();
	    }
	    outStream = new BufferedOutputStream(new FileOutputStream(file), 1024);
	    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
	    outStream.flush();
	} catch (IOException ex) {
	    ex.printStackTrace();
	} finally {
	    if (null != outStream) {
		try {
		    outStream.close();
		} catch (IOException ex) {
		}
	    }
	}
    }

    public static boolean checkSd() {
	return (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED));
    }

    /**
     * @param context
     * @return false means no sdcard.
     */
    public static boolean needSDCard(Context context) {
	if (!checkSd()) {
	    Toast.makeText(context, NO_SDCARD, Toast.LENGTH_LONG).show();
	    return false;
	}
	return true;
    }

    /**
     * @param image
     * @param saveFilePath
     * @param angle
     * @return
     */
    public static Bitmap rotateImage(Bitmap image, int angle, String saveFilePath) {
	if (image == null) {
	    return null;
	}

	double angle_double = Math.rint(angle / 90);
	if (angle_double == 0) {
	    return image;
	}

	int tempAngle = (int) (angle_double * 90);
	Matrix matrix = new Matrix();
	matrix.postRotate(tempAngle);
	Bitmap rotateImage = Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), matrix, true);
	if (saveFilePath != null) {
	    if (!"".equals(saveFilePath)) {
		try {
		    saveFile(rotateImage, saveFilePath);
		} catch (Exception e) {
		    return image;
		}
	    }
	}
	return rotateImage;
    }

    /**
     * @param imageBitmap The bitmap need to be saved
     * @param saveFilePath save path
     * @throws IOException
     */
    public static void saveFile(Bitmap imageBitmap, String saveFilePath) throws IOException {
	saveFile(imageBitmap, saveFilePath, Bitmap.CompressFormat.JPEG);
    }

    /**
     * @param imageBitmap The bitmap need to be saved
     * @param saveFilePath save path
     * @param _CompressFormat Hint to the compressor, 0-100. 0 meaning compress for small size, 100 meaning compress for
     *            max quality. Some formats, like PNG which is lossless, will ignore the quality setting
     * @throws IOException Exception happened when it's not possible to create the file.
     */
    public static void saveFile(Bitmap imageBitmap, String saveFilePath, Bitmap.CompressFormat _CompressFormat)
	throws IOException {
	File t_fileImage = new File(saveFilePath);
	if (!t_fileImage.exists() && !t_fileImage.isFile()) {
	    t_fileImage.createNewFile();
	}
	FileOutputStream out = new FileOutputStream(t_fileImage);
	if (imageBitmap.compress(_CompressFormat, 100, out)) {
	    out.flush();
	    out.close();
	}
    }

    /**
     * @param bitmap
     * @param pixels
     * @return
     */
    public static Bitmap toRoundCorner(Bitmap bitmap, int pixels) {
	Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);
	Canvas canvas = new Canvas(output);

	final int color = 0xff424242;
	final Paint paint = new Paint();
	final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
	final RectF rectF = new RectF(rect);
	final float roundPx = pixels;

	paint.setAntiAlias(true);
	canvas.drawARGB(0, 0, 0, 0);
	paint.setColor(color);
	canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

	paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
	canvas.drawBitmap(bitmap, rect, rect, paint);

	return output;
    }

    /**
     * To calculate a sample size value based on a target width and height
     * @param options
     * @param reqWidth targeted width
     * @param reqHeight targeted height
     * @return
     */
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
	// Raw height and width of image
	final int height = options.outHeight;
	final int width = options.outWidth;
	int inSampleSize = 1;

	if (height > reqHeight || width > reqWidth) {
	    if (width > height) {
		inSampleSize = Math.round((float) height / (float) reqHeight);
	    } else {
		inSampleSize = Math.round((float) width / (float) reqWidth);
	    }
	}
	return inSampleSize;
    }

    /**
     * @param path the path of the source
     * @param reqWidth target width
     * @param reqHeight target height
     * @return
     */
    public static Bitmap decodeBitmapByDecodeFile(String path, int reqWidth, int reqHeight) {
	// First decode with inJustDecodeBounds=true to check dimensions
	BitmapFactory.Options options = new BitmapFactory.Options();
	options.inJustDecodeBounds = true;
	BitmapFactory.decodeFile(path, options);

	// Calculate inSampleSize
	options.inSampleSize = BitmapUtils.calculateInSampleSize(options, reqWidth, reqHeight);

	// Decode bitmap with inSampleSize set
	options.inJustDecodeBounds = false;
	return BitmapFactory.decodeFile(path, options);
    }

    /**
     * Get the bitmap from resource
     * @param res
     * @param resId
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    public static Bitmap decodeBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight) {

	// First decode with inJustDecodeBounds=true to check dimensions
	final BitmapFactory.Options options = new BitmapFactory.Options();
	options.inJustDecodeBounds = true;
	BitmapFactory.decodeResource(res, resId, options);

	// Calculate inSampleSize
	options.inSampleSize = BitmapUtils.calculateInSampleSize(options, reqWidth, reqHeight);

	// Decode bitmap with inSampleSize set
	options.inJustDecodeBounds = false;
	return BitmapFactory.decodeResource(res, resId, options);
    }

    public static int getTargetHeight(String path, int reqWidth) {
	int height = 0;
	BitmapFactory.Options options = new BitmapFactory.Options();
	options.inJustDecodeBounds = true;
	BitmapFactory.decodeFile(path, options);
	if (options.outWidth > reqWidth) {
	    float h = ((float) options.outHeight / (float) options.outWidth);
	    DecimalFormat df = new DecimalFormat("0.00");
	    String filesize = df.format(h);
	    height = (int) (Float.parseFloat(filesize) * reqWidth);
	} else {
	    height = options.outHeight;
	}
	return height;
    }

    //16 24 30 48...
    public static int getMemoryClass(Context context) {
	return ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();
    }

    public static long getUsableSpace(File path) {
	final StatFs stats = new StatFs(path.getPath());
	return (long) stats.getBlockSize() * (long) stats.getAvailableBlocks();
    }

    public static int getBitmapSize(Bitmap bitmap) {
	// Pre HC-MR1
	return bitmap.getRowBytes() * bitmap.getHeight();
    }

    public static Bitmap toRoundCornerOnUp(Bitmap bitmap, int rx) {

	Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);
	Canvas canvas = new Canvas(output);

	final int color = 0xff424242;
	final Paint paint = new Paint();
	final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
	final Rect aim = new Rect(0, rx, bitmap.getWidth(), bitmap.getHeight());
	final RectF rectF = new RectF(rect);

	paint.setAntiAlias(true);
	canvas.drawARGB(0, 0, 0, 0);
	paint.setColor(color);
	canvas.drawRoundRect(rectF, rx, rx, paint);
	canvas.drawRect(aim, paint);
	paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
	canvas.drawBitmap(bitmap, rect, rect, paint);

	return output;
    }

    public static Bitmap Bytes2Bimap(byte[] b) {
	if (b.length != 0) {
	    return BitmapFactory.decodeByteArray(b, 0, b.length);
	} else {
	    return null;

	}

    }

    public static Bitmap drawableToBitmap(Drawable drawable) {

	Bitmap bitmap = Bitmap.createBitmap(

	drawable.getIntrinsicWidth(),

	drawable.getIntrinsicHeight(),

	drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888

	: Bitmap.Config.RGB_565);

	Canvas canvas = new Canvas(bitmap);
	drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());

	drawable.draw(canvas);

	return bitmap;

    }

    public static byte[] Bitmap2Bytes(Bitmap bm) {

	ByteArrayOutputStream baos = new ByteArrayOutputStream();

	bm.compress(Bitmap.CompressFormat.JPEG, 100, baos);

	try {
	    baos.flush();
	    bm.recycle();
	} catch (IOException e) {
	    e.printStackTrace();
	}
	byte[] b = baos.toByteArray();

	return b;

    }

    public static byte[] drawable2Byte(Drawable drawable) {
	return Bitmap2Bytes(drawableToBitmap(drawable));
    }

}