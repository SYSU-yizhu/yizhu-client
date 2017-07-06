package com.sysu.yizhu.Activity.Business.AskQuestion;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.sysu.yizhu.R;
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
 * Created by QianZixuan on 2017/4/30.
 * Description: 提问fragment
 */
public class AskQuestionFragment extends Fragment{
    private static final String getAllQuestionIdUrl = "http://112.74.165.37:8080/question/getAllId";
    private static final String getQusetionByIdUrl = "http://112.74.165.37:8080/question/digest/";

    private PtrFrameLayout mPtrFrame;
    private FloatingActionButton addQuestion;
    private ListView question_list;

    private SimpleAdapter adapter;

    private List<Map<String, String>> list = new ArrayList<Map<String, String>>();

    private int count;
    private int list_count;
    private List<String> data = new ArrayList<String>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.ask_question_layout, container, false);

        mPtrFrame = (PtrFrameLayout) view.findViewById(R.id.ask_question_ptr_frame);
        addQuestion = (FloatingActionButton) view.findViewById(R.id.addQuestion);
        question_list = (ListView) view.findViewById(R.id.question_list);

        final PtrClassicDefaultHeader  header = new PtrClassicDefaultHeader(getActivity().getApplicationContext());
        header.setPadding(0, PtrLocalDisplay.dp2px(15), 0, 0);

        mPtrFrame.setHeaderView(header);
        mPtrFrame.addPtrUIHandler(header);

        mPtrFrame.setPtrHandler(new PtrHandler() {
            @Override
            public void onRefreshBegin(PtrFrameLayout frame) {//下拉刷新
                getData();
                mPtrFrame.refreshComplete();
            }

            @Override
            public boolean checkCanDoRefresh(PtrFrameLayout frame, View content, View header) {
                return PtrDefaultHandler.checkContentCanBePulledDown(frame, content, header);
            }
        });

        addQuestion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() != null) {
                    Intent intent = new Intent();
                    intent.setClass(getActivity(), AddQuestionActivity.class);
                    getActivity().startActivity(intent);
                }
            }
        });

        adapter = new SimpleAdapter(getActivity(), list, R.layout.question_item_layout,
                new String[] {"userName", "title", "createDate"},
                new int[] {R.id.questionItemUserName, R.id.questionItemTitle, R.id.questionItemCreateDate});
        question_list.setAdapter(adapter);

        question_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (getActivity() != null) {
                    Intent intent = new Intent();
                    intent.setClass(getActivity(), QuestionDetailActivity.class);
                    intent.putExtra("questionId", list.get(position).get("questionId")); //传送questionId
                    getActivity().startActivity(intent);
                }
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        getData();
    }

    private List<Map<String, String>> getData() {
        data.clear(); //清空问题Id
        list.clear(); //清空Adapter中的数据
        count = 0;
        list_count = 0;

        HttpUtil.get(getAllQuestionIdUrl, new HttpUtil.HttpResponseCallBack() { //获得所有问题id
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
                                Map<String, String> map = new HashMap<String, String>();
                                list.add(map);
                            }
                            for (int i = 0; i < count; i++) {
                                HttpUtil.get(getQusetionByIdUrl + data.get(i), new HttpUtil.HttpResponseCallBack() { //根据id获取问题
                                    @Override
                                    public void onSuccess(int code, String result) {
                                        switch (code) {
                                            case 200:
                                                JSONObject object = null;
                                                try {
                                                    object = new JSONObject(result);

                                                    Map<String, String> map = new HashMap<String, String>();
                                                    map.put("questionId", object.optString("questionId"));
                                                    map.put("userId", object.optString("userId"));
                                                    map.put("userName", object.optString("userName"));
                                                    map.put("title", object.optString("title"));
                                                    map.put("createDate", object.optString("createDate"));
                                                    list.set((count - 1) - data.indexOf(object.optString("questionId")) ,map);
                                                    list_count++;
                                                    if (list_count == count) {
                                                        Collections.sort(list, new MyComparator()); //排序

                                                        adapter.notifyDataSetChanged(); //更新adapter数据
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
            return Integer.valueOf(map2.get("questionId")) - Integer.valueOf(map1.get("questionId"));
        }
    }
}
