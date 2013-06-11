package controllers.util;

import java.util.ArrayList;
import java.util.List;

public class TripletData {
	public TripletData(String subjectId, String subjectUri) {
		this.id = subjectId;
		this.subjectUri = subjectUri;
		this.triplets = new ArrayList<Triplet>();
		
	}
	
	public Triplet findTripletWithPredicate(String predicate){
		String lookFor = String.format("<%s>",predicate);
		for (Triplet triplet : triplets) {
			if(triplet.predicate.equalsIgnoreCase(lookFor))
				return triplet;
		}
		return Triplet.createEmptyTriplet();
	}

	public String id;
	public List<Triplet> triplets;
	public String[] headers;
	public String subjectUri;
}
