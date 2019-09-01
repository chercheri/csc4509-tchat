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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * This class contains the interception of the calls to the actions to receive
 * messages. The behaviour is controlled by the boolean constant
 * {@link #interceptionEnabled}. When set, the default method
 * {@link chat.common.Action#executeOrIntercept(Entity, MsgContent)} redirects
 * the receipt of the message to the static method
 * {@link #intercept(Entity, Optional) #intercept(Entity,
 * Optional&lt;MsgContent&gt;)}. The latter method loops on all the
 * interceptors, which are stored on the static collection
 * {@link #COLLECTION_OF_INTERCEPTORS}. Each interceptor may intercept the message, that is
 * may delay its receipt.
 * 
 * @author Denis Conan
 */
public final class Interceptors {
	/**
	 * states whether some non-determinism is introduced to test distributed
	 * algorithms. This is done by re-routing in the default method
	 * {@link chat.common.Action#executeOrIntercept(Entity, MsgContent)}.
	 */
	private static boolean interceptionEnabled = false;

	/**
	 * the collection of collections of interceptors organised for entities.
	 */
	private static final HashMap<Entity, List<Interceptor<? extends MsgContent>>> COLLECTION_OF_INTERCEPTORS = new HashMap<>();

	/**
	 * Utility classes must not have a default or public constructor.
	 */
	private Interceptors() {
	}

	/**
	 * gets the boolean value of the attribute {@link #interceptionEnabled}.
	 * 
	 * @return the boolean value.
	 */
	public static boolean isInterceptionEnabled() {
		return interceptionEnabled;
	}

	/**
	 * sets the boolean value of the attribute {@link #interceptionEnabled}.
	 * 
	 * @param interceptionEnabled
	 *            the new boolean value.
	 */
	public static void setInterceptionEnabled(final boolean interceptionEnabled) {
		Interceptors.interceptionEnabled = interceptionEnabled;
	}

	/**
	 * adds an interceptor.
	 * 
	 * @param <C>
	 *            the type of the message to intercept.
	 * @param name
	 *            the name of the interceptor to add.
	 * @param entity
	 *            the reference to the entity (client or server).
	 * @param conditionForIntercepting
	 *            the function lambda that states whether the message must be
	 *            delayed.
	 * @param conditionForExecuting
	 *            the function method reference that states whether the treatment of
	 *            the message must be applied now.
	 * @param treatmentOfADelayedMsg
	 *            the consumer lambda that perform the treatment on delayed
	 *            messages.
	 */
	public static <C extends MsgContent> void addAnInterceptor(final String name, final Entity entity,
			final Predicate<C> conditionForIntercepting, final Predicate<C> conditionForExecuting,
			final Consumer<C> treatmentOfADelayedMsg) {
		Objects.requireNonNull(name, "argument name cannot be null");
		if ("".equals(name)) {
			throw new IllegalArgumentException("argument name cannot be empty string");
		}
		Objects.requireNonNull(entity, "null entity");
		Objects.requireNonNull(conditionForIntercepting, "argument conditionForDelaying cannot be null");
		Objects.requireNonNull(conditionForExecuting, "argument conditionForTreatment cannot be null");
		Objects.requireNonNull(treatmentOfADelayedMsg, "argument treatmentOfADelayedMsg");
		List<Interceptor<? extends MsgContent>> interceptors = COLLECTION_OF_INTERCEPTORS.getOrDefault(entity,
				new ArrayList<>());
		interceptors.add(new Interceptor<C>(name, entity, conditionForIntercepting, conditionForExecuting,
				treatmentOfADelayedMsg));
		COLLECTION_OF_INTERCEPTORS.put(entity, interceptors);
	}

	/**
	 * This method is called by the default method
	 * {@link chat.common.Action#executeOrIntercept(Entity, MsgContent)} when the
	 * interception mechanism is activated, that is {@link #isInterceptionEnabled}
	 * is {@code true}. The method loops on the collection of interceptors and apply
	 * the method {@link Interceptor#doIntercept(Optional)
	 * Interceptor#doIntercept(Optional&lt;MsgContent&gt;)} of
	 * interceptors. The role of the latter method is to intercept the message, that
	 * is to delay the delivering/treatment of the message.
	 * 
	 * @param entity
	 *            the reference to the entity (client or server).
	 * @param msg
	 *            the message to treat.
	 * @return the message, if no intercepted.
	 */
	public static Optional<MsgContent> intercept(final Entity entity, final Optional<MsgContent> msg) {
		Objects.requireNonNull(entity, "argument entity cannot be null");
		List<Interceptor<? extends MsgContent>> listOfInterceptors = COLLECTION_OF_INTERCEPTORS.getOrDefault(entity,
				Collections.emptyList());
		for (Interceptor<? extends MsgContent> interceptor : listOfInterceptors) {
			if (msg.isPresent() && !interceptor.doIntercept(msg).isPresent()) {
				return Optional.empty();
			}
		}
		return msg;
	}
}
