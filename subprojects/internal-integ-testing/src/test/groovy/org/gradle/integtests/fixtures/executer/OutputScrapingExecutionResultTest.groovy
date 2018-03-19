/*
 * Copyright 2018 the original author or authors.
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

package org.gradle.integtests.fixtures.executer

import spock.lang.Unroll

class OutputScrapingExecutionResultTest extends AbstractExecutionResultTest {
    def "can have empty output"() {
        def result = OutputScrapingExecutionResult.from("", "")

        expect:
        result.output.empty
        result.normalizedOutput.empty
        result.error.empty
    }

    def "normalizes line ending in output"() {
        def output = "\n\r\nabc\r\n\n"
        def error = "\r\n\nerror\n\r\n"
        def result = OutputScrapingExecutionResult.from(output, error)

        expect:
        result.output == "\n\nabc\n\n"
        result.normalizedOutput == "\n\nabc\n\n"
        result.error == "\n\nerror\n\n"
    }

    def "retains trailing line ending in output"() {
        def output = "\n\nabc\n"
        def error = "\nerror\n"
        def result = OutputScrapingExecutionResult.from(output, error)

        expect:
        result.output == output
        result.normalizedOutput == output
        result.error == error
    }

    def "can assert build output is present in main content"() {
        def output = """
message
message 2

BUILD SUCCESSFUL in 12s

post build
"""
        when:
        def result = OutputScrapingExecutionResult.from(output, "")

        then:
        result.assertOutputContains("message")
        result.assertOutputContains("\nmessage\nmessage 2")
        result.assertOutputContains("\nmessage\nmessage 2\n")
        result.assertOutputContains("\r\nmessage\r\nmessage 2")

        when:
        result.assertOutputContains("missing")

        then:
        def e = thrown(AssertionError)
        error(e).startsWith(error('''
            Did not find expected text in build output.
            Expected: missing
             
            Build output:
            =======
             
            message
            message 2
             
            Output:
        '''))

        when:
        result.assertOutputContains("post build")

        then:
        def e2 = thrown(AssertionError)
        error(e2).startsWith(error('''
            Did not find expected text in build output.
            Expected: post build
             
            Build output:
            =======
             
            message
            message 2
             
            Output:
        '''))

        when:
        result.assertOutputContains("BUILD")

        then:
        def e3 = thrown(AssertionError)
        error(e3).startsWith(error('''
            Did not find expected text in build output.
            Expected: BUILD
             
            Build output:
            =======
             
            message
            message 2
             
            Output:
        '''))

        when:
        result.assertOutputContains("message extra")

        then:
        def e4 = thrown(AssertionError)
        error(e4).startsWith(error('''
            Did not find expected text in build output.
            Expected: message extra
             
            Build output:
            =======
             
            message
            message 2
             
            Output:
        '''))

        when:
        result.assertOutputContains("\n\nmessage\n\n")

        then:
        def e6 = thrown(AssertionError)
        error(e6).startsWith(error('''
            Did not find expected text in build output.
            Expected: 

            message
            
            
            
            Build output:
            =======
             
            message
            message 2
             
            Output:
        '''))
    }

    def "can assert post build output is present"() {
        def output = """
message

BUILD SUCCESSFUL in 12s

post build
more post build
"""
        when:
        def result = OutputScrapingExecutionResult.from(output, "")

        then:
        result.assertHasPostBuildOutput("post build")
        result.assertHasPostBuildOutput("post build\nmore post build\n")
        result.assertHasPostBuildOutput("post build\r\nmore post build\r\n")
        result.assertHasPostBuildOutput("\npost build\n")
        result.assertHasPostBuildOutput("\r\npost build\r\nmore")

        when:
        result.assertHasPostBuildOutput("missing")

        then:
        def e = thrown(AssertionError)
        error(e).startsWith(error('''
            Did not find expected text in post-build output.
            Expected: missing
            
            Post-build output:
            =======
             
            post build
            more post build
             
            Output:
        '''))

        when:
        result.assertHasPostBuildOutput("message")

        then:
        def e2 = thrown(AssertionError)
        error(e2).startsWith(error('''
            Did not find expected text in post-build output.
            Expected: message
            
            Post-build output:
            =======
             
            post build
            more post build
             
            Output:
        '''))

        when:
        result.assertHasPostBuildOutput("BUILD")

        then:
        def e3 = thrown(AssertionError)
        error(e3).startsWith(error('''
            Did not find expected text in post-build output.
            Expected: BUILD
            
            Post-build output:
            =======
             
            post build
            more post build
             
            Output:
        '''))

        when:
        result.assertHasPostBuildOutput("post build extra")

        then:
        def e4 = thrown(AssertionError)
        error(e4).startsWith(error('''
            Did not find expected text in post-build output.
            Expected: post build extra
            
            Post-build output:
            =======
             
            post build
            more post build
             
            Output:
        '''))

        when:
        result.assertHasPostBuildOutput("post build\n\n")

        then:
        def e5 = thrown(AssertionError)
        error(e5).startsWith(error('''
            Did not find expected text in post-build output.
            Expected: post build


            
            Post-build output:
            =======
             
            post build
            more post build
             
            Output:
        '''))
    }

    @Unroll
    def "can assert output is not present = #text"() {
        def output = """
message

BUILD SUCCESSFUL in 12s

post build
more post build
"""
        def errorOut = """
broken
"""
        when:
        def result = OutputScrapingExecutionResult.from(output, errorOut)

        then:
        result.assertNotOutput("missing")
        result.assertNotOutput("missing extra")
        result.assertNotOutput("missing\n\nBUILD FAILED")
        result.assertNotOutput("missing\n\n\nBUILD")

        when:
        result.assertNotOutput(text)

        then:
        def e = thrown(AssertionError)
        error(e).startsWith(error("""
Found unexpected text in build output.
Expected not present: ${text}

Output:
"""))

        where:
        text               | _
        "message"          | _
        "message\n\nBUILD" | _
        "BUILD"            | _
        "post"             | _
        "broken"           | _
        "broken\n"         | _
    }

    def "can assert task is present when task output is split into several groups"() {
        def output = """
before

> Task :a
> Task :b

> Task :a
some content

> Task :b
other content

> Task :a
all good

> Task :b SKIPPED

after

BUILD SUCCESSFUL in 12s

post build
"""
        when:
        def result = OutputScrapingExecutionResult.from(output, "")

        then:
        result.assertTasksExecuted(":a", ":b")
        result.assertTasksExecuted([":a", ":b"])
        result.assertTasksExecuted(":a", ":b", ":a", [":a", ":b"], ":b")

        and:
        result.assertTasksExecutedInOrder(":a", ":b")
        result.assertTasksExecutedInOrder([":a", ":b"])

        and:
        result.executedTasks == [":a", ":b"]
        result.skippedTasks == [":b"] as Set

        and:
        result.assertTasksNotSkipped(":a")
        result.assertTasksNotSkipped(":a", ":a", [":a"])

        and:
        result.assertTaskNotSkipped(":a")

        and:
        result.assertTasksSkipped(":b")
        result.assertTasksSkipped(":b", ":b", [":b"])

        and:
        result.assertTaskSkipped(":b")

        when:
        result.assertTasksExecuted(":a")

        then:
        def e = thrown(AssertionError)
        error(e).startsWith(error('''
            Build output does not contain the expected tasks.
            Expected: [:a]
            Actual: [:a, :b]
        '''))

        when:
        result.assertTasksExecuted(":a", ":b", ":c")

        then:
        def e2 = thrown(AssertionError)
        error(e2).startsWith(error('''
            Build output does not contain the expected tasks.
            Expected: [:a, :b, :c]
            Actual: [:a, :b]
        '''))

        when:
        result.assertTasksExecutedInOrder(":b", ":a")

        then:
        def e3 = thrown(AssertionError)
        e3.message.startsWith(":a does not occur in expected order (expected: exact([:b, :a]), actual [:a, :b])")

        when:
        result.assertTasksExecutedInOrder(":a")

        then:
        def e4 = thrown(AssertionError)
        error(e4).startsWith(error('''
            Build output does not contain the expected tasks.
            Expected: [:a]
            Actual: [:a, :b]
        '''))

        when:
        result.assertTasksExecutedInOrder(":a", ":b", ":c")

        then:
        def e5 = thrown(AssertionError)
        error(e5).startsWith(error('''
            Build output does not contain the expected tasks.
            Expected: [:a, :b, :c]
            Actual: [:a, :b]
        '''))

        when:
        result.assertTasksSkipped()

        then:
        def e6 = thrown(AssertionError)
        error(e6).startsWith(error('''
            Build output does not contain the expected skipped tasks.
            Expected: []
            Actual: [:b]
        '''))

        when:
        result.assertTasksSkipped(":a")

        then:
        def e7 = thrown(AssertionError)
        error(e7).startsWith(error('''
            Build output does not contain the expected skipped tasks.
            Expected: [:a]
            Actual: [:b]
        '''))

        when:
        result.assertTasksSkipped(":b", ":c")

        then:
        def e8 = thrown(AssertionError)
        error(e8).startsWith(error('''
            Build output does not contain the expected skipped tasks.
            Expected: [:b, :c]
            Actual: [:b]
        '''))

        when:
        result.assertTaskSkipped(":a")

        then:
        def e9 = thrown(AssertionError)
        error(e9).startsWith(error('''
            Build output does not contain the expected skipped task.
            Expected: :a
            Actual: [:b]
        '''))

        when:
        result.assertTasksNotSkipped()

        then:
        def e10 = thrown(AssertionError)
        error(e10).startsWith(error('''
            Build output does not contain the expected non skipped tasks.
            Expected: []
            Actual: [:a]
        '''))

        when:
        result.assertTasksNotSkipped(":b")

        then:
        def e11 = thrown(AssertionError)
        error(e11).startsWith(error('''
            Build output does not contain the expected non skipped tasks.
            Expected: [:b]
            Actual: [:a]
        '''))

        when:
        result.assertTasksNotSkipped(":a", ":c")

        then:
        def e12 = thrown(AssertionError)
        error(e12).startsWith(error('''
            Build output does not contain the expected non skipped tasks.
            Expected: [:a, :c]
            Actual: [:a]
        '''))

        when:
        result.assertTaskNotSkipped(":b")

        then:
        def e13 = thrown(AssertionError)
        error(e13).startsWith(error('''
            Build output does not contain the expected non skipped task.
            Expected: :b
            Actual: [:a]
        '''))
    }

    def "creates failure result"() {
        def output = """
message

BUILD FAILED in 12s

post build
"""
        when:
        def result = OutputScrapingExecutionResult.from(output, "")

        then:
        result instanceof OutputScrapingExecutionFailure
    }

    def "can capture tasks with multiple headers"() {
        def output = """
> Task :compileMyTestBinaryMyTestJava
> Task :myTestBinaryTest

MyTest > test FAILED
    java.lang.AssertionError at MyTest.java:10

1 test completed, 1 failed

> Task :myTestBinaryTest FAILED


FAILURE: Build failed with an exception.

BUILD FAILED in 13s
2 actionable tasks: 2 executed

"""
        when:
        def result = OutputScrapingExecutionResult.from(output, "")

        then:
        result.assertTasksExecutedInOrder(":compileMyTestBinaryMyTestJava", ":myTestBinaryTest")
    }
}
