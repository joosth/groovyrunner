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
package org.open_t.alfresco.groovy;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

import org.springframework.extensions.webscripts.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import org.alfresco.model.ContentModel;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.junit.Test
import org.codehaus.groovy.control.CompilerConfiguration
import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.*
import org.codehaus.groovy.control.CompilerConfiguration
import org.springframework.extensions.webscripts.AbstractWebScript;

/**
 * GroovyRunner web script
 * Executes posted script
 * @author Joost Horward
 */
public class GroovyRunner extends AbstractWebScript implements ApplicationContextAware

{

	private static final Log logger = LogFactory.getLog(GroovyRunner.class);
    def groovyRunnerService
    boolean requireAdmin

    //Beans beans
    // Two concurrent variables that are stored here and serve as a common storage/counting place for threads
    AtomicInteger counter
    ConcurrentHashMap map

    @Autowired
	public ApplicationContext applicationContext;

    // Receive application context and initialize global variables
	void setApplicationContext(org.springframework.context.ApplicationContext appCtx) {
		this.applicationContext=appCtx
        counter=new AtomicInteger()
        counter.set(0)
        map=new ConcurrentHashMap();
	}

    // Execute the webscript
    public void execute(WebScriptRequest req, WebScriptResponse res) throws IOException
    {
        // Bail out if user needs to be admin but is not
        if (requireAdmin) {
            if (!groovyRunnerService.currentUserIsAdmin()) {
                return
            }
        }

		logger.debug "Executing GroovyRunner webscript"
		String script=req.getContent().getContent();

		logger.debug "The content is ${script}"

		String response

        // Set up a binding for the script
		Binding binding = new Binding()
		binding.response=response
		binding.req=req
		binding.res=res

        def printstream=new PrintStream(res.getOutputStream())
        binding.out=printstream

        binding.http=applicationContext.getBean("groovyRunner.httpHelper")

        // Don't use ANSI for testhelper in web console
        binding.test=applicationContext.getBean("groovyRunner.testHelper",printstream,req.req.getHeader("X-Requested-With")!="XMLHttpRequest")

		binding.runner=groovyRunnerService
        binding.counter=counter
        binding.map=map
        binding.applicationContext=applicationContext

        // use GroovyRunnerDelegate as script delegate
        def configuration = new CompilerConfiguration()
        configuration.setScriptBaseClass("org.open_t.alfresco.groovy.GroovyRunnerDelegate")

        // Add imports
        def importCustomizerClass=null
        try {
            importCustomizerClass=Class.forName("org.codehaus.groovy.control.customizers.ImportCustomizer",false,classLoader)
            imports=importCustomizerClass.newInstance()
            imports.addStarImports('org.alfresco.service.cmr.repository','org.alfresco.service.namespace','org.alfresco.model')
            configuration.addCompilationCustomizers(imports)

        } catch (Exception e){
            // too bad, no star imports
            script="""import org.alfresco.service.cmr.repository.*
                      import org.alfresco.service.namespace.*
                      import org.alfresco.model.*
                      """+script
        }

        // Execute the script and return the result
		try {
			response=new GroovyShell(binding,configuration).run(script,"GroovyRunnerScript.groovy",[])
		} catch (Throwable e) {
            e.printStackTrace(printstream)
			logger.error e.message
		}
        if(binding.test.testCount>0) {
            binding.test.finish();
        }

		logger.debug "GroovyRunner webscript completed"
	}
}