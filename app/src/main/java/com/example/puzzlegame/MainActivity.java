package com.example.puzzlegame;

import static com.example.puzzlegame.Util.*;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.example.puzzlegame.databinding.ActivityMainBinding;
import com.example.puzzlegame.models.ImagePiece;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;



public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    //将imageview组件对应的ID编入数组以方便维护
    private final int[] sourceList=new int[8];
    private final int[] divideList=new int[9];

    //private ImageView s1,s2,s3,s4,s5,s6,s7,s8;
    private ArrayList<ImageView> source;
    //private ImageView p1,p2,p3,p4,p5,p6,p7,p8,p9;
    private ArrayList<ImageView> divide;

    private TextView timeCounter;
    private TextView resultPrinter;
    private TextView infoPrinter;

    private Button confirmSelection;
    private Button submitPuzzle;
    private Button giveUpPuzzle;

    // 九宫格对应的搜索方向
    ArrayList<int[]> dirs;
    // bitmap最后一次切割的图片列表
    private List<ImagePiece> imgPieces;
    // 积分, 表示成功和失败的结果
    private int Credits = 0;
    // 游戏是否已经开始, 用于控制按键的有效性
    private boolean gameStarted = false;
    // 一次游戏的开始时间
    private Calendar startTime;
    // 计时器, 用于正向计时
    private Timer timer;
    // R.drawable._null对应的Bitmap
    private Bitmap nullBitmap;
    // animation
    RotateAnimation anim= new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f);

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 处理这里的this红色波浪线报错: lifecycle 更新到2.2.0版本
        com.example.puzzlegame.databinding.ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 初始化nullBitmap
        Resources res = getResources();
        nullBitmap = BitmapFactory.decodeResource(res, R.drawable._null);

        // source组件初始化
        // private ImageView s1,s2,s3,s4,s5,s6,s7,s8;
        source = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            int id = getResources().getIdentifier("source"+(i+1),"id", getPackageName());
            source.add(findViewById(id));
            // 初始化按键响应事件, tag, 在对应列表中记录部件id
            source.get(i).setOnClickListener(this);
            source.get(i).setTag(getResources().getIdentifier("img"+(i+1),"drawable", getPackageName()));
            sourceList[i]=id;
        }

        // 拼图组件初始化
        // 所有的tag先初始化为nullBitmap
        divide = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            int id = getResources().getIdentifier("img"+(i+1),"id", getPackageName());
            divide.add(findViewById(id));
            // 初始化按键响应事件, tag, 在对应列表中记录部件id
            divide.get(i).setOnClickListener(this);
            divide.get(i).setImageResource(R.drawable._null);
            divide.get(i).setTag(nullBitmap);
            divideList[i]=id;
        }

        // 绑定组件
        Button confirmSelection = binding.startPuzzle;
        Button submitPuzzle = binding.endPuzzle;
        Button giveUpPuzzle = binding.giveUpGame;
        timeCounter = binding.CounterTime;
        resultPrinter = binding.puzzleResult;
        infoPrinter = binding.puzzleInfo;

        // 按钮绑定点击事件
        confirmSelection.setOnClickListener(this);
        submitPuzzle.setOnClickListener(this);
        giveUpPuzzle.setOnClickListener(this);

        // 初始化搜索方向列表
        dirs = new ArrayList<>();
        dirs.add(new int[]{1,3});
        dirs.add(new int[]{-1,1,3});
        dirs.add(new int[]{-1,3});
        dirs.add(new int[]{-3,1,3});
        dirs.add(new int[]{-3,1,3,-1});
        dirs.add(new int[]{-1,-3,3});
        dirs.add(new int[]{-3,1});
        dirs.add(new int[]{-1,-3,1});
        dirs.add(new int[]{-1,-3});
    }

    @SuppressLint("SetTextI18n")
    private void updateDivideImages(ImageView view){
        // 使用view对应的源图片更新拼图框内的图片
        if(!gameStarted){
            int lastChoseImageID = (int) view.getTag();
            Resources res = getResources();
            Bitmap bmp = BitmapFactory.decodeResource(res, lastChoseImageID);
            // 留一个空用于交换位置. 8号位没有被更新, 仍然是null
            imgPieces = ImageSplitter.split(bmp,3,3).subList(0,8);
            for (ImagePiece piece:imgPieces) {
                divide.get(piece.index).setImageDrawable(new BitmapDrawable(getResources(),piece.bitmap));
                divide.get(piece.index).setTag(piece.bitmap);
            }
            // 在imgPieces里面加上null对应的bitmap便于后续判定
            imgPieces.add(new ImagePiece(8,nullBitmap));
        }else{
            infoPrinter.setText("游戏尚未开始,请不要在现在选择拼图题目!");
        }
    }

    public boolean imageNull(ImageView view){
        // 拼图框内的imageview检测自身是否已经被赋予了有效的图片
        return Util.isEqual(nullBitmap,(Bitmap) view.getTag());
    }

    @SuppressLint("SetTextI18n")
    private void performPuzzleMove(ImageView view){
        // 点击拼图框内的图片时, 执行图片的移动操作
        if(!gameStarted){
            infoPrinter.setText("游戏尚未开始,请不要在现在解题!");
        }else if(!imageNull(view)){
            // 当图片非空的时候执行移动
            int indNow = Util.useLoop(divideList,view.getId());

            for (int d : dirs.get(indNow)) {
                int searchViewInd = indNow+d;
                if(searchViewInd < 0 || searchViewInd > 9) {
                    continue;
                }
                ImageView targetView = findViewById(divideList[searchViewInd]);
                // 如果搜索到的目标方格是对应的nullBitmap, 允许图片移动
                if(imageNull(targetView)){
                    anim.reset();
                    // 当前view先转一圈
                    anim.setDuration(300);
                    view.startAnimation(anim);
                    // runOnUiThread的TimerTask 执行下一次的view更新

                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            runOnUiThread(() -> {
                                targetView.setImageDrawable(
                                        new BitmapDrawable(getResources(), (Bitmap) view.getTag()));
                                targetView.setTag(view.getTag());
                                // 当前的view设为nullBitmap
                                view.setImageDrawable(
                                        new BitmapDrawable(getResources(),nullBitmap));
                                view.setTag(nullBitmap);
                                // 重要:这里解绑定动画非常关键!
                                view.setAnimation(null);
                                targetView.startAnimation(anim);
                            });
                        }
                        //delay 300ms, 执行一次
                    },300);
                }
            }
        }
        // empty action if this view is already empty
    }

    private boolean judgePuzzleComplete(){
        // 点击完成拼图的时候, 判断是否执行完毕
        for (ImagePiece piece : imgPieces) {
            if(!Util.isEqual(piece.bitmap,(Bitmap) divide.get(piece.index).getTag())){
                return false;
            }
        }
        return true;
    }

    // 定义主线程体
    Runnable timerRunnable=new Runnable() {
        public volatile boolean exit = false;
        @Override
        public void run() {
            while(!exit){
                // 如果游戏结束,退出此线程
                exit=gameStarted;
            }
        }
    };

    private void flush(){
        /* 打乱图片
        *  由逆序数定义可以推导出拼图有且仅有两类. 要保证拼图可还原, 只需要保证拼图类别不发生变化即可.
        *  设拼图规模为n*m, 空白格位于mat[n-1][m-1]
        *  根据二维数组生成一个大小为n*m的一维数组data[0]...data[n*m-1],
        *  则令p=n+m+con, con表示这个序列的逆序数. 保持随机生成的一维数组p值奇偶不变即可.
        * */
        int[] indList = new int[8];
        boolean available=false;
        // 3*3的p=6+0=0,偶数.
        // 生成p值同为偶数的序列
        while(!available){
            indList = Util.getrandomarray(8);
            int con=0;
            for (int i = 0; i < 8; i++) {
                for (int j = i+1; j < 8; j++) {
                    if(indList[i]>indList[j]) con++;
                }
            }
            available = (con % 2 == 0);
        }

        System.out.println(Arrays.toString(indList));
        // 重新组织拼图
        for (int i = 0; i < 8; i++) {
            int indNow = indList[i];
            System.out.println(indNow);
            ImagePiece piece = imgPieces.get(indNow);
            divide.get(i).setImageDrawable(new BitmapDrawable(getResources(),piece.bitmap));
            divide.get(i).setTag(piece.bitmap);
        }
    }

    @SuppressLint("SetTextI18n")
    private void startGame(){
        // 开始游戏, 并按规则重新组织拼图(出题)
        if(gameStarted){
            infoPrinter.setText("游戏已经开始, 请不要重复开始游戏!.");
        }else{
            // 游戏状态设置为已开始
            gameStarted=true;
            startTime = Calendar.getInstance();
            timer = new Timer();
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    // 在主线程内新建一个子线程用于计时
                    runOnUiThread(() -> {
                        Calendar now = Calendar.getInstance();
                        timeCounter.setText("当前用时: "
                                + (now.getTime().getTime() - startTime.getTime().getTime()) / 1000 + " s");
                    });
                }
            };
            //每过一秒获取一次当前的calendar,打印时间间隔.
            timer.schedule (timerTask, 1000L, 1000L);
            //打乱图片
            flush();
        }
    }

    @SuppressLint("SetTextI18n")
    private void endGame(){
        // 结束游戏, 恢复初始状态
        if(judgePuzzleComplete()){
            // 清除当前状态
            gameStarted = false;
            // 清空计时队列
            timer.cancel();
            // 打印结果
            Calendar now = Calendar.getInstance();
            resultPrinter.setText("拼图成功! "+
                    "本次用时: "+(now.getTime().getTime()-startTime.getTime().getTime())/1000+" s");
            infoPrinter.setText("拼图成功! 你的积分为: "+ (++Credits) +".");
        }else{
            infoPrinter.setText("拼图还没有完成!");
        }
    }

    @SuppressLint("SetTextI18n")
    private void giveUp(){
        // 放弃本次游戏, 统计信息并获得惩罚: credits-1
        if(gameStarted){
            // 恢复拼图
            for (ImagePiece piece:imgPieces) {
                divide.get(piece.index).setImageDrawable(new BitmapDrawable(getResources(),piece.bitmap));
            }
            // 清除当前状态
            gameStarted = false;
            // 清空计时队列
            timer.cancel();
            // 打印结果
            Calendar now = Calendar.getInstance();
            resultPrinter.setText("拼图失败! "+
                    "本次: "+(now.getTime().getTime()-startTime.getTime().getTime())/1000+" s");
            infoPrinter.setText("本轮游戏被取消. 你的当前积分为: "+ (--Credits) +".");
        }
        else{
            infoPrinter.setText("游戏没有开始, 你无法在现在取消游戏!");
        }
    }



    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view){
        // 全局点击事件
        int viewID = view.getId();
        if (viewID == R.id.startPuzzle) {
            // 开始主线程, 该线程在gameStarted=false的时候自动终止.
            new Thread(timerRunnable).start();
            startGame();
        }
        else if (viewID == R.id.endPuzzle) {
            endGame();
        }
        else if (viewID == R.id.giveUpGame) {
            giveUp();
        }
        else{
            // 这里使用循环遍历的方法查找部件在两个列表里面的ID
            int sourceInd = Util.useLoop(sourceList,viewID);
            int divideInd = Util.useLoop(divideList,viewID);
            if(sourceInd >= 0){
                // 点击的是source里面的图片
                updateDivideImages(findViewById(viewID));
            }else if(divideInd >= 0){
                // 点击的是拼图框里面的图片
                performPuzzleMove(findViewById(viewID));
            }else{
                // 点击了没有定义的部件
                throw new UnsupportedOperationException("not found command id");
            }
        }
    }
}