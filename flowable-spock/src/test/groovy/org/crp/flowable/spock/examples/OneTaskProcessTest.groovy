package org.crp.flowable.spock.examples

import org.flowable.engine.impl.test.PluggableFlowableTestCase
import org.flowable.engine.test.Deployment
import org.junit.jupiter.api.Test

class OneTaskProcessTest extends PluggableFlowableTestCase {

	@Test
	@Deployment(resources = "org/crp/flowable/spock/examples/oneTask.bpmn20.xml")
	void "start one task process"() {
		runtimeService.createProcessInstanceBuilder().
				processDefinitionKey("oneTaskProcess").
				start()

		assert runtimeService.createProcessInstanceQuery().count() == 1
	}

	@Test
	void "start one task process with method deployment"() {
		deployOneTaskTestProcess()

		runtimeService.createProcessInstanceBuilder().
				processDefinitionKey("oneTaskProcess").
				start()

		assert runtimeService.createProcessInstanceQuery().count() == 1
	}

}
