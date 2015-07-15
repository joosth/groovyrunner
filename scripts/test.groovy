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

/**
 * Test script for groovyRunner
 * @author Joost Horward
 */

import org.alfresco.repo.transaction.AlfrescoTransactionSupport
import org.alfresco.repo.security.authentication.AuthenticationUtil

testHome=null

test.run "Cleanup previous test home", {
    testHome=findNode("groovyrunner-tests")
    if (testHome) {
        nodeService.deleteNode(testHome)
    }
    assert !testHome.exists()
}

test.run "Create test home", {
    testHome=createPath("groovyrunner-tests")
}

def test1
test.run "Create node", {
    test1=testHome.createNode("test-1","cm:content",null,['cm:failedThumbnailSource'])
    test1.text="The quick brown fox jumps over the lazy dog"
}

test.run "Check node", {
    assert test1.class==org.alfresco.service.cmr.repository.NodeRef
    assert test1.name=="test-1"
    assert test1.type=="cm:content"
    assert test1.text=="The quick brown fox jumps over the lazy dog"
    assert test1.exists()
}

test.run "Check node properties", {
    test1.properties.each { name,value ->
        assert name.getClass()==java.lang.String
        assert name.contains(":")
    }
    assert test1.properties.getClass()==java.util.LinkedHashMap
    assert test1.cm_modified.class==java.util.Date
    assert test1.cm_created.class==java.util.Date
}

test.run "Check aspects", {
    assert test1.aspects.class==java.util.ArrayList
    test1.aspects.each { aspect ->
        assert aspect.class==java.lang.String
        assert aspect.contains(":")
    }
}

test.run "Add versionable aspect" , {
    test1.addAspect("cm:versionable")
}

test.run "Check versionable aspect" , {
    assert test1.aspects.contains ("cm:versionable")
    assert test1.hasAspect ("cm:versionable")
    assert test1.autoVersion==true
    assert test1.initialVersion==true
    assert test1.versionLabel=="1.0"
}

test.run "Add cm:effectivity" , {
    test1.addAspect("cm:effectivity")
}

test.run "Check cm:effectivity" , {
    assert test1.hasAspect("cm:effectivity")
}

test.run "Remove cm:effectivity" , {
    test1.removeAspect("cm:effectivity")
}

test.run "Check cm:effectivity is missing" , {
    assert test1.hasAspect("cm:effectivity")==false
}

test.run "Update document", {
    test1.text="Another quick brown fox jumps over the lazy dog"
}

test.run "Test versioning info afer update" , {
    assert test1.versionLabel=="1.1"
}

test.run "Add title property" , {
    test1.title="The title of the quick brown fox jumps over the lazy dog"
}

test.run "Check aspects after adding title", {
    test1.aspects.contains("cm:titled")
    test1.title=="The title of the quick brown fox jumps over the lazy dog"
}

test.run "Checking NodeRefMeta list function", {
    testHome.list.getClass()==java.util.ArrayList
    testHome.list.each { node ->
        assert node.name=="test-1"
    }
}

test.run "Checking NodeRefMeta permissions", {
    testHome.permissions.getClass()==java.util.ArrayList
    testHome.permissions.each { permission ->
        assert permission.permission=="Consumer"
        assert permission.authority=="GROUP_EVERYONE"
        assert permission.class==org.alfresco.repo.security.permissions.impl.AccessPermissionImpl
        assert permission.inherited==true
        assert permission.setDirectly==false
    }
    assert testHome.inherit==true
}

test.run "Check path", {
    assert test1.path=="/Company Home/groovyrunner-tests"
}

test.run "Check assoc path", {
    assert test1.assocPath=="/app:company_home/cm:groovyrunner-tests/cm:test-1"
}

test.run "Check findNodeByChildAssoc", {
    assert findNodeByChildAssoc("cm:groovyrunner-tests/cm:test-1")==test1
}

test.run "Check findNode works correctly with an assocpath", {
    assert findNode("cm:groovyrunner-tests/cm:test-1")==test1
}

test.run "Mimetype", {
    assert test1.mimetype=="text/plain"
}

test.run "Parent", {
    assert test1.parent==testHome
}

test.run "Set content", {
    test1.content='Some content'
}

test.run "Get content", {
    assert test1.content=="Some content"
}

test.run "Mimetype", {
    assert test1.mimetype=="application/octet-stream"
}

def savedquery1
test.run "Create saved query 1", {
    savedquery1=testHome.createNode("savedquery-1","cm:savedquery",[title:'test'],["cm:effectivity","cm:titled"])
    assert savedquery1.type=="cm:savedquery"
    assert savedquery1.hasAspect("cm:effectivity")
}

def savedquery2
test.run "Create saved query 2", {
    savedquery2=testHome.createNode("savedquery-2")
    savedquery2.type="cm:savedquery"
    assert savedquery2.type=="cm:savedquery"
}

test.run "Create site" , {
    createSite("groovyrunner2","GroovyRunner","GroovyRunner test site")
}

siteNodeString=""
test.run "Check groovyrunner site" , {
    def siteNode=findNode("Sites/groovyrunner2")
    assert siteNode.type=="st:site"
    siteNodeString=siteNode.toString()
}

test.run "Delete groovyrunner site" , {
    deleteSite("groovyrunner2")
}

test.run "Purge groovyrunner site from archive" , {
    def archivedNode=new NodeRef(siteNodeString.replace("workspace","archive"))
    nodeArchiveService.purgeArchivedNode(archivedNode)
}

transactionTestFlag=false
test.run "Transaction",  {
    def currentTransactionId=AlfrescoTransactionSupport.getTransactionId()
    withTransaction {
        assert AlfrescoTransactionSupport.getTransactionId()!=currentTransactionId
        afterTransaction {
            transactionTestFlag=true
        }
    }
    assert AlfrescoTransactionSupport.getTransactionId()==currentTransactionId
    assert transactionTestFlag
}

uploadTestNode=null

test.run "Remove previous upload-test" , {
    uploadTestNode=findNode("groovyrunner-tests/upload-test.txt")
    if (uploadTestNode) {
        nodeService.deleteNode(uploadTestNode)
    }
}

test.run "Test upload" , {
    def f=File.createTempFile("groovyrunner","txt")
    f.text="The quick brown fox jumps over the lazy dog"
    uploadTestNode=testHome.createNode("upload-test.txt")
    uploadTestNode.upload(f.absolutePath)
}

test.run "Check upload", {
    assert uploadTestNode.text=="The quick brown fox jumps over the lazy dog"
}

test.run "currentUserIsAdmin" , {
    assert currentUserIsAdmin()
    runAs "mjackson" , {
        assert AuthenticationUtil.getRunAsUser()=="mjackson"
        assert AuthenticationUtil.getFullyAuthenticatedUser()=="mjackson"
    }
}

test.run "qnameMap" , {
    def map=["cm:name":"name","title":"title"]
    def qmap= qnameMap(map)
    qmap.each { qname,value ->
        assert qname.class==org.alfresco.service.namespace.QName
        // because the values are the same as the property names they match here.
        assert qname.toString()=="{http://www.alfresco.org/model/content/1.0}${value}"
    }
    def pmap=prefixMap(qmap)
    assert pmap["cm:name"]=="name"
    assert pmap["cm:title"]=="title"
}

test.run "setProperties", {
    test1.setProperties("cm:description":"Test description")
    assert test1.description=="Test description"
}

test.run "inheritPermissions", {
    assert test1.permissions.size()==1
    test1.inheritPermissions(false)
    assert test1.permissions.size()==0
}


test.run "setPermissions", {
    test1.setPermission("GROUP_EVERYONE","Consumer")
    assert test1.permissions.size()==1
}


test.run "deletePermissions", {
    test1.deletePermissions()
    assert test1.permissions.size()==0
}

test.run "randomName" , {
    def name1= randomName()
    def name2= randomName()
    assert name1.size()==32
    assert name2.size()==32
    assert name1!=name2
}

test.run "Search" , {
    def result= search(query:'''@cm\\:name:"new-user-email.html.ftl" ''')
    assert result.class==org.alfresco.repo.search.impl.lucene.SolrJSONResultSet
    assert result.numberFound==1
    result.each {
        assert it.nodeRef.name=="new-user-email.html.ftl"
    }
    // TODO check paging and sorting
}

targetNode==null
test.run "Transform" , {
    sourceNode=testHome.createNode("transform-source","cm:content",null,['cm:failedThumbnailSource'])
    sourceNode.text="Transformation test source"
    assert sourceNode.mimetype=="text/plain"
    targetNode=testHome.createNode("transform-target")
    transform(sourceNode,targetNode,"image/png")
}

test.run "Check transform result" , {
    assert targetNode.name=="transform-target"
    assert targetNode.mimetype=="image/png"
}

