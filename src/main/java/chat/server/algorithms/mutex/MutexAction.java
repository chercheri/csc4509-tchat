package chat.server.algorithms.mutex;

import java.util.Objects;
import java.util.function.BiConsumer;

import chat.common.Action;
import chat.common.Entity;
import chat.common.MsgContent;
import chat.server.Server;

/**
 * 
 * @author chercheri
 *
 */
public enum MutexAction implements Action {
	
	/**
	 * the enumerator for the action of the token message of the mutual exclusion algorithm.
	 */
	JETON_MSG(MutexTokenContent.class,
			(Entity server, MsgContent content) -> ((Server) server).receiveMutexToken((MutexTokenContent) content)),
	/**
	 * the enumerator for the action of the request message of the mutual exclusion algorithm.
	 */
	DEMANDE_MSG(MutexRequestContent.class,
			(Entity server, MsgContent content) -> ((Server) server).receiveMutexRequest((MutexRequestContent) content));
	/**
	 * the type of the content.
	 */
	private final Class<? extends MsgContent> contentClass;

	/**
	 * the lambda expression of the action.
	 */
	private final BiConsumer<Entity, MsgContent> actionFunction;

	/**
	 * is the constructor of message type object.
	 * 
	 * @param contentClass   the type of the content.
	 * @param actionFunction the lambda expression of the action.
	 */
	MutexAction(final Class<? extends MsgContent> contentClass,
			final BiConsumer<Entity, MsgContent> actionFunction) {
		Objects.requireNonNull(contentClass, "argument contentClass cannot be null");
		Objects.requireNonNull(actionFunction, "argument actionFunction cannot be null");
		System.out.println("NAME " + contentClass.getName());
		this.contentClass = contentClass;
		this.actionFunction = actionFunction;
	}

	/**
	 * gets the type of the content.
	 * 
	 * @return the type of the content.
	 */
	public Class<? extends MsgContent> contentClass() {
		return contentClass;
	}

	/**
	 * gets the lambda expression of the action.
	 * 
	 * @return the lambda expression.
	 */
	public BiConsumer<Entity, MsgContent> actionFunction() {
		return actionFunction;
	}
}
