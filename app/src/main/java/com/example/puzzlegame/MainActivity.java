package com.example.puzzlegame;

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
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

class Util{
    public static boolean isEqual(Bitmap expectedBitmap, Bitmap actualBitmap) {
        // 判断两个Bitmap是否相等
        int nonMatchingPixels = 0;
        int allowedMaxNonMatchPixels = 10;
        if(expectedBitmap == null || actualBitmap == null) {return false;}
        int[] expectedBmpPixels = new int[expectedBitmap.getWidth() * expectedBitmap.getHeight()];
        expectedBitmap.getPixels(expectedBmpPixels, 0, expectedBitmap.getWidth(), 0, 0, expectedBitmap.getWidth(), expectedBitmap.getHeight());
        int[] actualBmpPixels = new int[actualBitmap.getWidth() * actualBitmap.getHeight()];
        actualBitmap.getPixels(actualBmpPixels, 0, actualBitmap.getWidth(), 0, 0, actualBitmap.getWidth(), actualBitmap.getHeight());
        if (expectedBmpPixels.length != actualBmpPixels.length) {return false;}
        for (int i = 0; i < expectedBmpPixels.length; i++) {
            if (expectedBmpPixels[i] != actualBmpPixels[i]) {nonMatchingPixels++;}
        }
        return nonMatchingPixels <= allowedMaxNonMatchPixels;
    }

    //得到数组内容从0到log-1的随机数组
    public static int[] getrandomarray(int log){
        int[] result = new int[log];
        for (int i = 0; i < log; i++) {
            result[i] = i;
        }
        for (int i = 0; i < log; i++) {
            int random = (int) (log * Math.random());
            int temp = result[i];
            result[i] = result[random];
            result[random] = temp;
        }
        return result;
    }

    // 数组循环找下标
    public static int useLoop(int[] arr, int targetValue) {
        for (int i = 0; i < arr.length; i++) {
            if(arr[i]==targetValue) return i;
        }
        return -1;
    }
}

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    //将imageview组件对应的ID编入数组以方便维护
    private final int[] sourceList=new int[8];
    private final int[] divideList=new int[10];

    //private ImageView s1,s2,s3,s4,s5,s6,s7,s8;
    private ArrayList<ImageView> source;
    //private ImageView p1,p2,p3,p4,p5,p6,p7,p8,p9,pout;
    private ArrayList<ImageView> divide;

    private TextView timeCounter;
    private TextView resultPrinter;
    private TextView infoPrinter;

    private Button confirmSelection;
    private Button submitPuzzle;
    private Button giveUpPuzzle;

    // 九宫格对应的搜索方向
    ArrayList<int[]> dirs;
    // bitmap切割的图片列表
    private List<ImagePiece> imgPieces;
    // 最后一次所选择的图片ID, 用于在放弃时恢复拼图
    private int lastChoseImageID;
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
//        MainViewModel model = new ViewModelProvider(this).get(MainViewModel.class);
        com.example.puzzlegame.databinding.ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 初始化nullBitmap
        Resources res = getResources();
        nullBitmap = BitmapFactory.decodeResource(res, R.drawable._null);

        // source组件初始化
        //private ImageView s1,s2,s3,s4,s5,s6,s7,s8;
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
        // 对九号部件初始化
        divide.add(findViewById(R.id.imgout));
        divide.get(9).setOnClickListener(this);
        divide.get(9).setImageResource(R.drawable._null);
        divide.get(9).setTag(nullBitmap);
        divideList[9]=R.id.imgout;

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
        dirs.add(new int[]{-3,1,3});
        dirs.add(new int[]{-1,-3,1});
        dirs.add(new int[]{-1,-3});
        dirs.add(new int[]{-3});
    }

    @SuppressLint("SetTextI18n")
    private void updateDivideImages(ImageView view){
        // 使用view对应的源图片更新拼图框内的图片
        if(!gameStarted){
            lastChoseImageID = (int) view.getTag();
            Resources res = getResources();
            Bitmap bmp = BitmapFactory.decodeResource(res, lastChoseImageID);
            imgPieces = ImageSplitter.split(bmp,3,3);
            for (ImagePiece piece:imgPieces) {
                divide.get(piece.index).setImageDrawable(new BitmapDrawable(getResources(),piece.bitmap));
                divide.get(piece.index).setTag(piece.bitmap);
            }
        }else{
            infoPrinter.setText("game has already started! please don't change image now.");
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
            infoPrinter.setText("game not started! please don't solve puzzle now.");
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
                    anim.setDuration(700);
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
                        //delay 700ms, 执行一次
                    },700); // 延时0.7秒
                }
            }
        }
        // empty action if this view is already empty
    }

    private boolean judgePuzzleComplete(){
        // 点击完成拼图的时候, 判断是否执行完毕
        // 如果存在tag与null对应的子图则直接返回false
        for (int i = 0; i < 9; i++) {
            if(imageNull(divide.get(i))){
                return false;
            }
        }
        // 否则判断当前组合是否对应到正确的图形组合
        for (int i = 0; i < 9; i++) {
            if(!Util.isEqual(imgPieces.get(i).bitmap, (Bitmap) divide.get(i).getTag())){
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

            // 打乱图片
            // 生成随机的0-8的数组, 将6号位设置为特定拼图, 使的游戏可以完成
            int[] indList = Util.getrandomarray(9);
            for (int i = 0; i < 9; i++) {
                if(indList[i]==6){
                    int tmp=indList[6];
                    indList[6]=6;
                    indList[i]=tmp;
                    break;
                }
            }

            // 重新组织拼图
            for (int i = 0; i < 9; i++) {
                int indNow = indList[i];
                ImagePiece piece = imgPieces.get(indNow);
                divide.get(i).setImageDrawable(new BitmapDrawable(getResources(),piece.bitmap));
                divide.get(i).setTag(piece.bitmap);
            }
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
            Resources res = getResources();
            Bitmap bmp = BitmapFactory.decodeResource(res, lastChoseImageID);
            List<ImagePiece> imgPieces = ImageSplitter.split(bmp,3,3);
            for (ImagePiece piece:imgPieces) {
                divide.get(piece.index).setImageDrawable(new BitmapDrawable(getResources(),piece.bitmap));
            }
            divide.get(9).setImageResource(R.drawable._null);
            divide.get(9).setTag(nullBitmap);
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