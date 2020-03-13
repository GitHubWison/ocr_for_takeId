package com.mdsd.tool;

import android.graphics.Point;

public class IdcardPositionBean {

    public int x;
    public int y;
    public int width;
    public int height;

    public IdcardPositionBean(Point idNoTopLeftPoint, int idNoHeight, float leftRate, float topRate, float rightRate, float bottomRate) {
        float idNoHeightf = (float) idNoHeight;
        this.x = (int) (idNoTopLeftPoint.x - idNoHeightf * leftRate);
        this.y = (int) (idNoTopLeftPoint.y - idNoHeightf * topRate);
        this.width = (int) (Math.abs(leftRate - rightRate) * idNoHeightf);
        this.height = (int) (Math.abs(topRate - bottomRate) * idNoHeightf);
    }
}
