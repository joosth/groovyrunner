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
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

/**
 * NodeRef meta class
 * provides virtual properties and methods for nodes
 * @author Joost Horward
 */
class NodeRefMeta {

    def groovyRunnerService
    def permissionService
    def namespaceService
    def nodeService

	def init() {
        // Get property map
		NodeRef.metaClass.getProps = { ->
			def props=nodeService.getProperties(delegate)
	        // We use groovy 1.7.5 in older Alfresco versions ... so we avoid collectEntries ...
    	    def prps=[:]
        	props.each { key,value -> prps.put(key.toPrefixString(namespaceService),value)}
	        return prps
		}

        // Pseudo properties
		NodeRef.metaClass.getProperty = { propertyName ->
			switch (propertyName) {
				case "properties":
					return getProps()
				break
				case "aspects":
					return getAspects()
				break
				case "type":
					return getType()
				break
				case "list":
					return getList()
				break

                case "text":
                    return groovyRunnerService.getText(delegate)
                break

                case "content":
                    return groovyRunnerService.getText(delegate)
                break

				case "permissions":
					return permissionService.getAllSetPermissions(delegate);
				break
				case "inherit":
					return permissionService.getInheritParentPermissions(delegate);
				break

                case "path":
                    return getPath();
                break

                case "assocPath":
                    return groovyRunnerService.getAssocPath(delegate);
                break

                case "mimetype":
                    return groovyRunnerService.getMimetype(delegate);
                break

                case "parent":
                    return nodeService.getPrimaryParent(delegate).getParentRef();
                break

				default:
					if (propertyName.contains('_')) {
						propertyName=propertyName.replace('_',':')
					}
					def propName=propertyName.contains(':')?propertyName:"cm:${propertyName}"
					return nodeService.getProperty(delegate,QName.createQName(propName,namespaceService))
			}
		}

		NodeRef.metaClass.setProperty = { propertyName,value ->
            if (value.getClass()==org.codehaus.groovy.runtime.GStringImpl) {
                value=value.toString()
            }
			switch (propertyName) {
				case "properties":
					return groovyRunnerService.setProperties(delegate,value)
				break

                case "text":
                	groovyRunnerService.putText (delegate,value)
                break

                case "content":
                    groovyRunnerService.putText (delegate,value,"application/octet-stream")
                break

				case "type":
					groovyRunnerService.setType(delegate,value)
				break
				default:
					if (propertyName.contains('_')) {
						propertyName=propertyName.replace('_',':')
					}
					def propName=propertyName.contains(':')?propertyName:"cm:${propertyName}"
				return nodeService.setProperty(delegate,QName.createQName(propName,namespaceService),value)
			}
		}

        // Get type as prefix:name
		NodeRef.metaClass.getType = {
			nodeService.getType(delegate).toPrefixString(namespaceService)
		}

        // Get aspects as prefix:name map
		NodeRef.metaClass.getAspects = { ->
			nodeService.getAspects(delegate).collect { it.toPrefixString(namespaceService) }
		}

        // Get children of node
		NodeRef.metaClass.getList = { ->
            def childassocs= nodeService.getChildAssocs(delegate)
            return childassocs?.collect { it.childRef }
		}

        // Get node path
        NodeRef.metaClass.getPath = { ->
            return nodeService.getPath(delegate).toDisplayPath(nodeService, permissionService);
        }

        // Allows for node.groovyServiceMethod(...)
        NodeRef.metaClass.methodMissing = { name, args ->
            def newargs=[delegate]
            args.each { newargs.add(it) }
            groovyRunnerService.invokeMethod(name,newargs)
        }
	}
}