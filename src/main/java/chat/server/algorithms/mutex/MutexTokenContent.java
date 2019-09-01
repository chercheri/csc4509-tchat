package chat.server.algorithms.mutex;

import chat.common.VectorClock;
import chat.server.algorithms.ServerToServerMsgContent;
/**
 * 
 * @author chercheri
 *
 */
public class MutexTokenContent extends ServerToServerMsgContent {

	/**
	 * version number for serialization.
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * vecteur demande[].
	 */
	private VectorClock token;
	
	/**
	 * 
	 * @param sender
	 * @identity of remote server
	 *  	l'identié du serveur source.
	 * @param jeton
	 * 		le vecteur demande.
	 */
	public MutexTokenContent(final int sender, final VectorClock jeton, final int identityOfRemoteServer) {
		super(sender, identityOfRemoteServer);
		this.token = new VectorClock(jeton);
	}
	
	/**
	 * 
	 * @param sender
	 *  	l'identié du serveur source.
	 * @param jeton
	 * 		le vecteur demande.
	 */
	public MutexTokenContent(final int sender, final VectorClock jeton) {
		super(sender);
		this.token = new VectorClock(jeton);
	}
	
	public MutexTokenContent() {
		super(0);
		this.token = new VectorClock();
	}
	
	public MutexTokenContent(MutexTokenContent other) {
		super(other.getSender());
		this.token = new VectorClock(other.getTokenVector());
	}
	/**
	 * 
	 * @return
	 * 	le vecteur demande[]
	 */
	public VectorClock getTokenVector() {
		return token;
	}


}
