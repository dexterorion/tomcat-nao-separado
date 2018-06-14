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
package org.apache.catalina.tribes.group;

import java.util.Arrays;
import java.util.List;

import org.apache.catalina.tribes.Member;

/**
 * <p>
 * Title: Membership - Absolute Order
 * </p>
 *
 * <p>
 * Description: A simple, yet agreeable and efficient way of ordering members
 * </p>
 * <p>
 * Ordering members can serve as a basis for electing a leader or coordinating
 * efforts.<br>
 * This is stinky simple, it works on the basis of the <code>Member</code>
 * interface and orders members in the following format:
 * 
 * <ol>
 * <li>IP comparison - byte by byte, lower byte higher rank</li>
 * <li>IPv4 addresses rank higher than IPv6, ie the lesser number of bytes, the
 * higher rank</li>
 * <li>Port comparison - lower port, higher rank</li>
 * <li>UniqueId comparison- byte by byte, lower byte higher rank</li>
 * </ol>
 * 
 * </p>
 *
 * @author Filip Hanik
 * @version 1.0
 * @see org.apache.catalina.tribes.Member
 */
public class AbsoluteOrder {
	private static final AbsoluteOrderAbsoluteComparator comp = new AbsoluteOrderAbsoluteComparator();

	public AbsoluteOrder() {
		super();
	}

	public static void absoluteOrder(Member[] members) {
		if (members == null || members.length <= 1)
			return;
		Arrays.sort(members, comp);
	}

	public static void absoluteOrder(List<Member> members) {
		if (members == null || members.size() <= 1)
			return;
		java.util.Collections.sort(members, comp);
	}

	public static AbsoluteOrderAbsoluteComparator getComp() {
		return comp;
	}
}
