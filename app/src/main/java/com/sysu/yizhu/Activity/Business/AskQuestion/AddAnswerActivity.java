package com.sysu.yizhu.Activity.Business.AskQuestion;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.sysu.yizhu.R;
import com.sysu.yizhu.Util.AppManager;
import com.sysu.yizhu.Util.HttpUtil;

import java.util.HashMap;

/**
 * Created by QianZixuan on 2017/6/27.
 */
public class AddAnswerActivity extends AppCompatActivity {
    private static final String AnswerUrl = "http://112.74.165.37:8080/question/answer";
    private String questionId;

    private EditText add_answer_content;
    private Button add_answer_submit;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_answer_layout);
        AppManager.getAppManager().addActivity(AddAnswerActivity.this);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        questionId = bundle.getString("questionId");

        add_answer_content = (EditText) findViewById(R.id.add_answer_content);
        add_answer_submit = (Button) findViewById(R.id.add_answer_submit);

        add_answer_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(add_answer_content.getText())) {
                    Toast.makeText(AddAnswerActivity.this, "请填写回答内容", Toast.LENGTH_SHORT).show();
                } else {
                    HashMap<String, String> params = new HashMap<String, String>();
                    params.put("questionId", questionId);
                    params.put("content", add_answer_content.getText().toString());
                    HttpUtil.post(AnswerUrl, params, new HttpUtil.HttpResponseCallBack() {
                        @Override
                        public void onSuccess(int code, String result) {
                            switch (code) {
                                case 200:
                                    Toast.makeText(AddAnswerActivity.this, "回答成功！", Toast.LENGTH_SHORT).show();
                                    AppManager.getAppManager().finishActivity();
                                    break;
                                case 401:
                                    Toast.makeText(AddAnswerActivity.this, "未登录！", Toast.LENGTH_SHORT).show();
                                    break;
                                case 450:
                                    Toast.makeText(AddAnswerActivity.this, "该提问id不存在！", Toast.LENGTH_SHORT).show();
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
        });
    }
}
