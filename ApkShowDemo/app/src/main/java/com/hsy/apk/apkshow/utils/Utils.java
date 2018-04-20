package com.hsy.apk.apkshow.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import com.hsy.apk.apkshow.entity.FileInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: syhuang
 * @date: 2018/4/20
 */

public class Utils {
    static String TAG = Utils.class.getSimpleName();

    /**
     * 存储卡获取 指定文件
     *
     * @param context
     * @param extension
     * @return
     */
    public static List<FileInfo> getSpecificTypeFiles(Context context, String[] extension) {
        List<FileInfo> fileInfoList = new ArrayList<FileInfo>();

        //内存卡文件的Uri
        Uri fileUri = MediaStore.Files.getContentUri("external");
        //筛选列，这里只筛选了：文件路径和含后缀的文件名
        String[] projection = new String[]{
                MediaStore.Files.FileColumns.DATA, MediaStore.Files.FileColumns.TITLE
        };

        //构造筛选条件语句
        String selection = "";
        for (int i = 0; i < extension.length; i++) {
            if (i != 0) {
                selection = selection + " OR ";
            }
            selection = selection + MediaStore.Files.FileColumns.DATA + " LIKE '%" + extension[i] + "'";
        }
        //按时间降序条件
        String sortOrder = MediaStore.Files.FileColumns.DATE_MODIFIED;

        Cursor cursor = context.getContentResolver().query(fileUri, projection, selection, null, sortOrder);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                try {
                    String data = cursor.getString(0);
                    FileInfo fileInfo = new FileInfo();
                    fileInfo.setFilePath(data);

                    long size = 0;
                    try {
                        File file = new File(data);
                        size = file.length();
                        fileInfo.setSize(size);
                    } catch (Exception e) {

                    }
                    fileInfoList.add(fileInfo);
                } catch (Exception e) {
                    Log.i("FileUtils", "------>>>" + e.getMessage());
                }

            }
        }
        Log.i(TAG, "getSize ===>>> " + fileInfoList.size());
        return fileInfoList;
    }

}
