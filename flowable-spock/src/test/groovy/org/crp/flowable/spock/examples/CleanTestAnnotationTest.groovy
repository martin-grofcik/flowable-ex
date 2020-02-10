package org.crp.flowable.spock.examples

import org.flowable.common.engine.impl.test.CleanTest
import org.flowable.engine.impl.test.PluggableFlowableTestCase
import org.junit.jupiter.api.Test

/**
 * @author martin.grofcik
 */
class CleanTestAnnotationTest extends PluggableFlowableTestCase {

	@Test
	@CleanTest
	void "apply clean test annotation"() {
		deployOneTaskTestProcess()
		assert repositoryService.createDeploymentQuery().count() == 1
		deploymentIdsForAutoCleanup.remove(0)
	}

}
