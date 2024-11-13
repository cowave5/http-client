package com.cowave.commons.client.http.request;

import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author shanhuiming
 *
 */
@Data
@RequiredArgsConstructor
public class Options {
    private final int connectTimeout;
    private final int readTimeout;
    private final int retryTimes;
    private final int retryInterval;

    public Options() {
        this(10000, 601000, 1, 1000);
    }
}
