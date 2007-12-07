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

package org.springframework.jdbc.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;

/**
 * Utility methods for PreparedStatementSetter/Creator and CallableStatementCreator
 * implementations, providing sophisticated parameter management (including support
 * for LOB values).
 *
 * <p>Used by PreparedStatementCreatorFactory and CallableStatementCreatorFactory,
 * but also available for direct use in custom setter/creator implementations.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 1.1
 * @see PreparedStatementSetter
 * @see PreparedStatementCreator
 * @see CallableStatementCreator
 * @see PreparedStatementCreatorFactory
 * @see CallableStatementCreatorFactory
 * @see SqlParameter
 * @see SqlTypeValue
 * @see org.springframework.jdbc.core.support.SqlLobValue
 */
public abstract class StatementCreatorUtils {

	private static final Log logger = LogFactory.getLog(StatementCreatorUtils.class);


	/**
	 * Set the value for a parameter. The method used is based on the SQL type
	 * of the parameter and we can handle complex types like arrays and LOBs.
	 * @param ps the prepared statement or callable statement
	 * @param paramIndex index of the parameter we are setting
	 * @param param the parameter as it is declared including type
	 * @param inValue the value to set
	 * @throws SQLException if thrown by PreparedStatement methods
	 */
	public static void setParameterValue(
	    PreparedStatement ps, int paramIndex, SqlParameter param, Object inValue)
	    throws SQLException {

		setParameterValueInternal(ps, paramIndex, param.getSqlType(), param.getTypeName(), param.getScale(), inValue);
	}

	/**
	 * Set the value for a parameter. The method used is based on the SQL type
	 * of the parameter and we can handle complex types like arrays and LOBs.
	 * @param ps the prepared statement or callable statement
	 * @param paramIndex index of the parameter we are setting
	 * @param sqlType the SQL type of the parameter
	 * @param inValue the value to set (plain value or a SqlTypeValue)
	 * @throws SQLException if thrown by PreparedStatement methods
	 * @see SqlTypeValue
	 */
	public static void setParameterValue(
	    PreparedStatement ps, int paramIndex, int sqlType, Object inValue)
	    throws SQLException {

		setParameterValueInternal(ps, paramIndex, sqlType, null, null, inValue);
	}

	/**
	 * Set the value for a parameter. The method used is based on the SQL type
	 * of the parameter and we can handle complex types like arrays and LOBs.
	 * @param ps the prepared statement or callable statement
	 * @param paramIndex index of the parameter we are setting
	 * @param sqlType the SQL type of the parameter
	 * @param typeName the type name of the parameter
	 * (optional, only used for SQL NULL and SqlTypeValue)
	 * @param inValue the value to set (plain value or a SqlTypeValue)
	 * @throws SQLException if thrown by PreparedStatement methods
	 * @see SqlTypeValue
	 */
	public static void setParameterValue(
	    PreparedStatement ps, int paramIndex, int sqlType, String typeName, Object inValue)
	    throws SQLException {

		setParameterValueInternal(ps, paramIndex, sqlType, typeName, null, inValue);
	}

	/**
	 * Set the value for a parameter. The method used is based on the SQL type
	 * of the parameter and we can handle complex types like arrays and LOBs.
	 * @param ps the prepared statement or callable statement
	 * @param paramIndex index of the parameter we are setting
	 * @param sqlType the SQL type of the parameter
	 * @param typeName the type name of the parameter
	 * (optional, only used for SQL NULL and SqlTypeValue)
	 * @param scale the number of digits after the decimal point
	 * (for DECIMAL and NUMERIC types)
	 * @param inValue the value to set (plain value or a SqlTypeValue)
	 * @throws SQLException if thrown by PreparedStatement methods
	 * @see SqlTypeValue
	 */
	private static void setParameterValueInternal(
	    PreparedStatement ps, int paramIndex, int sqlType, String typeName, Integer scale, Object inValue)
	    throws SQLException {

		String typeNameToUse = typeName;
		int sqlTypeToUse = sqlType;
		Object inValueToUse = inValue;

		// override type info?
		if (inValue instanceof SqlParameterValue) {
			SqlParameterValue parameterValue = (SqlParameterValue)inValue;
			if (logger.isDebugEnabled()) {
				logger.debug("Overriding typeinfo with runtime info from SqlParameterValue: column index " + paramIndex +
						", SQL type " + parameterValue.getSqlType() +
						", Type name " + parameterValue.getTypeName());
			}
			if (parameterValue.getSqlType() != SqlTypeValue.TYPE_UNKNOWN) {
				sqlTypeToUse = parameterValue.getSqlType();
			}
			if (parameterValue.getTypeName() != null) {
				typeNameToUse = parameterValue.getTypeName();
			}
			inValueToUse = parameterValue.getValue();
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Setting SQL statement parameter value: column index " + paramIndex +
					", parameter value [" + inValueToUse +
					"], value class [" + (inValueToUse != null ? inValueToUse.getClass().getName() : "null") +
					"], SQL type " + (sqlTypeToUse == SqlTypeValue.TYPE_UNKNOWN ? "unknown" : Integer.toString(sqlTypeToUse)));
		}

		if (inValueToUse == null) {
			if (sqlTypeToUse == SqlTypeValue.TYPE_UNKNOWN) {
				boolean useSetObject = false;
				try {
					DatabaseMetaData dbmd = ps.getConnection().getMetaData();
					String databaseProductName = dbmd.getDatabaseProductName();
					String jdbcDriverName = dbmd.getDriverName();
					useSetObject = (databaseProductName.indexOf("Informix") != -1 ||
							jdbcDriverName.indexOf("Apache Derby Embedded") != -1);
				}
				catch (Throwable ex) {
					logger.debug("Could not check database or driver name", ex);
				}
				if (useSetObject) {
					ps.setObject(paramIndex, null);
				}
				else {
					ps.setNull(paramIndex, Types.NULL);
				}
			}
			else if (typeNameToUse != null) {
				ps.setNull(paramIndex, sqlTypeToUse, typeNameToUse);
			}
			else {
				ps.setNull(paramIndex, sqlTypeToUse);
			}
		}

		else {  // inValue != null
			if (inValueToUse instanceof SqlTypeValue) {
				((SqlTypeValue) inValueToUse).setTypeValue(ps, paramIndex, sqlTypeToUse, typeNameToUse);
			}
			else if (sqlTypeToUse == Types.VARCHAR) {
				ps.setString(paramIndex, inValueToUse.toString());
			}
			else if (sqlTypeToUse == Types.DECIMAL || sqlTypeToUse == Types.NUMERIC) {
				if (inValueToUse instanceof BigDecimal) {
					ps.setBigDecimal(paramIndex, (BigDecimal) inValueToUse);
				}
				else if (scale != null) {
					ps.setObject(paramIndex, inValueToUse, sqlTypeToUse, scale.intValue());
				}
				else {
					ps.setObject(paramIndex, inValueToUse, sqlTypeToUse);
				}
			}
			else if (sqlTypeToUse == Types.DATE) {
				if (inValueToUse instanceof java.util.Date) {
					if (inValueToUse instanceof java.sql.Date) {
						ps.setDate(paramIndex, (java.sql.Date) inValueToUse);
					}
					else {
						ps.setDate(paramIndex, new java.sql.Date(((java.util.Date) inValueToUse).getTime()));
					}
				}
				else if (inValueToUse instanceof Calendar) {
					Calendar cal = (Calendar) inValueToUse;
					ps.setDate(paramIndex, new java.sql.Date(cal.getTime().getTime()), cal);
				}
				else {
					ps.setObject(paramIndex, inValueToUse, Types.DATE);
				}
			}
			else if (sqlTypeToUse == Types.TIME) {
				if (inValueToUse instanceof java.util.Date) {
					if (inValueToUse instanceof java.sql.Time) {
						ps.setTime(paramIndex, (java.sql.Time) inValueToUse);
					}
					else {
						ps.setTime(paramIndex, new java.sql.Time(((java.util.Date) inValueToUse).getTime()));
					}
				}
				else if (inValueToUse instanceof Calendar) {
					Calendar cal = (Calendar) inValueToUse;
					ps.setTime(paramIndex, new java.sql.Time(cal.getTime().getTime()), cal);
				}
				else {
					ps.setObject(paramIndex, inValueToUse, Types.TIME);
				}
			}
			else if (sqlTypeToUse == Types.TIMESTAMP) {
				if (inValueToUse instanceof java.util.Date) {
					if (inValueToUse instanceof java.sql.Timestamp) {
						ps.setTimestamp(paramIndex, (java.sql.Timestamp) inValueToUse);
					}
					else {
						ps.setTimestamp(paramIndex, new java.sql.Timestamp(((java.util.Date) inValueToUse).getTime()));
					}
				}
				else if (inValueToUse instanceof Calendar) {
					Calendar cal = (Calendar) inValueToUse;
					ps.setTimestamp(paramIndex, new java.sql.Timestamp(cal.getTime().getTime()), cal);
				}
				else {
					ps.setObject(paramIndex, inValueToUse, Types.TIMESTAMP);
				}
			}
			else if (sqlTypeToUse == SqlTypeValue.TYPE_UNKNOWN) {
				if (isStringValue(inValueToUse)) {
					ps.setString(paramIndex, inValueToUse.toString());
				}
				else if (isDateValue(inValueToUse)) {
					ps.setTimestamp(paramIndex, new java.sql.Timestamp(((java.util.Date) inValueToUse).getTime()));
				}
				else if (inValueToUse instanceof Calendar) {
					Calendar cal = (Calendar) inValueToUse;
					ps.setTimestamp(paramIndex, new java.sql.Timestamp(cal.getTime().getTime()));
				}
				else {
					// Fall back to generic setObject call without SQL type specified.
					ps.setObject(paramIndex, inValueToUse);
				}
			}
			else {
				// Fall back to generic setObject call with SQL type specified.
				ps.setObject(paramIndex, inValueToUse, sqlTypeToUse);
			}
		}
	}

	/**
	 * Check whether the given value can be treated as a String value.
	 */
	private static boolean isStringValue(Object inValue) {
		// Consider any CharSequence (including JDK 1.5's StringBuilder) as String.
		return (inValue instanceof CharSequence || inValue instanceof StringWriter);
	}

	/**
	 * Check whether the given value is a <code>java.util.Date</code>
	 * (but not one of the JDBC-specific subclasses).
	 */
	private static boolean isDateValue(Object inValue) {
		return (inValue instanceof java.util.Date && !(inValue instanceof java.sql.Date ||
				inValue instanceof java.sql.Time || inValue instanceof java.sql.Timestamp));
	}

	/**
	 * Clean up all resources held by parameter values which were passed to an
	 * execute method. This is for example important for closing LOB values.
	 * @param paramValues parameter values supplied. May be <code>null</code>.
	 * @see DisposableSqlTypeValue#cleanup()
	 * @see org.springframework.jdbc.core.support.SqlLobValue#cleanup()
	 */
	public static void cleanupParameters(Object[] paramValues) {
		if (paramValues != null) {
			cleanupParameters(Arrays.asList(paramValues));
		}
	}

	/**
	 * Clean up all resources held by parameter values which were passed to an
	 * execute method. This is for example important for closing LOB values.
	 * @param paramValues parameter values supplied. May be <code>null</code>.
	 * @see DisposableSqlTypeValue#cleanup()
	 * @see org.springframework.jdbc.core.support.SqlLobValue#cleanup()
	 */
	public static void cleanupParameters(Collection paramValues) {
		if (paramValues != null) {
			for (Iterator it = paramValues.iterator(); it.hasNext();) {
				Object inValue = it.next();
				if (inValue instanceof DisposableSqlTypeValue) {
					((DisposableSqlTypeValue) inValue).cleanup();
				}
			}
		}
	}

}
