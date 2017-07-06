package com.sysu.yizhu.Activity.Business.AskQuestion;

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
 * Created by QianZixuan on 2017/6/7.
 * Description: 添加提问
 */
public class AddQuestionActivity extends AppCompatActivity {
    private static final String url = "http://112.74.165.37:8080/question/ask";

    private EditText title;
    private EditText content;
    private Button submit;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_question_layout);

        AppManager.getAppManager().addActivity(AddQuestionActivity.this);

        title = (EditText) findViewById(R.id.add_question_title);
        content = (EditText) findViewById(R.id.add_question_content);
        submit = (Button) findViewById(R.id.add_question_submit);

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(title.getText())) {
                    Toast.makeText(AddQuestionActivity.this, "请填写提问标题！", Toast.LENGTH_SHORT).show();
                } else if (TextUtils.isEmpty(content.getText())) {
                    Toast.makeText(AddQuestionActivity.this, "请填写提问内容！", Toast.LENGTH_SHORT).show();
                } else {
                    submit();
                }
            }
        });
    }

    private void submit() {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("title", title.getText().toString());
        params.put("content", content.getText().toString());
        HttpUtil.post(url, params, new HttpUtil.HttpResponseCallBack() {
            @Override
            public void onSuccess(int code, String result) {
                switch (code) {
                    case 200:
                        Toast.makeText(AddQuestionActivity.this, "提问成功！", Toast.LENGTH_SHORT).show();
                        AppManager.getAppManager().finishActivity();
                        break;
                    case 401:
                        Toast.makeText(AddQuestionActivity.this, "未登录！", Toast.LENGTH_SHORT).show();
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