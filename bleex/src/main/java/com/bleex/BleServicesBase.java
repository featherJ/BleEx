package com.bleex;

import android.content.Context;

import java.util.List;
import java.util.UUID;

/**
 * BleEx服务基类
 *
 * @author Agua.L
 */
public class BleServicesBase {

    protected BleServicesBase self;
    public BleServicesBase(Context context, UUID mainService) {
        this.self = this;
        this.context = context;
    }
}
