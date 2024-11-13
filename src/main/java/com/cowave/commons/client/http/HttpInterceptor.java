package com.cowave.commons.client.http;

import com.cowave.commons.client.http.request.HttpRequest;

/**
 *
 * @author shanhuiming
 *
 */
public interface HttpInterceptor {

    void apply(HttpRequest httpRequest);
}
