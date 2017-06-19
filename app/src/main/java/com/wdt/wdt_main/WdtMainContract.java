package com.wdt.wdt_main;

import android.net.Uri;

import com.wdt.BasePresenter;
import com.wdt.BaseView;

import java.util.List;

/**
 * Created by motibartov on 22/05/2017.
 */

public interface WdtMainContract {

    interface View extends BaseView {
        void showBtReadings(String s1, String s2);
        void showBtConnectionState(boolean isConnected);
        void showEcuText(String s1, String s2);
    }

    interface Presenter extends BasePresenter {

        void upKey();
        void downKey();

        void plusKey();
        void minusKey();

    }

}
