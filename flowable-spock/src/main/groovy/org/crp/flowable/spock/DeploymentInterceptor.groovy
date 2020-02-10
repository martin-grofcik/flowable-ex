/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.crp.flowable.spock

import org.apache.commons.lang3.StringUtils
import org.flowable.common.engine.api.FlowableObjectNotFoundException
import org.flowable.engine.ProcessEngine
import org.flowable.engine.repository.DeploymentBuilder
import org.spockframework.runtime.extension.IMethodInterceptor
import org.spockframework.runtime.extension.IMethodInvocation

/**
 * author martin.grofcik
 */
class DeploymentInterceptor implements IMethodInterceptor{
    final Deployment annotation

    DeploymentInterceptor(Deployment annotation) {
        this.annotation = annotation
    }

    void intercept(IMethodInvocation invocation) throws Throwable {
        if (!(invocation.instance instanceof PluggableFlowableSpecification)) {
            throw new FlowableSpockException(invocation.instance.class + " must extend " + PluggableFlowableSpecification.class)
        }
        String deploymentId = null
        try {
            deploymentId = deployResources(invocation)
            invocation.proceed()
        } finally {
            removeDeployment(deploymentId, (ProcessEngine) invocation.instance.processEngine)
        }
    }

    String deployResources(IMethodInvocation invocation) {
        DeploymentBuilder deploymentBuilder = invocation.instance.processEngine.getRepositoryService().createDeployment().name(invocation.instance.getClass().getSimpleName() + "." + invocation.feature.name)

        for (String resource : annotation.resources()) {
            deploymentBuilder.addClasspathResource(resource)
        }

        if (StringUtils.isNotEmpty(annotation.tenantId())) {
            deploymentBuilder.tenantId(annotation.tenantId())
        }

        return deploymentBuilder.deploy().getId()

    }

    @SuppressWarnings("GroovyUnusedCatchParameter")
    static void removeDeployment(String deploymentId, ProcessEngine processEngine) {
        try {
            processEngine.getRepositoryService().deleteDeployment(deploymentId, true)

        } catch (FlowableObjectNotFoundException e) {
            // Deployment was already deleted by the test case. Ignore.
        }

    }
}
