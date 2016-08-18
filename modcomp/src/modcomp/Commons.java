package modcomp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import de.ovgu.featureide.fm.core.Feature;
import de.ovgu.featureide.fm.core.FeatureModel;
import de.ovgu.featureide.fm.core.configuration.Configuration;
import de.ovgu.featureide.fm.core.configuration.SelectableFeature;
import de.ovgu.featureide.fm.core.configuration.Selection;
import jmetal.core.Solution;
import jmetal.core.SolutionSet;
import jmetal.encodings.variable.Binary;

public class Commons {

	/**
	 * ignore qas
	 * examples: pollution, tastedeviation, responsetime, energyconsumption
	 */
	private static String[] ignoredQualityAttributes = new String[]{};
	
	/**
	 * propagate decisions
	 * true: if annother feature is neccessary for selection, it is selected automatically
	 * false: selection is done only manually, no calculations are done in addition
	 * 
	 * here: false is appropriate
	 */
	public static boolean PROPAGATION = false;
	public static boolean ABSTRACTFEATURES = false;
	
	private static boolean isIgnoredQualityAttribute(String qa){
		boolean isIn = false;
		for (int i = 0; i < ignoredQualityAttributes.length; i++) {
			if(ignoredQualityAttributes[i].equals(qa)){
				isIn = true;
				break;
			}
		}
		return isIn;
	}
	
	public static void calculateObjectiveValues(HashMap<String,ArrayList<Double>> qualityAttributeToValues,
			HashMap <String, QualityAttributeWithValue[]> qualityAttributesWithValuesForFeature,
			Configuration c, Solution solution){
		//sumup values of selected features			
		//clear values from previous config
		for(ArrayList<Double> values : qualityAttributeToValues.values()){
			values.clear();
		}
		
		String featureName;
		QualityAttributeWithValue[] qualityAttributes;
		for(Feature f : c.getSelectedFeatures()){
			featureName = f.getName();
			qualityAttributes = qualityAttributesWithValuesForFeature.get(featureName);
			if(qualityAttributes != null){
				for(QualityAttributeWithValue qa : qualityAttributes){					
//					System.out.println("1: " + qa.getAttributeName());
//					System.out.println("2: " + qa.getValue());
//					System.out.println("3: " + qualityAttributeToValues);
//					System.out.println("4: " + qualityAttributeToValues.get(qa.getAttributeName()));
					qualityAttributeToValues.get(qa.getAttributeName()).add(qa.getValue());
				}
			}
		}
		
		//aggregate values
		int i=0;	
//		System.out.println(printConfiguration(c));
//		for(String qa : qualityAttributeToValues.keySet()){
		for(ArrayList<Double> values : qualityAttributeToValues.values()){
			double aggregatedValue=0;
//			System.out.println("\nQA" + i + " (" + qa + "):");
//			int j=0;
//			for(double value : qualityAttributeToValues.get(qa)){
			for(double value : values){
//				System.out.println("val" + j + ": " + value);
				aggregatedValue += value; //TODO: other function than sum are possible, e.g. Mean, Median
//				aggregatedValue += Math.round(value*100.0)/100.0; //round to two digets
//				j++;
			}
			aggregatedValue = Math.round(aggregatedValue*10.0)/10.0; //FIXME round to one digets for pretty logging, depends on precious of values (here only #.#)
//			System.out.println("sum: " + aggregatedValue);
			solution.setObjective(i, aggregatedValue); //NSGA-II minimizes values TODO: provide min/max parameter per attribute	
			//solution.setObjective(i, -(sum/values.size())); //mean: sum divided by number of values
			i++; //next objective
		}
	}
	
	
	/**
	 * decode decisionvariables to configuration
	 */
	public static Configuration decodeSolutionToConfiguration(Solution solution, FeatureModel featureModel){
		List<String> featureNames = featureModel.getConcreteFeatureNames();
		Binary variable = ((Binary)solution.getDecisionVariables()[0]);	
		Configuration c = new Configuration(featureModel, PROPAGATION, ABSTRACTFEATURES);
		for(int i=0; i<variable.bits_.length(); i++){
			if(variable.bits_.get(i)){
				c.setManual(featureNames.get(i) , Selection.SELECTED);
			}
		}
		if(c.isValid()){
//			System.out.println("valid");
			return c;
		}
		else{
//			System.out.println("\ninvalid");
//			System.out.println(c);
			return null;
		}
	}
	
	/**
	 * Adding values of objectives per configuration (once during calculation)
	 * @param featureModel
	 * @return
	 */
	public static HashMap<String,ArrayList<Double>> initObjectives(FeatureModel featureModel){
		//In Beschreibung der Wurzel des FMs sind die Qualitaetsattribute zeilenweise aufgefuehrt
		String rootDesc = featureModel.getRoot().getDescription();
		if(rootDesc.isEmpty()){
			System.out.println("Description of root node is empty; undefined quality attributes.");
			System.exit(-1);
		}
//		else{
//			System.out.println("Quality attributes defined in root node of feature model:" + System.getProperty("line.separator") + rootDesc);
//		}
		//Init Hashmap for adding values of objectives per configuration (once during calculation)
		HashMap<String,ArrayList<Double>> qualityAttributeToValues = new HashMap<String, ArrayList<Double>>();
		
		System.out.print("Quality attributes:");
		int i=0;
		for(String qaname : rootDesc.split(System.getProperty("line.separator"))){
			if(!isIgnoredQualityAttribute(qaname)){
				qualityAttributeToValues.put(qaname, new ArrayList<Double>());
				System.out.print(" " + i + ":" + qaname);
				i++;
			}
			else{
				System.out.print(" IGNORED:" + qaname);
			}
		}
		System.out.println();
		return qualityAttributeToValues;
	}
	
	/**
	 * Values fuer jedes konkrete Feature aus FM rausholen und in Hashmap ablegen
	 * @param featureModel
	 */
	public static HashMap <String, QualityAttributeWithValue[]> cacheAttributeValuesForFeatures(FeatureModel featureModel, HashMap<String,ArrayList<Double>> qualityAttributeToValues){
		//Values fuer jedes konkrete Feature aus FM rausholen und in Hashmap ablegen
		HashMap <String, QualityAttributeWithValue[]> qualityAttributesWithValuesForFeature = new HashMap<String, QualityAttributeWithValue[]>();
		
		String featureName;
		String featureDesc;
		String[] lines;
		String[] splittedLine;
		QualityAttributeWithValue[] qualityAttributes;
		ArrayList<QualityAttributeWithValue> qualityAttributesList;
		String attribute;
		double value;
		for(Feature f : featureModel.getConcreteFeatures()){
			featureName = f.getName();
			featureDesc = f.getDescription();
			//Format: attribute=value
			//Value range of [0.0;1.0]
			//note: attributes can be missing, but each attribute must be defined in root
			if(!featureDesc.isEmpty()){
				lines = featureDesc.split(System.getProperty("line.separator"));
				qualityAttributesList = new ArrayList<QualityAttributeWithValue>();
//				int noAttributes = 0;
				for(int i=0;i<lines.length;i++){
					splittedLine = lines[i].split("=");
					attribute = splittedLine[0];
					if(!isIgnoredQualityAttribute(attribute)){
						if(!qualityAttributeToValues.containsKey(attribute)){
							System.err.println("Quality attribute " + attribute + " of feature " + featureName + " is not defined in root of feature model.");
							System.exit(-1); 
						}
						if(splittedLine.length==2){
							//value is available
							value = Double.parseDouble(splittedLine[1]);
//							qualityAttributes[noAttributes] = new QualityAttributeWithValue(attribute, value);
							qualityAttributesList.add(new QualityAttributeWithValue(attribute, value));
//							noAttributes++;
						}
						else{
							System.err.println("Value for quality attribute " + attribute + " of feature " + featureName + " is missing, please follow format attribute=value in range of [0.0;1;0]");
							System.exit(-1); 
						}
					}
				}
				qualityAttributes = new QualityAttributeWithValue[qualityAttributesList.size()];
				//FIXME: Dirty list2array hack
				for (int i = 0; i < qualityAttributes.length; i++) {
					qualityAttributes[i] = qualityAttributesList.get(i);
				}
				qualityAttributesWithValuesForFeature.put(featureName,qualityAttributes);
			}
		}
		return qualityAttributesWithValuesForFeature;
	}
	
	/**
	 * Prints FeatureModel
	 * @param featureModel
	 */
	public static void printFeatureModel(FeatureModel featureModel){
		System.out.println("All concrete Features:");
		int j=0;
		for (String s:  featureModel.getConcreteFeatureNames()){
			System.out.println(j + ": "+ s);
			j++;
		}
	}
	
	/**
	 * Prints Population
	 * @param population
	 */
	public static void printSolutions(SolutionSet population){
		for (int sol=0; sol < population.size(); sol++) {
			Solution currentSol = population.get(sol);
			System.out.println(currentSol.toString());
			System.out.println(currentSol.getDecisionVariables()[0].toString());
		}
	}
	
	public static String getConfigurationString(Configuration conf) {
		StringBuilder builder = new StringBuilder();
		if(conf != null){
			for (SelectableFeature feature : conf.getFeatures()) {
				if (feature.getSelection() == Selection.SELECTED && feature.getFeature().isConcrete()) {
					builder.append(feature.getFeature().getName());
					builder.append("; ");
				}
			}
		}
		return builder.toString();
	}
	
	public static String printFaultyFeatures(HashSet<String> faultyFeatures){
		String str = "Faults:";
		if(faultyFeatures != null){
			for (String fault : faultyFeatures) {
				str += " " + fault;
			}
		}
		else{
			str += " n/a";
		}
		return str;
	}
	
}
