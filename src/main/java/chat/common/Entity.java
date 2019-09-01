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

/**
 * This interface defines the concept of entity or process of a distributed
 * algorithm. It can for instance be a client or server.
 * 
 * @author Denis Conan
 * 
 */
public interface Entity {
	/**
	 * gets the identity of the entity (client or server).
	 * 
	 * @return the identity of the entity.
	 */
	int identity();
}
