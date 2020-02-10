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

import org.crp.flowable.spock.internal.AbstractFlowableTestCase
import org.crp.flowable.spock.internal.InternalFlowableSpecification
import org.flowable.common.engine.api.FlowableException
import org.flowable.common.engine.impl.cfg.CommandExecutorImpl
import org.flowable.common.engine.impl.interceptor.CommandExecutor
import org.flowable.common.engine.impl.interceptor.CommandInterceptor
import org.flowable.engine.ProcessEngine
import org.flowable.engine.ProcessEngines
import org.flowable.engine.impl.interceptor.CommandInvoker
import org.flowable.engine.impl.interceptor.LoggingExecutionTreeCommandInvoker
import org.flowable.engine.test.EnableVerboseExecutionTreeLogging
import org.junit.platform.commons.support.AnnotationSupport

abstract class PluggableFlowableSpecification extends InternalFlowableSpecification {

    private static final String PROCESS_ENGINE = "cachedProcessEngine"

    @SuppressWarnings("unused")
    def setup() throws Exception {
        repositoryService = processEngine.getRepositoryService()
        runtimeService = processEngine.getRuntimeService()
        taskService = processEngine.getTaskService()
        formService = processEngine.getFormService()
        historyService = processEngine.getHistoryService()
        identityService = processEngine.getIdentityService()
        managementService = processEngine.getManagementService()
        dynamicBpmnService = processEngine.getDynamicBpmnService()
        processMigrationService = processEngine.getProcessMigrationService()
    }

    @SuppressWarnings("unused")
    def cleanup() throws Exception {
        // Always reset authenticated user to avoid any mistakes
        processEngine.getIdentityService().setAuthenticatedUserId(null)

        try {
            AbstractFlowableTestCase.validateHistoryData(processEngine)
        } finally {
            doFinally()
        }
    }

    protected ProcessEngine createProcessEngine() {
        def engine = ProcessEngines.getProcessEngine(PROCESS_ENGINE)
        if (engine == null) {
            engine = initializeProcessEngine()
        }

        // Enable verbose execution tree debugging if needed
        if (AnnotationSupport.isAnnotated(this.getClass(), EnableVerboseExecutionTreeLogging.class)) {
            swapCommandInvoker(engine, true)
        }
        return engine
    }

    protected static ProcessEngine initializeProcessEngine() {
        ProcessEngines.destroy() // Just to be sure we're not getting any previously cached version

        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine()
        if (processEngine == null) {
            throw new FlowableException("no default process engine available")
        }
        return processEngine
    }

    protected static void swapCommandInvoker(ProcessEngine processEngine, boolean debug) {
        CommandExecutor commandExecutor = processEngine.getProcessEngineConfiguration().getCommandExecutor()
        if (commandExecutor instanceof CommandExecutorImpl) {
            CommandExecutorImpl commandExecutorImpl = (CommandExecutorImpl) commandExecutor

            CommandInterceptor previousCommandInterceptor = null
            CommandInterceptor commandInterceptor = commandExecutorImpl.getFirst()

            while (commandInterceptor != null) {

                boolean matches = debug ? (commandInterceptor instanceof CommandInvoker) : (commandInterceptor instanceof LoggingExecutionTreeCommandInvoker)
                if (matches) {

                    CommandInterceptor commandInvoker = debug ? new LoggingExecutionTreeCommandInvoker() : new CommandInvoker()
                    if (previousCommandInterceptor != null) {
                        previousCommandInterceptor.setNext(commandInvoker)
                    } else {
                        commandExecutorImpl.setFirst(previousCommandInterceptor)
                    }
                    break

                } else {
                    previousCommandInterceptor = commandInterceptor
                    commandInterceptor = commandInterceptor.getNext()
                }
            }

        }
    }

}
