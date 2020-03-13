package studio.dex.ocrforidcard;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.mdsd.tool.IDBean;
import com.mdsd.tool.IdentitiyIdCardCallBack;
import com.mdsd.tool.IdentityIdCardManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@SuppressWarnings("deprecation")
public class TakePicActivity extends AppCompatActivity implements SurfaceHolder.Callback, Camera.PreviewCallback {
    private SurfaceView previewSv;
    private Camera camera;
    private ImageView photoIv;
    private FrameLayout previewFl;
    private TextView idCardAreaTv;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    //    身份证宽高比
    private static final float IDCARD_WIDTH_SPLIT_HIGHT = 1.59f;
    //    身份证宽度占整个预览界面的比例
    private static final float IDCARD_WITH_RATE_IN_VIEW = 0.4f;
    //    聚焦成功后过段时间再拍摄照片，否则会导致拍出的照片模糊
    private static final int TAKE_PIC_AFTER_FOCUS_TIME = 500;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_pic_layout);
        previewSv = findViewById(R.id.previewSv);
        photoIv = findViewById(R.id.photoIv);
        previewFl = findViewById(R.id.previewFl);
        idCardAreaTv = findViewById(R.id.idCardAreaTv);
        initPreviewSurfaceView();
    }


    private void initPreviewSurfaceView() {
        SurfaceHolder surfaceHolder = previewSv.getHolder();
        previewSv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    getCamera().autoFocus(null);

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        surfaceHolder.addCallback(this);

    }

    private Camera getCamera() {
        if (camera == null) {
            camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        }
        return camera;
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        configCamera();
        try {
            getCamera().setPreviewDisplay(holder);
            getCamera().setPreviewCallback(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
        getCamera().startPreview();
    }

    private void configCamera() {


        Camera.Parameters parameters = getCamera().getParameters();
        BestCameraSize bestSize = findBestSize(parameters);
        getCamera().setParameters(parameters);
        resizePreview(bestSize.width, bestSize.height);
    }

    //    重新调整预览界面的大小
    private void resizePreview(int previewPicWidth, int previewPicHeight) {


        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;
        float deviceRate = ((float) width) / ((float) height);
        float previewRate = ((float) previewPicWidth) / ((float) previewPicHeight);
        int previewW = 0;
        int previewH = 0;
        if (previewRate > deviceRate) {
            previewW = width;
            previewH = width * previewPicHeight / previewPicWidth;
        } else {
            previewW = height * previewPicWidth / previewPicHeight;
            previewH = height;

        }
        RelativeLayout.LayoutParams preViewLp = (RelativeLayout.LayoutParams) previewSv.getLayoutParams();
        preViewLp.width = previewW;
        preViewLp.height = previewH;
//        重新调整身份证放置框的大小
        float idCardW = ((float) previewW) * IDCARD_WITH_RATE_IN_VIEW;
        float idCardH = idCardW / IDCARD_WIDTH_SPLIT_HIGHT;
        ViewGroup.LayoutParams idCardLp = idCardAreaTv.getLayoutParams();
        idCardLp.width = (int) idCardW;
        idCardLp.height = (int) idCardH;


    }

    private BestCameraSize findBestSize(Camera.Parameters parameters) {
        Camera.Size previewSize = parameters.getPreviewSize();
        return new BestCameraSize(previewSize);
    }

    private Boolean isProcessingCard = false;

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    public void closePreviewPhoto(View view) {
        previewFl.setVisibility(View.GONE);
    }

    @Override
    public void onPreviewFrame(final byte[] data, Camera camera) {
        if (isProcessingCard) {
            return;
        }
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                isProcessingCard = true;
                getCamera().autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, final Camera camera) {
                        if (!success) {
//                           自动聚焦失败也需要将图片处理flag置为false否则将永远无法进入图片处理
                            isProcessingCard = false;
                            return;
                        }
                       new Handler().postDelayed(new Runnable() {
                           @Override
                           public void run() {
                               Camera.Size previewSize = camera.getParameters().getPreviewSize();
                               Bitmap bitmap = TakePicUtil.convertPreviewFrameData(data, previewSize.width, previewSize.height);
//                    裁剪照片，将身份证所在的区域裁剪出来
                               int preBitmapWidth = bitmap.getWidth();
                               int preBitmapHeight = bitmap.getHeight();
                               int idCardWidth = (int) (((float) preBitmapWidth) * IDCARD_WITH_RATE_IN_VIEW);
                               int idCardHeight = (int) (((float) idCardWidth) / IDCARD_WIDTH_SPLIT_HIGHT);
                               Point idCardStartPoint = new Point(preBitmapWidth / 2 - idCardWidth / 2, preBitmapHeight / 2 - idCardHeight / 2);
                               Bitmap idCardBitmap = Bitmap.createBitmap(bitmap, idCardStartPoint.x, idCardStartPoint.y, idCardWidth, idCardHeight);
                               IdentityIdCardManager.instance.identyIdCardByBitmap(TakePicActivity.this, idCardBitmap, new IdentitiyIdCardCallBack() {
                                   @Override
                                   public void onError(String s) {
                                   }

                                   @Override
                                   public void onSuccess(IDBean idBean) {
//                                       LogUtil.log(idBean.toString());
                                       setPatientInfo(idBean);
                                   }

                                   @Override
                                   public void onIdNoTrue() {
                                       showLoading();
                                   }

                                   @Override
                                   public void onFinish() {
                                       isProcessingCard = false;
                                       dismissLoading();
                                   }
                               });

                           }
                       },TAKE_PIC_AFTER_FOCUS_TIME);

                    }
                });

            }
        });


    }
    private void showLoading(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.progressingLl).setVisibility(View.VISIBLE);
            }
        });
    }
    private void dismissLoading(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.progressingLl).setVisibility(View.GONE);
            }
        });

    }
    private void setPatientInfo(final IDBean idBean){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView cardInfoTv = findViewById(R.id.cardInfoTv);
                cardInfoTv.setText(String.format("姓名:%s 性别:%s 出生日期: %s年%s月%s日",
                        idBean.getName(),idBean.getGender(),idBean.getYear(),
                        idBean.getMonth(),idBean.getDay()));

            }
        });
    }

    public void openLight(View view) {

    }

    private void openOrCloseLight() {
        try {
            Camera.Parameters parameters = getCamera().getParameters();
            String flashMode = parameters.getFlashMode();
            parameters.setFlashMode(flashMode.equals(Camera.Parameters.FLASH_MODE_TORCH) ? Camera.Parameters.FLASH_MODE_OFF : Camera.Parameters.FLASH_MODE_TORCH);
            getCamera().setParameters(parameters);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


//    private File getRootFile() {
//        return getExternalFilesDir(null);
//    }


    private class BestCameraSize {
        private int width;
        private int height;

        public BestCameraSize(Camera.Size cameraSize) {
            this.width = cameraSize.width;
            this.height = cameraSize.height;
        }

        public BestCameraSize(int width, int height) {
            this.width = width;
            this.height = height;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        @NonNull
        @Override
        public String toString() {
            return "w=" + width + " : h=" + height;
        }
    }

    public static class TakePicUtil {
        //        将获取到的图片进行格式的转换
        static Bitmap convertPreviewFrameData(byte[] data, int width, int height) {

            YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, width, height, null);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, byteArrayOutputStream);
            byte[] distData = byteArrayOutputStream.toByteArray();
            return BitmapFactory.decodeByteArray(distData, 0, distData.length);
        }

        //        保存图片到文件
        static void savePicWithBitmap(Bitmap preBitmap, File file) {
            if (file == null || preBitmap == null) {
                return;
            }
            try {

                FileOutputStream fileOutputStream = new FileOutputStream(file);
                preBitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
                fileOutputStream.close();
                fileOutputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        static File getIdCardPicFile(Context context) {
            return new File(context.getExternalFilesDir(null), "idCardPic.png");
        }

        static void deleteFile(File file) {
            if (file.exists()) {
                file.delete();
            }
        }

    }
}
