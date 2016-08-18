package modcomp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import de.ovgu.featureide.fm.core.FeatureModel;
import de.ovgu.featureide.fm.core.configuration.Configuration;
import de.ovgu.featureide.fm.core.configuration.Selection;
import de.ovgu.featureide.fm.core.io.UnsupportedModelException;
import de.ovgu.featureide.fm.core.io.xml.XmlFeatureModelReader;
import jmetal.core.Algorithm;
import jmetal.core.Solution;
import jmetal.core.SolutionSet;
import jmetal.encodings.variable.Binary;
import jmetal.experiments.Settings;
import jmetal.experiments.SettingsFactory;
import jmetal.util.Distance;
import jmetal.util.JMException;
import modcomp.ModCompSolution.Origin;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class SampleHandler extends AbstractHandler {
	
	private final String algorithmName   = "NSGAIIBinary";
	private final String problemName     = "ModComp";
	private final int MAXTRIES = 5;
	private int tries;
	
	private SolutionSet solutionsFirstRun; //faults are not respected
	private SolutionSet faultySolutionsFirstRun; //solutions that get invalid while respecting faults
	private SolutionSet healtySolutionsFirstRun; //solutions that remain valid while respecting faults
	private SolutionSet solutionsSecondRun; //faults are respected (configurations are deactivated)
	
	private FileWriter resultFileStream = null;
	private BufferedWriter resultFileOut = null;

	/**
	 * the command has been executed, so extract extract the needed information
	 * from the application context.
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		tries = 0; //reset tries
		start();
		return null;
	}
	
	private void start(){
		FeatureModel featureModel = loadFeatureModel(Config.modelFilePath);
		HashSet<String> faultyFeatures = loadFaultyFeatures(Config.faultsFilePath);
		faultySolutionsFirstRun = new SolutionSet();
		healtySolutionsFirstRun = new SolutionSet();

		if(featureModel != null){
			prepareResultFile();
			step1(featureModel, faultyFeatures);
			closeResultFile();
		}
	}
	
	/**
	 * Preform first run without respecting faults
	 * @param featureModel
	 * @param faultyFeatures
	 */
	private void step1(FeatureModel featureModel, HashSet<String> faultyFeatures){
		writeToResultFile("\n1: Preform first run without respecting faults.");
		LocalDateTime t1 = LocalDateTime.now();
		writeToResultFile("Start at: " + t1);
		solutionsFirstRun = getUniques(analyse(featureModel, null));
		LocalDateTime t2 = LocalDateTime.now();
		writeToResultFile("End at  : " + t2 + " (" + ChronoUnit.SECONDS.between(t1, t2) + " sec.)");
		step2(featureModel, faultyFeatures);
	}
	
	/**
	 * Separate faulty and healthy configurations from first run.
	 * @param featureModel
	 * @param faultyFeatures
	 */
	private void step2(FeatureModel featureModel, HashSet<String> faultyFeatures){
		//Clear faults from first iteration
		writeToResultFile("\n2: Separate faulty and healthy configurations from first run.");
		boolean foundFault = proceedFaults(featureModel, faultyFeatures);
		
		if(foundFault)
			step3(featureModel, faultyFeatures);
		else{
			writeToResultFile("Faults affected no changes wrt. current sampling.");
			if(tries < MAXTRIES){
				tries++;
				writeToResultFile(tries + ". try of sampling will be started...");
				closeResultFile();
				start();
			}
			else{
				writeToResultFile("Sampling is abouted after " + tries + " tries.");
				closeResultFile();
			}
				
		}
	}
	
	

	
	/**
	 * Preform second run under consideration of fault
	 * @param featureModel
	 * @param faultyFeatures
	 */
	private void step3(FeatureModel featureModel, HashSet<String> faultyFeatures){
		//2. Run with faults
		writeToResultFile("\n3: Preform second run under consideration of faults.");
		LocalDateTime t1 = LocalDateTime.now();
		writeToResultFile("Start at: " + t1);
		solutionsSecondRun = getUniques(analyse(featureModel, faultyFeatures));
		LocalDateTime t2 = LocalDateTime.now();
		writeToResultFile("End at  : " + t2 + " (" + ChronoUnit.SECONDS.between(t1, t2) + " sec.)");
		
		step4(featureModel);
	}
	
	/**
	 * Measure and compare Euclidean distances between Pareto frontiers
	 */
	private void step4(FeatureModel featureModel){
		//Measure and compare distances between Pareto frontiers
		writeToResultFile("\n4: Measure and compare Euclidean distances between Pareto frontiers.");
		measaureDistances(featureModel);
		
		step5();
	}
	
	/**
	 * TODO: Summaries and present results to developer
	 * 1) Faulty solutions
	 * 2) List of distances to neighbors for a given threshold of qualities
	 * 3) Nearest neighbors for reconfiguration
	 * 4) Summary (#neighbors in 1st and 2nd run, efficiency of re-sampling in %; #new neighbors...)
	 */
	private void step5(){
		//Generate for each faulty a distancemap to all feasible alternatives
		//TODO Extract table for paper
		writeToResultFile("Distances Maps");
		HashMap<Solution,ArrayList<ModCompSolution>> distancesMap = distancesMap();
		for(Solution faultySolution : distancesMap.keySet()){
			ArrayList<ModCompSolution> neighborsOrderedByDistances = distancesMap.get(faultySolution);
			writeToResultFile("FaultySolution " + getSolutionString(faultySolution));
//			int newNeigbors=0;
			for(ModCompSolution solDis : neighborsOrderedByDistances){
				writeToResultFile(solDis.getDistance() + " -> " + getSolutionString(solDis.getSolution()) + " from " + solDis.getOrigin());
//				if(solDis.getOrigin() == Origin.SECOND){
//					newNeigbors++;
//				}
			}
//			writeToResultFile("Number of new neighbors: " + newNeigbors + " (" + faultySolutionsFirstRun.size() + " were faulty before).");
		}

		//TODO: All neighbors under the given distance threshold are added to a new set of solutions, representing the reconfiguration space.
		writeToResultFile("Gaps in 2nd Run");
		writeToResultFile(calculateGaps(solutionsSecondRun)); //TODO: Merge best-fitting neighbors in new solutionset
		closeResultFile();
	}

	private SolutionSet analyse(FeatureModel featureModel, HashSet<String> faultyFeatures){	
		writeToResultFile(Commons.printFaultyFeatures(faultyFeatures));
		SolutionSet population = null;
		Algorithm algorithm;         // The algorithm to use
		Settings settings = null;

		//forward featuremodel and faulty features for further processing
		Object [] settingsParams = {problemName,featureModel,faultyFeatures} ;
		try {
			settings = (new SettingsFactory()).getSettingsObject(algorithmName, settingsParams) ;
			algorithm = settings.configure();
			population = algorithm.execute();
		} catch (JMException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		return population;
	}
	
	private SolutionSet getUniques(SolutionSet solutions){
		SolutionSet retValue = new SolutionSet();
		HashSet<String> included = new HashSet<String>();
		for (int i=0; i< solutions.size();i++) {
			Solution sol = solutions.get(i);
			if (!included.contains(sol.getDecisionVariables()[0].toString())) {
				retValue.setCapacity(retValue.getCapacity()+1);
				retValue.add(sol);
				included.add(sol.getDecisionVariables()[0].toString());
			}
		}
		return retValue;
	}
	
	
	/**
	 * 1) decode decisionvariables to feature selections
	 * 1.1) only select features without fault annotations!
	 * 2) check validity of configuration
	 * 2.1) if non-valid: store solution as unrealizable
	 * 2.2.) if valid: do nothing and smile! 
	 */
	private boolean proceedFaults(FeatureModel featureModel, HashSet<String> faultyFeatures){
		writeToResultFile(Commons.printFaultyFeatures(faultyFeatures));
		//re-caching qualities for selected features
		HashMap<String,ArrayList<Double>> qualityAttributeToValues = Commons.initObjectives(featureModel);
		HashMap <String, QualityAttributeWithValue[]> qualityAttributesWithValuesForFeature = Commons.cacheAttributeValuesForFeatures(featureModel,qualityAttributeToValues);
		
		List<String> featureNames = featureModel.getConcreteFeatureNames();
		for (int sol=0; sol < solutionsFirstRun.size(); sol++) {
			Solution solution = solutionsFirstRun.get(sol);
			Binary variable = ((Binary)solution.getDecisionVariables()[0]);	
			Configuration c = new Configuration(featureModel, Commons.PROPAGATION, Commons.ABSTRACTFEATURES); //TODO: check propagation issues
			for(int i=0; i<variable.bits_.length(); i++){
				if(variable.bits_.get(i)){
					String currentFeatureName = featureNames.get(i);
					if(!faultyFeatures.contains(currentFeatureName)){
						//that the point: only select non-faulty features!!!
						c.setManual(currentFeatureName , Selection.SELECTED);
						//System.out.println("Feature " + currentFeatureName + " was selected (not faulty).");
					}
//					else{
//						System.out.println("Selection of faulty feature " + currentFeatureName + " was prohibited!");
//						c.setManual(currentFeatureName , Selection.UNSELECTED);
//					}
				}
			}
			if(!c.isValid()){
//				System.out.println("Configuration is now non-valid!");
				//store solution as faulty
				faultySolutionsFirstRun.setCapacity(faultySolutionsFirstRun.getCapacity()+1);
				faultySolutionsFirstRun.add(solution);
			}
			else{
//				System.out.println("Configuration is still valid!");
				//store solution as still healthy
				healtySolutionsFirstRun.setCapacity(healtySolutionsFirstRun.getCapacity()+1);
//				//Re-calculate sum and mean of quality values wrt. one feature less!!
				Commons.calculateObjectiveValues(qualityAttributeToValues, qualityAttributesWithValuesForFeature, c, solution);
				healtySolutionsFirstRun.add(solution);		
			}
//			System.out.println("Solution: " + solution + "\n");
		}
		return faultySolutionsFirstRun.size() > 0;
	}
	
	private void writeStats(FeatureModel featureModel){
		writeToResultFile("Solution Sets");
		if(solutionsFirstRun != null){
			writeToResultFile("SolutionsFirstRun (#" + solutionsFirstRun.size() + "):");
			writeToResultFile(getSolutionsConfigurationString(solutionsFirstRun, featureModel));	
		}	
		if(faultySolutionsFirstRun != null){
			writeToResultFile("FaultySolutionsFirstRun (#" + faultySolutionsFirstRun.size() + "):");
			writeToResultFile(getSolutionsConfigurationString(faultySolutionsFirstRun, featureModel));
		}
		if(healtySolutionsFirstRun != null){
			writeToResultFile("HealtySolutionsFirstRun (#" + healtySolutionsFirstRun.size() + "):");
			writeToResultFile(getSolutionsConfigurationString(healtySolutionsFirstRun, featureModel));
		}
		if(solutionsSecondRun != null){
			writeToResultFile("SolutionsSecondRun (#" + solutionsSecondRun.size() + "):");
			writeToResultFile(getSolutionsConfigurationString(solutionsSecondRun, featureModel));
		}
	}
	
	/**
	 * Measure distances of two solutions
	 * 1: Measure distances between faultySolutions and healty ones from 1. Run
	 * 2: Measure distances between faultySolutions and all solutions from 2. Run
	 * 3: Compare distances to find nearest neighbors (important: don't compare faultySolutions to each other!)
	 * 4: TODO Respect Pareto Dominance!
	 */
	private void measaureDistances(FeatureModel featureModel){
		writeStats(featureModel);

		int newNeigbors=0;
		Distance dis = new Distance();	
		for (int i = 0; i < faultySolutionsFirstRun.size(); i++){
			Solution faultySolution = faultySolutionsFirstRun.get(i);
			double distanceToHealtySolutionsFristRun = Double.POSITIVE_INFINITY;
			double distanceToSolutionsSecondRun = Double.POSITIVE_INFINITY;		
			try {
				distanceToHealtySolutionsFristRun = dis.distanceToSolutionSetInObjectiveSpace(faultySolution, healtySolutionsFirstRun);	
				distanceToSolutionsSecondRun = dis.distanceToSolutionSetInObjectiveSpace(faultySolution, solutionsSecondRun);
			} catch (JMException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(distanceToSolutionsSecondRun < distanceToHealtySolutionsFristRun){
				writeToResultFile("Found nearer neighbour in second run.");
				writeToResultFile("\tFaulty   : " + getSolutionString(faultySolution));
//				writeToResultFile("\tConf. F. : " + Commons.getConfigurationString(Commons.decodeSolutionToConfiguration(faultySolution, featureModel)));
				Solution nearest = nearestSolutionAndDistanceInObjectiveSpace(faultySolution,solutionsSecondRun, Origin.SECOND).getSolution();
				writeToResultFile("\tNei. New : " + getSolutionString(nearest));
//				writeToResultFile("\tConf. N. : " + Commons.getConfigurationString(Commons.decodeSolutionToConfiguration(nearest, featureModel)));
				writeToResultFile("\tDistance : " + distanceToSolutionsSecondRun);
				Solution old = nearestSolutionAndDistanceInObjectiveSpace(faultySolution,healtySolutionsFirstRun, Origin.FIRSTHEALTHY).getSolution();
				if(old == null){
					writeToResultFile("\tNo neighbor exists in first run.");
				}
				else{
					writeToResultFile("\tNei. Old : " + getSolutionString(old));
//					writeToResultFile("\tConf. O. : " + Commons.getConfigurationString(Commons.decodeSolutionToConfiguration(old, featureModel)));
					writeToResultFile("\tDistance : " + distanceToHealtySolutionsFristRun);
				}
				newNeigbors++;
			}
			else{
				writeToResultFile("No nearer neighbor found.");
				writeToResultFile("\tFaulty   : " + getSolutionString(faultySolution));
//				writeToResultFile("\tConf. F. : " + Commons.getConfigurationString(Commons.decodeSolutionToConfiguration(faultySolution, featureModel)));
			}
		}
		writeToResultFile("Number of new nearer neighbors: " + newNeigbors + " (" + faultySolutionsFirstRun.size() + " were faulty before).");
	}

	
	/**
	 * TODO: to be implemented 
	 * 
	 * For optimization of the design space, the largest distances between faulty configurations and nearest 
	 * neighbors are investigated. To prepare visualization in a comprehensive manner, our implementation make 
	 * pair-wise comparisons, to find the larges distance between two quality dimensions.	
	 */
	private String calculateGaps(SolutionSet solutions) {
		String res = "Largest quality gaps:";
		if(solutions != null && solutions.size() > 0){	
			int noObjectives = solutions.get(0).getNumberOfObjectives();
			double[] diff = new double[noObjectives];//Auxiliar var
			double[] maxDiff = new double[noObjectives]; //maximal difference between two solutions for ONE objective
			HashMap<Integer,ArrayList<Solution>> highDiffs = new HashMap<Integer, ArrayList<Solution>>(); //FIXME: dirty
			Solution high1 = null;
			Solution high2 = null;
			for (int obj = 0; obj < noObjectives;obj++){
				for (int i = 0; i < solutions.size(); i++) {
					Solution sol1 = solutions.get(i);
					for (int j = i; j < solutions.size(); j++) {
						Solution sol2 = solutions.get(j);
						//find max. distance between qualities
						diff[obj] = sol1.getObjective(obj) - sol2.getObjective(obj);
						if(maxDiff[obj]<=diff[obj]){
							maxDiff[obj] = diff[obj];
							high1 = sol1;
							high2 = sol2;
						}
					} 
				}
				ArrayList<Solution> highList = new ArrayList<Solution>();
				highList.add(high1);
				highList.add(high2);
				highDiffs.put(obj, highList);
			}

			for (int i : highDiffs.keySet()) {
				res += "\nObjective " + i + ": " + maxDiff[i] + " for " + getSolutionString(highDiffs.get(i).get(0)) + " / " + getSolutionString(highDiffs.get(i).get(1));
			}
		}
		else
			res += "\nn/a";
		return res;
	}

	private HashMap<Solution,ArrayList<ModCompSolution>> distancesMap(){
		HashMap<Solution,ArrayList<ModCompSolution>> map = new HashMap<Solution, ArrayList<ModCompSolution>>();
		Distance dis = new Distance();

		for (int i = 0; i < faultySolutionsFirstRun.size(); i++) {
			Solution faultySolution = faultySolutionsFirstRun.get(i);
			ArrayList<ModCompSolution> neighbors = new ArrayList<ModCompSolution>();
			for (int j = 0 ; j < healtySolutionsFirstRun.size(); j++) {
				double distance = 0;
				Solution neighbor = healtySolutionsFirstRun.get(j);
				distance = dis.distanceBetweenObjectives(faultySolution, neighbor);
				neighbors.add(new ModCompSolution(neighbor, distance, Origin.FIRSTHEALTHY));
			}
			for (int j = 0 ; j < solutionsSecondRun.size(); j++) {
				double distance = 0;
				Solution neighbor = solutionsSecondRun.get(j);
				distance = dis.distanceBetweenObjectives(faultySolution, neighbor);
				neighbors.add(new ModCompSolution(neighbor, distance, Origin.SECOND));
			}
			Collections.sort(neighbors);
			map.put(faultySolution, neighbors);
		}
		return map;   
	}
	
	private String getSolutionString(Solution solution){
		if(solution == null)
			return "n/a";
		else
			return solution.toString() + " (" + ((Binary)solution.getDecisionVariables()[0]).toString() + ")";
	}
	
	private String getSolutionDistanceString(ModCompSolution solutionDis){
		return getSolutionString(solutionDis.getSolution()) + " [" + solutionDis.getDistance() + "]";
	}
	
	private String getSolutionsString(SolutionSet solutions){
		String str = "";
		for (int i = 0; i < solutions.size(); i++){
			Solution sol = solutions.get(i);
			for (int j=0; j < sol.getNumberOfObjectives();j++) {
				str += sol.getObjective(j)+" ";
			}
			str += " (" + ((Binary)sol.getDecisionVariables()[0]).toString() + ")\n";
		}
		return str;
	}
	
	private String getSolutionsConfigurationString(SolutionSet solutions, FeatureModel featureModel){
		boolean infin;
		String str = "";
		for (int i = 0; i < solutions.size(); i++){
			infin = false;
			Solution sol = solutions.get(i);
			for (int j=0; j < sol.getNumberOfObjectives();j++) {
				if(sol.getObjective(j) == Double.POSITIVE_INFINITY){
					infin = true;
					break;
				}
				str += sol.getObjective(j)+" ";
			}
			if(!infin){
				str += " (" + ((Binary)sol.getDecisionVariables()[0]).toString() + ") " + Commons.getConfigurationString(Commons.decodeSolutionToConfiguration(sol, featureModel)) + "\n";
			}
		}
		return str;
	}
	
	/**
	   * Return the index of the nearest solution in the solution set to a given solution
	   * @param solution
	   * @param solutionSet
	   * @return  The index of the nearest solution; -1 if the solutionSet is empty
	   */
	  private ModCompSolution nearestSolutionAndDistanceInObjectiveSpace(Solution solution, SolutionSet solutionSet, ModCompSolution.Origin origin) {
	    Distance dis = new Distance();
//	    int index = -1 ;
		Solution nearestSolution = null;
		double minimumDistance = Double.MAX_VALUE;
	    
	    for (int i = 0 ; i < solutionSet.size(); i++) {
	    	double distance = 0;
	    	nearestSolution = solutionSet.get(i);
	    	distance = dis.distanceBetweenObjectives(solution, nearestSolution);
	    	if (distance < minimumDistance) {
	    		minimumDistance = distance ;
//	    		index = i ;
	    	}
	    }

	    return new ModCompSolution(nearestSolution, minimumDistance, origin);
	  }
	
	/**
	 * Parse FeatureModel
	 * @param modelFilePath
	 * @return
	 */
	private FeatureModel loadFeatureModel(String modelFilePath){
		FeatureModel featureModel = null;
		try {
			featureModel = new FeatureModel(); 
			XmlFeatureModelReader reader = new XmlFeatureModelReader(featureModel);
			reader.readFromFile(new File(modelFilePath));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return featureModel;
	}
	
	private void prepareResultFile(){
		String DATE_FORMAT = "yyyyMMdd-HHmm";
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
		Calendar c1 = Calendar.getInstance(); // today
		String timestamp = sdf.format(c1.getTime());
		String resultFolderPathTimed = Config.resultFolderPath + "/" + timestamp + ".txt";
		File resultFile = new File(resultFolderPathTimed);
		try {
			if(!resultFile.exists())
				resultFile.createNewFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			resultFileStream = new FileWriter(resultFile.getAbsoluteFile(), true);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		resultFileOut = new BufferedWriter(resultFileStream);
	}
	
	private void writeToResultFile(String result){
		writeToResultFile(result, true);	
	}
	
	private void writeToResultFile(String result, boolean alsoPrint){
		try {
			resultFileOut.write(result+"\n");
			if(alsoPrint)
				System.out.println(result);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void closeResultFile(){
		try {
			resultFileOut.close();
			resultFileStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * parse FaultFile to FaultString
	 * File contains line-wise names of features, that represent faulty system elements
	 * @param faultsFilePath
	 * @return
	 */
	private HashSet<String> loadFaultyFeatures(String faultsFilePath){
		HashSet<String> faultyFeatures = new HashSet<String>();
		try (BufferedReader br = new BufferedReader(new FileReader(faultsFilePath))) {
			String line;
//			System.out.println("Faults:");
			while ((line = br.readLine()) != null) {
				faultyFeatures.add(line);
//				System.out.println(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return faultyFeatures;
	}
	
}
