package controllers.util;

/***
 * class representing the triplet {subject - predicate - data}
 * @author gleb
 *
 */
public class Triplet {
	public String subject;
	public String object;
	public String predicate;

	public Triplet(String subject) {
		this.subject = subject;
	}
	
	// empty triplet used for searching in triplet collection if no result found
	static Triplet createEmptyTriplet(){
		Triplet t = new Triplet("EMPTY_TRIPLET");
		t.object = "";
		t.predicate = "";
		return t;
	}
}
