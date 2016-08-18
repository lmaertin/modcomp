package modcomp;

public class Config {

	//Adjust paths, because new eclipse instances has no access to files in hosting instance.
	public static final String baseDir = "/Users/maertin/Documents/workspace-modcomp/modcomp";
	public static final String modelFilePath = baseDir + "/model/model.xml";
	public static final String faultsFilePath = baseDir + "/model/faults.txt";
	public static final String resultFolderPath = baseDir + "/output";
	
	//NSGA-II Settings
	public static final int populationSize = 2000; //default: 100
	public static final int iterations = 20000; //default: 250 => 250000 evals 
	//Approx. runtime on test system
	//100 x 200 = 16 sec
	//100 x 400 = 28 sec
	//100 x 800 = 54 sec
	//2000 x 100 = 166 sec
	//2000 x 200 = 328 sec
	//2000 x 20000 = a couple of hours...
	
}
