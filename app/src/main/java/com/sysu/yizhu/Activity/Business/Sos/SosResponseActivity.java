package com.sysu.yizhu.Activity.Business.Sos;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.sysu.yizhu.R;
import com.sysu.yizhu.Util.AppManager;
import com.sysu.yizhu.Util.HttpUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import in.srain.cube.views.ptr.PtrClassicDefaultHeader;
import in.srain.cube.views.ptr.PtrDefaultHandler;
import in.srain.cube.views.ptr.PtrFrameLayout;
import in.srain.cube.views.ptr.PtrHandler;
import in.srain.cube.views.ptr.util.PtrLocalDisplay;

/**
 * Created by QianZixuan on 2017/7/1.
 */
public class SosResponseActivity extends AppCompatActivity {
    private static final String getAllValidSosIdUrl = "http://112.74.165.37:8080/sos/allValidId";
    private static final String getSosByIdUrl = "http://112.74.165.37:8080/sos/get/"; // +sosId

    private SimpleAdapter adapter;

    private List<Map<String, String>> list = new ArrayList<Map<String, String>>();

    private PtrFrameLayout mPtrFrame;
    private ListView sos_response_push_list;

    private int count;
    private int list_count;
    private List<String> data = new ArrayList<String>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sos_response_layout);

        mPtrFrame = (PtrFrameLayout) findViewById(R.id.sos_response_ptr_frame);
        sos_response_push_list = (ListView) findViewById(R.id.sos_response_push_list);

        final PtrClassicDefaultHeader header = new PtrClassicDefaultHeader(getApplicationContext());
        header.setPadding(0, PtrLocalDisplay.dp2px(15), 0, 0);

        mPtrFrame.setHeaderView(header);
        mPtrFrame.addPtrUIHandler(header);

        mPtrFrame.setPtrHandler(new PtrHandler() {
            @Override
            public void onRefreshBegin(PtrFrameLayout frame) {
                getData();
                mPtrFrame.refreshComplete();
            }

            @Override
            public boolean checkCanDoRefresh(PtrFrameLayout frame, View content, View header) {
                return PtrDefaultHandler.checkContentCanBePulledDown(frame, content, header);
            }
        });

        adapter = new SimpleAdapter(SosResponseActivity.this, list, R.layout.sos_response_push_list_item_layout,
                new String[] {"createTime", "state", "pushUserId"},
                new int[] {R.id.sos_response_push_list_createTime, R.id.sos_response_push_list_finished, R.id.sos_response_push_list_pushUserId});
        sos_response_push_list.setAdapter(adapter);

        sos_response_push_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent();
                intent.setClass(SosResponseActivity.this, SosResponseDetailActivity.class);
                intent.putExtra("sosId", list.get(position).get("sosId")); //传送questionId
                SosResponseActivity.this.startActivity(intent);
            }
        });

        AppManager.getAppManager().addActivity(SosResponseActivity.this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        getData();
    }

    private List<Map<String, String>> getData() {
        data.clear();
        list.clear(); //清空Adapter中的数据
        count = 0;
        list_count = 0;

        HttpUtil.get(getAllValidSosIdUrl, new HttpUtil.HttpResponseCallBack() { //获得所有id
            @Override
            public void onSuccess(int code, String result) {
                switch (code) {
                    case 200:
                        JSONObject object = null;
                        try {
                            object = new JSONObject(result);
                            count = object.optInt("count");
                            JSONArray jsonArray = object.optJSONArray("data");
                            for (int i = 0; i < jsonArray.length(); i++) {
                                data.add( jsonArray.getString(i) );
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        } finally {
                            for (int i = 0; i < count; i++) {
                                Map<String, String> map;map = new HashMap<String, String>();
                                list.add(map);
                            }
                            for (int i = 0; i < count; i++) {
                                HttpUtil.get(getSosByIdUrl + data.get(i), new HttpUtil.HttpResponseCallBack() { //根据id获取
                                    @Override
                                    public void onSuccess(int code, String result) {
                                        switch (code) {
                                            case 200:
                                                JSONObject object = null;
                                                try {
                                                    object = new JSONObject(result);

                                                    Map<String, String> map;map = new HashMap<String, String>();
                                                    map.put("sosId", object.optString("sosId"));
                                                    map.put("latitude", object.optString("latitude"));
                                                    map.put("longitude", object.optString("longitude"));
                                                    map.put("createTime", object.optString("createTime"));
                                                    map.put("pushUserId", object.optString("pushUserId"));
                                                    if (object.optBoolean("finished")) {
                                                        map.put("state", "求救结束");
                                                    } else {
                                                        map.put("state", "正在求救");
                                                    }
                                                    list.set((count - 1) - data.indexOf(object.optString("sosId")), map);
                                                    list_count++;
                                                    if (list_count == count) {
                                                        Collections.sort(list, new MyComparator()); //排序

                                                        adapter.notifyDataSetChanged();
                                                    }
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
        return list;
    }

    class MyComparator implements Comparator
    {
        //这里的o1和o2就是list里任意的两个对象，然后按需求把这个方法填完整就行了
        public int compare(Object o1, Object o2)
        {
            //比较规则
            Map<String, String> map1 = (Map<String, String>) o1;
            Map<String, String> map2 = (Map<String, String>) o2;
            return Integer.valueOf(map2.get("sosId")) - Integer.valueOf(map1.get("sosId"));
        }
    }
}
