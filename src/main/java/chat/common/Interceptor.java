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

import static chat.common.Log.INTERCEPT;
import static chat.common.Log.LOG_ON;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * This class contains the interceptor for an entity (client or server). The
 * method {@link #doIntercept(Optional)
 * #doIntercept(Optional&lt;MsgContent&gt;)} is called before the message
 * receipt, and more precisely from the method
 * {@link Interceptors#intercept(Entity, Optional)
 * Interceptors#intercept(Entity, Optional&lt;MsgContent&gt;)}. A message is
 * intercepted when the lambda expression of method reference
 * {@link #conditionForIntercepting} returns {@code true}. If so, a
 * {@link TreatDelayedMessage} thread is created and started that will later run
 * the lambda expression of the method reference {@link #treatmentOfADelayedMsg}
 * if the method reference returns {@code true}.
 * 
 * @param <C>
 *            the type of the message to intercept.
 * 
 * @author Denis Conan
 */
public class Interceptor<C extends MsgContent> {
	/**
	 * the name of the interceptor.
	 */
	private final String name;
	/**
	 * the reference to the entity (client or server).
	 */
	private final Entity entity;
	/**
	 * the function method reference for deciding whether a message must be delayed.
	 * The synchronisation is made into the methods that call this function.
	 */
	private final Predicate<C> conditionForIntercepting;
	/**
	 * the function method reference for deciding when the treatment of the message
	 * must be applied. The synchronisation is made into the methods that call this
	 * function.
	 */
	private final Predicate<C> conditionForExecuting;
	/**
	 * the consumer method reference for the treatment of the delayed messages. The
	 * synchronisation is made into the methods that call this function.
	 */
	private final Consumer<C> treatmentOfADelayedMsg;

	/**
	 * constructs an interceptor for the given entity (client or server).
	 * 
	 * @param name
	 *            the name of the interceptor.
	 * @param entity
	 *            the reference to the entity (client or server).
	 * @param conditionForIntercepting
	 *            the function method reference that states whether the message must
	 *            be delayed.
	 * @param conditionForExecuting
	 *            the function method reference that states whether the treatment of
	 *            the message must be applied now.
	 * @param treatmentOfADelayedMsg
	 *            the consumer lambda that perform the treatment on delayed
	 *            messages.
	 */
	public Interceptor(final String name, final Entity entity, final Predicate<C> conditionForIntercepting,
			final Predicate<C> conditionForExecuting, final Consumer<C> treatmentOfADelayedMsg) {
		Objects.requireNonNull(name, "argument name cannot be null");
		if ("".equals(name)) {
			throw new IllegalArgumentException("argument name cannot be empty string");
		}
		Objects.requireNonNull(entity, "argument entity cannot be null");
		Objects.requireNonNull(conditionForIntercepting, "argument conditionForDelaying cannot be null");
		Objects.requireNonNull(conditionForExecuting, "argument conditionForTreatment cannot be null");
		Objects.requireNonNull(treatmentOfADelayedMsg, "argument treatmentOfADelayedMsg");
		this.name = name;
		this.entity = entity;
		this.conditionForIntercepting = conditionForIntercepting;
		this.conditionForExecuting = conditionForExecuting;
		this.treatmentOfADelayedMsg = treatmentOfADelayedMsg;
		assert invariant();
	}

	/**
	 * checks the invariant of the object.
	 * 
	 * @return true when satisfied.
	 */
	public boolean invariant() {
		return name != null && !"".equals(name) && entity != null && conditionForIntercepting != null
				&& conditionForExecuting != null && treatmentOfADelayedMsg != null;
	}

	/**
	 * gets the name of the interceptor.
	 * 
	 * @return the name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * gets the reference to the entity (client or server).
	 * 
	 * @return the reference to the entity.
	 */
	public Entity getEntity() {
		return entity;
	}

	/**
	 * applies the condition of the interceptor, and either create a thread to delay
	 * the delivering/treatment of the message (return an empty {@code Optional}) or
	 * return the same message.
	 * 
	 * @param msg
	 *            the message to intercept.
	 * @return the message, if no intercepted.
	 */
	@SuppressWarnings("unchecked") // due to (unsafe) downcast in Function.apply
	public Optional<C> doIntercept(final Optional<MsgContent> msg) {
		Objects.requireNonNull(msg, "argument content cannot be null");
		synchronized (entity) {
			// TODO avoid testing cast using exception
			try {
				if (msg.isPresent() && conditionForIntercepting.test((C) msg.get())) {
					new Thread(new TreatDelayedMessage<C>(this, entity, (C) msg.get())).start();
					if (LOG_ON && INTERCEPT.isInfoEnabled()) {
						INTERCEPT.info("interceptor " + name + " at entity " + entity.identity()
								+ " intercepts message: " + msg.get());
					}
					return Optional.empty();
				} else {
					return Optional.ofNullable((C) msg.orElse(null));
				}
			} catch (Exception e) {
				return Optional.ofNullable((C) msg.orElse(null));
			}
		}
	}

	/**
	 * launches the treatment of the delayed message. This method is a delegate from
	 * method {@link TreatDelayedMessage#run()}.
	 * 
	 * @param content
	 *            the content of the delayed message.
	 * @return {@code true} when the treatment has been applied and the calling
	 *         thread can end its execution.
	 */
	public boolean doTreatDelayedMessage(final C content) {
		Objects.requireNonNull(content, "null content");
		synchronized (entity) {
			if (this.conditionForExecuting.test((C) content)) {
				try {
					treatmentOfADelayedMsg.accept((C) content);
					if (LOG_ON && INTERCEPT.isInfoEnabled()) {
						INTERCEPT.info("treatment by the interceptor " + name + ": " + content);
					}
				} catch (ClassCastException e) {
						INTERCEPT.error(e);
					throw new RuntimeException(
							"pb in Interceptor::doTreatDelayedMessage (" + e.getLocalizedMessage() + ")");
				}
				return true;
			} else {
				if (LOG_ON && INTERCEPT.isDebugEnabled()) {
					INTERCEPT.debug("bad condition for executing by the interceptor " + name + ": " + content);
				}
				return false;
			}
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((entity == null) ? 0 : entity.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		@SuppressWarnings("unchecked")
		Interceptor<C> other = (Interceptor<C>) obj;
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		if (entity == null) {
			if (other.entity != null) {
				return false;
			}
		} else if (!entity.equals(other.entity)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "Interceptor [name=" + name + ", entity=" + entity.identity() + "]";
	}
}
