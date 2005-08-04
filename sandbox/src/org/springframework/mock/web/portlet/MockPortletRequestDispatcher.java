/*
 * Copyright 2002-2005 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.mock.web.portlet;

import java.io.IOException;

import javax.portlet.PortletException;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Mock implementation of the PortletRequestDispatcher interface.
 *
 * @author John A. Lewis
 */
public class MockPortletRequestDispatcher implements PortletRequestDispatcher {

	private final Log logger = LogFactory.getLog(getClass());

	private final String url;

	public MockPortletRequestDispatcher(String url) {
		this.url = url;
	}

	
	//---------------------------------------------------------------------
	// PortletRequestDispatcher methods
	//---------------------------------------------------------------------
	
    public void include(RenderRequest request, RenderResponse response)
            throws PortletException, IOException {
		if (!(response instanceof MockRenderResponse))
			throw new IllegalArgumentException("MockPortletRequestDispatcher requires MockRenderResponse");
		((MockRenderResponse) response).setIncludedUrl(this.url);
		if (logger.isDebugEnabled())
			logger.debug("MockPortletRequestDispatcher: including URL [" + this.url + "]");
    }
    
}
