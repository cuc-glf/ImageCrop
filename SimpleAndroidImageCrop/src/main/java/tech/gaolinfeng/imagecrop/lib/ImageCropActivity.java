package tech.gaolinfeng.imagecrop.lib;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * Created by gaolf on 15/12/21.
 */
public class ImageCropActivity extends Activity {

    private static final String REQUEST_IMAGE_PATH = "REQUEST_IMAGE_PATH";
    private static final String REQUEST_TARGET_PATH = "REQUEST_TARGET_PATH";
    private static final String REQUEST_CROP_RECT = "REQUEST_CROP_RECT";
    private static final String REQUEST_CROP_IN_CIRCLE = "REQUEST_CROP_IN_CIRCLE";

    public static Intent createIntent(Activity from, String imageFilePath, String targetPath, String cropRect, boolean cropInCircle) {
        Intent intent = new Intent(from, ImageCropActivity.class);
        intent.putExtra(REQUEST_IMAGE_PATH, imageFilePath);
        intent.putExtra(REQUEST_TARGET_PATH, targetPath);
        intent.putExtra(REQUEST_CROP_RECT, cropRect);
        intent.putExtra(REQUEST_CROP_IN_CIRCLE, cropInCircle);
        return intent;
    }

    private String inPath, outPath;
    private Rect cropRect;
    private boolean isCircle;

    private CropImageView cropImageView;
    private View loadingView;
    private int inSampleSize;                       // 读取时，如果图片过大，会被缩小后读取，因此裁剪时需要补上该值

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent() != null) {
            inPath = getIntent().getStringExtra(REQUEST_IMAGE_PATH);
            outPath = getIntent().getStringExtra(REQUEST_TARGET_PATH);
            isCircle = getIntent().getBooleanExtra(REQUEST_CROP_IN_CIRCLE, true);
            String cropRectStr = getIntent().getStringExtra(REQUEST_CROP_RECT);
            if (cropRectStr.matches("\\s*\\d+\\s*,\\s*\\d+\\s*,\\s*\\d+\\s*,\\s*\\d+\\s*")) {
                String[] cropRectStrArray = cropRectStr.split(",");
                int[] rectData = new int[4];
                for (int i = 0; i < rectData.length; i++) {
                    rectData[i] = Integer.parseInt(cropRectStrArray[i].trim());
                }
                cropRect = new Rect(rectData[0], rectData[1], rectData[2], rectData[3]);
            } else {
                throw new RuntimeException(
                        "imageCropActivity only accepts cropRect with format [left, top, right, bottom]");
            }
        }

        setContentView(R.layout.activity_image_crop);
        cropImageView = (CropImageView) findViewById(R.id.crop_image);
        cropImageView.setCropCircle(isCircle);

        // 根据手机屏幕大小找出合适的inSampleSize
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(inPath, options);
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int screenWidth = dm.widthPixels;
        int screenHeight = dm.heightPixels;
        int imgWidth = options.outWidth;
        int imgHeight = options.outHeight;
        int inSampleSizeWidth = 1, inSampleSizeHeight = 1;
        while (screenWidth * inSampleSizeWidth * 2 < imgWidth) {
            inSampleSizeWidth *= 2;
        }
        while (screenHeight * inSampleSizeHeight * 2 < imgHeight) {
            inSampleSizeHeight *= 2;
        }
        inSampleSize = Math.max(inSampleSizeWidth, inSampleSizeHeight);
        options.inJustDecodeBounds = false;
        options.inSampleSize = inSampleSize;
        Bitmap bitmap = BitmapFactory.decodeFile(inPath, options);
        cropImageView.setImageBitmap(bitmap);
        cropImageView.setEdge(cropRect);
        cropImageView.startCrop();

        loadingView = findViewById(R.id.crop_image_cropping_layer);

        findViewById(R.id.crop_image_ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadingView.setVisibility(View.VISIBLE);

                RectF imgRect = cropImageView.getCurrentRect();
                CropTask task = new CropTask(
                        ImageCropActivity.this, inPath, outPath, isCircle, cropRect, imgRect,
                        (int) cropImageView.getRawWidth(), (int) cropImageView.getRawHeight(), inSampleSize);
                task.execute();
            }
        });
        findViewById(R.id.crop_image_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    private static class CropTask extends AsyncTask<Void, Void, Integer> {
        private static final int SUCCESS = 0;
        private static final int FAIL_CROP = 1;
        private static final int FAIL_OUT_PATH = 2;

        private String inPath;
        private String outPath;
        private Rect cropRect;
        private RectF imgRect;
        private int rawImgWidth;
        private int rawImgHeight;
        private int rawInSampleSize;
        private boolean cropCircle;
        private Activity context;

        public CropTask(Activity context, String inPath, String outPath, boolean cropCircle, Rect cropRect, RectF imgRect, int rawImgWidth, int rawImgHeight, int rawInSampleSize) {
            this.inPath = inPath;               // 要裁剪的原图路径
            this.outPath = outPath;             // 要输出的裁剪路径
            this.cropCircle = cropCircle;       // 是否裁剪成圆形
            this.cropRect = cropRect;           // CropImageView指定的裁剪区域
            this.imgRect = imgRect;             // CropImageView中图片被移动、缩放后的区域
            this.rawImgWidth = rawImgWidth;     // CropImageView中图片原大小
            this.rawImgHeight = rawImgHeight;   // CropImageView中图片原大小
            this.rawInSampleSize = rawInSampleSize; // 图片文件如果过大，在解码时就会指定一个inSampleSize解出一个较小的bitmap，避免爆内存；在裁剪时，计算裁剪区域在原图中的区域也需要考虑这个inSampleSize
            this.context = context;
        }

        @Override
        protected Integer doInBackground(Void... params) {

            // 计算这个rect在原图中的区域
            Rect mappedCropRect = new Rect(
                    (int) ((cropRect.left - imgRect.left) * rawImgWidth * rawInSampleSize / imgRect.width()),
                    (int) ((cropRect.top - imgRect.top) * rawImgHeight * rawInSampleSize / imgRect.height()),
                    (int) ((cropRect.right - imgRect.left) * rawImgWidth * rawInSampleSize / imgRect.width()),
                    (int) ((cropRect.bottom - imgRect.top) * rawImgHeight * rawInSampleSize / imgRect.height())
            );

            // 1, 检查这个rect内的像素数
            int rawCropArea = mappedCropRect.width() * mappedCropRect.height();     // 原图中裁剪区域点数
            int cropArea = cropRect.width() * cropRect.height();                    // 实际需要的点数
            int inSampleSize = 1;
            while (cropArea * inSampleSize * 2 < rawCropArea) {
                inSampleSize *= 2;
            }

            Bitmap bmp;
            try {
                if (Build.VERSION.SDK_INT > 9) {
                    BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(inPath, true);
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = inSampleSize;
                    bmp = decoder.decodeRegion(mappedCropRect, options);
                } else {
                    // sdk < 10的系统不支持BitmapRegionDecoder方法
                    // 把原图从系统中读取进来，进行内存裁剪
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = inSampleSize;
                    Bitmap originBitmap = BitmapFactory.decodeFile(inPath, options);
                    bmp = Bitmap.createBitmap(originBitmap,
                            mappedCropRect.left, mappedCropRect.top,
                            mappedCropRect.width(), mappedCropRect.height());
                    if (originBitmap != bmp) {
                        originBitmap.recycle();
                    }
                }
            } catch (Exception e){
                e.printStackTrace();
                return FAIL_CROP;
            }
            Bitmap b;
            if (cropCircle) {
                b = ImageUtil.toRoundBitmap(bmp);
                bmp.recycle();
            } else {
                b = bmp;
            }
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(outPath);
                b.compress(Bitmap.CompressFormat.PNG, 70, fos);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return FAIL_OUT_PATH;
            } finally {
                IOUtil.closeQuietly(fos);
            }
            return SUCCESS;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
            switch (integer) {
                case SUCCESS:
                    context.setResult(RESULT_OK);
                    context.finish();
                    break;
                case FAIL_CROP:
                    Toast.makeText(context, "图片裁剪失败", Toast.LENGTH_SHORT).show();
                    break;
                case FAIL_OUT_PATH:
                    Toast.makeText(context, "输出路径无效", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

}
