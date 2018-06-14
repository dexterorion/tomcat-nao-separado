/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tomcat.util.bcel.classfile;

import org.apache.tomcat.util.bcel.Constants;

public class SimpleElementValue extends ElementValue
{
    private final int index;

    public SimpleElementValue(int type, int index, ConstantPool cpool) {
        super(type, cpool);
        this.index = index;
    }

    /**
     * @return Value entry index in the cpool
     */
    public int getIndex()
    {
        return index;
    }
    

    // Whatever kind of value it is, return it as a string
    @Override
    public String stringifyValue()
    {
        switch (getType())
        {
        case 'I':
            ConstantInteger c = (ConstantInteger) getCpool().getConstant(getIndex(),
                    Constants.CONSTANT_Integer);
            return Integer.toString(c.getBytes());
        case 'J':
            ConstantLong j = (ConstantLong) getCpool().getConstant(getIndex(),
                    Constants.CONSTANT_Long);
            return Long.toString(j.getBytes());
        case 'D':
            ConstantDouble d = (ConstantDouble) getCpool().getConstant(getIndex(),
                    Constants.CONSTANT_Double);
            return Double.toString(d.getBytes());
        case 'F':
            ConstantFloat f = (ConstantFloat) getCpool().getConstant(getIndex(),
                    Constants.CONSTANT_Float);
            return Float.toString(f.getBytes());
        case 'S':
            ConstantInteger s = (ConstantInteger) getCpool().getConstant(getIndex(),
                    Constants.CONSTANT_Integer);
            return Integer.toString(s.getBytes());
        case 'B':
            ConstantInteger b = (ConstantInteger) getCpool().getConstant(getIndex(),
                    Constants.CONSTANT_Integer);
            return Integer.toString(b.getBytes());
        case 'C':
            ConstantInteger ch = (ConstantInteger) getCpool().getConstant(
                    getIndex(), Constants.CONSTANT_Integer);
            return String.valueOf((char)ch.getBytes());
        case 'Z':
            ConstantInteger bo = (ConstantInteger) getCpool().getConstant(
                    getIndex(), Constants.CONSTANT_Integer);
            if (bo.getBytes() == 0) {
                return "false";
            }
            return "true";
        case 's':
            ConstantUtf8 cu8 = (ConstantUtf8) getCpool().getConstant(getIndex(),
                    Constants.CONSTANT_Utf8);
            return cu8.getBytes();
        default:
            throw new RuntimeException(
                    "SimpleElementValue class does not know how to stringify type "
                            + getType());
        }
    }
}
