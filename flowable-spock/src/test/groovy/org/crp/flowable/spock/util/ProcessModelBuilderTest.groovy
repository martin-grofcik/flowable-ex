package org.crp.flowable.spock.util

import org.flowable.bpmn.model.BpmnModel
import org.flowable.engine.impl.test.PluggableFlowableTestCase
import org.junit.jupiter.api.Test

import static org.crp.flowable.spock.util.ProcessModelBuilder.*

class ProcessModelBuilderTest extends PluggableFlowableTestCase{

    @Test
    void "list model create"() {
        assertThatModelWorks model('oneTaskProcess', [startEvent(id: 'start'), userTask(id: 'userTask'), endEvent(id: 'end')])
    }

    @Test
    void 'rightShift model create'() {
        assertThatModelWorks( (model('oneTaskProcess') >>
               startEvent(id: 'start') >> userTask(id: 'userTask') >> endEvent(id: 'end'))
        .build())
    }

    void assertThatModelWorks(BpmnModel model) {
        def deployment = repositoryService.createDeployment().addBpmnModel('oneTaskProcess.bpmn20.xml', model).deploy()
        try {
            def process = runtimeService.createProcessInstanceBuilder().processDefinitionKey('oneTaskProcess').start()
            assert taskService.createTaskQuery().processInstanceId(process.id).singleResult().taskDefinitionKey == 'userTask'
        } finally {
            repositoryService.deleteDeployment(deployment.id, true)
        }
    }
}
