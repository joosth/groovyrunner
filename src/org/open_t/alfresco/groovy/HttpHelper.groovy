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
import org.apache.http.*;
import org.apache.http.client.*;
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.auth.*
import org.apache.http.client.methods.*
import org.apache.http.entity.*
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.protocol.HttpContext
import org.apache.http.util.EntityUtils;
import org.springframework.web.multipart.*
import java.net.*;

/**
 * Helper for HTTP access
 * @author Joost Horward
 */
class HttpHelper {

    CredentialsProvider credentialsProvider

    // default request headers
    def headers=["Content-Type": "application/json","Accept":"application/json"]

    /**
     * Add credentials to the stored credentials provider
     * parameters:
     * - host
     * - port
     * - realm
     * - schema
     * - username
     * - password
     */
    def addCredentials(params) {
        AuthScope scope=new AuthScope(params.host,params.port,params.realm,params.scheme)
        def credentials = new UsernamePasswordCredentials(params.username, params.password);
        credentialsProvider.setCredentials(scope,credentials)
    }

    // Any initialization should go here
    def init() {
        credentialsProvider = new BasicCredentialsProvider();
    }

    /**
     * Read from url, return text
     * params:
     * - url
     * - credentialsProvider
     */
    def read(params) {
		def client=new DefaultHttpClient();
        if (params.credentialsProvider) {
            client.setCredentialsProvider(params.credentialsProvider)
        } else {
            client.setCredentialsProvider(credentialsProvider)
        }
    	try {
    		// Encode URL. This will encode a space as %20 etc.
    		// Note: the URI constructor that accepts a single string does not perform escaping
    		def url=new URI(null,params.url,null).toString()
			HttpGet httpget = new HttpGet(url);
            def requestHeaders=params.headers?:headers

            requestHeaders.each { key,value ->
                httpget.addHeader(key,value)
            }

			HttpResponse response = client.execute(httpget);
			HttpEntity entity = response.getEntity();

			if (response.statusLine.statusCode==200) {
				def theContent= entity.content.text
				return theContent
			} else {
				return null
			}

		} catch(Exception e) {
            println "HTTP GET error ${e}"
			return null
    	} finally {
			client.getConnectionManager().shutdown();
    	}
    }


    /**
     * Write to URL, return text
     * params:
     * - method
     * - url
     * - text
     * - mimeType
     * - credentialsProvider
     */
    def write (params) {
        def url=new URI(null,params.url,null).toString()
		def rsp
        def client=new DefaultHttpClient();
         if (params.credentialsProvider) {
            client.setCredentialsProvider(params.credentialsProvider)
        } else {
            client.setCredentialsProvider(credentialsProvider)
        }

		try {
			def httpwrite
            switch(params.method) {
                case "PUT":
                    httpwrite = new HttpPut(url);
                break
                case "POST":
                    httpwrite = new HttpPost(url);
                break
                case "DELETE":
                    httpwrite = new HttpDelete(url);
                break
            }

            def requestHeaders=params.headers?:headers
            requestHeaders.each { key,value ->
                httpwrite.addHeader(key,value)
            }

            if (params.text) {
                def entity= new StringEntity(params.text,params.mimeType,null)
                httpwrite.setEntity(entity)
            }

			HttpResponse response = client.execute(httpwrite);

			HttpEntity resEntity = response.getEntity();
			rsp=resEntity.content.text

    	} catch (Exception e) {
    		println "HTTP ${method} error ${e}"
       	}  finally {
			client.getConnectionManager().shutdown();
		}
        return rsp
	}

    def get(params) { return read(params) }
	def post(params) { params.method="POST" ; return write(params) }
    def put(params) { params.method="PUT" ; return write(params) }
    def delete(params) { params.method="DELETE" ; return write(params) }
}

