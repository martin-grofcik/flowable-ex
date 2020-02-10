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


import org.flowable.common.engine.api.FlowableOptimisticLockingException
import org.flowable.engine.ProcessEngine
import org.flowable.engine.RepositoryService
import org.spockframework.runtime.extension.ExtensionUtil
import org.spockframework.runtime.extension.IMethodInterceptor
import org.spockframework.runtime.extension.IMethodInvocation

/**
 * author martin.grofcik
 */
class CleanTestInterceptor implements IMethodInterceptor{
    final CleanTest annotation

    CleanTestInterceptor(Deployment annotation) {
        this.annotation = annotation
    }

    void intercept(IMethodInvocation invocation) throws Throwable {
        if (!(invocation.instance instanceof PluggableFlowableSpecification)) {
            throw new FlowableSpockException(invocation.instance.class + " must extend " + PluggableFlowableSpecification.class)
        }
        List<Throwable> exceptions = new ArrayList<>()

        try {
            invocation.proceed();
        } catch (Throwable t) {
            exceptions.add(t)
        } finally {
            try {
                removeDeployments(((ProcessEngine) invocation.instance.processEngine).repositoryService)
            } catch (Throwable t) {
                exceptions.add(t)
            }
        }

        ExtensionUtil.throwAll("Exceptions during test cleanup", exceptions);
    }


    protected static void removeDeployments(RepositoryService repositoryService) {
        for (org.flowable.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
            try {
                repositoryService.deleteDeployment(deployment.getId(), true);
            } catch (FlowableOptimisticLockingException flowableOptimisticLockingException) {
                repositoryService.deleteDeployment(deployment.getId(), true);
            }
        }
    }
}
