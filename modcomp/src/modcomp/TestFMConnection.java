package modcomp;

import java.io.File;
import java.io.FileNotFoundException;

import de.ovgu.featureide.fm.core.FeatureModel;
import de.ovgu.featureide.fm.core.configuration.Configuration;
import de.ovgu.featureide.fm.core.configuration.Selection;
import de.ovgu.featureide.fm.core.io.UnsupportedModelException;
import de.ovgu.featureide.fm.core.io.xml.XmlFeatureModelReader;

public class TestFMConnection {
	
	public static void main(String[] args) {
		new TestFMConnection().start();
	}
	
	private void start(){
		FeatureModel featureModel = loadFeatureModel(Config.modelFilePath);
		Configuration c = new Configuration(featureModel, Commons.PROPAGATION, Commons.ABSTRACTFEATURES);
		addFeaturesTest(c);
		System.out.println(Commons.getConfigurationString(c));
		System.out.println("valid: " + c.isValid());
	}
	
	private void addFeaturesTest(Configuration c){
		//Still; NotePayment; Postmix; Gravity; WaterValve; CokeValve; WaterInjector; ChangeCoins; ChangeNotes;
		c.setManual("Still", Selection.SELECTED);
		c.setManual("NotePayment", Selection.SELECTED);
		c.setManual("Postmix", Selection.SELECTED);
		c.setManual("Gravity", Selection.SELECTED);
		c.setManual("WaterValve", Selection.SELECTED);
		c.setManual("CokeValve", Selection.SELECTED);
		c.setManual("WaterInjector", Selection.SELECTED);
		c.setManual("ChangeCoins", Selection.SELECTED);
		c.setManual("ChangeNotes", Selection.SELECTED);
		c.setManual("Pump", Selection.SELECTED);
	}
	
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
	
}
