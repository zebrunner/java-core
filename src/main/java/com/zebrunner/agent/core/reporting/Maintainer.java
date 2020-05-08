package com.zebrunner.agent.core.reporting;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {
        ElementType.METHOD,
        ElementType.TYPE
})
public @interface Maintainer {

    String value();

}