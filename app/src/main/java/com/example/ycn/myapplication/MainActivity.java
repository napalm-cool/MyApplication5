package com.example.ycn.myapplication;

import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Iterator;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

      /*  FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/
        initView();         //收集主界面控件信息
        refreshUI(false);  //刷新主界面

        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // 判断GPS是否正常启动
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "请开启GPS导航...", Toast.LENGTH_SHORT).show();
            // 返回开启GPS导航设置界面
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(intent, 0);
            return;
        }

        // 为获取地理位置信息时设置查询条件
        String bestProvider = lm.getBestProvider(getCriteria(), true);
        // 获取位置信息
        // 如果不设置查询要求，getLastKnownLocation方法传入的参数为LocationManager.GPS_PROVIDER
        Location location = lm.getLastKnownLocation(bestProvider);
        updateView(location);
        // 监听状态
        lm.addGpsStatusListener(listener);
        // 绑定监听，有4个参数
        // 参数1，设备：有GPS_PROVIDER和NETWORK_PROVIDER两种
        // 参数2，位置信息更新周期，单位毫秒
        // 参数3，位置变化最小距离：当位置距离变化超过此值时，将更新位置信息
        // 参数4，监听
        // 备注：参数2和3，如果参数3不为0，则以参数3为准；参数3为0，则通过时间来定时更新；两者为0，则随时刷新

        // 1秒更新一次，或最小位移变化超过1米更新一次；
        // 注意：此处更新准确度非常低，推荐在service里面启动一个Thread，在run中sleep(10000);然后执行handler.sendMessage(),更新位置
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, locationListener);
        ////////////////////////////////////////////////////////////////////////////////////////
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private EditText editText;
    private LocationManager lm;
    private static final String TAG = "GpsActivity";

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        lm.removeUpdates(locationListener);
    }


    // 位置监听
    private LocationListener locationListener = new LocationListener() {

        //位置信息变化时触发
        public void onLocationChanged(Location location) {
            updateView(location);//去显示，同时send;
            Log.i(TAG, "时间：" + location.getTime());
            Log.i(TAG, "经度：" + location.getLongitude());
            Log.i(TAG, "纬度：" + location.getLatitude());
            Log.i(TAG, "海拔：" + location.getAltitude());
        }

        // GPS状态变化时触发
        public void onStatusChanged(String provider, int status, Bundle extras) {
            switch (status) {
                // GPS状态为可见时
                case LocationProvider.AVAILABLE:
                    Log.i(TAG, "当前GPS状态为可见状态");
                    break;
                // GPS状态为服务区外时
                case LocationProvider.OUT_OF_SERVICE:
                    Log.i(TAG, "当前GPS状态为服务区外状态");
                    break;
                // GPS状态为暂停服务时
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    Log.i(TAG, "当前GPS状态为暂停服务状态");
                    break;
            }
        }

        //GPS开启时触发
        public void onProviderEnabled(String provider) {
            Location location = lm.getLastKnownLocation(provider);
            updateView(location);
        }

        //GPS禁用时触发
        public void onProviderDisabled(String provider) {
            updateView(null);
        }

    };

    // 状态监听
    GpsStatus.Listener listener = new GpsStatus.Listener() {
        public void onGpsStatusChanged(int event) {
            switch (event) {
                // 第一次定位
                case GpsStatus.GPS_EVENT_FIRST_FIX:
                    Log.i(TAG, "第一次定位");
                    break;
                // 卫星状态改变
                case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                    Log.i(TAG, "卫星状态改变");
                    // 获取当前状态
                    GpsStatus gpsStatus = lm.getGpsStatus(null);
                    // 获取卫星颗数的默认最大值
                    int maxSatellites = gpsStatus.getMaxSatellites();
                    // 创建一个迭代器保存所有卫星
                    Iterator<GpsSatellite> iters = gpsStatus.getSatellites()
                            .iterator();
                    int count = 0;
                    while (iters.hasNext() && count <= maxSatellites) {
                        GpsSatellite s = iters.next();
                        count++;
                    }
                    System.out.println("搜索到：" + count + "颗卫星");
                    break;
                // 定位启动
                case GpsStatus.GPS_EVENT_STARTED:
                    Log.i(TAG, "定位启动");
                    break;
                // 定位结束
                case GpsStatus.GPS_EVENT_STOPPED:
                    Log.i(TAG, "定位结束");
                    break;
            }
        };
    };
    public String  gpssend;
    //实时更新文本内容
    private void updateView(Location location) {
        if (location != null) {
/*            editText.setText("设备位置信息\n\n经度：");
            editText.append(String.valueOf(location.getLongitude()));
            editText.append("\n纬度：");
            editText.append(String.valueOf(location.getLatitude()));*/
            //send $CARXX,主机号，主机时间，经度，纬度
            gpssend = "$CARX,1,"+String.valueOf(location.getLatitude())+","+String.valueOf(location.getLongitude())+","+String.valueOf(location.getTime());
            editText.setText(gpssend);
            if(tcpClient.isConnected()) {
                new Thread() {
                    @Override
                    public void run() {
                        // 在这里进行 http request.网络请求相关操作
                        Log.e("gpssend", gpssend);
                        tcpClient.getTransceiver().sendMSG(gpssend);
                    }
                }.start();
            }
        } else {
            // 清空EditText对象
            editText.getEditableText().clear();
        }
    }

    //返回查询条件
    private Criteria getCriteria() {
        Criteria criteria = new Criteria();
        // 设置定位精确度 Criteria.ACCURACY_COARSE比较粗略，Criteria.ACCURACY_FINE则比较精细
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        // 设置是否要求速度
        criteria.setSpeedRequired(false);
        // 设置是否允许运营商收费
        criteria.setCostAllowed(false);
        // 设置是否需要方位信息
        criteria.setBearingRequired(false);
        // 设置是否需要海拔信息
        criteria.setAltitudeRequired(false);
        // 设置对电源的需求
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        return criteria;
    }
/////////////////////////////////////收发信息部份////////////////////////////////////////////
    private EditText editIP;
    private EditText editMsg;
    private Button btnLink;
    private Button btnSend;
    private TextView textReceive;

    private Handler mHandler = new Handler(Looper.getMainLooper());
    private TcpClient tcpClient = new TcpClient() {
        @Override
        public void onConnect() {
            refreshUI(true);
        }

        @Override
        public void onConnectBreak() {
            Log.e("6","Break");
            refreshUI(false);
            tcpClient.disConnected();
         }

        @Override
        public void onReceive(final String s) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    textReceive.append("server:" + s + "\n\n");
                    if (s.equals("57")){
                        context=MainActivity.this;
                        Toast.makeText(context,"优先放行，警慎驾驶！",Toast.LENGTH_LONG).show();
                        playSoundPool(R.raw.fx);
                    }
                    if (s.equals("56")){
                        context=MainActivity.this;
                        Toast.makeText(context,"已进入引导区！",Toast.LENGTH_LONG).show();
                        playSoundPool(R.raw.jr);
                    }
                }
            });
        }

        @Override
        public void onConnectFalied() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "连接失败",Toast.LENGTH_LONG).show();
                }
            });
        }
        //发送成功的回调
        @Override
        public void onSendSuccess(final String s) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    textReceive.setText("");
                    textReceive.append("我:" + s + "\n\n"
                    );
                }
            });
        }


    };

    private void refreshUI(final boolean isConnected) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                editIP.setEnabled(!isConnected);
               // editPort.setEnabled(!isConnected);
                btnLink.setText("");
                btnLink.setText(isConnected  ? "断开" : "连接");
              //  btnSend.setBackgroundResource(isConnected ?
             //           R.drawable.send_button : R.drawable.send_button_disabled );
            }
        });
    }

/*    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        refreshUI(false);
    }*/
    Handler handler=new Handler();

    Runnable runnable=new Runnable(){
        @Override
        public void run() {
            // TODO Auto-generated method stub
            Log.e("isCon", String.valueOf( tcpClient.isConnected()));
            if(!tcpClient.isConnected()) { //如果没有连接则再次连接
                myCon();//连接
            }
           //要做的事情，这里再次调用此Runnable对象，以实现每两秒实现一次的定时器操作
            handler.postDelayed(this, 5000);
        }
    };

    public void onbutton(View v){
        context=MainActivity.this;
        Toast.makeText(context,"5秒检查",Toast.LENGTH_LONG).show();
        handler.postDelayed(runnable, 5000);
    }

    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_Link:
                myCon();
                break;
            case R.id.btn_sendMsg:
                    //editMsg.setText("");
                if(tcpClient.isConnected()){
                    new Thread() {
                        @Override
                        public void run() {
                            // 在这里进行 http request.网络请求相关操作
                            String msg = editMsg.getText().toString();
                            tcpClient.getTransceiver().sendMSG(msg);
                        }
                    }.start();
                }
                break;
            default:
                break;
        }
    }

    private void myCon(){
        if(tcpClient.isConnected()){
            tcpClient.disConnected();
        }else {
            String host = editIP.getText().toString().trim();
            tcpClient.connect(host);
        }

    }

    @Override
    protected void onStop() {
        tcpClient.disConnected();
        super.onStop();
    }

    private void initView() {
        setContentView(R.layout.activity_main);
        editText = (EditText) findViewById(R.id.editText);
        editIP = (EditText)findViewById(R.id.ed_ip);
        editMsg = (EditText)findViewById(R.id.edit_msg);
        btnLink = (Button)findViewById(R.id.btn_Link);
        btnSend = (Button)findViewById(R.id.btn_sendMsg);
        textReceive = (TextView)findViewById(R.id.tx_receive);
         //listView = (ListView)findViewById(R.id.listView);
        //btnSend.setOnClickListener(this);
        //btnLink.setOnClickListener(this);
    }
    ////////////////////////////////声音部份///////////////////////////////////////////////////
    private static Context context;
    private static SoundPool sp;;
    private static int soundId;

    public static void initSoundPool(int resId) { // 初始化声音池
        sp = new SoundPool(
                3, // maxStreams参数，该参数为设置同时能够播放多少音效
                AudioManager.STREAM_MUSIC, // streamType参数，该参数设置音频类型，在游戏中通常设置为：STREAM_MUSIC
                1 // srcQuality参数，该参数设置音频文件的质量，目前还没有效果，设置为0为默认值。
        );
        soundId = sp.load(context, resId, 1);
    }

    public static void playSound() { // 播放声音,参数sound是播放音效的id，参数number是播放音效的次数
        sp.play(
                soundId, // 播放的音乐id
                1, // 左声道音量
                1, // 右声道音量
                1, // 优先级，0为最低
                0, // 循环次数，0不循环，-1永远循环
                1 // 回放速度 ，该值在0.5-2.0之间，1为正常速度
        );
    }

    public static void stopSound() { // 播放声音,参数sound是播放音效的id，参数number是播放音效的次数
        if (sp != null && soundId > 0) {
            sp.stop(soundId);
            sp.release();
        }
    }

    public void playSoundPool(int resId)
    {
        initSoundPool(resId);

        sp.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId,
                                       int status) {
                playSound();
            }
        });
    }





}
