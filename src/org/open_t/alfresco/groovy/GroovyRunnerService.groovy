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

import groovy.lang.Closure;

import org.alfresco.repo.admin.registry.RegistryService;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.admin.RepoAdminService;
import org.alfresco.service.cmr.audit.AuditService;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.lock.LockService;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.CopyService;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.cmr.repository.NodeRef;
import javax.transaction.UserTransaction;
import javax.transaction.Status;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.cmr.security.AuthenticationService
import org.alfresco.service.cmr.security.PersonService
import org.alfresco.service.cmr.security.NoSuchPersonException
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.AuthorityService;
import groovy.util.GroovyTestCase
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.model.ContentModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.site.SiteVisibility;
import org.alfresco.util.ApplicationContextHelper;
import org.alfresco.service.cmr.search.*
import org.alfresco.service.cmr.repository.*;
import org.alfresco.repo.content.transform.ContentTransformer;

import org.apache.http.*;
import org.apache.http.client.*;
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.auth.*
import org.apache.http.client.methods.*
import org.apache.http.entity.*
import org.apache.http.protocol.HttpContext
import org.apache.http.util.EntityUtils;
import org.springframework.web.multipart.*
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.*
import groovy.lang.Script

// Transaction listener packages
import org.alfresco.repo.transaction.TransactionListener;
import org.alfresco.repo.transaction.TransactionListenerAdapter;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;

/**
 * GroovyService
 * Provides service methods for GroovyRunner
 * @author Joost Horward
 */
class GroovyRunnerService implements ApplicationContextAware  {
	private static final Log logger = LogFactory.getLog(GroovyRunnerDelegate.class);


	NamespaceService namespaceService
	SearchService searchService
	NodeService nodeService
	FileFolderService fileFolderService
	ContentService contentService
	Repository repository;
	ServiceRegistry serviceRegistry
	LockService lockService

	AuthenticationService authenticationService

	PersonService personService
	VersionService versionService
	CopyService copyService
	DictionaryService dictionaryService
	AuditService auditService
	RegistryService registryService
	RepoAdminService repoAdminService

	AuthorityService authorityService
	PermissionService permissionService
	SiteService siteService

	@Autowired
	public ApplicationContext applicationContext;

	def RED="\33[31m"
	def GREEN="\33[32m"
	def BLACK="\33[30m"
	def WHITE="\33[37m"
	def BLUE ="\33[22;34m"

	def NC= "\33[0m"

    def output=""
    def out

	void setApplicationContext(org.springframework.context.ApplicationContext appCtx) {
		this.applicationContext=appCtx
	}

	// See http://en.wikipedia.org/wiki/ANSI_escape_code
	def ansi(code) {
        	String.format("%c[%s",0x1B,code);
	}

    /**
     * Find a node based on a path string
     * Returns null if the node cannot be found
     */
	def findNode(pathString) {
		NodeRef companyHomeRef = repository.getCompanyHome();
        if (pathString=="/" || pathString=="") {
            return companyHomeRef
        }
        if (pathString && pathString.contains(":") ) {
            return findNodeByChildAssoc(pathString)
        }

		def path=new ArrayList<String>(Arrays.asList(pathString.split("/")))
		try {
			return fileFolderService.resolveNamePath(companyHomeRef, path).getNodeRef();
		} catch (Exception e) {
			return null
		}
	}

    /**
     * Find a node based on a child assoic path string
     * Returns null if the node cannot be found
     */
	def findNodeByChildAssoc(pathString) {
        def pathElements=pathString.split('/')
        def node=repository.getCompanyHome();
        pathElements.each { element ->
            node=nodeService.getChildAssocs(node,ContentModel.ASSOC_CONTAINS,
            QName.createQName(element,namespaceService)).childRef[0]
        }
        return node
    }

    /*
     * Get the associatiuon path of the node as namespace:prefix elemens
     */
    def getAssocPath(node) {
        nodeService.getPath(node).toPrefixString(namespaceService);
    }

    /**
     * Create the given path
     * Returns the node at the bottom of the path
     */
	def createPath(pathString) {
		def node
		def path=new ArrayList<String>(Arrays.asList(pathString.split("/")))
		def currentPath=""
		NodeRef currentNode = repository.getCompanyHome();
		path.each { pathElement ->
				currentPath+=pathElement
				node=findNode(currentPath)
				if (!node) {
					node=createNode(currentNode,pathElement,"cm:folder",[])
				}
                currentNode=node
				currentPath+='/'
		}
		return node
	}

    /**
     * Delete a Share site
     */
	def deleteSite(siteName) {
		try {
			siteService.deleteSite(siteName)
		} catch (Exception e) {
		}
	}

    /**
     * Create a site by name
     */
	def createSite(siteName,siteTitle,siteDescription,createDocumentLibrary=true) {
		siteService.createSite("site-dashboard", siteName, siteTitle, siteDescription, SiteVisibility.PUBLIC);
        if (createDocumentLibrary) {
  		   siteService.createContainer(siteName, "documentLibrary", ContentModel.TYPE_FOLDER, null);
        }
		siteService.createContainer(siteName, "surf-config", ContentModel.TYPE_FOLDER, null);
		def surfConfig=findNode("Sites/${siteName}/surf-config")
		def components=createNode(surfConfig,"components","cm:folder",[])
		def pages=createNode(surfConfig,"pages","cm:folder",[])
		def siteFolder=createNode(pages,"site","cm:folder",[])
		def thisSiteFolder=createNode(siteFolder,siteName,"cm:folder",[])

		def dashboardXml=createNode(thisSiteFolder,"dashboard.xml","cm:content",[])

		def dashboardXmlContent="""<?xml version="1.0" encoding="UTF-8"?>
	<page>
		<title>Collaboration Site Dashboard</title>
		<title-id>page.siteDashboard.title</title-id>
		<description>Collaboration site's dashboard page</description>
		<description-id>page.siteDashboard.description</description-id>
		<authentication>user</authentication>
		<template-instance>dashboard-2-columns-wide-right</template-instance>
		<properties><sitePages>[{"pageId":"documentlibrary"}]</sitePages></properties>
	</page>"""


		ContentWriter writer=contentService.getWriter(dashboardXml, ContentModel.PROP_CONTENT, true)
		writer.putContent(dashboardXmlContent)


		def navigationXml=createNode(components,"page.navigation.site~${siteName}~dashboard.xml","cm:content",[])
		ContentWriter writer2=contentService.getWriter(navigationXml, ContentModel.PROP_CONTENT, true)
		def navigationXmlContent="""<?xml version="1.0" encoding="UTF-8"?>
<component><guid>page.navigation.site~${siteName}~dashboard</guid><scope>page</scope><region-id>navigation</region-id><source-id>site/${siteName}/dashboard</source-id><url>/components/navigation/collaboration-navigation</url></component>"""
		writer2.putContent(navigationXmlContent)

		def titleXml=createNode(components,"page.title.site~${siteName}~dashboard.xml","cm:content",[])
		ContentWriter writer3=contentService.getWriter(titleXml, ContentModel.PROP_CONTENT, true)
		def titleXmlContent="""<?xml version="1.0" encoding="UTF-8"?>
<component><guid>page.title.site~${siteName}~dashboard</guid><scope>page</scope><region-id>title</region-id><source-id>site/${siteName}/dashboard</source-id><url>/components/title/collaboration-title</url></component>"""
		writer3.putContent(titleXmlContent)
	}


    /*
     * Run the given closure
     * This is left in her for backwards compatibility
     */
    def run (Closure c) {
		return c()
	}


    /**
     * Execute the given closure in a transaction
     */
	def withTransaction (Closure c) {
		def rethrowException=null
		UserTransaction trx = serviceRegistry.getTransactionService().getNonPropagatingUserTransaction();
		def result
		try {
			logger.trace "Starting transaction ${trx}"
			trx.begin()
			result=c()
			logger.trace "Committing transaction ${trx}"
			trx.commit()
			logger.trace "Committed transaction ${trx}"
		} catch (Exception e) {
			logger.error "Caught exception in GroovyRunner transaction: ${e.message}"
			rethrowException=e
            if (trx.status!=Status.STATUS_ROLLEDBACK) {
                logger.trace "Rolling back transaction ${trx}"
                trx.rollback()
                logger.trace "Transaction ${trx} rolled back"
            }
		}

		if (rethrowException) {
			throw rethrowException
		}
		return result
	}

    // This is done this way because overriding System.out is NOT thread safe

	void log (message) {
		print "\n     ${message}"
	}

    void print (message) {
		out.print(message)
	}

    void println (message) {
		out.println(message)
	}

    /**
     * This is left in here for backwards compatibility
     */
	def test(name,Closure c) {
		def testStartTime=System.currentTimeMillis()
		print name
		withTransaction (c)
		def testCompletedTime=System.currentTimeMillis()
		def duration=testCompletedTime-testStartTime
		def durationString=String.format("%5d",duration)
		println "${ansi('70G')}${GREEN}[ ${durationString} ms ]${NC}"
	}

    /**
     * Create a node, return the NodeRef
     */
	def createNode(parentNodeRef,name,stype="cm:content",properties=null,aspects=null) {
		def typeQName=QName.createQName(stype,namespaceService)
		NodeRef node = fileFolderService.create(parentNodeRef, name, typeQName).getNodeRef();
		if (properties) {
			setProperties(node,properties)
		}

		if (aspects) {
            aspects.each { aspect ->
                addAspect(node,aspect)
            }
		}

		return node
	}

    /**
     * Upload a file to the node content
     */
	def upload (node,path,mimeType="text/plain") {
		ContentWriter writer=contentService.getWriter(node, ContentModel.PROP_CONTENT, true)
		writer.setMimetype(mimeType)
		writer.putContent(new File(path))
	}

    /**
     * Store text in node content
     */
	def putText (node,text,mimeType="text/plain") {
		ContentWriter writer=contentService.getWriter(node, ContentModel.PROP_CONTENT, true)
		writer.setMimetype(mimeType)
		writer.putContent(text)
	}

    /**
     * Get text from node content
     */
    def getText(node) {
        def reader = contentService.getReader(node, ContentModel.PROP_CONTENT);
        def s =reader?.getContentString();
        return s
    }

    /**
     *  Get properties in a map
     *  Keys have the form prefix:name
     */
	def getProperties(nodeRef) {

		def props=nodeService.getProperties(nodeRef)
		// We use groovy 1.7.5 in Alfresco ... so no collectEntries ...
		def prps=[:]
		props.each { key,value -> prps.put(key.toPrefixString(namespaceService),value)}
		return prps
	}

    /**
     * Determine if node exists
     */
    def exists(nodeRef) {
        return nodeService.exists(nodeRef)
    }

    /**
     * get mimetype of node
     */
    def getMimetype(nodeRef) {
        ContentData contentData = (ContentData) nodeService.getProperty(nodeRef, ContentModel.PROP_CONTENT)
        return contentData.getMimetype()
    }

	/**
	 * Create a map of QName-value pairs from a map of prefix:name-value pairs
	 */
	def qnameMap(props) {
		def prps=[:]
		props.each { key,value ->
			def pxKey=key.contains(':')?key:"cm:${key}"
			prps.put(QName.createQName(pxKey,namespaceService),value)
		}
		return prps
	}

    /**
     * Convert a QName:value map to a prefixString:value map
     */
    def prefixMap(props) {
		def prps=[:]
		props.each { key,value -> prps.put(key.toPrefixString(namespaceService),value)}
		return prps
    }

    /**
     * Set properties to nodeRef
     * @param nodeRef The NodeRef
     * @param props a map of prefix:name -> value
     */
	def setProperties(nodeRef,props) {
		qnameMap(props).each { qname,value -> nodeService.setProperty(nodeRef,qname,value) }
	}

    /**
     * Add aspect to node, optionally set properties
     */
	def addAspect(nodeRef,aspect,properties=null) {
		def aspectQName=QName.createQName(aspect,namespaceService)
		nodeService.addAspect(nodeRef, aspectQName, qnameMap(properties));
	}

    /**
     * Remove aspect from node
     */
	def removeAspect(nodeRef,aspect) {
		def aspectQName=QName.createQName(aspect,namespaceService)
		nodeService.removeAspect(nodeRef, aspectQName)
	}

    /**
     * Check if node has aspect
     */
    def hasAspect(nodeRef,aspect) {
		def aspectQName=QName.createQName(aspect,namespaceService)
		return nodeService.hasAspect(nodeRef, aspectQName)
	}

    /**
     * Set the type of a node
     */
	def setType(nodeRef,contentType) {
		def contentTypeQName=QName.createQName(contentType,namespaceService)
	   	nodeService.setType(nodeRef, contentTypeQName)
	}

    /**
     * Create a user
     */
	def createPerson(properties,password=null) {
		NodeRef p
		def qmap=qnameMap(properties)
		def userName=qmap[ContentModel.PROP_USERNAME]
        if (personService.personExists(userName)) {
            p=personService.getPerson(userName,false)
        } else {
			p=personService.createPerson(qmap)
		}

        if (password) {
            try {
                withTransaction {
                    authenticationService.createAuthentication(userName, password.toCharArray());
                }
            } catch (Exception e) {
                authenticationService.setAuthentication(userName, password.toCharArray());
            }
        }

		return p
	}

    /**
     * Create a group
     */
	def createGroup(name) {
		if (!authorityService.authorityExists("GROUP_${name}")) {
			authorityService.createAuthority(AuthorityType.GROUP, name);
		}
	}

    /**
     * Delete a group
     */
	def deleteGroup(name) {
		if (authorityService.authorityExists("GROUP_${name}")) {
			authorityService.deleteAuthority("GROUP_${name}");
		}
	}

    /**
     * Add user to group
     */
	def addToGroup(group,name) {
		authorityService.addAuthority(group, name);
	}

    /**
     * Remove user from group
     */
	def removeFromGroup(group,name) {
		authorityService.removeAuthority(group, name);
	}

    /**
     * Set the inherit permissions flag
     */
	def inheritPermissions(nodeRef,inheritFlag) {
		permissionService.setInheritParentPermissions(nodeRef,inheritFlag);
	}

    /**
     * Set permission
     */
	def setPermission(nodeRef,name,role) {
		permissionService.setPermission(nodeRef, name, role, true);
	}

    /**
     * Delete permissions
     */
	def deletePermissions(nodeRef) {
		permissionService.deletePermissions(nodeRef);
	}

    /**
     * Run closure as other user
     */
	def runAs (String user,Closure closure) {
		def result= AuthenticationUtil.runAs(new RunAsWork<Object>() {
			@Override
			public Object doWork() throws Exception {
				def currentUser=AuthenticationUtil.getFullyAuthenticatedUser()
				AuthenticationUtil.setFullyAuthenticatedUser(user)
				logger.trace "before: runasuser=${AuthenticationUtil.getRunAsUser()} user=${user}, echt: ${AuthenticationUtil.getFullyAuthenticatedUser()}"
				def result =closure()
				AuthenticationUtil.setFullyAuthenticatedUser(currentUser)
			   return result
			}
		 }, user);
	}

    /**
     * Create a random name
     */
	def randomName() {
		def random=new Random()
		def chars='0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz'
		def s=""
		for (i in 1..32) {
			def digit=random.nextInt(chars.length());
			s+=chars[digit]
			}
	return s
	}

    /**
     * Performs FTS search
     * @param query
     * @param sort
     * @parm max
     */

    def search(params) {
        def rootNodeRef = nodeService.getRootNode(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE)
        def sp = new SearchParameters()
        sp.setLanguage(searchService.LANGUAGE_FTS_ALFRESCO)
        sp.setQuery(params.query)
        sp.addStore(rootNodeRef.getStoreRef());
        if (params.skip) {
            sp.setSkipCount(params.skip)
        }
        sp.setMaxItems(params.max?:10)
        if (params.sort) {
            params.sort.each {name,value ->
                boolean ascFlag=false
                if (value==true || value=="asc") {
                    ascFlag=true
                }
                def sortName=name.contains(':')?name:"cm:${name}"
                def sortQNameString=QName.createQName(sortName,namespaceService).toString()
                sp.addSort("@${sortQNameString}",ascFlag);
            }
        } else {
            sp.addSort("@{http://www.alfresco.org/model/content/1.0}created",true);
        }

        // contains numberFound and iterator containing results with nodeRef property
        ResultSet results = searchService.query(sp);
        return results
    }

    /**
     * Perform content transformation
     */
    def transform (sourceNode,targetNode,mimeType) {
        ContentReader reader = contentService.getReader(sourceNode, ContentModel.PROP_CONTENT)
        ContentData contentData = (ContentData) nodeService.getProperty(sourceNode, ContentModel.PROP_CONTENT)
        String sourceMimetype = contentData.getMimetype()
        ContentWriter writer = contentService.getWriter(targetNode, ContentModel.PROP_CONTENT, true)
        writer.setMimetype(mimeType)
        // Do the conversion
        ContentTransformer transformer = contentService.getTransformer(sourceMimetype, mimeType);
        if (transformer) {
            transformer.transform(reader, writer);
            return targetNode
        } else {
            return null
        }
    }
    /**
     * Checks if current user has admin permissions
     */
    boolean currentUserIsAdmin() {
        def username = AuthenticationUtil.getFullyAuthenticatedUser();
        username?authorityService.isAdminAuthority(username):false
    }

    /**
    * Run closure after transaction
    */
    def afterTransaction (Closure closure) {
        def listener=new AfterTransactionListener(closure)
        AlfrescoTransactionSupport.bindListener(listener);
    }

}
