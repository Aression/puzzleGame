package com.example.puzzlegame.models;

import android.graphics.Bitmap;

public class ImagePiece {

    public int index = 0;

    public Bitmap bitmap = null;

    public ImagePiece(int ind, Bitmap bmp){
        this.index=ind;
        this.bitmap=bmp;
    }

    public ImagePiece(){

    }

}