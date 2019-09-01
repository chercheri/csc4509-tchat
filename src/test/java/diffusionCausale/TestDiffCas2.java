//CHECKSTYLE:OFF
package diffusionCausale;

import static chat.common.Log.LOGGER_NAME_CHAT;
import static chat.common.Log.LOGGER_NAME_COMM;
import static chat.common.Log.LOGGER_NAME_ELECTION;
import static chat.common.Log.LOGGER_NAME_DIFFCAUS;
import static chat.common.Log.LOGGER_NAME_GEN;
import static chat.common.Log.LOGGER_NAME_TEST;
import static chat.common.Log.LOG_ON;
import static chat.common.Log.TEST;
import static org.junit.Assert.assertEquals;

import java.util.function.Consumer;
import java.util.function.Predicate;

import org.apache.log4j.Level;
import org.junit.Test;

import chat.client.Client;
import chat.client.algorithms.chat.ChatMsgContent;
import chat.common.Interceptors;
import chat.common.Log;
import chat.common.MsgContent;
import chat.common.Scenario;
import chat.server.Server;

public class TestDiffCas2  extends Scenario {

	@Test
	@Override
	public void constructAndRun() throws Exception {
		Log.configureALogger(LOGGER_NAME_CHAT, Level.INFO);
		Log.configureALogger(LOGGER_NAME_COMM, Level.WARN);
		Log.configureALogger(LOGGER_NAME_ELECTION, Level.WARN);
		Log.configureALogger(LOGGER_NAME_GEN, Level.WARN);
		Log.configureALogger(LOGGER_NAME_TEST, Level.WARN);
		Log.configureALogger(LOGGER_NAME_DIFFCAUS, Level.WARN);
		Log.configureALogger(LOGGER_NAME_DIFFCAUS, Level.INFO);

		
		if (LOG_ON && TEST.isInfoEnabled()) {
			TEST.info("starting the server...");
		}
		Server s0 = instanciateAServer("0");
		sleep(500);
		
		if (LOG_ON && TEST.isInfoEnabled()) {
			TEST.info("starting the clients...");
		}
				// start the clients
		Client c0 = instanciateAClient(2050);
		sleep(500);
		Client c1 = instanciateAClient(2050);
		sleep(500);
		Client c2 = instanciateAClient(2050);
		sleep(500);
		
		if (LOG_ON && TEST.isInfoEnabled()) {
			TEST.info("starting Clients");
		}
		
		Interceptors.setInterceptionEnabled(true);
		// client c2 : intercepteur entre les clients
		Predicate<ChatMsgContent> conditionForInterceptingI1OnC2 =
		        msg -> msg.getSender() == c0.identity(); //le message est diffusé par c0
		Predicate<ChatMsgContent> conditionForExecutingI1OnC2 =
		        msg -> c0.getHorloge().getEntry(c0.identity()) == 1 && c0.getHorloge().getEntry(c1.identity()) == 1; //le vecteur d'horloge de c0 indique qu'il a livré les deux messages
		Consumer<ChatMsgContent> treatmentI1OnC2 =
				msg -> chat.client.algorithms.chat.ChatAction.CHAT_MESSAGE
				.execute(c2, new ChatMsgContent(msg.getSender(), msg.getSequenceNumber(),msg.getHorloge(), msg.getContent()+", intercepted at client c2 by i1"));
		
		Interceptors.addAnInterceptor("i1", c2, conditionForInterceptingI1OnC2, conditionForExecutingI1OnC2, treatmentI1OnC2);
		//sleep(1000);
		
//		emulateAnInputLineFromTheConsoleForAClient(c0, "message 1 from c0");
//		//sleep(1000);
//
//		emulateAnInputLineFromTheConsoleForAClient(c0, "message 2 from c0");
//		//sleep(1000);
		
		emulateAnInputLineFromTheConsoleForAClient(c0, "message 3 from c0");
		sleep(1000);
		
		emulateAnInputLineFromTheConsoleForAClient(c1, "message 4 from c1");
		sleep(1000);
		System.out.flush();
		
		
		assertEquals(c0.getHorloge().getEntry(c0.identity()),1);
		assertEquals(c0.getHorloge().getEntry(c1.identity()),1);
		assertEquals(c0.getHorloge().getEntry(c2.identity()),0);
		
		//client 0 recoit un message de la part de client 1.
		assertEquals(c0.getNbChatMsgContentReceived(), 1);
		//client 0 livre les 2 messages.
		assertEquals(c0.getNbChatMsgContentDelivered(), 2);
		
		assertEquals(c1.getHorloge().getEntry(c0.identity()),1);
		assertEquals(c1.getHorloge().getEntry(c1.identity()),1);
		assertEquals(c1.getHorloge().getEntry(c2.identity()),0);
		
		//client 1 livre les 2 messages.
		assertEquals(c1.getNbChatMsgContentDelivered(), 2);
		//client 2 recoit le msg de la part de c0.
		assertEquals(c1.getNbChatMsgContentReceived(), 1);
		
		assertEquals(c2.getHorloge().getEntry(c0.identity()),1);
		assertEquals(c2.getHorloge().getEntry(c1.identity()),1);
		assertEquals(c2.getHorloge().getEntry(c2.identity()),0);
		
		//client 2 recoit les 2 messages.
		assertEquals(c2.getNbChatMsgContentReceived(), 2);
		//client 2 livre les 2 messages.
		assertEquals(c2.getNbChatMsgContentDelivered(), 2);
	}


}


