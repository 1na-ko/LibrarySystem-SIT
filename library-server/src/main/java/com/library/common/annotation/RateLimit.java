package com.library.common.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    String key() default "";
    int limit() default 10;
    int window() default 60;
    TimeUnit timeUnit() default TimeUnit.SECONDS;
    String message() default "操作过于频繁，请稍后重试";
}
