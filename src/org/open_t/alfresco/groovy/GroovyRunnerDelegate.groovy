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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.*
import groovy.lang.Script

/**
 * GroovyRunner script delegate class
 * - runs missing methods against groovyRunnerService
 * - fetches spring beans for missing proberties (auto-injects services)
 * - auto-injects log
 * @author Joost Horward
 */
abstract class GroovyRunnerDelegate extends groovy.lang.Script {
	private static final Log log = LogFactory.getLog(GroovyRunnerDelegate.class);

    /**
     * Redirect unkwnon method calls to groovyRunnerService
     */
    def methodMissing(String name,args) {
        def applicationContext=binding.applicationContext
        def groovyRunnerService=applicationContext.getBean("groovyRunnerService")
        return groovyRunnerService.invokeMethod(name,args)
    }

    /**
     * Get any bean as a property. This allows us to acces nodeService, fileFolderService etc.
     */
    def propertyMissing(String name) {
        def value=null

        if (name=='log') {
            return log
        }
        def applicationContext=binding.applicationContext
        try {
            value=applicationContext.getBean(name)
            binding[name]=value
        } catch (Exception e) {
            // Do nothing
        }
        if  (value) {
            log.trace "Returning bean ${name}"
            return value
        }
        return null
    }
}
