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
package electionTest;

import static chat.common.Log.LOGGER_NAME_CHAT;
import static chat.common.Log.LOGGER_NAME_COMM;
import static chat.common.Log.LOGGER_NAME_ELECTION;
import static chat.common.Log.LOGGER_NAME_GEN;
import static chat.common.Log.LOGGER_NAME_TEST;
import static chat.common.Log.LOG_ON;
import static chat.common.Log.TEST;
import static chat.common.Log.ELECTION;

import static org.junit.Assert.assertEquals;

import java.util.function.Consumer;
import java.util.function.Predicate;

import org.apache.log4j.Level;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import chat.TestForPointToPointMessage;
import chat.client.Client;
import chat.common.Interceptors;
import chat.common.Log;
import chat.common.Scenario;
import chat.server.Server;
import chat.server.algorithms.election.ElectionTokenContent;

public class TestManyCandidates extends Scenario {

	@Test
	@Override
	public void constructAndRun() throws Exception {
		Log.configureALogger(LOGGER_NAME_CHAT, Level.INFO);
		Log.configureALogger(LOGGER_NAME_COMM, Level.WARN);
		Log.configureALogger(LOGGER_NAME_ELECTION, Level.WARN);
		Log.configureALogger(LOGGER_NAME_ELECTION, Level.INFO);
		Log.configureALogger(LOGGER_NAME_GEN, Level.WARN);
		Log.configureALogger(LOGGER_NAME_TEST, Level.WARN);
		if (LOG_ON && TEST.isInfoEnabled()) {
			TEST.info("starting the servers...");
		}
		
		// Topologie du tp
		
		Server s1 = instanciateAServer("0");
		sleep(500);
		Server s2 = instanciateAServer("1 localhost 0");
		sleep(500);
		Server s3 = instanciateAServer("2 localhost 1");
		sleep(500);
		Server s4 = instanciateAServer("3 localhost 2");
		sleep(500);
		Server s5 = instanciateAServer("4 localhost 0 localhost 1");
		sleep(500);
		Server s6 = instanciateAServer("5 localhost 2");
		sleep(500);
		
		if (LOG_ON && TEST.isInfoEnabled()) {
			TEST.info("servers started...");
		}
		
		// serveur 3 : intercepteur entre le serveur 6 et le serveur 5
		Predicate<ElectionTokenContent> conditionForInterceptingI1OnS3 =
		        msg -> msg.getSender() == s5.identity(); // on intercepte la vague gagnante
		Predicate<ElectionTokenContent> conditionForExecutingI1OnS3 =
		        msg -> true;
		Consumer<ElectionTokenContent> treatmentI1OnS3 =
		        msg -> chat.server.algorithms.election.ElectionAction.TOKEN_MESSAGE.execute(s3, msg);
		Interceptors.addAnInterceptor("i1", s3,
		        conditionForInterceptingI1OnS3, conditionForExecutingI1OnS3, treatmentI1OnS3);
		
		sleep(1000);
		
		// Serveur=6
		emulateAnInputLineFromTheConsoleForAServer(s6, "candidate");
		ELECTION.info("le serveur 6 est lancé");
		
		// Serveur=5 
		emulateAnInputLineFromTheConsoleForAServer(s5, "candidate");
		ELECTION.info("le serveur 5 est lancé");
		
		sleep(100);

		//le gagnant est = Serveur5 (identifiant = 4)
		assertEquals(s1.getLeader(), 4);
		assertEquals(s2.getLeader(), 4);
		assertEquals(s3.getLeader(), 4);
		assertEquals(s4.getLeader(), 4);
		assertEquals(s5.getLeader(), 4);
		assertEquals(s6.getLeader(), 4);
		
	}
}
