package com.mdsd.tool;


public class IdentityIdCardConst {
    //身份证号码长宽比参考误差
    private static final float ID_NO_RATE_MIN_REFER = 11.09f;
    private static final float ID_NO_RATE_MAX_REFER = 11.91f;
    //    身份证号码长宽比标准
    static final float ID_NO_RATE_STANDARD_REFER = 11.66f;

    static boolean isIdNoRateInRange(float rate) {
        return (rate >= ID_NO_RATE_MIN_REFER) && (rate <= ID_NO_RATE_MAX_REFER);

    }

}
