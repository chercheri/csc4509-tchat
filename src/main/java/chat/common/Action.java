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

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * This interface defines the interface of the actions of the algorithms. An
 * action has an action number, which is computed by static methods of the
 * enumeration of the algorithms. The method ({@link #execute}) is called for
 * executing the action. The context of the call are the process or entity of
 * the distributed algorithm (client or server) and the message that has just
 * been received.
 * 
 * @author Denis Conan
 * 
 */
public interface Action {
	/**
	 * index of the first message type of the first algorithm of the server.
	 */
	int OFFSET_SERVER_ALGORITHMS = 0;
	/**
	 * index of the first message type of the first algorithm of the client.
	 */
	int OFFSET_CLIENT_ALGORITHMS = 1000;

	/**
	 * number of actions per algorithms.
	 */
	int NB_MAX_ACTIONS_PER_ALGORITHM = 20;

	/**
	 * gets the type of the message.
	 * 
	 * @return the type of the message.
	 */
	Class<? extends MsgContent> contentClass();

	/**
	 * gets the lambda expression of the action to execute.
	 * 
	 * @return the lambda expression of the action.
	 */
	BiConsumer<Entity, MsgContent> actionFunction();

	/**
	 * executes the algorithmic part corresponding to this action. The
	 * synchronisation is made into the method that is going to be executed.
	 * 
	 * @param entity
	 *            the reference to the process or entity (e.g. client or server).
	 * @param msg
	 *            the message in treatment.
	 */
	default void execute(Entity entity, MsgContent msg) {
		Objects.requireNonNull(entity, "argument entity cannot be null");
		Objects.requireNonNull(msg, "argument content cannot be null");
		if (!contentClass().isInstance(msg)) {
			throw new IllegalArgumentException("msg of type " + msg.getClass().getCanonicalName()
					+ "() is not an instance of " + contentClass().getCanonicalName());
		}
		actionFunction().accept(entity, msg);
	}

	/**
	 * executes the action due to the receipt of the message {@code msg} or
	 * intercepts the call of the action for instance to eventually re-schedule the
	 * receipt of the message so that some non-determinism is introduced. The
	 * behaviour is controlled by the boolean value
	 * {@link Interceptors#isInterceptionEnabled()}. The synchronisation is made
	 * into the method that is going to be executed.
	 * 
	 * @param entity
	 *            the reference of the entity (client or server).
	 * @param content
	 *            the message to treat.
	 */
	default void executeOrIntercept(final Entity entity, final MsgContent content) {
		Objects.requireNonNull(entity, "argument entity cannot be null");
		Objects.requireNonNull(content, "argument content cannot be null");
		Optional<MsgContent> msg = Optional.of(content);
		if (Interceptors.isInterceptionEnabled()) {
			msg = Interceptors.intercept(entity, msg);
		}
		msg.ifPresent(m -> execute(entity, m));
	}
}
