package com.mdsd.tool;

public interface IdentitiyIdCardCallBack {
    void onError(String s);

    void onSuccess(IDBean idBean);

    void onIdNoTrue();

    void onFinish();
}
