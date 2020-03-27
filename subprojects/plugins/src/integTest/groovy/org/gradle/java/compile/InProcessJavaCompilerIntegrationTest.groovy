/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
package org.gradle.java.compile

class InProcessJavaCompilerIntegrationTest extends JavaCompilerIntegrationSpec {
    def compilerConfiguration() {
        '''
compileJava.options.with {
    fork = false
}
'''
    }

    def logStatement() {
        "Java compiler API"
    }

    def "compile with target compatibility"() {
        given:
        goodCode()
        buildFile << """
java.targetCompatibility = JavaVersion.VERSION_1_9 // ignored
compileJava.targetCompatibility = '1.8'
compileJava.sourceCompatibility = '1.8'
compileJava {
    doFirst {
        assert configurations.apiElements.attributes.getAttribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE) == 8
        assert configurations.runtimeElements.attributes.getAttribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE) == 8
        assert configurations.compileClasspath.attributes.getAttribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE) == 8
        assert configurations.runtimeClasspath.attributes.getAttribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE) == 8
    }
}
"""

        expect:
        succeeds 'compileJava'
        bytecodeVersion() == 52
    }

    def "compile with target compatibility set in plugin extension"() {
        given:
        goodCode()
        buildFile << """
java.targetCompatibility = JavaVersion.VERSION_1_8
java.sourceCompatibility = JavaVersion.VERSION_1_8
compileJava {
    doFirst {
        assert configurations.apiElements.attributes.getAttribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE) == 8
        assert configurations.runtimeElements.attributes.getAttribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE) == 8
        assert configurations.compileClasspath.attributes.getAttribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE) == 8
        assert configurations.runtimeClasspath.attributes.getAttribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE) == 8
    }
}
"""

        expect:
        succeeds 'compileJava'
        bytecodeVersion() == 52
    }
}
