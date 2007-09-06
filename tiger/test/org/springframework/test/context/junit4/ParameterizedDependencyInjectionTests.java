/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.test.context.junit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import junit.framework.JUnit4TestAdapter;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.beans.Employee;
import org.springframework.beans.Pet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

/**
 * Simple JUnit 4 based unit test which demonstrates how to use JUnit's
 * {@link Parameterized} Runner in conjunction with
 * {@link ContextConfiguration @ContextConfiguration}, the
 * {@link DependencyInjectionTestExecutionListener}, and a
 * {@link TestContextManager} to provide dependency injection to a
 * <em>parameterized test instance</em>.
 *
 * @author Sam Brannen
 * @since 2.1
 */
@RunWith(Parameterized.class)
@ContextConfiguration
@TestExecutionListeners( { DependencyInjectionTestExecutionListener.class })
public class ParameterizedDependencyInjectionTests implements ApplicationContextAware {

	private static final List<Employee> employees = new ArrayList<Employee>();

	private ApplicationContext applicationContext;

	private final String employeeBeanName;

	private final String employeeName;

	@Autowired
	private Pet pet;

	private final TestContextManager testContextManager;


	public ParameterizedDependencyInjectionTests(final String employeeBeanName, final String employeeName)
			throws Exception {
		this.testContextManager = new TestContextManager(getClass());
		this.employeeBeanName = employeeBeanName;
		this.employeeName = employeeName;
	}

	@Parameters
	public static Collection<String[]> employeeData() {
		return Arrays.asList(new String[][] { { "employee1", "John Smith" }, { "employee2", "Jane Smith" } });
	}

	// XXX Remove suite() once we've migrated to Ant 1.7 with JUnit 4 support.
	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(ParameterizedDependencyInjectionTests.class);
	}

	@BeforeClass
	public static void clearEmployees() {
		employees.clear();
	}

	@Before
	public void injectDependencies() throws Exception {

		this.testContextManager.prepareTestInstance(this);
	}

	public final void setApplicationContext(final ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Test
	public final void verifyPetAndEmployee() {

		// Verifying dependency injection:
		assertNotNull("The pet field should have been autowired.", this.pet);

		// Verifying 'parameterized' support:
		final Employee employee = (Employee) this.applicationContext.getBean(this.employeeBeanName);
		employees.add(employee);
		assertEquals("Verifying the name of the employee configured as bean [" + this.employeeBeanName + "].",
				this.employeeName, employee.getName());
	}

	@AfterClass
	public static void verifyNumParameterizedRuns() {
		assertEquals("Verifying the number of times the parameterized test method was executed.",
				employeeData().size(), employees.size());
	}

}
