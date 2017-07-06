package com.sysu.yizhu.Activity.Business.AskHelp;

import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.TextureMapView;
import com.baidu.mapapi.model.LatLng;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.sysu.yizhu.R;
import com.sysu.yizhu.UserData;
import com.sysu.yizhu.Util.HttpUtil;
import com.sysu.yizhu.Util.SerializableJson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * Created by QianZixuan on 2017/4/30.
 * Description: 求助fragment
 */
public class AskHelpFragment extends Fragment {
    private TextureMapView mMapView;
    private BaiduMap mBaiduMap;
    private LocationClient locClient;
    private LocationClientOption locClientOpt;
    private AskHelpFragment.MyLocationListener mListener = new AskHelpFragment.MyLocationListener();
    private LatLng nowLocation;

    boolean isRequest; //手动请求
    boolean isFirstLoc; //初次定位

    private Button ask_help_locate;
    private FloatingActionButton refreshHelpBtn;
    private FloatingActionButton pushBtn;

    private static final String SERVER_HOST = "http://112.74.165.37:8080";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (getActivity() != null)
            SDKInitializer.initialize(getActivity().getApplicationContext());

        View view = inflater.inflate(R.layout.ask_help_layout, container, false);
        //获取地图控件引用
        mMapView = (TextureMapView) view.findViewById(R.id.ask_help_bmapView);
        ask_help_locate = (Button) view.findViewById(R.id.ask_help_locate);

        isRequest = false;
        isFirstLoc = true;

        mMapView.showScaleControl(false);
        mMapView.showZoomControls(false);

        mBaiduMap = mMapView.getMap();
        mBaiduMap.setMyLocationEnabled(true);
        BitmapDescriptor bitmapDesc = BitmapDescriptorFactory.fromResource(R.drawable.location);
        mBaiduMap.setMyLocationConfigeration(new MyLocationConfiguration(MyLocationConfiguration.LocationMode.NORMAL, true, bitmapDesc));

        initBMapMarkerClickListener(mBaiduMap, getActivity());
        findMyLocation();
        refreshAskHelps();

        ask_help_locate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isRequest = true;
                findMyLocation();
            }
        });

        refreshHelpBtn = (FloatingActionButton) view.findViewById(R.id.ask_help_refresh_btn);
        refreshHelpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refreshAskHelps();
            }
        });

        pushBtn = (FloatingActionButton) view.findViewById(R.id.ask_help_push_btn);
        pushBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                makePushDialog();
            }
        });

        return  view;
    }

    // 接口方法，更新定位
    private void updateLocation() {
        String url = SERVER_HOST + "/user/updateLocation";
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("latitude", String.valueOf(nowLocation.latitude));
        params.put("longitude", String.valueOf(nowLocation.longitude));
        HttpUtil.post(url, params, new HttpUtil.HttpResponseCallBack() {
            @Override
            public void onSuccess(int code, String result) {
                switch (code) {
                    case 200:
                        /*if (getActivity() != null)
                            Toast.makeText(getActivity(), "更新定位成功！", Toast.LENGTH_SHORT).show();*/
                        break;
                    case 401:
                        if (getActivity() != null)
                            Toast.makeText(getActivity(), "未登录！", Toast.LENGTH_SHORT).show();
                        break;
                    case 403:
                        if (getActivity() != null)
                            Toast.makeText(getActivity(), "定位失败！", Toast.LENGTH_SHORT).show();
                        break;
                    case 450:
                        if (getActivity() != null)
                            Toast.makeText(getActivity(), "未记录ObjectId！", Toast.LENGTH_SHORT).show();
                        break;
                    case 500:
                        if (getActivity() != null)
                            Toast.makeText(getActivity(), "服务器错误！", Toast.LENGTH_SHORT).show();
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

    // 接口方法，发起求助
    private void pushAskHelp(LatLng location, String title, String detail, String needs) {
        String url = SERVER_HOST + "/help/push";
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("latitude", String.valueOf(location.latitude));
        params.put("longitude", String.valueOf(location.longitude));
        params.put("title", title);
        params.put("detail", detail);
        params.put("needs", needs);
        HttpUtil.post(url, params, new HttpUtil.HttpResponseCallBack() {
            @Override
            public void onSuccess(int code, String result) {
                switch (code) {
                    case 200:
                        if (getActivity() != null)
                            Toast.makeText(getActivity(), "求助成功！", Toast.LENGTH_SHORT).show();
                        break;
                    case 401:
                        if (getActivity() != null)
                            Toast.makeText(getActivity(), "未登录！", Toast.LENGTH_SHORT).show();
                        break;
                    case 402:
                        if (getActivity() != null)
                            Toast.makeText(getActivity(), "需求人数无效！应在1-10人间", Toast.LENGTH_SHORT).show();
                        break;
                    case 403:
                        if (getActivity() != null)
                            Toast.makeText(getActivity(), "定位失败！", Toast.LENGTH_SHORT).show();
                        break;
                    case 450:
                        if (getActivity() != null)
                            Toast.makeText(getActivity(), "未记录ObjectId！", Toast.LENGTH_SHORT).show();
                        break;
                    case 500:
                        if (getActivity() != null)
                            Toast.makeText(getActivity(), "服务器错误！", Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        break;
                }
                refreshAskHelps();
            }

            @Override
            public void onFailure(String result, Exception e) {

            }
        });

    }

    // 接口方法，响应求助
    private void responseHelp(String helpId) {
        String url = SERVER_HOST + "/help/response";
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("helpId", helpId);
        HttpUtil.post(url, params, new HttpUtil.HttpResponseCallBack() {
            @Override
            public void onSuccess(int code, String result) {
                switch (code) {
                    case 200:
                        if (getActivity() != null)
                            Toast.makeText(getActivity(), "成功！快去目的地帮助他人吧！", Toast.LENGTH_SHORT).show();
                        break;
                    case 401:
                        if (getActivity() != null)
                            Toast.makeText(getActivity(), "未登录！", Toast.LENGTH_SHORT).show();
                        break;
                    case 402:
                        if (getActivity() != null)
                            Toast.makeText(getActivity(), "该求助人数已满！", Toast.LENGTH_SHORT).show();
                        break;
                    case 403:
                        if (getActivity() != null)
                            Toast.makeText(getActivity(), "已响应，不能重复响应！", Toast.LENGTH_SHORT).show();
                        break;
                    case 404:
                        if (getActivity() != null)
                            Toast.makeText(getActivity(), "该求助已结束！", Toast.LENGTH_SHORT).show();
                        break;
                    case 450:
                        if (getActivity() != null)
                            Toast.makeText(getActivity(), "未记录ObjectId！", Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        break;
                }
                refreshAskHelps();
            }

            @Override
            public void onFailure(String result, Exception e) {

            }
        });
    }

    // 接口方法，结束求助
    private void finishHelp(String helpId) {
        String url = SERVER_HOST + "/help/finish";
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("helpId", helpId);
        HttpUtil.post(url, params, new HttpUtil.HttpResponseCallBack() {
            @Override
            public void onSuccess(int code, String result) {
                switch (code) {
                    case 200:
                        if (getActivity() != null)
                            Toast.makeText(getActivity(), "成功！已结束。", Toast.LENGTH_SHORT).show();
                        break;
                    case 401:
                        if (getActivity() != null)
                            Toast.makeText(getActivity(), "未登录！", Toast.LENGTH_SHORT).show();
                        break;
                    case 404:
                        if (getActivity() != null)
                            Toast.makeText(getActivity(), "该求助已结束！", Toast.LENGTH_SHORT).show();
                        break;
                    case 450:
                        if (getActivity() != null)
                            Toast.makeText(getActivity(), "未记录ObjectId！", Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        break;
                }
                refreshAskHelps();
            }

            @Override
            public void onFailure(String result, Exception e) {

            }
        });
    }

    private void makePushDialog() {
        LayoutInflater factory = LayoutInflater.from(getActivity());
        View dialogView = factory.inflate(R.layout.ask_help_push, null);

        final TextView titleTv = (TextView) dialogView.findViewById(R.id.ask_help_title_text_view);
        final TextView detailTv = (TextView) dialogView.findViewById(R.id.ask_help_detail_text_view);
        final TextView needsTv = (TextView) dialogView.findViewById(R.id.ask_help_needs_text_view);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(dialogView);

        builder.setPositiveButton("求助", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                pushAskHelp(nowLocation, titleTv.getText().toString(), detailTv.getText().toString(), needsTv.getText().toString());
            }
        });

        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.create();
        builder.show();
    }

    private void refreshAskHelps() {
        mBaiduMap.clear();
        String url = SERVER_HOST + "/help/allValidId";
        HttpUtil.get(url, new HttpUtil.HttpResponseCallBack() { //获得所有有效求助id
            @Override
            public void onSuccess(int code, String result) {
                switch (code) {
                    case 200:
                        JSONObject object = null;
                        int count = 0;
                        String[] data = new String[]{};
                        try {
                            object = new JSONObject(result);
                            System.out.println(object);
                            count = Integer.parseInt(object.optString("count"));
                            String strTemp = object.optString("data");
                            if (strTemp.length() > 2) {
                                strTemp = strTemp.substring(1, strTemp.length()-1);
                                data = strTemp.split(",");
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        } finally {
                            String helpContentUrl = SERVER_HOST + "/help/get/";
                            for (int i = 0; i < count; i++) {
                                HttpUtil.get(helpContentUrl + data[i], new HttpUtil.HttpResponseCallBack() { //根据id获取求助内容
                                    @Override
                                    public void onSuccess(int code, String result) {
                                        switch (code) {
                                            case 200:
                                                JSONObject object = null;
                                                try {
                                                    object = new JSONObject(result);
                                                    SerializableJson json = new SerializableJson();
                                                    json.put("helpId", object.optString("helpId"));
                                                    json.put("latitude", object.optString("latitude"));
                                                    json.put("longitude", object.optString("longitude"));
                                                    json.put("finished", object.optString("finished"));
                                                    json.put("title", object.optString("title"));
                                                    json.put("detail", object.optString("detail"));
                                                    json.put("needs", object.optString("needs"));
                                                    json.put("responseNum", object.optString("responseNum"));
                                                    json.put("pushUserId", object.optString("pushUserId"));
                                                    showAskHelp(json);
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }
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
                        }
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

    public void findMyLocation() {
        locClient = new LocationClient(this.getActivity());
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

    // 在Activity中初始化地图后即调用该方法
    private void initBMapMarkerClickListener(BaiduMap mBaiduMap, final Context context) {
        mBaiduMap.setOnMarkerClickListener(new BaiduMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(final Marker marker) {
                //获得marker中的数据
                final SerializableJson helpData = (SerializableJson) marker.getExtraInfo().get("helpData");
                if (helpData != null) {
                    LayoutInflater factory = LayoutInflater.from(context);
                    View dialogView = factory.inflate(R.layout.ask_help_detail, null);

                    ((TextView) dialogView.findViewById(R.id.ask_help_title_text_view)).setText(helpData.get("title"));
                    ((TextView) dialogView.findViewById(R.id.ask_help_detail_text_view)).setText(helpData.get("detail"));
                    ((TextView) dialogView.findViewById(R.id.ask_help_needs_text_view)).setText(helpData.get("needs"));
                    ((TextView) dialogView.findViewById(R.id.ask_help_response_num_text_view)).setText(helpData.get("responseNum"));

                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setView(dialogView);

                    final Boolean self = helpData.get("pushUserId").equals(UserData.getInstance().getUserId());
                    String positiveText = self ? "结束求助" : "前往求助";

                    builder.setPositiveButton(positiveText, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (self) {
                                finishHelp(helpData.get("helpId"));
                            } else {
                                responseHelp(helpData.get("helpId"));
                            }

                        }
                    });
                    builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

                    builder.create();
                    builder.show();
                }


                return true;
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mMapView.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mMapView.onPause();
    }

    private void showAskHelp(SerializableJson helpData) {
        LatLng latLng = new LatLng(Double.valueOf(helpData.get("latitude")), Double.valueOf(helpData.get("longitude")));
        // 图标
        BitmapDescriptor bitmap = BitmapDescriptorFactory.fromResource(R.drawable.locate_help_icon);

        OverlayOptions overlayOptions = new MarkerOptions().position(latLng)
                .icon(bitmap).zIndex(10);
        Marker marker = (Marker) (mBaiduMap.addOverlay(overlayOptions));

        Bundle bundle = new Bundle();
        bundle.putSerializable("helpData", helpData);
        marker.setExtraInfo(bundle);
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
            nowLocation = new LatLng(location.getLatitude(),location.getLongitude());
            MapStatusUpdate u = MapStatusUpdateFactory.newLatLngZoom(nowLocation, 18.0f);
            if (isFirstLoc || isRequest) {
                mBaiduMap.animateMapStatus(u);
                isRequest = false;
            }
            isFirstLoc = false;
            if (getActivity() != null) {
                updateLocation();
            }
        }

        @Override
        public void onConnectHotSpotMessage(String s, int i) {

        }
    }
}
