//!!rrr
package com.dji.sdk.sample.internal.controller;
import android.graphics.SurfaceTexture;
import android.media.MediaFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import androidx.appcompat.app.AppCompatActivity;
import com.dji.sdk.sample.R;
import org.opencv.core.Mat;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import java.nio.ByteBuffer;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.camera.VideoFeeder;

public class HumanDetection extends AppCompatActivity implements TextureView.SurfaceTextureListener {

    private TextureView textureView;
    private DJICodecManager codecManager;
    private VideoFeeder.VideoDataListener videoDataListener;

    private VideoFeeder.VideoDataListener primaryVideoDataListener =
            (videoBuffer, size) -> {
                if (codecManager != null) {
                    codecManager.sendDataToDecoder(videoBuffer, size);
                }
            };

    private boolean surfaceReady = false;

    private boolean isProductConnected() {
        return DJISampleApplication.getProductInstance() != null
                && DJISampleApplication.getProductInstance().isConnected();
    }

    private void startVideoIfReady() {
        if (!surfaceReady || !isProductConnected()) return;

        // 建立 / 重建 codecManager
        if (codecManager == null) {
            // 兩種建構子擇一（依你 SDK 版本）：
            // 1) 透過 TextureView（較新的 sample 常用）
            // codecManager = new DJICodecManager(getApplicationContext(), textureView, textureView.getWidth(), textureView.getHeight());

            // 2) 透過 SurfaceTexture（較舊的）
            SurfaceTexture st = textureView.getSurfaceTexture();
            codecManager = new DJICodecManager(getApplicationContext(), st,
                    textureView.getWidth(), textureView.getHeight());
        }

        // 綁定主影像流（判空很重要，避免 NPE）
        VideoFeeder vf = VideoFeeder.getInstance();
        if (vf != null && vf.getPrimaryVideoFeed() != null) {
            vf.getPrimaryVideoFeed().addVideoDataListener(primaryVideoDataListener);
        } else {
            Log.w("HumanDetection", "VideoFeeder or primary feed is null; will retry later");
        }
    }

    private void stopVideo() {
        VideoFeeder vf = VideoFeeder.getInstance();
        if (vf != null && vf.getPrimaryVideoFeed() != null && primaryVideoDataListener != null) {
            vf.getPrimaryVideoFeed().removeVideoDataListener(primaryVideoDataListener);
        }
        if (codecManager != null) {
            codecManager.cleanSurface();
            codecManager.destroyCodec();
            codecManager = null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_human_detection);

        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "Unable to load OpenCV!");
        } else {
            Log.d("OpenCV", "OpenCV loaded successfully!");
        }

        textureView = findViewById(R.id.videoTex);
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                surfaceReady = true;
                startVideoIfReady();
            }

            @Override public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) { }
            @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                surfaceReady = false;
                stopVideo();
                return true; // 我們不再使用這個 surface 了
            }
            @Override public void onSurfaceTextureUpdated(SurfaceTexture surface) { }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 回到畫面時，如果已連線且 surface 就緒，啟動影像
        startVideoIfReady();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 離開畫面或被遮蔽時，先停掉避免崩潰與資源外洩
        stopVideo();
    }

    @Override
    protected void onDestroy() {
        stopVideo();
        super.onDestroy();
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
