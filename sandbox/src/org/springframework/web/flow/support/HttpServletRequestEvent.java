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
package org.springframework.web.flow.support;

import java.util.Date;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.util.StringUtils;
import org.springframework.web.flow.Event;
import org.springframework.web.flow.FlowConstants;

/**
 * A flow event that orginated from an incoming HTTP servlet request.
 * 
 * @author Keith Donald
 * @author Erwin Vervaet
 */
public class HttpServletRequestEvent extends Event {
	
	/**
	 * The response associated with the request that originated this event.
	 */
	private HttpServletResponse response;

	/**
	 * The event timestamp.
	 */
	private long timestamp = new Date().getTime();

	/**
	 * The name of the event id request parameter.
	 */
	private String eventIdParameterName;

	/**
	 * The name of the event id request attribute, if parameters are not used.
	 */
	private String eventIdAttributeName;

	/**
	 * The name of the current state id parameter.
	 */
	private String currentStateIdParameterName;

	/**
	 * The parameter name/value delimiter, for parsing when the parameter name
	 * and value is encoded within one parameter value.
	 */
	private String parameterNameValueDelimiter;

	/**
	 * Construct a flow event for the specified HTTP servlet request.
	 * @param request the HTTP servlet request
	 * @param response the HTTP servlet response associated with the request
	 */
	public HttpServletRequestEvent(HttpServletRequest request, HttpServletResponse response) {
		super(request);
		this.response = response;
		this.eventIdParameterName = FlowConstants.EVENT_ID_PARAMETER;
		this.eventIdAttributeName = FlowConstants.EVENT_ID_REQUEST_ATTRIBUTE;
		this.currentStateIdParameterName = FlowConstants.CURRENT_STATE_ID_PARAMETER;
		this.parameterNameValueDelimiter = "_";
	}

	/**
	 * Construct a flow event for the specified HTTP servlet request.
	 * @param request the HTTP servlet request
	 * @param response the HTTP servlet response associated with the request
	 * @param eventIdParameterName name of the event id parameter in the request
	 * @param eventIdAttributeName name of the event id attribute in the request
	 * @param currentStateIdParameterName name of the current state id parameter
	 *        in the request
	 * @param parameterValueDelimiter delimiter used when a parameter value is
	 *        sent as part of the name of a request parameter
	 *        (e.g. "_eventId_value=bar")
	 */
	public HttpServletRequestEvent(HttpServletRequest request, HttpServletResponse response,
			String eventIdParameterName, String eventIdAttributeName,
			String currentStateIdParameterName, String parameterValueDelimiter) {
		super(request);
		this.response = response;
		this.eventIdParameterName = eventIdParameterName;
		this.eventIdAttributeName = eventIdAttributeName;
		this.currentStateIdParameterName = currentStateIdParameterName;
		this.parameterNameValueDelimiter = parameterValueDelimiter;
	}

	/**
	 * Returns the HTTP servlet request that originated this event.
	 */
	public HttpServletRequest getRequest() {
		return (HttpServletRequest)getSource();
	}
	
	/**
	 * Returns the HTTP servlet response associated with the HTTP
	 * servlet request that originated this event.
	 */
	public HttpServletResponse getResponse() {
		return response;
	}

	public String getId() {
		String eventId = searchForRequestParameter(getRequest(), eventIdParameterName, parameterNameValueDelimiter);
		if (!StringUtils.hasText(eventId)) {
			// see if the eventId is set as a request attribute (put there by a
			// servlet filter)
			eventId = (String)getRequest().getAttribute(eventIdAttributeName);
		}
		return eventId;
	}

	/**
	 * Obtain a named parameter from an HTTP servlet request. This method will
	 * try to obtain a parameter value using the following algorithm:
	 * <ol>
	 * <li>Try to get the parameter value from the request using just the given
	 * <i>logical</i> name. This handles request parameters of the form
	 * <tt>logicalName = value</tt>. For normal request parameters, e.g.
	 * submitted using a hidden HTML form field, this will return the requested
	 * value.</li>
	 * <li>Try to obtain the parameter value from the parameter name, where the
	 * parameter name in the request is of the form
	 * <tt>logicalName_value = xyz</tt> with "_" being the specified
	 * delimiter. This deals with parameter values submitted using an HTML form
	 * submit button.</li>
	 * <li>If the value obtained in the previous step has a ".x" or ".y"
	 * suffix, remove that. This handles cases where the value was submitted
	 * using an HTML form image button. In this case the parameter in the
	 * request would actually be of the form <tt>logicalName_value.x = 123</tt>.
	 * </li>
	 * </ol>
	 * @param request the current HTTP request
	 * @param logicalName the <i>logical</i> name of the request parameter
	 * @param delimiter the delimiter to use
	 * @return the value of the parameter, or <code>null</code> if the
	 *         parameter does not exist in given request
	 */
	protected String searchForRequestParameter(HttpServletRequest request, String logicalName, String delimiter) {
		// first try to get it as a normal name=value parameter
		String value = request.getParameter(logicalName);
		if (value != null) {
			return value;
		}
		// if no value yet, try to get it as a name_value=xyz parameter
		String prefix = logicalName + delimiter;
		Enumeration paramNames = request.getParameterNames();
		while (paramNames.hasMoreElements()) {
			String paramName = (String)paramNames.nextElement();
			if (paramName.startsWith(prefix)) {
				value = paramName.substring(prefix.length());
				// support images buttons, which would submit parameters as
				// name_value.x=123
				if (value.endsWith(".x") || value.endsWith(".y")) {
					value = value.substring(0, value.length() - 2);
				}
				return value;
			}
		}
		// we couldn't find the parameter value
		return null;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public String getStateId() {
		return getRequest().getParameter(currentStateIdParameterName);
	}

	public Object getParameter(String parameterName) {
		return getRequest().getParameter(parameterName);
	}

	public Map getParameters() {
		return getRequest().getParameterMap();
	}
}