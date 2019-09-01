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
package chat.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class defines a vector clock with a Map. A vector clock is serializable
 * to be inserted in messages and is cloneable for copying to/from messages.
 * 
 * NB: according to Fidge 1991, "each pair consists of a process instance
 * identifier and a numerical counter value representing the value of the
 * counter in the process as perceived the process. Each process thus maintains
 * knowledge of the counters in all other processes of which it has heard.
 * Process instances not represented in the set have the default counter value
 * O."
 */
public class VectorClock implements Serializable {
	/**
	 * serial version unique identifier for serialization.
	 */
	private static final long serialVersionUID = 2L;
	/**
	 * the map of the vector clock. The key is the identity (integer) of the process
	 * and the object is the value (integer) of the clock of the process.
	 */
	private Map<Integer, Integer> vector;

	/**
	 * the constructor with an empty {@link java.util.Map}.
	 */
	public VectorClock() {
		vector = new HashMap<>();
		assert invariant();
	}

	/**
	 * the constructor with a {@link java.util.Map} built as a clone of an existing
	 * object.
	 * 
	 * @param vectorClock
	 *            the origin object to clone.
	 */
	public VectorClock(final VectorClock vectorClock) {
		vector = new HashMap<>(vectorClock.vector);
		assert invariant();
	}

	/**
	 * checks the invariant of the class: scalar clock are greater than or equal to
	 * 0.
	 * 
	 * NB: the method is final so that the method is not overridden in potential
	 * subclasses because it is called in the constructor.
	 * 
	 * @return a boolean stating whether the invariant is maintained.
	 */
	public final boolean invariant() {
		boolean result = true;
		for (Integer value : vector.values()) {
			if (value.intValue() < 0) {
				return false;
			}
		}
		return result;
	}

	/**
	 * gets the value (integer) of the clock of a process (integer key). The
	 * returned value is either the value found or the default value <tt>0</tt>.
	 * 
	 * @param key
	 *            the identifier (integer) of the process.
	 * @return the clock value.
	 */
	public int getEntry(final int key) {
		return vector.getOrDefault(key, 0);
	}

	/**
	 * sets the value (integer) of the clock of the process (integer key). If the
	 * corresponding key does not already exists in the map, it is inserted.
	 * 
	 * @param key
	 *            the identifier (integer) of the process. An
	 *            IllegalArgumentException is thrown in case of a negative value.
	 * @param value
	 *            the value of the clock.
	 */
	public void setEntry(final int key, final int value) {
		if (key < 0) {
			throw new IllegalArgumentException("identite de processus non valide (" + key + ")");
		}
		vector.put(key, value);
		assert invariant();
	}

	/**
	 * increments the clock of a given process (integer). If the corresponding key
	 * does not already exists in the map, it is inserted with the value 1, that is
	 * to say as if it were 0 before the call.
	 * 
	 * @param key
	 *            the identifier (integer) of the process.
	 */
	public void incrementEntry(final int key) {
		if (key < 0) {
			throw new IllegalArgumentException("identite de processus non valide (" + key + ")");
		}
		vector.put(key, getEntry(key) + 1);
		assert invariant();
	}

	/**
	 * computes the maximum of the vector and the one provided in the argument. If a
	 * given key exists in one of the vector but not in the other, the value is set
	 * to the one that is present.
	 * 
	 * @param other
	 *            the other vector clock for the computation.
	 */
	public void max(final VectorClock other) {
		if (other != null) {
			List<Integer> keys = new ArrayList<>(vector.keySet());
			keys.addAll(other.vector.keySet());
			for (Integer key : keys) {
				setEntry(key, Math.max(vector.getOrDefault(key, 0), other.vector.getOrDefault(key, 0)));
			}
		}
		assert invariant();
	}

	/**
	 * states whether this vector clock is greater or equals the vector clock
	 * provided in the argument <tt>other</tt>.
	 * 
	 * @param other
	 *            the vector clock to compare to.
	 * @return the boolean of the condition greater or equal.
	 */
	public boolean isGreaterOrEquals(final VectorClock other) {
		if (other != null) {
			List<Integer> keys = new ArrayList<>(vector.keySet());
			keys.addAll(other.vector.keySet());
			for (Integer key : keys) {
				if (this.getEntry(key) < other.getEntry(key)) {
					return false;
				}
			}
			return true;
		} else {
			return false;
		}
	}

	/**
	 * states whether this vector clock equals the vector clock provided in the
	 * argument <tt>other</tt>. This method does not override
	 * {@link Object#equals(Object)} because the two collections may be different
	 * while this method returns {@code true}.
	 * 
	 * @param other
	 *            the vector clock to compare to.
	 * @return the boolean of the condition greater or equal.
	 */
	public boolean isEqualTo(final VectorClock other) {
		if (other != null) {
			List<Integer> keys = new ArrayList<>(vector.keySet());
			keys.addAll(other.vector.keySet());
			for (Integer key : keys) {
				if (this.getEntry(key) != other.getEntry(key)) {
					return false;
				}
			}
			return true;
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		return vector.toString();
	}
}
