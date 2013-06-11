package controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.jena.atlas.logging.Log;

import play.mvc.Controller;

import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.query.ResultSetRewindable;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.sparql.serializer.SerializationContext;
import com.hp.hpl.jena.sparql.util.FmtUtils;

import controllers.util.TripletData;
import controllers.util.Triplet;

public class Application extends Controller {

	// constants
	private static final int LIMIT = 20;

	private static final String EHR_REGISTER_URL = "http://ehitisregister.ee/sparql";
	private static final String EHR_REGISTER_FROM = "http://ehitisregister.ee/ehr#";

	private static final String ADS_REGISTER_FROM = "https://riha.eesti.ee/riha/onto/infoyhiskond.kindlustavad_systeemid.ads/2011/r1#";
	private static final String ADS_PREDICATE_URI = "http://ehitisregister.ee/schemas/ehr/ehitiseAadress";
	private static final String ADS_PREDICATE_ID = "http://ehitisregister.ee/schemas/ehr/aadressiidentifikaator";
	private static final String ADS_DATA_URI_PREDICATE = "http://ehitisregister.ee/schemas/ehr/ehitisAadressilOmabADSaadressi";

	private static final String ADS_ADDRESS_TEXT_PREDICATE = "https://riha.eesti.ee/riha/onto/infoyhiskond.kindlustavad_systeemid.ads/2011/r1/Aadress_tekstina";
	private static final String ADS_ADDRESS_TEXT_LATITUDE = "https://riha.eesti.ee/riha/onto/infoyhiskond.kindlustavad_systeemid.ads/2011/r1/latitude";
	private static final String ADS_ADDRESS_TEXT_LONGITUDE = "https://riha.eesti.ee/riha/onto/infoyhiskond.kindlustavad_systeemid.ads/2011/r1/longitude";

	private static final String EHR_STREET_NEIGHBOURS_PREDICATE = "http://ehitisregister.ee/schemas/ehr/aadressitase5";
	private static final String EHR_ADDRESS_TEXT_PREDICATE = "http://ehitisregister.ee/schemas/ehr/aadresstekstina";
	private static final String EHR_ADDRESS_CREATED_PREDICATE = "http://ehitisregister.ee/schemas/ehr/aadressloodud";

	private final static String EHR_LINKS_MATERIALS_PREDICATE = "http://ehitisregister.ee/schemas/ehr/ehitisematerjalideurl";
	private final static String EHR_LINKS_TECHSYSTEMS_PREDICATE = "http://ehitisregister.ee/schemas/ehr/ehitisetehnosysteemideurl";
	private final static String EHR_LINKS_DOCUMENTS_PREDICATE = "http://ehitisregister.ee/schemas/ehr/ehitisedokumentideurl";
	private final static String EHR_LINKS_PARTS_PREDICATE = "http://ehitisregister.ee/schemas/ehr/ehitiseosadeurl";
	private final static String EHR_LINKS_OTHER_DATA_PREDICATE = "http://ehitisregister.ee/schemas/ehr/ehitisemuudandmedurl";
	private static final String EHR_CODE_PREDICATE = "http://ehitisregister.ee/schemas/ehr/ehitiseEHRKood";
	private static final String EHR_REGISTRY_CODE_PREDICATE = "http://ehitisregister.ee/schemas/ehr/ehitusregistriNumber";
	private static final String EHR_USAGE_PREDICATE = "http://ehitisregister.ee/schemas/ehr/ehitiseKasutamisotstarve";
	private static final String EHR_USAGE_CODE_TEXT_PREDICATE = "http://ehitisregister.ee/schemas/ehr/kasutamisotstarbeKoodTekstina";

	private final static String[] linkKeys = new String[] { "Materials",
			"Technical systems", "Documents", "Parts", "Other data" };
	private final static String[] linkPredicates = new String[] {
			EHR_LINKS_MATERIALS_PREDICATE, EHR_LINKS_TECHSYSTEMS_PREDICATE,
			EHR_LINKS_DOCUMENTS_PREDICATE, EHR_LINKS_PARTS_PREDICATE,
			EHR_LINKS_OTHER_DATA_PREDICATE };

	/**
	 * renders search form
	 */
	public static void index() {
		render();
	}

	/**
	 * processes building URI, calls {@link #search(String)}
	 * 
	 * @param addressLink
	 *            - i.e. <http://ehitisregister.ee/ehr/Aadress/2646612#this>
	 */
	public static void searchByLink(String addressLink) {
		String id = null;
		try {
			id = java.net.URLDecoder.decode(addressLink, "UTF-8").substring(38);
		} catch (UnsupportedEncodingException e) {
			error("Wrong URL format");
		}
		id = id.substring(0, id.length() - 6);
		search(id);

	}

	public static void search(String id) {
		if (id != null) {
			renderArgs.put("buildingId", id);
			// get initial data
			TripletData building = getBuildingData(id);
			String buildingAddressUri = building
					.findTripletWithPredicate(ADS_PREDICATE_URI).object;

			// find address
			String addressAdsUri = getTripletData(buildingAddressUri,
					building.findTripletWithPredicate(ADS_PREDICATE_ID).object,
					null, null, null, null).findTripletWithPredicate(
					ADS_DATA_URI_PREDICATE).object;

			TripletData addressAdsData = getTripletData(addressAdsUri, "uri",
					null, null, null, null, ADS_REGISTER_FROM);

			renderArgs
					.put("adsTextAddress",
							addressAdsData
									.findTripletWithPredicate(ADS_ADDRESS_TEXT_PREDICATE).object
									.replace("\"", ""));
			// coordinates
			renderArgs
					.put("adsLatitude",
							cleanDouble(addressAdsData
									.findTripletWithPredicate(ADS_ADDRESS_TEXT_LATITUDE).object));
			renderArgs
					.put("adsLongitude",
							cleanDouble(addressAdsData
									.findTripletWithPredicate(ADS_ADDRESS_TEXT_LONGITUDE).object));

			// address creation date
			renderArgs
					.put("creationDate",
							building.findTripletWithPredicate(EHR_ADDRESS_CREATED_PREDICATE).object
									.replace("\"", ""));

			// lets find some neighbours
			String streetId = building
					.findTripletWithPredicate(EHR_STREET_NEIGHBOURS_PREDICATE).object;
			TripletData streetNeighbourIds = getTripletData(null, null,
					String.format("<%s>", EHR_STREET_NEIGHBOURS_PREDICATE),
					"Street neighbours", streetId, streetId);
			ArrayList<TripletData> neighbours = new ArrayList<TripletData>();
			for (Triplet triplet : streetNeighbourIds.triplets) {
				neighbours.add(getTripletData(triplet.subject, triplet.subject,
						String.format("<%s>", EHR_ADDRESS_TEXT_PREDICATE),
						"Text address", null, null));
			}

			renderArgs.put("neighbours", neighbours);

			// and some links about this building
			String buildingLinksUri = String.format(
					"<http://ehitisregister.ee/ehr/EhitiseLingid/%s#this>", id);
			TripletData buildingLinks = getTripletData(buildingLinksUri,
					"Useful links about building", null, null, null, null);
			HashMap<String, String> links = new HashMap<String, String>();
			if (buildingLinks.triplets.size() != 0) {

				for (int i = 0; i < linkKeys.length; i++) {
					links.put(linkKeys[i], buildingLinks
							.findTripletWithPredicate(linkPredicates[i]).object
							.replace("\"", ""));
				}
			}
			renderArgs.put("links", links);

			// large info pack, not for all buildings, quite stupid, over 100
			// triplets
			String buildingLargeInfoUri = String.format(
					"<http://ehitisregister.ee/ehr/Ehitis/%s#this>", id);
			TripletData buildingLargeInfo = getTripletData(
					buildingLargeInfoUri, "A lot of info", null, null, null,
					null, 300);
			renderArgs.put("ehrCode", buildingLargeInfo
					.findTripletWithPredicate(EHR_CODE_PREDICATE).object
					.replace("\"", ""));
			renderArgs
					.put("ehrRegistryCode",
							buildingLargeInfo
									.findTripletWithPredicate(EHR_REGISTRY_CODE_PREDICATE).object
									.replace("\"", ""));

			// usage link if easier to find from this large datapack
			String usageUri = buildingLargeInfo
					.findTripletWithPredicate(EHR_USAGE_PREDICATE).object;
			if (usageUri.length() > 0) {
				TripletData usageData = getTripletData(usageUri, "Usage data",
						null, null, null, null);
				renderArgs
						.put("usageText",
								usageData
										.findTripletWithPredicate(EHR_USAGE_CODE_TEXT_PREDICATE).object
										.replace("\"", ""));
			}

			renderArgs.put("result", building);
			render();
		} else {
			index();
		}
	}

	/**
	 * simple sample wrapper to get building data
	 * 
	 * @param buildingId
	 * @return
	 */
	private static TripletData getBuildingData(String buildingId) {
		String buildingIdUri = String.format(
				"<http://ehitisregister.ee/ehr/Aadress/%s#this>", buildingId);
		return getTripletData(buildingIdUri, buildingId, null, null, null, null);
	}

	/**
	 * overload for
	 * {@link #getTripletData(String, String, String, String, String, String, String, int)

	 */
	private static TripletData getTripletData(String s, String sAlias,
			String p, String pAlias, String o, String oAlias) {
		return getTripletData(s, sAlias, p, pAlias, o, oAlias,
				EHR_REGISTER_FROM, LIMIT);
	}

	/**
	 * overload for
	 * {@link #getTripletData(String, String, String, String, String, String, String, int)

	 */
	private static TripletData getTripletData(String s, String sAlias,
			String p, String pAlias, String o, String oAlias, int limit) {
		return getTripletData(s, sAlias, p, pAlias, o, oAlias,
				EHR_REGISTER_FROM, limit);
	}

	/**
	 * overload for
	 * {@link #getTripletData(String, String, String, String, String, String, String, int)

	 */
	private static TripletData getTripletData(String s, String sAlias,
			String p, String pAlias, String o, String oAlias, String from) {
		return getTripletData(s, sAlias, p, pAlias, o, oAlias, from, LIMIT);
	}

	/***
	 * Gets triplet results from SPARQL endpoint
	 * 
	 * @param s
	 *            - uri for subject, optional
	 * @param sAlias
	 *            - alias for subject, optional
	 * @param p
	 *            - uri for predicate, optional
	 * @param pAlias
	 *            - alias for predicate, optional
	 * @param o
	 *            - value for object, optional
	 * @param oAlias
	 *            - alias for object, optional
	 * @param from
	 *            - table uri to select from
	 * @param limit
	 *            - limit number of results
	 * @return
	 */
	private static TripletData getTripletData(String s, String sAlias,
			String p, String pAlias, String o, String oAlias, String from,
			int limit) {
		String queryStr = String.format(
				"select * FROM <%s>  WHERE {?s ?p ?o } LIMIT %s", from,
				Integer.toString(limit));
		if (s != null)
			queryStr = queryStr.replace("?s", s);
		if (p != null)
			queryStr = queryStr.replace("?p", p);
		if (o != null)
			queryStr = queryStr.replace("?o", o);

		Query query = QueryFactory.create(queryStr);
		Log.debug(Application.class, query.toString());
		// Execute the query and obtain results
		QueryExecution qe = QueryExecutionFactory.sparqlService(
				EHR_REGISTER_URL, query);// create(query);
		ResultSet results = qe.execSelect();

		int numCols = results.getResultVars().size();

		SerializationContext ctx = new SerializationContext(query);
		String[] row = new String[numCols];
		TripletData result = new TripletData(sAlias, s);

		if (results.getResultVars().size() == 0) {
			System.out.println("==== No variables ====");
			return result;
		}

		int idxSubject = -1;
		int idxPredicate = -1;
		int idxObject = -1;

		for (int col = 0; col < numCols; col++) {
			String rVar = results.getResultVars().get(col);
			row[col] = rVar;
			if (rVar.equals("s"))
				idxSubject = col;
			if (rVar.equals("p"))
				idxPredicate = col;
			if (rVar.equals("o"))
				idxObject = col;
		}

		result.headers = row.clone();

		for (; results.hasNext();) {
			Triplet triplet = new Triplet(s);
			QuerySolution rBind = results.nextSolution();
			for (int col = 0; col < numCols; col++) {
				String rVar = results.getResultVars().get(col);
				row[col] = getVarValueAsString(rBind, rVar, ctx);
			}
			triplet.subject = idxSubject > -1 ? row[idxSubject] : sAlias;
			triplet.predicate = idxPredicate > -1 ? row[idxPredicate] : pAlias;
			triplet.object = idxObject > -1 ? row[idxObject] : oAlias;

			result.triplets.add(triplet);

		}
		// Important - free up resources used running the query
		qe.close();
		return result;
	}

	protected static String getVarValueAsString(QuerySolution rBind,
			String varName, SerializationContext context) {
		RDFNode obj = rBind.get(varName);

		if (obj == null)
			return " ";

		return FmtUtils.stringForRDFNode(obj, context);
	}

	/***
	 * clean datatype markup for coordinates
	 * @param val
	 * @return
	 */
	protected static String cleanDouble(String val) {
		return val.substring(1, 15);
	}
}