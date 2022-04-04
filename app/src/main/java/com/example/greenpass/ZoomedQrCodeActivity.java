package com.example.greenpass;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import jp.wasabeef.glide.transformations.RoundedCornersTransformation;

public class ZoomedQrCodeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zoomed_qr_code);
        String imagePath = getIntent().getStringExtra("imagePath");

        ImageView qrCodeImageView = (ImageView) findViewById(R.id.qrCodeImageView);

        Glide.with(this)
                .load(imagePath)
                .into(qrCodeImageView);
    }
}