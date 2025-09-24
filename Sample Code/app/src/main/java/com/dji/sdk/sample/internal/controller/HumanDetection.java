package com.dji.sdk.sample.internal.controller;

import android.graphics.SurfaceTexture;
import android.media.MediaFormat;
import android.os.Bundle;
import android.view.TextureView;

import androidx.appcompat.app.AppCompatActivity;

import com.dji.sdk.sample.R;

import org.opencv.core.Mat;

import java.nio.ByteBuffer;

import dji.sdk.codec.DJICodecManager;
import dji.sdk.camera.VideoFeeder;

public class HumanDetection extends AppCompatActivity implements TextureView.SurfaceTextureListener {

    private TextureView textureView;
    private DJICodecManager codecManager;
    private VideoFeeder.VideoDataListener videoDataListener;



    private TextureView textureView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_human_detection);

        textureView = findViewById(R.id.videoTex);
        textureView.setSurfaceTextureListener(this);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        codecManager = new DJICodecManager(this, surface, width, height);

        videoDataListener = (videoBuffer, size) -> {
            if (codecManager != null) {
                codecManager.sendDataToDecoder(videoBuffer, size);
            }
        };
        VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(videoDataListener);

        codecManager.enabledYuvData(true);
        codecManager.setYuvDataCallback(new DJICodecManager.YuvDataCallback() {
            @Override
            public void onYuvDataReceived(MediaFormat mediaFormat, ByteBuffer yuvFrame,
                                          int dataSize, int w, int h) {
                Mat frame = yuvToMat(yuvFrame, w, h);
                boolean detected = detectHuman(frame);
                // TODO: 根據 detected 控制飛行
            }
        });
    }

    @Override public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int w, int h) {}
    @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (videoDataListener != null) {
            VideoFeeder.getInstance().getPrimaryVideoFeed().removeVideoDataListener(videoDataListener);
        }
        if (codecManager != null) {
            codecManager.destroyCodec();
            codecManager = null;
        }
        return true;
    }
    @Override public void onSurfaceTextureUpdated(SurfaceTexture surface) {}

    // TODO: OpenCV 轉換與人偵測
    private Mat yuvToMat(ByteBuffer yuvFrame, int w, int h) { return new Mat(); }
    private boolean detectHuman(Mat frame) { return false; }
}
