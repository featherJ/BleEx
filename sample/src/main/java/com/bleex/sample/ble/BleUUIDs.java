package com.bleex.sample.ble;

import java.util.UUID;

public class BleUUIDs {
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
    public static final UUID VERIFY_CENTRAL = UUID.fromString("10000001-0000-0000-0000-000000000000");
    /**
     * A uuid of characteristic used to test notifying from peripheral device
     */
    public static final UUID BASE_NOTIFY_TEST = UUID.fromString("10000002-0000-0000-0000-000000000000");
    /**
     * A uuid of characteristic used to test reading from peripheral
     */
    public static final UUID BASE_READ_TEST = UUID.fromString("10000003-0000-0000-0000-000000000000");
    /**
     * A uuid of characteristic used to test writing from peripheral
     */
    public static final UUID BASE_WRITE_TEST = UUID.fromString("10000004-0000-0000-0000-000000000000");
    /**
     * A uuid of characteristic used to test requesting data under mtu limited from peripheral device
     */
    public static final UUID REQUEST_DATA_TEST = UUID.fromString("10000005-0000-0000-0000-000000000000");
    /**
     * A uuid of characteristic used to test receiving large data from central device
     */
    public static final UUID WRITE_LARGE_DATA_TO_PERIPHERAL_TEST = UUID.fromString("10000006-0000-0000-0000-000000000000");
    /**
     * A uuid of characteristic used to test writing large data to central device
     */
    public static final UUID WRITE_LARGE_DATA_TO_CENTRAL_TEST = UUID.fromString("10000007-0000-0000-0000-000000000000");
    /**
     * A uuid of characteristic used to test requesting large data from peripheral device
     */
    public static final UUID REQUEST_LARGE_DATA_TEST = UUID.fromString("10000008-0000-0000-0000-000000000000");
}
