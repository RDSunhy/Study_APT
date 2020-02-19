package com.example.study;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.annotations.AutoBind;
import com.example.annotations.BindView;
import com.example.api.MyButterKnife;

@AutoBind("Hello")
public class MainActivity extends AppCompatActivity{

    @BindView(R.id.tvTest)
    TextView tvTest;

    @Override
    protected void onCreate(Bundle savedInstanceState)  {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //调用我们的黄油刀 bind方法
        MyButterKnife.bind(this);
        tvTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this,"Hello World!",Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //解绑
        MyButterKnife.unBind(this);
    }
}
