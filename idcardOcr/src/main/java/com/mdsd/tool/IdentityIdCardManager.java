package com.mdsd.tool;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;

import org.opencv.android.StaticHelper;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public enum IdentityIdCardManager {
    instance;
    private boolean isInitedOpenCV = false;
    private ExecutorService executorService;

    IdentityIdCardManager() {
        isInitedOpenCV = false;
        executorService = Executors.newFixedThreadPool(2);
    }

    public void identyIdCardByBitmap(final Context context, final Bitmap preBitmap, final IdentitiyIdCardCallBack callBack) {
        if (preBitmap == null) {
            callBack.onError("未获取到图片信息");
            return;
        }


        executorService.execute(new Runnable() {
            @Override
            public void run() {
                //        检查OCR是否被初始化
                if (!isInitedOpenCV) {
                    IDCardLogUtil.log("OCR没有被初始化，正在初始化...");

                    boolean isInitSuccess = StaticHelper.initOpenCV(false);
                    if (!isInitSuccess) {
                        IDCardLogUtil.log("ocr初始化失败");
                        callBack.onError("ocr初始化失败!");
                        callBack.onFinish();
                        isInitedOpenCV = false;
                        return;
                    } else {
                        isInitedOpenCV = true;
                    }
                }


//        解析图片信息
//        首先获取到身份号码的信息，然后根据身份号来定位其他的信息
                Mat preMat = new Mat();
                Utils.bitmapToMat(preBitmap, preMat);
//        图片变灰
                Imgproc.cvtColor(preMat, preMat, Imgproc.COLOR_BGR2GRAY);
                IDCardLogUtil.log("灰度化...");
//                二值化
                Imgproc.threshold(preMat, preMat, 100, 255, Imgproc.THRESH_BINARY);
                IDCardLogUtil.log("二值化...");
//                膨胀
                Imgproc.erode(preMat, preMat, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(20, 10)));
                IDCardLogUtil.log("膨胀...");
//                轮廓检测
                List<MatOfPoint> matOfPoints = new ArrayList<>();
                Imgproc.findContours(preMat, matOfPoints, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
                IDCardLogUtil.log("轮廓检测...");
//                遍历轮廓找到和身份证号码比例相近的轮廓
                Bitmap idNoBitmap = null;
                Rect idCardRect = new Rect();
                Bitmap cuttedBitmap = null;
//                跟标准的比较差距
                float finalTempGateRate = 99f;

                for (MatOfPoint tempPoints : matOfPoints) {
                    Rect tempRect = Imgproc.boundingRect(tempPoints);
                    cuttedBitmap = Bitmap.createBitmap(preBitmap, tempRect.x, tempRect.y, tempRect.width, tempRect.height);
                    float tempRate = ((float) cuttedBitmap.getWidth()) / ((float) cuttedBitmap.getHeight());
                    boolean isNoInRange = IdentityIdCardConst.isIdNoRateInRange(tempRate);
                    IDCardLogUtil.log("轮廓比例=" + tempRate);
                    if (!isNoInRange) {
//                        callBack.onError("身份证号码识别失败，请校正图片后再试");
                        continue;
                    }

                    float rate = tempRate - IdentityIdCardConst.ID_NO_RATE_STANDARD_REFER;
                    if (Math.abs(rate) < finalTempGateRate) {
                        idNoBitmap = cuttedBitmap;
                        idCardRect = tempRect;
                        finalTempGateRate = rate;
                    }

                }
//                一轮遍历后最优的身份证号码图片就被过滤出来了
                if (idNoBitmap == null) {
                    callBack.onError("null-身份证号码识别失败，请校正图片后再试");
                    callBack.onFinish();
                    return;
                }
//                    准确的识别出了身份证号码
                callBack.onIdNoTrue();
//               通过身份证号码定位其他的信息
                int idNoHeight = idCardRect.height;
                Point idNoTopLeft = new Point(idCardRect.x, idCardRect.y);
                Bitmap nameBitmap = cutBitmapByPosition(preBitmap, new IdcardPositionBean(idNoTopLeft, idNoHeight,
                        3.39473684f, 9.60526316f, 0f, 7.31578947f));


                Bitmap genderBitmap = cutBitmapByPosition(preBitmap, new IdcardPositionBean(idNoTopLeft, idNoHeight,
                        3.39473684f, 7.55263158f,
                        1.84210526f, 5.71052632f));

                Bitmap yearBitmap = cutBitmapByPosition(preBitmap, new IdcardPositionBean(idNoTopLeft, idNoHeight,
                        3.39473684f, 5.97368421f,
                        1f, 4.21052632f));

                Bitmap monthBitmap = cutBitmapByPosition(preBitmap, new IdcardPositionBean(idNoTopLeft, idNoHeight,
                        0.210526316f, 5.71052632f,
                        -1.15789474f, 4.21052632f));

                Bitmap dayBitmap = cutBitmapByPosition(preBitmap, new IdcardPositionBean(idNoTopLeft, idNoHeight,
                        -1.84210526f, 5.71052632f,
                        -3.02631579f, 4.21052632f));


                String idNoStr = TessTwoManager.instance.start(context, idNoBitmap);
                IDCardLogUtil.log("身份证号=" + idNoStr);
                String nameStr = TessTwoManager.instance.start(context, nameBitmap);
                IDCardLogUtil.log("姓名=" + nameStr);
                String genderStr = TessTwoManager.instance.start(context, genderBitmap);
                IDCardLogUtil.log("性别=" + genderStr);
                String yearStr = TessTwoManager.instance.start(context, yearBitmap);
                IDCardLogUtil.log("出生年=" + yearStr);
                String monthStr = TessTwoManager.instance.start(context, monthBitmap);
                IDCardLogUtil.log("出生月=" + monthStr);
                String dayStr = TessTwoManager.instance.start(context, dayBitmap);
                IDCardLogUtil.log("出生日=" + dayStr);
//
                callBack.onSuccess(new IDBean(nameStr, genderStr, yearStr, monthStr, dayStr, idNoStr));
                callBack.onFinish();
                IDCardLogUtil.log("识别完成");

            }
        });


    }

    private Bitmap cutBitmapByPosition(Bitmap bitmap, IdcardPositionBean positionInfo) {
        int positionY = positionInfo.y < 0 ? 0 : positionInfo.y;
        int positionX = positionInfo.x < 0 ? 0 : positionInfo.x;
        return Bitmap.createBitmap(bitmap, positionX, positionY, positionInfo.width, positionInfo.height);
    }
}
