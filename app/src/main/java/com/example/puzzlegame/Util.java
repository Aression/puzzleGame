package com.example.puzzlegame;

import android.graphics.Bitmap;

import java.util.Random;

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
    public static int[] getRandomArray(int log){
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

    public static int getRandomInt(int min, int max){
        Random random = new Random();
        return random.nextInt(max)%(max-min+1) + min;
    }

    // 数组循环找下标
    public static int useLoop(int[] arr, int targetValue) {
        for (int i = 0; i < arr.length; i++) {
            if(arr[i]==targetValue) return i;
        }
        return -1;
    }
}
