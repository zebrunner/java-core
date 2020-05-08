package com.zebrunner.agent.core.registrar;

import com.zebrunner.agent.core.config.ConfigurationHolder;

/**
 * Core Zebrunner Agent API allowing to track test run events in Zebrunner
 */
public interface TestRunRegistrar {

    /**
     * Factory method allowing to obtain Zebrunner test run registrar
     * @return Zebrunner registrar instance
     */
    static TestRunRegistrar registrar() {
        return ConfigurationHolder.isEnabled() ? ReportingRegistrar.getInstance() : NoOpRegistrar.getInstance();
    }

    /**
     * Factory method allowing to obtain registrar built for debug purposes
     * @return debug registrar instance providing output to stdout
     */
    static TestRunRegistrar stdoutRegistrar() {
        return StdoutRegistrar.getInstance();
    }

    /**
     * Registers test run start
     * @param testRunStartDescriptor test run start descriptor capturing state at the beginning of the run
     */
    void start(TestRunStartDescriptor testRunStartDescriptor);

    /**
     * Registers test run finish
     * @param testRunFinishDescriptor test run finish descriptor capturing state at the end of the run
     */
    void finish(TestRunFinishDescriptor testRunFinishDescriptor);

    /**
     * Registers test start
     * @param uniqueId key that uniquely identifies specific test in scope of test run.
     *                 This value will be used later for test finish registration
     * @param testStartDescriptor test start descriptor
     */
    void startTest(String uniqueId, TestStartDescriptor testStartDescriptor);

    /**
     * Registers test finish
     * @param uniqueId key that uniquely identifies specific test in scope of test run.
     *                 Appropriate test start with matching id should be registered prior to test finish registration,
     *                 otherwise test won't be properly registered
     * @param testFinishDescriptor test result descriptor
     */
    void finishTest(String uniqueId, TestFinishDescriptor testFinishDescriptor);

}