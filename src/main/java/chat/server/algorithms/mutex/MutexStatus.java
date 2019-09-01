package chat.server.algorithms.mutex;
/**
 *  l'etat du serveur. 
 * @author chercheri
 *
 */
public enum MutexStatus {
	/**
	 * dans la section critique.
	 */
	DANS_CS,
	/**
	 * hors de la section critique.
	 */
	HORS_CS,
	/**
	 * en attente à l'entrée de la section critique (attend le jeton).
	 */
	EN_ATT,
}
