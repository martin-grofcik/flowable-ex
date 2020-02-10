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

import org.spockframework.runtime.extension.ExtensionAnnotation

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * author martin.grofcik
 */
@Retention(RetentionPolicy.RUNTIME)
@Target([ElementType.METHOD])
@ExtensionAnnotation(DeploymentExtension.class)
@interface Deployment {
	/** Specify resources that make up the process definition. */
	String[] resources() default [];

	/** Specify tenantId to deploy for */
	String tenantId() default "";

}
