package com.sysu.yizhu.Activity.Login;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.os.CountDownTimer;
import android.graphics.Color;

import com.sysu.yizhu.Activity.Business.MainActivity;
import com.sysu.yizhu.R;
import com.sysu.yizhu.UserData;
import com.sysu.yizhu.Util.AppManager;
import com.sysu.yizhu.Util.HttpUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by QianZixuan on 2017/4/19.
 * Description: 注册界面Activity
 */
public class SignUpActivity extends AppCompatActivity {
    private static final String getCodeUrl = "http://112.74.165.37:8080/user/sendSms/";
    private static final String signUpUrl = "http://112.74.165.37:8080/user/register";

    private EditText nameText;
    private TextView genderText;
    private TextView birthDateText;
    private EditText locationText;
    private EditText phoneNumText;
    private Button getCodeButton;
    private EditText codeText;
    private Button signUpButton;
    private EditText passwordText;
    private EditText confirmPasswordText;
    private RelativeLayout gender_holder;
    private RelativeLayout birthDate_holder;

    //存储当前年月日
    private int year;
    private int month;
    private int day;

    private int gender_choice;
    private String gender = "male";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sign_up_layout);

        AppManager.getAppManager().addActivity(SignUpActivity.this);

        nameText = (EditText) findViewById(R.id.sign_up_username_editText);
        genderText = (TextView) findViewById(R.id.sign_up_gender_textView);
        birthDateText = (TextView) findViewById(R.id.sign_up_birthDate_textView);
        locationText = (EditText) findViewById(R.id.sign_up_location_editText);
        phoneNumText = (EditText) findViewById(R.id.sign_up_phoneNum_editText);
        codeText = (EditText) findViewById(R.id.sign_up_code_editText);
        getCodeButton = (Button) findViewById(R.id.sign_up_getCode_button);
        passwordText = (EditText) findViewById(R.id.sign_up_password_editText);
        confirmPasswordText = (EditText) findViewById(R.id.sign_up_confirmPassword_editText);
        signUpButton = (Button) findViewById(R.id.sign_up_button);

        gender_holder = (RelativeLayout) findViewById(R.id.sign_up_gender_holder);
        birthDate_holder = (RelativeLayout) findViewById(R.id.sign_up_birthDate_holder);

        //初始化年月日
        Calendar c = Calendar.getInstance();
        year = c.get(Calendar.YEAR);
        month = c.get(Calendar.MONTH);
        day = c.get(Calendar.DAY_OF_MONTH);

        gender_holder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    if (gender.equals("male")) {
                        gender_choice = 0;
                    } else {
                        gender_choice = 1;
                    }

                    new AlertDialog.Builder(SignUpActivity.this)
                            .setTitle("性别")
                            /*.setIcon(R.drawable.add)*/
                            .setSingleChoiceItems(R.array.gender, gender_choice, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    gender_choice = which;
                                }
                            })
                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    switch (gender_choice) {
                                        case 0:
                                            gender = "male";
                                            genderText.setText("男");
                                            break;
                                        case 1:
                                            gender = "female";
                                            genderText.setText("女");
                                            break;
                                        default:
                                            break;
                                    }
                                }
                            })
                            .setNegativeButton("取消", null)
                            .show();
            }
        });

        birthDate_holder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialog dialog = new DatePickerDialog(SignUpActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        Date date = new Date(year - 1900, month, dayOfMonth);

                        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

                        birthDateText.setText(format.format(date));
                    }
                }, year, month, day);
                dialog.getDatePicker().setMaxDate(new Date().getTime());
                dialog.show();
            }
        });

        getCodeButton.setOnClickListener(new View.OnClickListener() { //获取验证码-按钮点击事件
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(phoneNumText.getText())) {
                    Toast.makeText(SignUpActivity.this, "请输入手机号", Toast.LENGTH_SHORT).show();
                } else {
                    //发送GET请求验证码
                    getCode();
                    //按钮60s倒计时
                    TimeCount time = new TimeCount(60000, 1000);
                    time.start();
                }
            }
        });

        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(phoneNumText.getText())) {
                    Toast.makeText(SignUpActivity.this, "请输入手机号", Toast.LENGTH_SHORT).show();
                } else if (TextUtils.isEmpty(codeText.getText())) {
                    Toast.makeText(SignUpActivity.this, "请输入验证码", Toast.LENGTH_SHORT).show();
                } else if (TextUtils.isEmpty(passwordText.getText())) {
                    Toast.makeText(SignUpActivity.this, "请输入密码", Toast.LENGTH_SHORT).show();
                } else if (TextUtils.isEmpty(confirmPasswordText.getText())) {
                    Toast.makeText(SignUpActivity.this, "请确认密码", Toast.LENGTH_SHORT).show();
                } else if (!passwordText.getText().toString().equals(confirmPasswordText.getText().toString())) {
                    Toast.makeText(SignUpActivity.this, "两次输入密码不一致", Toast.LENGTH_SHORT).show();
                } else {
                    //发送POST
                    signUp();
                }
            }
        });
    }

    class TimeCount extends CountDownTimer { //按钮倒计时类
        public TimeCount(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            getCodeButton.setBackgroundColor(Color.parseColor("#DCDCDC"));
            getCodeButton.setClickable(false);
            getCodeButton.setText(millisUntilFinished / 1000 +"秒后重新发送");
        }

        @Override
        public void onFinish() {
            getCodeButton.setText("重新获取验证码");
            getCodeButton.setClickable(true);
            getCodeButton.setBackgroundColor(Color.parseColor("#00AE98"));

        }
    }

    private void getCode() {
        HttpUtil.get(getCodeUrl + phoneNumText.getText().toString(), new HttpUtil.HttpResponseCallBack() {
            @Override
            public void onSuccess(int code, String result) {
                switch (code) {
                    case 200:
                        Toast.makeText(SignUpActivity.this, "验证码已发送到您手机上", Toast.LENGTH_SHORT).show();
                        break;
                    case 400:
                        Toast.makeText(SignUpActivity.this, "手机号码已注册", Toast.LENGTH_SHORT).show();
                        break;
                    case 403:
                        Toast.makeText(SignUpActivity.this, "手机号码无效", Toast.LENGTH_SHORT).show();
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

    private void signUp() {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("userId", phoneNumText.getText().toString());
        params.put("password", passwordText.getText().toString());
        params.put("code", codeText.getText().toString());
        params.put("name", nameText.getText().toString());
        params.put("gender", gender);
        params.put("birthDate", birthDateText.getText().toString());
        params.put("location", locationText.getText().toString());
        HttpUtil.post(signUpUrl, params, new HttpUtil.HttpResponseCallBack() {
            @Override
            public void onSuccess(int code, String result) {
                switch (code) {
                    case 200:
                        resultAnalysis(result);
                        Toast.makeText(SignUpActivity.this, "注册成功", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent();
                        intent.setClass(SignUpActivity.this, MainActivity.class);
                        SignUpActivity.this.startActivity(intent);
                        break;
                    case 400:
                        Toast.makeText(SignUpActivity.this, "手机号已被注册", Toast.LENGTH_SHORT).show();
                        break;
                    case 403:
                        Toast.makeText(SignUpActivity.this, "手机号码无效", Toast.LENGTH_SHORT).show();
                        break;
                    case 450:
                        Toast.makeText(SignUpActivity.this, "验证码错误", Toast.LENGTH_SHORT).show();
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

    private void resultAnalysis(String string) { //解析JSON数据或更新UI
        JSONObject object = null;
        try {
            object = new JSONObject(string);

            UserData.getInstance().setUserId(phoneNumText.getText().toString());
            UserData.getInstance().setPassword(passwordText.getText().toString());
            UserData.getInstance().setLoginState(true);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
