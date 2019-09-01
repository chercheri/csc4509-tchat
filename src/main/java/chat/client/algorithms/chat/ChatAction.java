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
package chat.client.algorithms.chat;

import java.util.Objects;
import java.util.function.BiConsumer;

import chat.client.Client;
import chat.common.Action;
import chat.common.Entity;
import chat.common.MsgContent;

/**
 * This Enumeration type declares the actions of the chat algorithm of the
 * client. Only one message content type can be received.
 * 
 * @author Denis Conan
 * 
 */
public enum ChatAction implements Action {
	/**
	 * the enumerator for the action of the chat message of the chat algorithm. The
	 * synchronisation is made into the client method that is going to be called.
	 * 
	 * NB: I do not know whether a re-factoring can avoid the cast.
	 */
	CHAT_MESSAGE(ChatMsgContent.class,
			(Entity client, MsgContent content) -> ((Client) client).receiveChatMsgContent((ChatMsgContent) content));

	/**
	 * the type of the content.
	 */
	private final Class<? extends MsgContent> contentClass;

	/**
	 * the lambda expression of the action.
	 */
	private final BiConsumer<Entity, MsgContent> actionFunction;

	/**
	 * constructs an enumerator by assigning the {@link #actionIndex}.
	 * 
	 * @param contentClass   the type of the content.
	 * @param actionFunction the lambda expression of the expression. In general,
	 *                       this is a call similar to
	 *                       {@code client.aMethod((cast to contentClass) message)}.
	 */
	ChatAction(final Class<? extends MsgContent> contentClass,
			final BiConsumer<Entity, MsgContent> actionFunction) {
		Objects.requireNonNull(contentClass, "argument contentClass cannot be null");
		Objects.requireNonNull(actionFunction, "argument actionFunction cannot be null");
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
