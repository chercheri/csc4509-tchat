package chat.server.algorithms.mutex;

import chat.server.algorithms.ServerToServerMsgContent;

/**
 * 
 * @author chercheri
 *
 */
public class MutexRequestContent extends ServerToServerMsgContent {

	/**
	 * version number for serialization.
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 *   !!!!je ne sais pas encore.
	 */
	private int ns;
	
	/**
	 * 
	 * @param sender
	 * 	l'indenté de la source qui demande le jeton
	 * @param ns
	 * 	le ns
	 */
	public MutexRequestContent(final int sender, final int ns, final int intendedRecipient) {
		super(sender, intendedRecipient);
		this.ns = ns;
	}
	/**
	 * 
	 * @return
	 *  retourne le ns
	 */
	public int getNs() {
		return ns;
	}
	
	
	

}
