// CHECKSTYLE:OFF
/**
This file is part of the CSC4509 teaching unit.

Copyright (C) 2012-2019 Télécom SudParis

This is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This software platform is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with the CSC4509 teaching unit. If not, see <http://www.gnu.org/licenses/>.

Initial developer(s): Denis Conan
Contributor(s):
 */
package chat;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import chat.common.VectorClock;

/**
 * This class contains the unit tests of the class <tt>VectorClock</tt>, which
 * implements the concept of vector clock as a map of (process id, clock value).
 * The peculiarity of the implementation, that is the parts that need careful
 * testing, are the map is dynamic: the set of values is not once for all, but
 * new entries are added the algorithm moves forward.
 * 
 * TODO the tests are not programmed from a decision table ---i.e., they were
 * not prepared. Therefore, a re-factoring is needed.
 * 
 * @author Denis Conan
 */
public class TestVectorClock {

	private VectorClock vc1;

	@Before
	public void setUp() throws Exception {
		vc1 = new VectorClock();
	}

	@After
	public void tearDown() throws Exception {
		vc1 = null;
	}

	@Test
	public void testGetEntry() throws Exception {
		Assert.assertNotNull(vc1.getEntry(0));
		Assert.assertNotNull(vc1.getEntry(0));
		Assert.assertNotNull(vc1.getEntry(1));
		Assert.assertNotNull(vc1.getEntry(1));
	}

	@Test
	public void testGetSetEntry() throws Exception {
		vc1.setEntry(0, 0);
		Assert.assertNotNull(vc1.getEntry(0));
		Assert.assertEquals(0, vc1.getEntry(0));
		Assert.assertNotNull(vc1.getEntry(1));
		vc1.setEntry(0, 1);
		Assert.assertNotNull(vc1.getEntry(0));
		Assert.assertEquals(1, vc1.getEntry(0));
		Assert.assertNotNull(vc1.getEntry(1));
		vc1.setEntry(1, 10);
		Assert.assertNotNull(vc1.getEntry(1));
		Assert.assertEquals(10, vc1.getEntry(1));
		Assert.assertNotNull(vc1.getEntry(0));
		Assert.assertNotSame(Integer.valueOf(10), vc1.getEntry(0));
	}

	@Test
	public void testIncrement() throws Exception {
		vc1.setEntry(0, 0);
		Assert.assertNotNull(vc1.getEntry(0));
		Assert.assertEquals(0, vc1.getEntry(0));
		vc1.incrementEntry(0);
		Assert.assertEquals(1, vc1.getEntry(0));
		Assert.assertNotNull(vc1.getEntry(1));
		vc1.incrementEntry(1);
		Assert.assertEquals(1, vc1.getEntry(0));
		Assert.assertEquals(1, vc1.getEntry(1));
		vc1.incrementEntry(1);
		Assert.assertEquals(1, vc1.getEntry(0));
		Assert.assertEquals(2, vc1.getEntry(1));
		vc1.incrementEntry(2);
		Assert.assertEquals(1, vc1.getEntry(0));
		Assert.assertEquals(2, vc1.getEntry(1));
		Assert.assertEquals(1, vc1.getEntry(2));
	}

	@Test
	public void testMax() throws Exception {
		vc1.max(null);
		Assert.assertNotNull(vc1.getEntry(0));
		Assert.assertEquals(0, vc1.getEntry(0));
		VectorClock vc2 = new VectorClock();
		vc1.max(vc2);
		Assert.assertNotNull(vc1.getEntry(0));
		Assert.assertEquals(0, vc1.getEntry(0));
		vc2.setEntry(1, 10);
		vc1.max(vc2);
		Assert.assertNotNull(vc1.getEntry(0));
		Assert.assertEquals(0, vc1.getEntry(0));
		Assert.assertNotNull(vc1.getEntry(1));
		Assert.assertEquals(10, vc1.getEntry(1));
		vc1.setEntry(2, 10);
		vc1.max(vc2);
		Assert.assertNotNull(vc1.getEntry(0));
		Assert.assertEquals(0, vc1.getEntry(0));
		Assert.assertNotNull(vc1.getEntry(1));
		Assert.assertEquals(10, vc1.getEntry(1));
		Assert.assertNotNull(vc1.getEntry(2));
		Assert.assertEquals(10, vc1.getEntry(2));
		vc2.setEntry(1, 100);
		vc1.max(vc2);
		Assert.assertNotNull(vc1.getEntry(0));
		Assert.assertEquals(0, vc1.getEntry(0));
		Assert.assertNotNull(vc1.getEntry(1));
		Assert.assertEquals(100, vc1.getEntry(1));
		Assert.assertNotNull(vc1.getEntry(2));
		Assert.assertEquals(10, vc1.getEntry(2));
	}

	@Test
	public void testIsGreaterOrEquals() throws Exception {
		VectorClock vc2 = new VectorClock();
		Assert.assertTrue(vc1.isGreaterOrEquals(vc2));
		vc2.incrementEntry(0);
		Assert.assertFalse(vc1.isGreaterOrEquals(vc2));
		vc1.incrementEntry(0);
		Assert.assertTrue(vc1.isGreaterOrEquals(vc2));
		vc2.incrementEntry(0);
		Assert.assertFalse(vc1.isGreaterOrEquals(vc2));
		vc1.incrementEntry(1);
		Assert.assertFalse(vc1.isGreaterOrEquals(vc2));
		VectorClock vc3 = new VectorClock();
		vc3.incrementEntry(5);
		vc3.incrementEntry(6);
		Assert.assertFalse(vc1.isGreaterOrEquals(vc3));
	}

	@Test
	public void testEquals() throws Exception {
		VectorClock vc2 = new VectorClock();
		Assert.assertTrue(vc1.isEqualTo(vc2));
		vc2.incrementEntry(0);
		Assert.assertFalse(vc1.isEqualTo(vc2));
		vc1.incrementEntry(0);
		Assert.assertTrue(vc1.isEqualTo(vc2));
		vc2.incrementEntry(0);
		Assert.assertFalse(vc1.isEqualTo(vc2));
		vc1.incrementEntry(1);
		Assert.assertFalse(vc1.isEqualTo(vc2));
		VectorClock vc3 = new VectorClock();
		vc3.incrementEntry(5);
		vc3.incrementEntry(6);
		Assert.assertFalse(vc1.isEqualTo(vc3));
	}
}
