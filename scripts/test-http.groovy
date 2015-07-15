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
 * Test script for groovyRunner http helper
 * @author Joost Horward
 */

import groovy.json.JsonSlurper
test.run "Get site info from localhost [http.get]" , {
    http.addCredentials(host:'localhost',port:8080,username:'admin',password:'admin')
    def s = http.get(url:'http://localhost:8080/alfresco/service/api/sites/swsdp')
    def slurper = new JsonSlurper()
    def result = slurper.parseText(s)
    assert result.sitePreset=="site-dashboard"
}

testNode=null

test.run "Delete test node" , {
    testNode=findNode("groovyrunner-tests/httphelper-test")
    if (testNode) {
        nodeService.deleteNode(testNode)
    }
}

test.run "Create test node" , {
    testHome=findNode("groovyrunner-tests")
    testNode=testHome.createNode("httphelper-test")
}

test.run "Add comment [http.post]" , {
    def text=""" { "title": "Comment title", "content":"Comment content" } """
    def s = http.post(url:"http://localhost:8080/alfresco/service/api/node/${testNode.toString().replace(':/','')}/comments",text:text)

    def slurper = new JsonSlurper()
    def result = slurper.parseText(s)
    assert result.item.title=="Comment title"
    assert result.item.content=="Comment content"
}

