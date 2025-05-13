package com.att.audio.server.util;

import lombok.Builder;
import lombok.Data;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class Authentication {

    public enum Method {
        GET, POST, PATCH, PUT, DELETE
    }

    @Data
    @Builder
    public static class AuthParam {
        private String apiKey;
        private String apiSecret;
        private String reqUrl;//请求URl
        private Method method;//URl的请求方法()
        private String data;

        public void valid() {
            if (this.apiKey == null || this.apiKey.isEmpty()) {
                throw new IllegalArgumentException("api key must not been empty!");
            }
            if (this.apiSecret == null || this.apiSecret.isEmpty()) {
                throw new IllegalArgumentException("api secret must not been empty!");
            }
            if (this.reqUrl == null || this.reqUrl.isEmpty()) {
                throw new IllegalArgumentException("request url must not been empty!");
            }
            if (this.method == null) {
                throw new IllegalArgumentException("method must not been empty!");
            }
        }
    }

    @Data
    public static class AUthResult {
        private String digest;
        private String date;
        private String authorization;
    }

    public static AUthResult auth(AuthParam param) {
        try {
            param.valid();
            SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            AUthResult result = new AUthResult();
            // System.err.println(format.format(new Date()));
            result.setDate(format.format(new Date()));
            result.setDigest("SHA-256=" + signBody());
            result.setAuthorization(getAuthorization(generateSignature(result.digest, result.date, param), param.apiKey));
            return result;
        } catch (Exception ex) {
            throw new RuntimeException("auth is error", ex);
        }
    }

    private static String signBody() throws Exception {
        String encodestr = "";
        try {
            byte[] tempBytes=MessageDigest.getInstance("SHA-256").digest();
            encodestr = Base64.getEncoder().encodeToString(tempBytes);
        } catch (NoSuchAlgorithmException e) {
            throw e;
        }
        return encodestr;
    }

    // 第0步
    private static String generateSignature(String digest, String date, AuthParam param) throws Exception {
        URL url = new URL(param.reqUrl);
        StringBuilder builder = new StringBuilder("host: ").append(url.getHost());
        if (url.getPort() != -1) {
            builder.append(":").append(url.getPort());
        }
        builder.append("\n").//
                append("date: ").append(date).append("\n").//
                append(param.method + " ").append(url.getPath()).append(" HTTP/1.1").append("\n").//
                append("digest: ").append(digest);
        // System.err.println(builder);
        return hmacsign(builder.toString(), param.apiSecret);
    }

    private static String getAuthorization(String sign, String aipKey) {
        return String.format("hmac api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"", //
                aipKey, "hmac-sha256", "host date request-line digest", sign);
    }

    // 第1步
    private static String hmacsign(String signature, String apiSecret) throws Exception {
        // System.err.println("hmacsign里打印的"+signature);
        Mac macInstance= Mac.getInstance("hmacsha256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(apiSecret.getBytes("UTF-8"), "hmacsha256");
        macInstance.init(secretKeySpec);
        byte[] hexDigits =   macInstance.doFinal(signature.getBytes("UTF-8"));
        // System.err.println("hmacsign最终结果"+Base64.getEncoder().encodeToString(hexDigits));
        return Base64.getEncoder().encodeToString(hexDigits);
    }
}
