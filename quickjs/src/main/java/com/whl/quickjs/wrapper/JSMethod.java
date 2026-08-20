package com.whl.quickjs.wrapper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记 Java 方法可被 QuickJS 反射调用（上游 TVBoxOS crawler/js 依赖此注解）。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface JSMethod {
}
