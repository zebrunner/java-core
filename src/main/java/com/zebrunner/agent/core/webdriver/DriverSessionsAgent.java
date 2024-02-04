package com.zebrunner.agent.core.webdriver;

import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.FieldProxy;
import net.bytebuddy.implementation.bind.annotation.Morph;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.NameMatcher;
import net.bytebuddy.pool.TypePool;
import org.openqa.selenium.remote.CommandInfo;
import org.openqa.selenium.remote.HttpCommandExecutor;
import org.openqa.selenium.remote.http.ClientConfig;
import org.openqa.selenium.remote.http.HttpClient;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static net.bytebuddy.implementation.MethodDelegation.to;
import static net.bytebuddy.matcher.ElementMatchers.any;
import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.isStatic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

@Slf4j
public class DriverSessionsAgent {

    private static final String REMOTE_WEB_DRIVER_CLASS_MAME = "org.openqa.selenium.remote.RemoteWebDriver";
    private static final String APPIUM_WEB_DRIVER_CLASS_MAME = "io.appium.java_client.AppiumDriver";

    private static final String START_SESSION_METHOD_MAME = "startSession";
    private static final String QUIT_METHOD_MAME = "quit";

    // getSessionId and getCapabilities are used by the agent interceptors
    private static final Set<String> PUBLIC_METHODS_TO_NOT_INTERCEPT = new HashSet<>(Arrays.asList(
            START_SESSION_METHOD_MAME, QUIT_METHOD_MAME, "getSessionId", "getCapabilities", "getCommandExecutor",
            "wait", "equals", "hashCode", "getClass", "notify", "notifyAll", "toString"
    ));
    // the rest of the public methods
    // setFileDetector, getErrorHandler, setErrorHandler, getTitle, getCurrentUrl, getScreenshotAs, findElements,
    // findElement, findElementById, findElementsById, findElementByLinkText, findElementsByLinkText,
    // findElementByPartialLinkText, findElementsByPartialLinkText, findElementByTagName, findElementsByTagName,
    // findElementByName, findElementsByName, findElementByClassName, findElementsByClassName, findElementByCssSelector,
    // findElementsByCssSelector, findElementByXPath, findElementsByXPath, getPageSource, getWindowHandles,
    // getWindowHandle, executeScript, executeAsyncScript, switchTo, navigate, manage, setLogLevel, perform,
    // resetInputState, getKeyboard, getMouse, getFileDetector, get, close

    public static void premain(String args, Instrumentation instrumentation) {
        try {
            new AgentBuilder.Default()
                    .with(new AgentBuilder.InitializationStrategy.SelfInjection.Eager())
                    .type(named(REMOTE_WEB_DRIVER_CLASS_MAME))
                    .transform((builder, type, classloader, module, protectionDomain) -> addInterceptors(builder))
                    // if ** <- AppiumDriver is created, then the startSession method in RemoteWebDriver is not called
                    .type(named(APPIUM_WEB_DRIVER_CLASS_MAME))
                    .transform((builder, type, classloader, module, protectionDomain) ->
                            builder.method(named(START_SESSION_METHOD_MAME))
                                    .intercept(to(startSessionInterceptor())))
                    .type(named("org.openqa.selenium.remote.HttpCommandExecutor"))
                    .transform((builder, typeDescription, classLoader, module, protectionDomain) -> builder.
                            defineConstructor(Modifier.PUBLIC)
                            .withParameters(Map.class, ClientConfig.class, HttpClient.Factory.class)
                            .intercept(MethodDelegation.withDefaultConfiguration().withBinders(
                                    Morph.Binder.install(HttpCommandExecutor.class)
                            ).to(HttpCommandExecutorInterceptor.class)))
                    .installOn(instrumentation);
        } catch (Exception e) {
            log.error("Could not add interceptors for RemoteWebDriver", e);
        }
    }

    public static ElementMatcher<? super MethodDescription> isPublicMethodToIntercept() {
        return isPublic()
                .and(not(isStatic()))
                .and(not(new NameMatcher<>(PUBLIC_METHODS_TO_NOT_INTERCEPT::contains)));
    }

    private static DynamicType.Builder<?> addInterceptors(DynamicType.Builder<?> builder) {
        return builder.method(isPublicMethodToIntercept())
                .intercept(to(publicMethodsInterceptor()))
                .method(named(START_SESSION_METHOD_MAME))
                .intercept(to(startSessionInterceptor()))
                .method(named(QUIT_METHOD_MAME))
                .intercept(to(quitSessionInterceptor()));
    }

    private static TypeDescription publicMethodsInterceptor() {
        return TypePool.Default.ofSystemLoader()
                .describe(PublicMethodInvocationInterceptor.class.getName())
                .resolve();
    }

    private static TypeDescription startSessionInterceptor() {
        return TypePool.Default.ofSystemLoader()
                .describe(StartSessionInterceptor.class.getName())
                .resolve();
    }

    private static TypeDescription quitSessionInterceptor() {
        return TypePool.Default.ofSystemLoader()
                .describe(QuitSessionInterceptor.class.getName())
                .resolve();
    }

}
