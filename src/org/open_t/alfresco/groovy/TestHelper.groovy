/**
 * GroovyRunner
 *
 * Copyright 2010-2015, Open-T B.V., and individual contributors as indicated
 * by the @author tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License
 * version 3 published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses
 */

package org.open_t.alfresco.groovy

/**
 * Helper for unit testing
 * @author Joost Horward
 */
class TestHelper {

    /**
     * constructor
     */
    TestHelper(PrintStream out,useAnsi=true) {
        this.out=out
        this.useAnsi=useAnsi
    }

    def groovyRunnerService

	boolean stopOnError=true
    boolean errorOccurred=false
    boolean transactional=true

    def startTime=System.currentTimeMillis()
    def errorMessage=""
    int testCount=0

    def out

   	// See http://en.wikipedia.org/wiki/ANSI_escape_code
    def useAnsi=true

    // Horizontal position, fallback to tab
    def ansiHorizontal(len) {
        useAnsi?String.format("%c[%s",0x1B,"${len}G"):"\t"
    }

    // Green ANSI color
    def ansiGreen() {
        useAnsi?"\33[32m":""
    }

    // Red ANSI color
    def ansiRed() {
        useAnsi?"\33[31m":""
    }

    // Reset ansi to normal text
    def ansiNormal() {
        useAnsi?"\33[0m":""
    }

    // Any initialization should go here
    def init() {
    }

    // Start a new test sequence
    def start() {
		testCount=0
		startTime=System.currentTimeMillis()
	}

    // Finish the test sequence, show the result
    def finish() {
        print "\n===> "
        def duration=System.currentTimeMillis()-startTime
        def durationString=String.format("%5d",duration)

        if (!errorOccurred) {
            println "${ansiGreen()}PASS${ansiHorizontal('70')}[ ${durationString} ms ]${ansiNormal()}"
        } else {
            println "${ansiRed()}FAIL: ${errorMessage}${ansiHorizontal('70')}[ ${durationString} ms ]${ansiNormal()}"
        }
	}

    /**
     * Run a test, wrapped in a transaction if enabled
     */
    def run (String name,Closure c) {
        if (testCount==0) {
            start()
        }

        if ( !errorOccurred || !stopOnError ) {
            testCount++
            c.setResolveStrategy(Closure.DELEGATE_FIRST)
            c.setDelegate(this)
            def testStartTime=System.currentTimeMillis()

            print String.format("%3d",testCount)
            print ". ${name} "

            try {
                if (transactional) {
                    groovyRunnerService.withTransaction(c)
                } else {
                    c()
                }
            } catch (Throwable e) {
                errorOccurred=true
                errorMessage=e.message
            }
            def testCompletedTime=System.currentTimeMillis()
            def duration=testCompletedTime-testStartTime
            def durationString=String.format("%5d",duration)
            if (!errorOccurred) {
                println "${ansiHorizontal('70')}${ansiGreen()}[ ${durationString} ms ]${ansiNormal()}"
            } else {
                print errorMessage
                println "${ansiHorizontal('70')}${ansiRed()}[ ${durationString} ms ]${ansiNormal()}"
            }
        }
	}

    // Show a message indented in the test results
    void log (message) {
		print "\n     ${message}"
	}

    // Print a message
    void print (message) {
		out.print(message)
	}

    // Print a message with CR
    void println (message) {
		out.println(message)
	}
}
