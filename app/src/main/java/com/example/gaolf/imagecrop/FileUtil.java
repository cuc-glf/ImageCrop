package com.example.gaolf.imagecrop;

import android.os.Environment;

/**
 * Created by gaolf on 15/12/24.
 */
public abstract class FileUtil {

    public static final String IMG_CACHE1 = App.getInstance().getFilesDir().getAbsolutePath() + "/img_cache1";
    public static final String IMG_CACHE2 = App.getInstance().getFilesDir().getAbsolutePath() + "/img_cache2";

    public static final String PUBLIC_CACHE = App.getInstance().getExternalFilesDir(Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/zyb_cache";

}
