package com.hsy.apk.apkshow;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

import com.hsy.apk.apkshow.utils.ApkUtils;
import com.hsy.apk.apkshow.utils.FileResLoaderUtils;
import com.hsy.apk.apkshow.utils.FileTypeUtils;
import com.hsy.apk.apkshow.utils.ToastUtils;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener,
        FileResLoaderUtils.OnLoadFinishedListener {
    String TAG = MainActivity.class.getSimpleName();
    private MyGridViewAdapter  adapter;
    private GridView           gridView;
    private PackageManager     manager;
    private TextView           applicationNumber;
    private FileResLoaderUtils drawableLoaderUtils;
    //用来异步加载图片

    List<PackageInfo>  packageInfoList = new ArrayList<>();
    List<PackageInfo>  apps            = new ArrayList<>();
    ArrayList<String>  mNameList       = new ArrayList<>();
    /**
     * 应用路径
     */

    ArrayList<String>  mPathList       = new ArrayList<>();
    ArrayList<String>  mPackageList    = new ArrayList<>();
//    ArrayList<Boolean> mCheckBoxList   = new ArrayList<>();

    //用来还原gridview的位置
    private int gridViewState_pos = 0;

    private BroadcastReceiver appReceiver = new AppReceiver();

    public static final int LOAD_DATA_FINISHED = 0;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case LOAD_DATA_FINISHED:
                    gridView.setAdapter(adapter);
                    gridView.setSelection(gridViewState_pos);
                    applicationNumber.setText(getResources().getString(R.string.installed_apps) + "(" + mNameList.size() + ")");
                    if (drawableLoaderUtils != null) {
                        for (int i = 0; i < mNameList.size(); i++) {
                            //启动异步任务加载图片,加载完成一个图片后会调用onLoadOneFinished
                            drawableLoaderUtils.load(MainActivity.this, mPathList.get(i));
                        }
                    }
                    adapter.notifyDataSetChanged();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        gridView = (GridView) findViewById(R.id.id_grid_view);
        applicationNumber = (TextView) findViewById(R.id.id_application_number);
        manager = this.getPackageManager();
        if (adapter == null) {
            //first loaded data
            drawableLoaderUtils = FileResLoaderUtils.getInstance(this);
            loadData();
        } else {
            handler.sendEmptyMessage(LOAD_DATA_FINISHED);
        }
        gridView.setOnItemClickListener(this);
        gridView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                    //stop
                    gridView.setTag(false);
                    ((BaseAdapter) gridView.getAdapter()).notifyDataSetChanged();
                } else { //scrolling
                    gridView.setTag(true);
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        });
        registerForContextMenu(gridView);
        //添加菜单
        //注册广播
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addDataScheme("package");
        registerReceiver(appReceiver, filter);
    }

    protected String getPositonPath(int position) {
        return mPathList.get(position);
    }

    protected boolean isApkInstalled(String path) {
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo menuInfo1 = (AdapterView.AdapterContextMenuInfo) menuInfo;
        int pos = menuInfo1.position;
        String path = getPositonPath(pos);
        menu.clear();
        int order = 0;
        menu.add(0, R.string.menu_open, order++, R.string.menu_open);
        if (FileTypeUtils.isApk(path) && isApkInstalled(path)) {
            // 已安装APK ---> 删除改为卸载
            menu.add(0, R.string.menu_uninstall, order++, R.string.menu_uninstall);
        } else {
            menu.add(0, R.string.menu_delete, order++, R.string.menu_delete);
        }
        menu.add(0, R.string.menu_property, order++, R.string.menu_property);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int pos = menuInfo.position;
        String path = getPositonPath(pos);
        switch (item.getItemId()) {
            //这个是针对已安装apk的
            case R.string.menu_uninstall:
                //uninstall apk
                Log.i(TAG, "context menu click--->menu_uninstall");
                String packageName = ApkUtils.getApkPackageName(MainActivity.this, path);
                Uri packageUri = Uri.parse("package:" + packageName);
                Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageUri);
                startActivity(uninstallIntent);
                break;
            /////////////////////////////////////////////////////////////////////////////////////
            case R.string.menu_open:
                if (FileTypeUtils.isApk(path) && isApkInstalled(path)) {
                    //已安装apk--->just open the app instead of install it
                    String packageName1 = ApkUtils.getApkPackageName(MainActivity.this, path);
                    Intent it = getPackageManager().getLaunchIntentForPackage(packageName1);
                    startActivity(it);
                } else {
                    FileTypeUtils.openFile(MainActivity.this, path);
                }
                break;

            case R.string.menu_delete:

                break;
            case R.string.menu_property:
                //跳转到该应用的详细设置页面，设置权限页面
                Intent localIntent = new Intent();
                localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                String packageName1 = ApkUtils.getApkPackageName(MainActivity.this, path);

                if (Build.VERSION.SDK_INT >= 9) {
                    localIntent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
                    localIntent.setData(Uri.fromParts("package", packageName1, null));
                } else if (Build.VERSION.SDK_INT <= 8) {
                    localIntent.setAction(Intent.ACTION_VIEW);
                    localIntent.setClassName("com.android.settings", "com.android.setting.InstalledAppDetails");
                    localIntent.putExtra("com.android.settings.ApplicationPkgName", packageName1);
                }
                startActivity(localIntent);

                break;
            default:
                break;
        }
        return super.onContextItemSelected(item);
    }

    public void loadData() {
        new Thread() {
            @Override
            public void run() {
                apps.clear();
                mNameList.clear();
                mPathList.clear();
                mPackageList.clear();
//                mCheckBoxList.clear();
                packageInfoList = manager.getInstalledPackages(0);
                for (int i = 0; i < packageInfoList.size(); i++) {
                    PackageInfo packageInfo = packageInfoList.get(i);
                    if ((packageInfo.applicationInfo.flags & packageInfo.applicationInfo.FLAG_SYSTEM) <= 0) {
                        //第三方应用
                        apps.add(packageInfo);
                        mNameList.add(manager.getApplicationLabel(packageInfo.applicationInfo).toString());
                        mPathList.add(packageInfo.applicationInfo.sourceDir);
                        mPackageList.add(packageInfo.packageName);
//                        mCheckBoxList.add(false);
                    } else {
                    }
                }
                adapter = new MyGridViewAdapter(MainActivity.this, mNameList, mPathList,  60);
                handler.sendEmptyMessage(LOAD_DATA_FINISHED);
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(appReceiver);
    }

    @Override
    public void onLoadOneFinished(String path, Object obj, boolean isAllFinished) {
        int index = mPathList.indexOf(path);
        if (gridView.getTag() == null || !(boolean) gridView.getTag()) {
            //gridview没有滑动
            if (index % 5 == 0 || isAllFinished) {
                ((BaseAdapter) (gridView.getAdapter())).notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//        mCheckBoxList = adapter.getmCheckBoxList();
//        mCheckBoxList.set(position, !mCheckBoxList.get(position));
        //        MainActivity activity = (MainActivity) getApplicationContext();
        //        if (mCheckBoxList.get(position)) {
        //            //checked--->add to sendfile-list
        //        } else {//unchecked--->remove from sendfile-list
        //        }
        adapter.notifyDataSetChanged();
    }

    public class AppReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String packageName = intent.getData().getSchemeSpecificPart();
            if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
                //卸载
                Log.i(TAG, "uninstalled a app--->" + packageName);
                int index = mPackageList.indexOf(packageName);
                String appName = "";
                if (index >= 0) {
                    appName = mNameList.get(index);
//                    boolean isChecked = mCheckBoxList.get(index);
                    String path = mPathList.get(index);
                    PackageInfo info = apps.get(index);
                    apps.remove(index);
                    packageInfoList.remove(info);
                    mNameList.remove(index);
                    mPathList.remove(index);
                    mPackageList.remove(index);
//                    mCheckBoxList.remove(index);
//                    if (isChecked) {
//                        //                        MainActivity activity = (MainActivity) getApplicationContext();
//                        //                        activity.removeFileFromSendFileList(path);
//                    }
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                }
                ToastUtils.toast(MainActivity.this, appName + " " + getResources().getString(R.string.package_uninstalled));
            } else if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {//新增
                Log.i(TAG, "installed a app------->" + packageName);
            }
        }
    }
}
