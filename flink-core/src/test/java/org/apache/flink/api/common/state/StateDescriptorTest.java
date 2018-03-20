/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.api.common.state;

import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.api.common.typeutils.base.StringSerializer;
import org.apache.flink.api.java.typeutils.runtime.kryo.KryoSerializer;
import org.apache.flink.core.fs.Path;
import org.apache.flink.core.testutils.CommonTestUtils;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests for the common/shared functionality of {@link StateDescriptor}.
 */
public class StateDescriptorTest {

	@Test
	public void testInitializeWithSerializer() throws Exception {
		final TypeSerializer<String> serializer = StringSerializer.INSTANCE;
		final TestStateDescriptor<String> descr = new TestStateDescriptor<>("test", serializer);

		assertTrue(descr.isSerializerInitialized());
		assertNotNull(descr.getSerializer());
		assertTrue(descr.getSerializer() instanceof StringSerializer);

		// this should not have any effect
		descr.initializeSerializerUnlessSet(new ExecutionConfig());
		assertTrue(descr.isSerializerInitialized());
		assertNotNull(descr.getSerializer());
		assertTrue(descr.getSerializer() instanceof StringSerializer);

		TestStateDescriptor<String> clone = CommonTestUtils.createCopySerializable(descr);
		assertTrue(clone.isSerializerInitialized());
		assertNotNull(clone.getSerializer());
		assertTrue(clone.getSerializer() instanceof StringSerializer);
	}

	@Test
	public void testInitializeSerializerBeforeSerialization() throws Exception {
		final TestStateDescriptor<String> descr = new TestStateDescriptor<>("test", String.class);

		assertFalse(descr.isSerializerInitialized());
		try {
			descr.getSerializer();
			fail("should fail with an exception");
		} catch (IllegalStateException ignored) {}

		descr.initializeSerializerUnlessSet(new ExecutionConfig());

		assertTrue(descr.isSerializerInitialized());
		assertNotNull(descr.getSerializer());
		assertTrue(descr.getSerializer() instanceof StringSerializer);

		TestStateDescriptor<String> clone = CommonTestUtils.createCopySerializable(descr);

		assertTrue(clone.isSerializerInitialized());
		assertNotNull(clone.getSerializer());
		assertTrue(clone.getSerializer() instanceof StringSerializer);
	}

	@Test
	public void testInitializeSerializerAfterSerialization() throws Exception {
		final TestStateDescriptor<String> descr = new TestStateDescriptor<>("test", String.class);

		assertFalse(descr.isSerializerInitialized());
		try {
			descr.getSerializer();
			fail("should fail with an exception");
		} catch (IllegalStateException ignored) {}

		TestStateDescriptor<String> clone = CommonTestUtils.createCopySerializable(descr);

		assertFalse(clone.isSerializerInitialized());
		try {
			clone.getSerializer();
			fail("should fail with an exception");
		} catch (IllegalStateException ignored) {}

		clone.initializeSerializerUnlessSet(new ExecutionConfig());

		assertTrue(clone.isSerializerInitialized());
		assertNotNull(clone.getSerializer());
		assertTrue(clone.getSerializer() instanceof StringSerializer);
	}

	@Test
	public void testInitializeSerializerAfterSerializationWithCustomConfig() throws Exception {
		// guard our test assumptions.
		assertEquals("broken test assumption", -1,
				new KryoSerializer<>(String.class, new ExecutionConfig()).getKryo()
						.getRegistration(File.class).getId());

		final ExecutionConfig config = new ExecutionConfig();
		config.registerKryoType(File.class);

		final TestStateDescriptor<Path> original = new TestStateDescriptor<>("test", Path.class);
		TestStateDescriptor<Path> clone = CommonTestUtils.createCopySerializable(original);

		clone.initializeSerializerUnlessSet(config);

		// serialized one (later initialized) carries the registration
		assertTrue(((KryoSerializer<?>) clone.getSerializer()).getKryo()
				.getRegistration(File.class).getId() > 0);
	}

	// ------------------------------------------------------------------------

	private static class TestStateDescriptor<T> extends StateDescriptor<State, T> {

		private static final long serialVersionUID = 1L;

		TestStateDescriptor(String name, TypeSerializer<T> serializer) {
			super(name, serializer, null);
		}

		TestStateDescriptor(String name, TypeInformation<T> typeInfo) {
			super(name, typeInfo, null);
		}

		TestStateDescriptor(String name, Class<T> type) {
			super(name, type, null);
		}

		@Override
		public State bind(StateBinder stateBinder) throws Exception {
			throw new UnsupportedOperationException();
		}

		@Override
		public Type getType() {
			throw new UnsupportedOperationException();
		}

		@Override
		public int hashCode() {
			return 584523;
		}

		@Override
		public boolean equals(Object o) {
			return o != null && o.getClass() == TestStateDescriptor.class;
		}
	}
}
