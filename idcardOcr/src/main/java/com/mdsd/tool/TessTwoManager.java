package com.mdsd.tool;

import android.content.Context;
import android.graphics.Bitmap;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public enum TessTwoManager {
    instance;
    private TessBaseAPI tessBaseAPI;

    public String start(Context context,Bitmap bitmap){
        TessBaseAPI tessTwo = getTessTwo(context);
        tessTwo.setImage(bitmap);
        return tessTwo.getUTF8Text();

    }
   public TessBaseAPI getTessTwo(Context context){
//        判断下是否有语言包的文件,如果没有则将语言包文件放到对应的目录中
       File languageFile = getLanguageFile(context);
       if (!languageFile.exists()) {
           IDCardLogUtil.log("没有语言包，正在复制语言包...");
           languageFile.getParentFile().mkdirs();
           try {
               InputStream is = context.getAssets().open("chi_sim.traineddata");
               FileOutputStream outputStream = new FileOutputStream(languageFile);
               int length = -1;
               byte[] buf = new byte[1024];
               while ((length = is.read(buf))!=-1) {
                   outputStream.write(buf,0,length);
               }
               outputStream.close();
               outputStream.flush();
           } catch (IOException e) {
               e.printStackTrace();
           }
       }
//        判断是否被初始话
       if (tessBaseAPI==null) {
           IDCardLogUtil.log("没有被初始话,正在初始化tessbaseapi...");
           tessBaseAPI = new TessBaseAPI();
           tessBaseAPI.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST,"1234567890xX");
           tessBaseAPI.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST,"～！￥……&×（）()~!@#$%^&*():{}【】<>?;′'\"|、|“”=+-_；：‘。《》qwertyuiopasdfghjklzcvbnmQWERTYUIOPASDFGHJKLZCVBNM");
           tessBaseAPI.init(languageFile.getParentFile().getParentFile().getPath(),"chi_sim");
       }
       return tessBaseAPI;
   }

    private File getLanguageFile(Context context){
        return new File(context.getExternalFilesDir(null),"/tessdata/chi_sim.traineddata");
    }
}
