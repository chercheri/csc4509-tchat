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
package chat.client.algorithms;

import static chat.common.Action.NB_MAX_ACTIONS_PER_ALGORITHM;
import static chat.common.Action.OFFSET_CLIENT_ALGORITHMS;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import chat.client.Client;
import chat.common.Action;
import chat.common.Entity;

/**
 * This Enumeration type declares the algorithms of the chat client. For now,
 * there is only one algorithm: the algorithm for exchanging chat messages.
 * 
 * TODO add new algorithms when necessary, update the description of the
 * enumeration, and remove this comment.
 * 
 * @author Denis Conan
 */
public enum Algorithm {
	/**
	 * the chat algorithm.
	 */
	ALGORITHM_CHAT(chat.client.algorithms.chat.ChatAction.values());

	/**
	 * collection of the actions of this algorithm enumerator of the client. The
	 * collection is built at class loading by parsing the algorithms; it is thus
	 * {@code static}. The collection is unmodifiable and the attribute is
	 * {@code final} so that no other collection can be substituted after being
	 * statically assigned.
	 */
	private final List<Enum<?>> actions;

	/**
	 * constructs an enumerator by assigning the enumeration of actions of this
	 * algorithm to the list of actions. See for instance the enumerators of the
	 * actions of the algorithm {@link chat.client.algorithms.chat.ChatAction}.
	 * 
	 * @param actions actions of this algorithm.
	 */
	Algorithm(final Enum<?>[] actions) {
		this.actions = Collections.unmodifiableList(Arrays.asList(actions));
	}

	/**
	 * the map (action numbers, actions). This map is used when receiving a message:
	 * the action number is the "message type" of the message to treat and the
	 * action to execute is found with this map.
	 */
	private static final Map<Integer, Action> MAP_OF_ACTION_NUMBER_ACTIONS;
	/**
	 * the map (actions, action numbers). This map is used when sending a message:
	 * the action is the enumerator the action of the algorithm and the action
	 * number (that is the "message type" to send) is found with this map.
	 */
	private static final Map<Enum<?>, Integer> MAP_OF_ACTION_ACTION_NUMBERS;

	static {
		Map<Integer, Action> mBis = new HashMap<>();
		Map<Enum<?>, Integer> mTer = new HashMap<>();
		for (Algorithm algorithm : Algorithm.values()) {
			for (Enum<?> action : algorithm.actions) {
				final int actionNumber = OFFSET_CLIENT_ALGORITHMS + algorithm.ordinal() * NB_MAX_ACTIONS_PER_ALGORITHM
						+ action.ordinal();
				mBis.put(actionNumber, (Action) action);
				mTer.put(action, actionNumber);
			}
		}
		MAP_OF_ACTION_NUMBER_ACTIONS = Collections.unmodifiableMap(mBis);
		MAP_OF_ACTION_ACTION_NUMBERS = Collections.unmodifiableMap(mTer);
	}

	/**
	 * computes the action number (an identifier) for the given action of an
	 * algorithm (the enumerator of the actions of the algorithm).
	 * 
	 * @param action the action of the algorithm.
	 * @return the corresponding action number (that serves as the message type of
	 *         the message to send).
	 */
	public static int getActionNumber(final Enum<?> action) {
		return MAP_OF_ACTION_ACTION_NUMBERS.getOrDefault(action, -1);
	}

	/**
	 * searches for the action to execute in the collection of algorithms of the
	 * algorithm of the client. The synchronisation is made into the client method
	 * that is going to be executed.
	 * 
	 * @param client       the reference to the client.
	 * @param actionNumber action number of the action to execute.
	 * @param content      content of the message just received.
	 */
	public static void execute(final Entity client, final int actionNumber, final Object content) {
		MAP_OF_ACTION_NUMBER_ACTIONS.entrySet().stream().filter(e -> e.getKey() == actionNumber).map(Entry::getValue)
				.filter(Objects::nonNull)
				.filter(action -> action.contentClass().isInstance(content) && client instanceof Client)
				.forEach(action -> action.executeOrIntercept(client, action.contentClass().cast(content)));
	}
}
