package com.example.puzzlegame;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private ResultProfileBinding binding;

    private ImageView s1,s2,s3,s4,s5,s6,s7,s8;
    private ImageView p1,p2,p3,p4,p5,p6,p7,p8,pout;
    private Button confirmSelection, submitPuzzle;
    private TextView timeCounter, resultPrinter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }
}