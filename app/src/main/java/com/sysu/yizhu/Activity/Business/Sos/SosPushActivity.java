package com.sysu.yizhu.Activity.Business.Sos;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.sysu.yizhu.R;
import com.sysu.yizhu.Util.AppManager;
import com.sysu.yizhu.Util.HttpUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by QianZixuan on 2017/7/1.
 */
public class SosPushActivity extends AppCompatActivity {
    private static final String sosPushResponseUrl = "http://112.74.165.37:8080/sos/response/"; //+sosId
    private static final String sosFinishUrl = "http://112.74.165.37:8080/sos/finish";

    private TextView sos_push_response_count;
    private ListView sos_push_response_list;
    private Button sos_push_finish;

    private SimpleAdapter adapter;

    private List<Map<String, String>> list = new ArrayList<Map<String, String>>();

    private Timer timer = new Timer();

    private String sosId;

    private int count;
    private String [] data;

    TimerTask task = new TimerTask() {
        @Override
        public void run() {
            getSosPushResponse();
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        task.cancel();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sos_push_layout);

        sos_push_response_count = (TextView) findViewById(R.id.sos_push_response_count);
        sos_push_response_list = (ListView) findViewById(R.id.sos_push_response_list);
        sos_push_finish = (Button) findViewById(R.id.sos_push_finish);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        sosId = bundle.getString("sosId");

        timer.schedule(task, 0, 5000);

        adapter = new SimpleAdapter(SosPushActivity.this, list, R.layout.sos_push_response_list_item_layout,
                new String[] {"userId"},
                new int[] {R.id.sos_push_response_list_userId});
        sos_push_response_list.setAdapter(adapter);

        sos_push_finish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sosFinish();
            }
        });

        AppManager.getAppManager().addActivity(SosPushActivity.this);
    }

    private List<Map<String, String>> getSosPushResponse() {
        HttpUtil.get(sosPushResponseUrl + sosId, new HttpUtil.HttpResponseCallBack() {
            @Override
            public void onSuccess(int code, String result) {
                switch (code) {
                    case 200:
                        JSONObject object = null;
                        count = 0;
                        data = new String[0];
                        list.clear();
                        try {
                            object = new JSONObject(result);
                            count = object.optInt("count");
                            JSONArray jsonArray = object.optJSONArray("data");
                            List<String> arrayList = new ArrayList<String>();
                            for (int i = 0; i < jsonArray.length(); i++) {
                                arrayList.add( jsonArray.getString(i) );
                            }
                            data = arrayList.toArray(new String[arrayList.size()]);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        } finally {
                            sos_push_response_count.setText(count + "");
                            for (int i = 0; i < data.length; i++) {
                                Map<String, String> map = new HashMap<String, String>();
                                map.put("userId", data[i]);
                                list.add(map);
                            }
                            adapter.notifyDataSetChanged(); //更新adapter数据
                        }
                        break;
                    case 404:
                        Toast.makeText(SosPushActivity.this, "Sos Id不存在或已完成", Toast.LENGTH_SHORT).show();
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

    private void sosFinish() {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("sosId", sosId);
        HttpUtil.post(sosFinishUrl, params, new HttpUtil.HttpResponseCallBack() {
            @Override
            public void onSuccess(int code, String result) {
                switch (code) {
                    case 200:
                        Toast.makeText(SosPushActivity.this, "结束求救成功", Toast.LENGTH_SHORT).show();
                        AppManager.getAppManager().finishActivity();
                        break;
                    case 401:
                        Toast.makeText(SosPushActivity.this, "未登录", Toast.LENGTH_SHORT).show();
                        break;
                    case 404:
                        Toast.makeText(SosPushActivity.this, "Sos Id不存在或已完成", Toast.LENGTH_SHORT).show();
                        break;
                    case 450:
                        Toast.makeText(SosPushActivity.this, "非发起用户无法结束该求救", Toast.LENGTH_SHORT).show();
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
