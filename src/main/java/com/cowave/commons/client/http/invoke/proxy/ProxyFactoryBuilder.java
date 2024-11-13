package com.cowave.commons.client.http.invoke.proxy;

import com.cowave.commons.client.http.request.meta.HttpMethodMetaParser;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.event.Level;
import com.cowave.commons.client.http.HttpExceptionHandler;
import com.cowave.commons.client.http.HttpInterceptor;
import com.cowave.commons.client.http.invoke.codec.HttpDecoder;
import com.cowave.commons.client.http.invoke.codec.HttpEncoder;
import com.cowave.commons.client.http.invoke.codec.decoder.JacksonDecoder;
import com.cowave.commons.client.http.invoke.codec.encoder.JacksonEncoder;
import com.cowave.commons.client.http.invoke.exec.HttpExecutor;
import com.cowave.commons.client.http.request.Options;
import com.cowave.commons.client.http.request.meta.HttpMethodMetaParserImpl;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author shanhuiming
 *
 */
@Data
@RequiredArgsConstructor
public class ProxyFactoryBuilder {
    private final HttpMethodMetaParser metaParser = new HttpMethodMetaParserImpl();
    private final List<HttpInterceptor> httpInterceptors = new ArrayList<>();
    private final Options options;
    private HttpEncoder encoder = new JacksonEncoder();
    private HttpDecoder decoder = new JacksonDecoder();
    private HttpExecutor httpExecutor;
    private HttpExceptionHandler exceptionHandler;

    public void addHttpInterceptor(HttpInterceptor httpInterceptor) {
        this.httpInterceptors.add(httpInterceptor);
    }

    public ProxyFactory newProxyFactory(Level level, boolean ignoreError) {
        HttpMethodInvokerFactory methodHandlerFactory = new HttpMethodInvokerFactory(
                httpExecutor, metaParser, encoder, decoder,
                options, httpInterceptors, exceptionHandler, ignoreError, level);
        return new ProxyFactory(methodHandlerFactory);
    }
}
