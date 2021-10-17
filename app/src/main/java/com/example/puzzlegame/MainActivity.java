package com.example.puzzlegame;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.puzzlegame.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private ActivityMainBinding binding;

    //将imageview组件编入数组以方便维护, 减少无用的代码量
    private final int[] sourceList={
            1000011,1000012,1000005,1000006,1000008,1000010,1000001,1000003
    };
    private final int[] divideList={
            1000209,1000208,1000180,1000178,1000183,1000181,1000185,1000184,1000189,1000376
    };
    //private ImageView s1,s2,s3,s4,s5,s6,s7,s8;
    private final ArrayList<ImageView> source
            = new ArrayList<ImageView>(){{
        for (int i = 0; i < 8; i++) {
            add(findViewById(sourceList[i]));
        }
    }};
    //private ImageView p1,p2,p3,p4,p5,p6,p7,p8,p9,pout;
    private final ArrayList<ImageView> divide
            = new ArrayList<ImageView>(){{
        for (int i = 0; i < 10; i++) {
            add(findViewById(divideList[i]));
        }
    }};

    private final Button confirmSelection = binding.startPuzzle;
    private final Button submitPuzzle=binding.endPuzzle;
    private final TextView timeCounter=binding.CounterTime;
    private final TextView resultPrinter = binding.puzzleResult;
    private final TextView infoPrinter = binding.puzzleInfo;

    private int successCounter=0;
    private boolean gameStarted = false;
    private Calendar startTime;
    private Timer timer;
    private TimerTask timerTask;


    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 处理这里的this红色波浪线报错: lifecycle 更新到2.2.0版本
        MainViewModel model = new ViewModelProvider(this).get(MainViewModel.class);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        for (ImageView item : source) {
            item.setOnClickListener(this);
            item.setTag(sourceList[source.indexOf(item)]);
        }
        // 拼图用的图片先全部设成null
        for (ImageView item : divide) {
            item.setOnClickListener(this);
            item.setImageResource(R.drawable._null);
        }
        confirmSelection.setOnClickListener(this);
        submitPuzzle.setOnClickListener(this);
    }

    private void updateDivideImages(ImageView view){
        // 使用view对应的源图片更新拼图框内的图片
    }

    private void checkImageViewSource(ImageView view){
        // 拼图框内的imageview检测自身是否已经被赋予了有效的图片

    }

    private void performPuzzleMove(ImageView view){
        // 点击拼图框内的图片时, 执行图片的移动操作
    }

    private boolean judgePuzzleComplete(){
        // 点击完成拼图的时候, 判断是否执行完毕
        return false;
    }

    private void startGame(){
        gameStarted=true;
        startTime = Calendar.getInstance();
        timer = new Timer();
        timerTask = new TimerTask(){
            @SuppressLint("SetTextI18n")
            public void run() {
                Calendar now = Calendar.getInstance();
                timeCounter.setText("当前用时: \n"
                        +(now.getTime().getTime()-startTime.getTime().getTime())/1000+" s");
            }
        };
        //每过一秒获取一次当前的calendar,打印时间间隔.
        timer.schedule (timerTask, 1000L, 1000L);
    }

    @SuppressLint("SetTextI18n")
    private void endGame(){
        if(judgePuzzleComplete()){
            // 清除当前状态
            gameStarted = false;
            // 清空计时队列
            timer.cancel();
            // 打印结果
            Calendar now = Calendar.getInstance();
            resultPrinter.setText("拼图成功! \n"+
                    "总计用时: "+(now.getTime().getTime()-startTime.getTime().getTime())/1000+" s");
            successCounter++;
            infoPrinter.setText("拼图成功! 你总共拼图成功了 "+successCounter+" 次.");
        }else{
            infoPrinter.setText("拼图未成功! 请继续努力!");
        }
    }

    public static int useLoop(int[] arr, int targetValue) {
        for (int i = 0; i < arr.length; i++) {
            if(arr[i]==targetValue) return i;
        }
        return -1;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view){
        int viewID = view.getId();
        if (viewID == R.id.startPuzzle) startGame();
        else if (viewID == R.id.endPuzzle) endGame();
        else{
            // 注意 asList方法 默认生成二维数组! e.g.: [1,2,3] --> [[1,2,3]]
            // Arrays.asList(array).contains(target);
            // 这里使用循环遍历的方法查找了
            int sourceInd = useLoop(sourceList,viewID);
            int divideInd = useLoop(divideList,viewID);
            if(sourceInd > 0){
                // 点击的是source里面的图片
            }else if(divideInd >0){
                // 点击的是拼图框里面的图片
            }else{
                throw new UnsupportedOperationException("not found comment id");
            }
        }
    }
}