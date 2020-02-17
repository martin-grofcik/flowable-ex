package org.flowable.ex.groovy.dsl

import org.flowable.engine.impl.test.PluggableFlowableTestCase
import org.flowable.engine.runtime.ProcessInstance
import org.flowable.engine.test.Deployment
import org.flowable.task.api.Task
import org.junit.jupiter.api.Test

class VariableTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    void getSetVariableInExecutionWithMetaClassExtension() {
        def processInstance = runtimeService.createProcessInstanceBuilder().
                processDefinitionKey("scriptTaskProcess").
                variables(["inputVariable": "inputVariableValue"]).
                start()

        assert runtimeService.hasVariable(processInstance.id, "newVariable")
        assert runtimeService.getVariable(processInstance.id, "newVariable") == "inputVariableValue"
    }

    @Test
    @Deployment
    void getSetVariableInTaskScopeWithMetaClassExtension() {
        ProcessInstance pi = runtimeService.createProcessInstanceBuilder().
                processDefinitionKey("userTaskWithScriptListenerProcess").
                variables(["inputVariable": "inputVariableValue"]).
                start()
        Task task = taskService.createTaskQuery().includeTaskLocalVariables().processInstanceId(pi.id).singleResult()

        assert taskService.hasVariable(task.getId(), "newVariable")
        assert taskService.getVariable(task.getId(), "newVariable") == "inputVariableValue"
    }

}
