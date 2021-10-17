package com.example.puzzlegame;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.puzzlegame.databinding.ActivityMainBinding;
import com.example.puzzlegame.models.ImagePiece;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private ActivityMainBinding binding;

    //将imageview组件编入数组以方便维护, 减少无用的代码量
    private final int[] sourceList=new int[8];
    private final int[] divideList=new int[10];
    //private ImageView s1,s2,s3,s4,s5,s6,s7,s8;
    private ArrayList<ImageView> source;
    //private ImageView p1,p2,p3,p4,p5,p6,p7,p8,p9,pout;
    private ArrayList<ImageView> divide;

    private Button confirmSelection ;
    private Button submitPuzzle;
    private Button giveUpPuzzle;
    private TextView timeCounter;
    private TextView resultPrinter;
    private TextView infoPrinter;

    private int lastChoseImageID;
    private int Credits = 0;
    private boolean gameStarted = false;
    private Calendar startTime;
    private Timer timer;
    private TimerTask timerTask;
    private Bitmap nullBitmap;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 处理这里的this红色波浪线报错: lifecycle 更新到2.2.0版本
        MainViewModel model = new ViewModelProvider(this).get(MainViewModel.class);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        System.out.println(R.id.source1);


        source = new ArrayList<ImageView>();
        for (int i = 0; i < 8; i++) {
            int id = getResources().getIdentifier("source"+(i+1),"id", getPackageName());
            source.add(findViewById(id));
            source.get(i).setOnClickListener(this);
            source.get(i).setTag(getResources().getIdentifier("img"+(i+1),"drawable", getPackageName()));
            sourceList[i]=id;
        }

        System.out.println(source.get(0).getId());

        divide = new ArrayList<ImageView>();
        for (int i = 0; i < 9; i++) {
            int id = getResources().getIdentifier("img"+(i+1),"id", getPackageName());
            divide.add(findViewById(id));
            divide.get(i).setOnClickListener(this);
            divide.get(i).setTag(new byte[]{});
            divideList[i]=id;
        }
        divide.add(findViewById(R.id.imgout));
        divideList[9]=R.id.imgout;
        divide.get(9).setOnClickListener(this);
        divide.get(9).setTag(new byte[]{});

        confirmSelection = binding.startPuzzle;
        submitPuzzle = binding.endPuzzle;
        giveUpPuzzle = binding.giveUpGame;
        timeCounter = binding.CounterTime;
        resultPrinter = binding.puzzleResult;
        infoPrinter = binding.puzzleInfo;

        confirmSelection.setOnClickListener(this);
        submitPuzzle.setOnClickListener(this);
        giveUpPuzzle.setOnClickListener(this);

        Resources res = getResources();
        nullBitmap = BitmapFactory.decodeResource(res, R.drawable._null);
    }

    private void updateDivideImages(ImageView view){
        // 使用view对应的源图片更新拼图框内的图片
        if(!gameStarted){
            lastChoseImageID = (int) view.getTag();
            Resources res = getResources();
            Bitmap bmp = BitmapFactory.decodeResource(res, lastChoseImageID);
            List<ImagePiece> imgPieces = ImageSplitter.split(bmp,3,3);
            for (ImagePiece piece:imgPieces) {
                divide.get(piece.index).setImageDrawable(new BitmapDrawable(getResources(),piece.bitmap));
                //todo: tag improvement to decrease the effcieny
                divide.get(piece.index).setTag(bmp);
            }
        }else{
            infoPrinter.setText("game has already started! please don't change image now.");
        }
    }

    public static boolean isEqual(Bitmap expectedBitmap, Bitmap actualBitmap) {
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
        if (nonMatchingPixels > allowedMaxNonMatchPixels) {return false;}
        return true;
    }

    private boolean imageNull(ImageView view){
        // 拼图框内的imageview检测自身是否已经被赋予了有效的图片
        return !isEqual(nullBitmap,(Bitmap) view.getTag());
    }

    private void performPuzzleMove(ImageView view){
        // 点击拼图框内的图片时, 执行图片的移动操作
        if(!gameStarted){
            infoPrinter.setText("game not started! please don't solve puzzle now.");
        }else if(!imageNull(view)){
            // if the image is not empty, perform the move action.
            int indNow = useLoop(divideList,view.getId());
            int [] dirs = {-3,+3,-1,+1};
            for (int d : dirs) {
                int searchViewInd = indNow+d;
                if(searchViewInd < 0 || searchViewInd > 9) continue;
                ImageView targetView = findViewById(searchViewInd);
                if(imageNull(targetView)){
                    // null view set to the current bitmap
                    targetView.setImageDrawable(
                            new BitmapDrawable(getResources(), nullBitmap));
                    Bitmap nullBmp = BitmapFactory.decodeResource(getResources(), R.drawable._null);
                    // current view set to null
                    view.setImageDrawable(
                            new BitmapDrawable(getResources(),nullBmp));
                }
            }
        }
        // empty action if the view is already empty
    }

    private boolean judgePuzzleComplete(){
        // 点击完成拼图的时候, 判断是否执行完毕
        return false;
    }

    Runnable timerRunnable=new Runnable() {
        public volatile boolean exit = false;
        @Override
        public void run() {
            while(!exit){
                exit=gameStarted;
            }
        }
    };

    private void startGame(){
        gameStarted=true;
        startTime = Calendar.getInstance();
        timer = new Timer();
        timerTask = new TimerTask(){
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    public void run() {
                        Calendar now = Calendar.getInstance();
                        timeCounter.setText("time used: "
                                +(now.getTime().getTime()-startTime.getTime().getTime())/1000+" s");
                    }
                });
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
            resultPrinter.setText("puzzle success! "+
                    "time used: "+(now.getTime().getTime()-startTime.getTime().getTime())/1000+" s");
            infoPrinter.setText("puzzle success! your credit: "+ (++Credits) +".");
        }else{
            infoPrinter.setText("puzzle not success yet!");
        }
    }

    private void giveUp(){
        // give up this game and receive punishment
        if(gameStarted){
            // recover image
            Resources res = getResources();
            Bitmap bmp = BitmapFactory.decodeResource(res, lastChoseImageID);
            List<ImagePiece> imgPieces = ImageSplitter.split(bmp,3,3);
            for (ImagePiece piece:imgPieces) {
                divide.get(piece.index).setImageDrawable(new BitmapDrawable(getResources(),piece.bitmap));
            }
            // 清除当前状态
            gameStarted = false;
            // 清空计时队列
            timer.cancel();
            // 打印结果
            Calendar now = Calendar.getInstance();
            resultPrinter.setText("puzzle failed! "+
                    "总计用时: "+(now.getTime().getTime()-startTime.getTime().getTime())/1000+" s");
            infoPrinter.setText("this turn of game is canceled. your credit: "+ (--Credits) +".");
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
        if (viewID == R.id.startPuzzle) {
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
            // 注意 asList方法 默认生成二维数组! e.g.: [1,2,3] --> [[1,2,3]]
            // Arrays.asList(array).contains(target);
            // 这里使用循环遍历的方法查找了

            int sourceInd = useLoop(sourceList,viewID);
            int divideInd = useLoop(divideList,viewID);
            if(sourceInd >= 0){
                // 点击的是source里面的图片
                updateDivideImages(findViewById(viewID));
            }else if(divideInd >= 0){
                // 点击的是拼图框里面的图片
                performPuzzleMove(findViewById(viewID));
            }else{
                throw new UnsupportedOperationException("not found command id");
            }
        }
    }
}