package com.cowave.commons.client.http.trace;

import com.cowave.commons.client.http.annotation.HttpClient;
import com.cowave.commons.client.http.annotation.HttpHeaders;
import com.cowave.commons.client.http.annotation.HttpLine;
import com.cowave.commons.client.http.response.HttpResponse;

import static com.cowave.commons.client.http.constants.HttpHeader.X_Request_ID;

/**
 *
 * @author shanhuiming
 *
 */
@HttpClient(url = "http://127.0.0.1:8080")
public interface TraceMockService {

    @HttpHeaders({X_Request_ID + ": 12345"})
    @HttpLine("TRACE /api/v1/trace")
    HttpResponse<String> trace();
}
