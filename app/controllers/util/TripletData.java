package controllers.util;

import java.util.ArrayList;
import java.util.List;
/***
 * Object, described by set of triples
 * @author gleb
 *
 */
public class TripletData {
	public String id;  // subject ID
	public List<Triplet> triplets; // set of triplets
	public String[] headers; // column headers for triplet, if any, generally not used in this demo app
	public String subjectUri;  //subject URI

	/**
	 * default object description constructor
	 * @param subjectId
	 * @param subjectUri
	 */
	public TripletData(String subjectId, String subjectUri) {
		this.id = subjectId;
		this.subjectUri = subjectUri;
		this.triplets = new ArrayList<Triplet>();
		
	}
	
	/***
	 * searches for triplet with given predicate
	 * @param predicate
	 * @return first triplet with given predicate, or empty triplet (see {@link Triplet.createEmptyTriplet})
	 */
	public Triplet findTripletWithPredicate(String predicate){
		String lookFor = String.format("<%s>",predicate);
		for (Triplet triplet : triplets) {
			if(triplet.predicate.equalsIgnoreCase(lookFor))
				return triplet;
		}
		return Triplet.createEmptyTriplet();
	}
}
