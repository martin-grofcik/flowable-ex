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
package org.crp.flowable.spock.internal

import org.crp.flowable.spock.util.ProcessModelBuilder
import org.flowable.engine.*
import org.flowable.engine.impl.ProcessEngineImpl
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl
import org.flowable.engine.impl.history.DefaultHistoryManager
import org.flowable.engine.impl.history.HistoryManager
import org.flowable.job.api.HistoryJob
import spock.lang.Specification

/**
 * author martin.grofcik
 */
abstract class InternalFlowableSpecification extends Specification {
    protected static List<String> deploymentIdsForAutoCleanup = new ArrayList<>()

    protected static ProcessEngineConfigurationImpl processEngineConfiguration
    protected static ProcessEngine processEngine
    protected RepositoryService repositoryService
    protected RuntimeService runtimeService
    protected TaskService taskService
    protected FormService formService
    protected HistoryService historyService
    protected IdentityService identityService
    protected ManagementService managementService
    protected DynamicBpmnService dynamicBpmnService
    protected ProcessMigrationService processMigrationService

    @SuppressWarnings("unused")
    def setupSpec() {
        if (processEngine == null) {
            processEngine = createProcessEngine()
        }
        processEngineConfiguration = ((ProcessEngineImpl) processEngine).getProcessEngineConfiguration()
    }

    protected abstract ProcessEngine createProcessEngine()

    protected static void doFinally() {
        ProcessEngineConfigurationImpl processEngineConfiguration = (ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration()
        boolean isAsyncHistoryEnabled = processEngineConfiguration.isAsyncHistoryEnabled()

        if (isAsyncHistoryEnabled) {
            ManagementService managementService = processEngine.getManagementService()
            List<HistoryJob> jobs = managementService.createHistoryJobQuery().list()
            for (HistoryJob job : jobs) {
                managementService.deleteHistoryJob(job.getId())
            }
        }

        HistoryManager asyncHistoryManager = null
        try {
            if (isAsyncHistoryEnabled) {
                processEngineConfiguration.setAsyncHistoryEnabled(false)
                asyncHistoryManager = processEngineConfiguration.getHistoryManager()
                processEngineConfiguration
                        .setHistoryManager(new DefaultHistoryManager(processEngineConfiguration,
                                processEngineConfiguration.getHistoryLevel(), processEngineConfiguration.isUsePrefixId()))
            }

            cleanDeployments(processEngine)

        } finally {

            if (isAsyncHistoryEnabled) {
                processEngineConfiguration.setAsyncHistoryEnabled(true)
                processEngineConfiguration.setHistoryManager(asyncHistoryManager)
            }

            processEngineConfiguration.getClock().reset()
        }
    }

    protected static void cleanDeployments(ProcessEngine processEngine) {
        ProcessEngineConfiguration processEngineConfiguration = processEngine.getProcessEngineConfiguration()
        for (String autoDeletedDeploymentId : deploymentIdsForAutoCleanup) {
            processEngineConfiguration.getRepositoryService().deleteDeployment(autoDeletedDeploymentId, true)
        }
        deploymentIdsForAutoCleanup.clear()
    }

    String deploy(String classpathResource) {
        def id = repositoryService.createDeployment().addClasspathResource(classpathResource).deploy().getId()
        deploymentIdsForAutoCleanup.add(id)
        return id
    }

    String deploy(ProcessModelBuilder processModelBuilder) {
        def id = repositoryService.createDeployment().
                addBpmnModel("${processModelBuilder.name}.bpmn20.xml" , processModelBuilder.build()).deploy().getId()
        deploymentIdsForAutoCleanup.add(id)
        return id
    }
}
