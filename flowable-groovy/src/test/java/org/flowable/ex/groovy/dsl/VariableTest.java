package org.flowable.ex.groovy.dsl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

public class VariableTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    void getSetVariableInExecutionWithMetaClassExtension() {
        ProcessInstance pi = runtimeService.createProcessInstanceBuilder().
                processDefinitionKey("scriptTaskProcess").
                variables(Collections.singletonMap("inputVariable", "inputVariableValue")).
                start();

        assertThat(runtimeService.hasVariable(pi.getId(), "newVariable")).isTrue();
        assertThat(runtimeService.getVariable(pi.getId(), "newVariable")).isEqualTo("inputVariableValue");
    }

    @Test
    @Deployment
    void getSetVariableInTaskScopeWithMetaClassExtension() {
        ProcessInstance pi = runtimeService.createProcessInstanceBuilder().
                processDefinitionKey("userTaskWithScriptListenerProcess").
                variables(Collections.singletonMap("inputVariable", "inputVariableValue")).
                start();
        Task task = taskService.createTaskQuery().includeTaskLocalVariables().processInstanceId(pi.getId()).singleResult();

        assertThat(taskService.hasVariable(task.getId(), "newVariable")).isTrue();
        assertThat(taskService.getVariable(task.getId(), "newVariable")).isEqualTo("inputVariableValue");
    }

}
