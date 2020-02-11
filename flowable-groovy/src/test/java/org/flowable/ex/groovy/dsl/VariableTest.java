package org.flowable.ex.groovy.dsl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;

public class VariableTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    void getSetVariableWithMetaClassExtension() {
        ProcessInstance pi = runtimeService.createProcessInstanceBuilder().
                processDefinitionKey("scriptTaskProcess").
                variables(Collections.singletonMap("inputVariable", "inputVariableValue")).
                start();

        assertThat(runtimeService.hasVariable(pi.getId(), "newVariable")).isTrue();
        assertThat(runtimeService.getVariable(pi.getId(), "newVariable")).isEqualTo("inputVariableValue");
    }

}
