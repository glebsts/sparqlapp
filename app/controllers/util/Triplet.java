package controllers.util;

public class Triplet {
	public String subject;
	public String object;
	public String predicate;

	public Triplet(String subject) {
		this.subject = subject;
	}
	
	static Triplet createEmptyTriplet(){
		Triplet t = new Triplet("EMPTY_TRIPLET");
		t.object = "";
		t.predicate = "";
		return t;
	}
}
