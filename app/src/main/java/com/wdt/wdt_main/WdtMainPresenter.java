package com.wdt.wdt_main;

import android.content.Context;

import com.wdt.bt_manager.WdtBtManager;

/**
 * Created by motibartov on 19/06/2017.
 */

public class WdtMainPresenter implements WdtMainContract.Presenter , WdtBtManager.WdtBtEventListener {

    Context mContext;
    WdtMainContract.View mMainView;
    WdtBtManager btManager;
    boolean mIsBtConnected;

    public WdtMainPresenter(Context context, WdtMainContract.View mainView){
        mContext = context;
        mMainView = mainView;
        btManager = new WdtBtManager(mContext, null, this);
    }


    @Override
    public void subscribe() {

    }

    @Override
    public void unSubscribe() {

    }

    @Override
    public void upKey() {
        byte[] raw = {(byte) 0xde, (byte) 0xdf, (byte) 0x70,
                (byte) 0x43, (byte) 0xb3};
        btManager.writeBtdata(raw);
    }

    @Override
    public void downKey() {
        byte[] raw = {(byte) 0xde, (byte) 0xdf, (byte) 0x70,
                (byte) 0x44, (byte) 0xb4};
        btManager.writeBtdata(raw);
    }

    @Override
    public void plusKey() {
        byte[] raw = {(byte) 0xde, (byte) 0xdf, (byte) 0x70,
                (byte) 0x41, (byte) 0xb1};
        btManager.writeBtdata(raw);
    }

    @Override
    public void minusKey() {
        byte[] raw = {(byte) 0xde, (byte) 0xdf, (byte) 0x70,
                (byte) 0x42, (byte) 0xb2};
        btManager.writeBtdata(raw);

    }

    @Override
    public void onBtAdapterUnavailable() {

    }

    @Override
    public void onBtConnected(boolean isConnected) {
        mMainView.showBtConnectionState(isConnected);
        mIsBtConnected = isConnected;
    }



    @Override
    public void onEcuData(String s1, String s2) {

    }
}
