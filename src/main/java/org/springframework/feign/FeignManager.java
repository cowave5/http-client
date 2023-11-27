package org.springframework.feign;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import feign.Client;
import feign.Request;
import feign.RequestInterceptor;
import feign.codec.Encoder;
import feign.jackson.JacksonEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.feign.annotation.FeignClient;
import org.springframework.feign.codec.EJacksonDecoder;
import org.springframework.feign.codec.EJacksonEncoder;
import org.springframework.feign.codec.FeignDecoder;
import org.springframework.feign.codec.ResponseDecoder;
import org.springframework.feign.invoke.FeignBuilder;
import org.springframework.feign.invoke.FeignInvocationHandlerFactory;
import org.springframework.feign.retryer.DefaultRetryer;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;

/**
 *
 * @author shanhuiming
 *
 */
public class FeignManager {

    private static ApplicationContext applicationContext;

    private static StringValueResolver valueResolver;

    private static final Map<String, Object> localFeigns = new ConcurrentHashMap<>();

    static void setApplicationContext(ApplicationContext applicationContext){
        FeignManager.applicationContext = applicationContext;
    }

    static void setStringValueResolver(StringValueResolver valueResolver){
        FeignManager.valueResolver = valueResolver;
    }

    public static <T> T get(Class<T> clazz, String url, int connectTimeoutMillis, int readTimeoutMillis) {
        FeignClient feign = AnnotationUtils.getAnnotation(clazz, FeignClient.class);
        Assert.notNull(feign, clazz + " is not a FeignClient");

        Logger logger = LoggerFactory.getLogger(feign.logger());
        return builder(feign, new Request.Options(connectTimeoutMillis, readTimeoutMillis))
                .target(clazz, url, null, applicationContext, valueResolver, logger);
    }

    public static <T> T get(Class<T> clazz, String url) {
        FeignClient feign = AnnotationUtils.getAnnotation(clazz, FeignClient.class);
        Assert.notNull(feign, clazz + " is not a FeignClient");

        Logger logger = LoggerFactory.getLogger(feign.logger());
        return builder(feign).
                target(clazz, url, null, applicationContext, valueResolver, logger);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getCache(Class<T> clazz, String url) {
        FeignClient feign = AnnotationUtils.getAnnotation(clazz, FeignClient.class);
        Assert.notNull(feign, clazz + " is not a FeignClient");

        String key = clazz.getName() + url;
        T exist = (T) localFeigns.get(key);
        if (exist != null) {
            return exist;
        }

        Logger logger = LoggerFactory.getLogger(feign.logger());
        T created = builder(feign).
                target(clazz, url, null, applicationContext, valueResolver, logger);
        T previous = (T) localFeigns.put(key, created);
        if (previous != null) {
            return previous;
        } else {
            return created;
        }
    }

    static FeignBuilder builder(FeignClient feign) {
        int connectTimeoutMillis = getInt(feign.connectTimeoutMillis(), feign.connectTimeoutMillisStr());
        int readTimeoutMillis = getInt(feign.readTimeoutMillis(), feign.readTimeoutMillisStr());
        return builder(feign, new Request.Options(connectTimeoutMillis, readTimeoutMillis));
    }

    @SuppressWarnings("deprecation")
    static FeignBuilder builder(FeignClient feign, Request.Options options) {
        long period = getLong(feign.period(), feign.periodStr());
        long maxPeriod = getLong(feign.maxPeriod(), feign.maxPeriodStr());
        int maxAttempts = getInt(feign.maxAttempts(), feign.maxAttemptsStr());

        FeignBuilder builder = new FeignBuilder().options(options)
                .retryer(new DefaultRetryer(period, maxPeriod, maxAttempts))
                .invocationHandlerFactory(new FeignInvocationHandlerFactory());

        String[] interceptors = applicationContext.getBeanNamesForType(RequestInterceptor.class);
        if(interceptors.length > 0) {
            String interceptor = interceptors[0];
            RequestInterceptor requestInterceptor = applicationContext.getBean(interceptor, RequestInterceptor.class);
            builder.requestInterceptor(requestInterceptor);
        }

        try{
            builder.encoder(encoder(feign)).decoder(decoder(feign));
            Class<?> sslSocketFactoryClass = feign.sslSocketFactory();
            Class<?> hostnameVerifierClass = feign.hostnameVerifier();
            if (SSLSocketFactory.class.isAssignableFrom(sslSocketFactoryClass)) {
                SSLSocketFactory sslSocketFactory;
                if (StringUtils.hasText(feign.sslCertPath()) && StringUtils.hasText(feign.sslPasswd())) {
                    Constructor<?> constructor = sslSocketFactoryClass.getConstructor(String.class, String.class);
                    sslSocketFactory = (SSLSocketFactory) constructor.newInstance(feign.sslCertPath(), feign.sslPasswd());
                } else {
                    sslSocketFactory = (SSLSocketFactory) sslSocketFactoryClass.newInstance();
                }

                if (!HostnameVerifier.class.isAssignableFrom(hostnameVerifierClass)) {
                    builder.client(new Client.Default(sslSocketFactory, null));
                } else {
                    builder.client(new Client.Default(sslSocketFactory, (HostnameVerifier) hostnameVerifierClass.newInstance()));
                }
            }
        }catch(Exception e){
            throw new RuntimeException(e);
        }
        return builder;
    }

    @SuppressWarnings("deprecation")
    static Encoder encoder(FeignClient feign) throws Exception {
        if (JacksonEncoder.class.isAssignableFrom(feign.encoder())) {
            ObjectMapper encoderMapper = new ObjectMapper();
            encoderMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL).configure(SerializationFeature.INDENT_OUTPUT, true)
                    .registerModules(Collections.emptyList());
            encoderMapper.setTimeZone(TimeZone.getDefault());
            encoderMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                    .configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
            return new EJacksonEncoder(encoderMapper);
        } else {
            return (Encoder) feign.encoder().newInstance();
        }
    }

    static FeignDecoder decoder(FeignClient feign){
        ObjectMapper decoderMapper = new ObjectMapper();
        decoderMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false)
                .registerModules(Collections.emptyList());
        decoderMapper.setTimeZone(TimeZone.getDefault());

        if(ResponseDecoder.class == feign.decoder()){
            return new ResponseDecoder(decoderMapper);
        }else{
            return new EJacksonDecoder(decoderMapper);
        }
    }

    private static int getInt(int defaultValue, String regex) {
        if (!StringUtils.hasText(regex)) {
            return defaultValue;
        }
        return Integer.parseInt(Objects.requireNonNull(valueResolver.resolveStringValue(regex)));
    }

    private static long getLong(long defaultValue, String regex) {
        if (!StringUtils.hasText(regex)) {
            return defaultValue;
        }
        return Long.parseLong(Objects.requireNonNull(valueResolver.resolveStringValue(regex)));
    }
}
