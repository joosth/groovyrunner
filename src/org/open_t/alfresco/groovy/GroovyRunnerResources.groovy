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

import org.springframework.extensions.webscripts.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.*
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.apache.commons.io.IOUtils

/**
 * GroovyRunner resources webscript
 * Fetches resources from /org/open_t/alfresco/groovy/resources in the jar
 * @author Joost Horward
 */
public class GroovyRunnerResources extends AbstractWebScript implements ApplicationContextAware

{
	private static final Log logger = LogFactory.getLog(GroovyRunnerResources.class);
    def mimetypeMap=[jpg:'image/jpeg',
                     jpeg:'image/jpeg',
                     png:'image/png',
                     gif:'image/gif',
                     svg:'image/svg+xml',
                     js:'application/javascript',
                     css:'text/css',
                     html:'text/html',
                     txt:'text/plain'
    ]

	public ApplicationContext applicationContext;

    // Setter for application context
	void setApplicationContext(org.springframework.context.ApplicationContext appCtx) {
		this.applicationContext=appCtx
	}

    // Execute the webscript
    public void execute(WebScriptRequest req, WebScriptResponse res) throws IOException
    {

		logger.debug "Executing GroovyRunnerResources webscript"
        def localPath
        if (req.pathInfo && req.pathInfo.length()>21) {
            // Strip the /open_t/groovyrunner from the path
            localPath=req.pathInfo.substring(21)
        } else {
            res.res.sendRedirect("${req.serviceContextPath}/open_t/groovyrunner/index.html")
            return
        }

        def inputStream

        logger.trace "Fetching ${localPath} from classpath"
        def resource=applicationContext.getResource("classpath:/org/open_t/alfresco/groovy/resources/${localPath}");
        if (resource.isReadable()) {
            inputStream=resource.getInputStream()

            // Simple mimetype detection for common file types
            def suffix=localPath.substring(localPath.lastIndexOf('.')+1)

            def contentType= mimetypeMap[suffix]?:"application/octet-stream"

            res.res.setHeader("Content-Type", contentType)

            IOUtils.copy(inputStream,res.res.outputStream)
            res.res.outputStream.flush()
        }
		logger.debug "GroovyRunnerResources webscript completed"
	}
}