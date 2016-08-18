package jmetal.problems;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import de.ovgu.featureide.fm.core.FeatureModel;
import de.ovgu.featureide.fm.core.configuration.Configuration;
import de.ovgu.featureide.fm.core.configuration.Selection;
import jmetal.core.Problem;
import jmetal.core.Solution;
import jmetal.encodings.solutionType.BinarySolutionType;
import jmetal.encodings.variable.Binary;
import jmetal.util.JMException;
import modcomp.Commons;
import modcomp.QualityAttributeWithValue;

public class ModComp extends Problem {

	private FeatureModel featureModel;
	private List<String> featureNames;
	private HashSet<String> faultyFeatures;
	/** Set to collect values for objectives **/
	private HashMap<String,ArrayList<Double>> qualityAttributeToValues; 
	private HashMap <String, QualityAttributeWithValue[]> qualityAttributesWithValuesForFeature;

	public ModComp(String solutionType, FeatureModel featureModel, HashSet<String> faultyFeatures) {
		this.featureModel = featureModel;
		this.faultyFeatures = faultyFeatures;
		featureNames = featureModel.getConcreteFeatureNames();
		qualityAttributeToValues = Commons.initObjectives(featureModel);
		qualityAttributesWithValuesForFeature = Commons.cacheAttributeValuesForFeatures(featureModel, qualityAttributeToValues);
		
		numberOfVariables_   = 1; //chromosome
		numberOfObjectives_  = qualityAttributeToValues.size();
		numberOfConstraints_ = 0;
		problemName_         = "TET_Demo";
		
		length_ = new int[numberOfVariables_];
		length_[0] = featureNames.size(); // Gene

		if (solutionType.compareTo("Binary") == 0)
			solutionType_ = new BinarySolutionType(this) ;
		else {
			System.err.println("TET_Demo: solution type " + solutionType + " invalid") ;
			System.exit(-1);
		}  

	}

	@Override
	public void evaluate(Solution solution) throws JMException {
		Binary variable = ((Binary)solution.getDecisionVariables()[0]);		
		Configuration c = new Configuration(featureModel, Commons.PROPAGATION, Commons.ABSTRACTFEATURES);
		for(int i=0; i<variable.bits_.length(); i++){
			if(variable.bits_.get(i)){
				//System.out.println("Add feature " + featureNames.get(i) + " (" + i + ") to conf.");
				String featureName = featureNames.get(i);
				if(faultyFeatures == null){
					c.setManual(featureName, Selection.SELECTED);
				}
				else if(!faultyFeatures.contains(featureName)){
					c.setManual(featureName, Selection.SELECTED);
				}
				else{
					//faulty feature is marked for selection
					//do not add feature to config, instead repair vector
					variable.bits_.set(i, false);
				}
			}
		}
		
		if(c.isValid()){
//			System.out.println("Solution " + solution.getDecisionVariables()[0] + " is valid!");
			Commons.calculateObjectiveValues(qualityAttributeToValues, qualityAttributesWithValuesForFeature, c, solution);
		}
		else{
//			System.out.println("Solution " + solution.getDecisionVariables()[0] + " is not valid!");
			//degradate solution
			for(int i=0;i<numberOfObjectives_;i++){
				solution.setObjective(i, Double.POSITIVE_INFINITY);
			}
		}
	}
}