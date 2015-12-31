package tech.gaolinfeng.imagecrop.demo;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import tech.gaolinfeng.imagecrop.lib.IOUtil;
import tech.gaolinfeng.imagecrop.lib.ImageCropActivity;

/**
 * Created by gaolf on 15/12/24.
 */
public class EntryActivity extends Activity {

    private static final int REQUEST_CODE_SELECT_IMAGE = 1;
    private static final int REQUEST_CODE_TAKE_PICTURE = 2;
    private static final int REQUEST_CODE_CROP = 3;

    private boolean cropCircle = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry);
        findViewById(R.id.entry_select_image).setOnClickListener(selectImageClick);
        findViewById(R.id.entry_take_picture).setOnClickListener(takePictureClick);
        ((CheckBox)findViewById(R.id.entry_crop_circle)).setOnCheckedChangeListener(cropCircleCheckListener);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_CODE_SELECT_IMAGE:
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    ContentResolver cr = getContentResolver();
                    InputStream is = null;
                    FileOutputStream fos = null;
                    boolean writeSucceed = true;

                    try {
                        is = cr.openInputStream(uri);
                        fos = new FileOutputStream(FileUtil.IMG_CACHE1);
                        int read = 0;
                        byte[] buffer = new byte[4096];
                        while ((read = is.read(buffer)) > 0) {
                            fos.write(buffer);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        writeSucceed = false;
                    } finally {
                        IOUtil.closeQuietly(is);
                        IOUtil.closeQuietly(fos);
                    }

                    if (writeSucceed) {
                        Intent intent = ImageCropActivity.createIntent(
                                EntryActivity.this, FileUtil.IMG_CACHE1, FileUtil.IMG_CACHE2, getCropAreaStr(), cropCircle);
                        startActivityForResult(intent, REQUEST_CODE_CROP);
                    } else {
                        Toast.makeText(EntryActivity.this, "无法打开图片文件，您的sd卡是否已满？", Toast.LENGTH_SHORT).show();
                    }

                } else {
                    // do nothing
                }
                break;

            case REQUEST_CODE_TAKE_PICTURE:
                if (resultCode == RESULT_OK) {
                    Intent intent = ImageCropActivity.createIntent(
                            EntryActivity.this, FileUtil.PUBLIC_CACHE, FileUtil.IMG_CACHE2, getCropAreaStr(), cropCircle);
                    startActivityForResult(intent, REQUEST_CODE_CROP);
                } else {
                    // do nothing
                }
                break;

            case REQUEST_CODE_CROP:
                if (resultCode == RESULT_OK) {
                    Bitmap bitmap = BitmapFactory.decodeFile(FileUtil.IMG_CACHE2);
                    ImageView image = (ImageView)findViewById(R.id.entry_result_image);
                    image.setImageBitmap(bitmap);
                    image.setVisibility(View.VISIBLE);
                } else {
                    // do nothing
                }
                break;
        }

    }

    private String getCropAreaStr() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int screenWidth = dm.widthPixels;
        int screenHeight = dm.heightPixels;
        int rectWidth = screenWidth / 2;
        int left = screenWidth / 2 - rectWidth / 2;
        int right = screenWidth / 2 + rectWidth / 2;
        int top = screenHeight / 2 - rectWidth / 2;
        int bottom = screenHeight / 2 + rectWidth / 2;
        return left + ", " + top + ", " + right + ", " + bottom;
    }

    private View.OnClickListener takePictureClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            File file = new File(FileUtil.PUBLIC_CACHE);
            if (file.exists()) {
                file.delete();
            }
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file)); // set the image file name
            startActivityForResult(intent, REQUEST_CODE_TAKE_PICTURE);
        }
    };

    private View.OnClickListener selectImageClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.putExtra("crop", false);
            intent.putExtra("return-data", true);
            startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE);
        }
    };

    private CompoundButton.OnCheckedChangeListener cropCircleCheckListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            cropCircle = isChecked;
        }
    };

}
