/*
 * The Spring Framework is published under the terms
 * of the Apache Software License.
 */
 
package org.springframework.beans.factory;

/**
 * Interface to be implemented by beans that need to react
 * once all their properties have been set by a BeanFactory:
 * for example, to perform initialization, or merely to check
 * that all mandatory properties have been set.
 * 
 * <strong>Note, </strong>If the bean also implements BeanFactoryAware, this
 * method will be invoked before BeanFactoryAware's <code>setBeanFactory</code>. 
 * 
 * @author Rod Johnson
 * @version $Revision: 1.2 $
 * 
 * @see org.springframework.beans.factory.BeanFactoryAware; 
 */
public interface InitializingBean {
	
	/**
	 * Invoked by a BeanFactory after it has set all bean properties supplied.
	 * <p>This method allows the bean instance to perform initialization only
	 * possible when all bean properties have been set and to throw an
	 * exception in the event of misconfiguration.
	 * @throws Exception in the event of misconfiguration (such
	 * as failure to set an essential property) or if initialization fails.
	 */
	void afterPropertiesSet() throws Exception;

}
