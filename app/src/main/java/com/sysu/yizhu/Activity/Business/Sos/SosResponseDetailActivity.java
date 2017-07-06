package com.sysu.yizhu.Activity.Business.Sos;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.sysu.yizhu.R;
import com.sysu.yizhu.UserData;
import com.sysu.yizhu.Util.AppManager;
import com.sysu.yizhu.Util.HttpUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by QianZixuan on 2017/7/1.
 */
public class SosResponseDetailActivity extends AppCompatActivity {
    private static final String updateLocationUrl = "http://112.74.165.37:8080/user/updateLocation";
    private static final String getSosByIdUrl = "http://112.74.165.37:8080/sos/get/"; // +sosId
    private static final String sosResponseUrl = "http://112.74.165.37:8080/sos/response";
    private static final String sosFinishUrl = "http://112.74.165.37:8080/sos/finish";

    private String sosId = "";
    private double sos_latitude = 0;
    private double sos_longitude = 0;
    private String sos_createTime = "";
    private boolean finished = false;
    private String pushUserId = "";

    private boolean isSosPusher = false;
    boolean isRequest; //手动请求
    boolean isFirstLoc; //初次定位

    private double latitude;
    private double longitude;

    private Button sos_response_detail_button;
    private Button sos_response_detail_locate;

    private MapView mMapView;
    private BaiduMap mBaiduMap;
    private LocationClient locClient;
    private LocationClientOption locClientOpt;

    private MyLocationListener mListener = new MyLocationListener();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sos_response_detail_layout);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        sosId = bundle.getString("sosId");

        isRequest = false;
        isFirstLoc = true;

        mMapView = (MapView) findViewById(R.id.sos_response_detail_bmapView);
        mMapView.showScaleControl(false);
        mMapView.showZoomControls(false);
        mBaiduMap = mMapView.getMap();
        mBaiduMap.setMyLocationEnabled(true);
        BitmapDescriptor bitmapDesc = BitmapDescriptorFactory.fromResource(R.drawable.location);
        mBaiduMap.setMyLocationConfigeration(new MyLocationConfiguration(MyLocationConfiguration.LocationMode.NORMAL, true, bitmapDesc));

        getSosById();
        findMyLocation();

        sos_response_detail_locate = (Button) findViewById(R.id.sos_response_detail_locate);
        sos_response_detail_button = (Button) findViewById(R.id.sos_response_detail_button);

        sos_response_detail_locate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isRequest = true;
                findMyLocation();

                LatLng ll = new LatLng(latitude, longitude);
                MapStatusUpdate u = MapStatusUpdateFactory.newLatLngZoom(ll, 18.0f);
                mBaiduMap.animateMapStatus(u);
            }
        });

        sos_response_detail_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isSosPusher) {
                    sosFinish();
                } else {
                    mBaiduMap.clear();
                    LatLng ll = new LatLng(sos_latitude, sos_longitude);
                    // 图标
                    BitmapDescriptor bitmap = BitmapDescriptorFactory.fromResource(R.drawable.locate_help_icon);

                    OverlayOptions overlayOptions = new MarkerOptions().position(ll)
                            .icon(bitmap).zIndex(10);
                    mBaiduMap.addOverlay(overlayOptions);

                    MapStatusUpdate u = MapStatusUpdateFactory.newLatLngZoom(ll, 18.0f);
                    mBaiduMap.animateMapStatus(u);

                    sosResponse();
                }
            }
        });

        AppManager.getAppManager().addActivity(SosResponseDetailActivity.this);
    }

    public void findMyLocation() {
        locClient = new LocationClient(SosResponseDetailActivity.this);
        locClient.registerLocationListener(mListener);
        locClientOpt = new LocationClientOption();
        locClientOpt.setOpenGps(true);// 打开gps
        locClientOpt.setCoorType("bd09ll");//可选，默认gcj02，设置返回的定位结果坐标系，如果配合百度地图使用，建议设置为bd09ll;
        locClientOpt.setScanSpan(0);//可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
//      locClientOpt.setIsNeedAddress(true);//可选，设置是否需要地址信息，默认不需要
//      locClientOpt.setNeedDeviceDirect(false);//可选，设置是否需要设备方向结果
//      locClientOpt.setLocationNotify(false);//可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
//      locClientOpt.setIgnoreKillProcess(true);//可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
//      locClientOpt.SetIgnoreCacheException(false);//可选，默认false，设置是否收集CRASH信息，默认收集
        locClient.setLocOption(locClientOpt);
        locClient.start();
    }

    private class MyLocationListener implements BDLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            // map view 销毁后不在处理新接收的位置
            if (location == null || mMapView == null){
                return;
            }

            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    .direction(0)
                    .latitude(location.getLatitude())
                    .longitude(location.getLongitude())
                    .build();
            mBaiduMap.setMyLocationData(locData);
            if (isFirstLoc || isRequest) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();

                isRequest = false;
            }
            isFirstLoc = false;
            updateLocation();
        }

        @Override
        public void onConnectHotSpotMessage(String s, int i) {

        }
    }

    private void updateLocation() {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("latitude", Double.toString(latitude));
        params.put("longitude", Double.toString(longitude));
        HttpUtil.post(updateLocationUrl, params, new HttpUtil.HttpResponseCallBack() {
            @Override
            public void onSuccess(int code, String result) {
                switch (code) {
                    case 200:
                        /*Toast.makeText(SosResponseDetailActivity.this, "更新定位成功！", Toast.LENGTH_SHORT).show();*/
                        break;
                    case 401:
                        Toast.makeText(SosResponseDetailActivity.this, "未登录", Toast.LENGTH_SHORT).show();
                        break;
                    case 403:
                        Toast.makeText(SosResponseDetailActivity.this, "经纬度错误", Toast.LENGTH_SHORT).show();
                        break;
                    case 450:
                        Toast.makeText(SosResponseDetailActivity.this, "用户未记录安装Id", Toast.LENGTH_SHORT).show();
                        break;
                    case 500:
                        Toast.makeText(SosResponseDetailActivity.this, "服务器错误", Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onFailure(String result, Exception e) {

            }
        });
    }

    private void getSosById() {
        HttpUtil.get(getSosByIdUrl + sosId, new HttpUtil.HttpResponseCallBack() { //根据id获取
            @Override
            public void onSuccess(int code, String result) {
                switch (code) {
                    case 200:
                        JSONObject object = null;
                        try {
                            object = new JSONObject(result);

                            sos_latitude = object.optDouble("latitude");
                            sos_longitude = object.optDouble("longitude");
                            sos_createTime = object.optString("createTime");
                            finished = object.optBoolean("finished");
                            pushUserId = object.optString("pushUserId");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        if (pushUserId.equals(UserData.getInstance().getUserId())) {
                            sos_response_detail_button.setText("结束求救");
                            isSosPusher = true;
                        } else {
                            sos_response_detail_button.setText("前往救助");
                            isSosPusher = false;
                        }

                        LatLng ll = new LatLng(sos_latitude, sos_longitude);
                        // 图标
                        BitmapDescriptor bitmap = BitmapDescriptorFactory.fromResource(R.drawable.locate_help_icon);

                        OverlayOptions overlayOptions = new MarkerOptions().position(ll)
                                .icon(bitmap).zIndex(10);
                       mBaiduMap.addOverlay(overlayOptions);

                        MapStatusUpdate u = MapStatusUpdateFactory.newLatLngZoom(ll, 18.0f);
                        mBaiduMap.animateMapStatus(u);
                        break;
                    case 404:
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onFailure(String result, Exception e) {

            }
        });
    }

    private void sosResponse() {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("sosId", sosId);
        HttpUtil.post(sosResponseUrl, params, new HttpUtil.HttpResponseCallBack() {
            @Override
            public void onSuccess(int code, String result) {
                switch (code) {
                    case 200:
                        Toast.makeText(SosResponseDetailActivity.this, "响应求救成功", Toast.LENGTH_SHORT).show();
                        break;
                    case 401:
                        Toast.makeText(SosResponseDetailActivity.this, "未登录", Toast.LENGTH_SHORT).show();
                        break;
                    case 403:
                        Toast.makeText(SosResponseDetailActivity.this, "已响应求救", Toast.LENGTH_SHORT).show();
                        break;
                    case 404:
                        Toast.makeText(SosResponseDetailActivity.this, "Sos Id不存在或已完成", Toast.LENGTH_SHORT).show();
                        break;
                    case 450:
                        Toast.makeText(SosResponseDetailActivity.this, "用户未记录安装Id", Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onFailure(String result, Exception e) {

            }
        });
    }

    private void sosFinish() {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("sosId", sosId);
        HttpUtil.post(sosFinishUrl, params, new HttpUtil.HttpResponseCallBack() {
            @Override
            public void onSuccess(int code, String result) {
                switch (code) {
                    case 200:
                        Toast.makeText(SosResponseDetailActivity.this, "结束求救成功", Toast.LENGTH_SHORT).show();
                        AppManager.getAppManager().finishActivity();
                        break;
                    case 401:
                        Toast.makeText(SosResponseDetailActivity.this, "未登录", Toast.LENGTH_SHORT).show();
                        break;
                    case 404:
                        Toast.makeText(SosResponseDetailActivity.this, "Sos Id不存在或已完成", Toast.LENGTH_SHORT).show();
                        break;
                    case 450:
                        Toast.makeText(SosResponseDetailActivity.this, "非发起用户无法结束该求救", Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onFailure(String result, Exception e) {

            }
        });
    }
}
