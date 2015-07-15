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
// Transaction listener packages
import org.alfresco.repo.transaction.TransactionListener;
import org.alfresco.repo.transaction.TransactionListenerAdapter;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;

/**
 * Transaction listener that executes a given closure upon afterCommit
 * @author Joost Horward
 */
class AfterTransactionListener extends TransactionListenerAdapter
    {
        Closure closure
        public AfterTransactionListener(Closure closure) {
            this.closure=closure
        }

        @Override
        public void afterCommit()
        {
            closure()
        }
  }