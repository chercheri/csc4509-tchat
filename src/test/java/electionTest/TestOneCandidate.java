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

import static chat.common.Log.ELECTION;
import static chat.common.Log.LOGGER_NAME_CHAT;
import static chat.common.Log.LOGGER_NAME_COMM;
import static chat.common.Log.LOGGER_NAME_ELECTION;
import static chat.common.Log.LOGGER_NAME_GEN;
import static chat.common.Log.LOGGER_NAME_TEST;
import static chat.common.Log.LOG_ON;
import static chat.common.Log.TEST;
import static org.junit.Assert.assertEquals;

import org.apache.log4j.Level;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;


import chat.common.Log;
import chat.common.Scenario;
import chat.server.Server;

public class TestOneCandidate extends Scenario {

	@Test
	@Override
	public void constructAndRun() throws Exception {
		Log.configureALogger(LOGGER_NAME_CHAT, Level.INFO);
		Log.configureALogger(LOGGER_NAME_COMM, Level.WARN);
		Log.configureALogger(LOGGER_NAME_ELECTION, Level.INFO);
		Log.configureALogger(LOGGER_NAME_GEN, Level.WARN);
		Log.configureALogger(LOGGER_NAME_TEST, Level.WARN);
		if (LOG_ON && TEST.isInfoEnabled()) {
			TEST.info("starting the servers...");
		}
		Server s0 = instanciateAServer("0");
		sleep(500);
		Server s1 = instanciateAServer("1 localhost 0");
		sleep(500);
		Server s2 = instanciateAServer("2 localhost 1");
		sleep(500);
		Server s3 = instanciateAServer("3 localhost 2");
		sleep(500);
		Server s4 = instanciateAServer("4 localhost 0 localhost 1");
		sleep(500);
		Server s5 = instanciateAServer("5 localhost 2");
		sleep(500);
		
		if (LOG_ON && TEST.isInfoEnabled()) {
			TEST.info("servers started...");
		}
		
		sleep(1000);
		
		emulateAnInputLineFromTheConsoleForAServer(s3, "candidate");
		ELECTION.info("le serveur 3 est lancé");
		
		sleep(100);

		
		assertEquals(s0.getLeader(), 3);
		assertEquals(s1.getLeader(), 3);
		assertEquals(s2.getLeader(), 3);
		assertEquals(s3.getLeader(), 3);
		assertEquals(s4.getLeader(), 3);
		assertEquals(s5.getLeader(), 3);
		
	}
}
