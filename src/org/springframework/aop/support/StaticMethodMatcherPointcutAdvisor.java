/*
 * The Spring Framework is published under the terms
 * of the Apache Software License.
 */

package org.springframework.aop.support;

import org.springframework.aop.Pointcut;
import org.springframework.aop.PointcutAdvisor;

/**
 * Convenient superclass for Advisors that are also static pointcuts.
 * @author Rod Johnson
 * @version $Id: StaticMethodMatcherPointcutAdvisor.java,v 1.3 2004-02-22 09:48:24 johnsonr Exp $
 */
public abstract class StaticMethodMatcherPointcutAdvisor extends StaticMethodMatcherPointcut implements PointcutAdvisor {

	private Object advice;
	
	public StaticMethodMatcherPointcutAdvisor() {
	}

	public StaticMethodMatcherPointcutAdvisor(Object advice) {
		this.advice = advice;
	}

	public boolean isPerInstance() {
		throw new UnsupportedOperationException("perInstance property of Advisor is not yet supported in Spring");
	}

	public Pointcut getPointcut() {
		return this;
	}

	/**
	 * @return
	 */
	public Object getAdvice() {
		return advice;
	}

	/**
	 * @param object
	 */
	public void setAdvice(Object object) {
		advice = object;
	}

}
