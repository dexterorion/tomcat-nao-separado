/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.el;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import javax.el.ELContext;
import javax.el.PropertyNotWritableException;
import javax.el.ValueExpression;

import org.apache.el.lang.ELSupport;
import org.apache.el.util.MessageFactory;
import org.apache.el.util.ReflectionUtil;


public final class ValueExpressionLiteral extends ValueExpression implements
        Externalizable {

    private static final long serialVersionUID = 1L;

    private Object value;

    private Class<?> expectedType;

    public ValueExpressionLiteral() {
        super();
    }

    public ValueExpressionLiteral(Object value, Class<?> expectedType) {
        this.setValueData(value);
        this.setExpectedTypeData(expectedType);
    }

    @Override
    public Object getValue(ELContext context) {
        if (this.getExpectedTypeData() != null) {
            return ELSupport.coerceToType(this.getValueData(), this.getExpectedTypeData());
        }
        return this.getValueData();
    }

    @Override
    public void setValue(ELContext context, Object value) {
        throw new PropertyNotWritableException(MessageFactory.get(
                "error.value.literal.write", this.getValueData()));
    }

    @Override
    public boolean isReadOnly(ELContext context) {
        return true;
    }

    @Override
    public Class<?> getType(ELContext context) {
        return (this.getValueData() != null) ? this.getValueData().getClass() : null;
    }

    @Override
    public Class<?> getExpectedType() {
        return this.getExpectedTypeData();
    }

    @Override
    public String getExpressionString() {
        return (this.getValueData() != null) ? this.getValueData().toString() : null;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof ValueExpressionLiteral && this
                .equals((ValueExpressionLiteral) obj));
    }

    public boolean equals(ValueExpressionLiteral ve) {
        return (ve != null && (this.getValueData() != null && ve.getValueData() != null && (this.getValueData() == ve.getValueData() || this.getValueData()
                .equals(ve.getValueData()))));
    }

    @Override
    public int hashCode() {
        return (this.getValueData() != null) ? this.getValueData().hashCode() : 0;
    }

    @Override
    public boolean isLiteralText() {
        return true;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(this.getValueData());
        out.writeUTF((this.getExpectedTypeData() != null) ? this.getExpectedTypeData().getName()
                : "");
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException,
            ClassNotFoundException {
        this.setValueData(in.readObject());
        String type = in.readUTF();
        if (!"".equals(type)) {
            this.setExpectedTypeData(ReflectionUtil.forName(type));
        }
    }

	public Object getValueData() {
		return value;
	}

	public void setValueData(Object value) {
		this.value = value;
	}

	public Class<?> getExpectedTypeData() {
		return expectedType;
	}

	public void setExpectedTypeData(Class<?> expectedType) {
		this.expectedType = expectedType;
	}
}
