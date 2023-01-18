package com.bleex.sample.ble;

import java.util.UUID;

public class BleUUIDs {
    /* --------------- service uuid --------------- */
    /**
     * The uuid of service 1
     */
    public static final UUID SERVICE_1 = UUID.fromString("10000000-0001-0000-0000-000000000000");
    /**
     * The uuid of service 2
     */
    public static final UUID SERVICE_2 = UUID.fromString("10000000-0002-0000-0000-000000000000");
    /**
     * A uuid of characteristic used to authenticate the central device
     */
    public static final UUID VERIFY_CENTRAL = UUID.fromString("10000000-1010-0000-0000-000000000000");

    /* --------------- base characteristics --------------- */
    /**
     * A uuid of characteristic used to test reading from peripheral
     */
    public static final UUID READ_TEST = UUID.fromString("10000001-0001-0000-0000-000000000000");
    /**
     * A uuid of characteristic used to test writing from peripheral
     */
    public static final UUID WRITE_TEST = UUID.fromString("10000001-0002-0000-0000-000000000000");
    /**
     * A uuid of characteristic used to test indicate from peripheral device
     */
    public static final UUID NOTIFY_TEST = UUID.fromString("10000001-0003-0000-0000-000000000000");
    /**
     * A uuid of characteristic used to test indicate from peripheral device
     */
    public static final UUID INDICATE_TEST = UUID.fromString("10000001-0004-0000-0000-000000000000");

    /* --------------- bleex characteristics --------------- */
    /**
     * A uuid of characteristic used to test requesting data under mtu limited from peripheral device
     */
    public static final UUID REQUEST_TEST = UUID.fromString("10000002-0001-0000-0000-000000000000");
    /**
     * A uuid of characteristic used to test requesting large data from peripheral device
     */
    public static final UUID REQUEST_LARGE_TEST = UUID.fromString("10000002-0002-0000-0000-000000000000");
    /**
     * A uuid of characteristic used to test receiving large data from central device
     */
    public static final UUID WRITE_LARGE_TEST = UUID.fromString("10000002-0003-0000-0000-000000000000");
    /**
     * A uuid of characteristic used to test writing large data to central device
     */
    public static final UUID INDICATE_LARGE_TEST = UUID.fromString("10000002-0004-0000-0000-000000000000");












}
