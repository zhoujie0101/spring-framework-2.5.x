/*
 * Copyright 2002-2004 the original author or authors.
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
package org.springframework.functor;

import java.io.Serializable;

/**
 * A predicate is a boolean expression that takes one argument and returns
 * a boolean result.
 * <p><p>
 * A unary predicate is a function object that tests a single argument
 * against some conditional expression.  For example, a "required" unary
 * predicate may return true if the provided argument is non-null, and
 * false otherwise.
 * 
 * @author  Keith Donald
 */
public interface UnaryPredicate extends Serializable {
    
    /**
     * Test the provided argument against this predicate's condition.
     * 
     * @param argument the argument value
     * @return true if the condition was satisfied, false otherwise
     */
    public boolean test(Object argument);
}
