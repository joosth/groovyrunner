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
 * Test script for groovyRunner user and group methods
 * @author Joost Horward
 */

import org.alfresco.repo.transaction.AlfrescoTransactionSupport
import org.alfresco.repo.security.authentication.AuthenticationUtil

userName="groovyrunnertestuser"
person=null
test.run "Get transaction", {
    def trx=ServiceRegistry.getTransactionService().getUserTransaction();
    log trx.class.name

}

test.run "Remove groovyrunner user", {
    personService.deletePerson(userName)
    assert personService.personExists(userName)==false
}

test.run "Create groovyrunner user" , {
    person=createPerson(["cm:userName":userName,email:"test@open-t.nl"])
    log "The userName is ${person.userName}"
}

test.run "Ensure groovyrunner user exists" , {
    assert personService.personExists(userName)
}

test.run "Check person properties", {
    assert person.email=="test@open-t.nl"
    assert person.userName==userName
}

test.run "RunAs and currentUserIsAdmin" , {
    assert currentUserIsAdmin()
    def runAsUser=""
    def fullyAuthenticatedUser=""
    runAs userName , {
        runAsUser=AuthenticationUtil.getRunAsUser()
        fullyAuthenticatedUser=AuthenticationUtil.getFullyAuthenticatedUser()
    }
    // Asserts are outside of runAs on purpose because they do not show up properly from inside runAs
    assert runAsUser==userName
    assert fullyAuthenticatedUser==userName
}


groupName="groovyrunnertestgroup"
testGroup=null

test.run "Delete group" , {
    deleteGroup(groupName)
}

test.run "Create group" , {
    testGroup=createGroup(groupName)
    assert testGroup=="GROUP_${groupName}"
}

test.run "Check if group exists" , {
    assert authorityService.authorityExists(testGroup)
}

test.run "Add test user to group", {
    addToGroup(testGroup,userName)
}

test.run "Get authorities for test user" , {
    def auths = authorityService.getAuthoritiesForUser(userName)
    assert auths.contains(testGroup)
}

test.run "Remove test user from group", {
    removeFromGroup(testGroup,userName)
}

test.run "Get authorities for test user" , {
    def auths = authorityService.getAuthoritiesForUser(userName)
    assert !auths.contains(testGroup)
}

test.run "Delete group" , {
    deleteGroup(groupName)
}


test.run "Check if group is removed" , {
    assert !authorityService.authorityExists(testGroup)
}


test.run "Remove groovyrunner user", {
    personService.deletePerson(userName)
}