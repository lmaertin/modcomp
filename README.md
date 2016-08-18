Tooling of "Fault-aware Pareto Frontier Exploration for Dependable System Architectures" (3rd ModComp WS @MODELS'16)

Basic Setup:
- Eclipse Mars2 (RCP)
- FeatureIDE 2.7.5 Plug-In (http://wwwiti.cs.uni-magdeburg.de/iti_db/research/featureide/)
- JavaSE-1.8
- Mac OS X El Capitan

Installation
- Clone git repository as new Plugin-Project
- Add Libaries to the folder /lib
  - commons-math3-3.6.1.jar (http://commons.apache.org/proper/commons-math/download_math.cgi  Binaries)
  - jmetal4.5.jar (https://sourceforge.net/projects/jmetal/files/jmetal4.5/) 

Configuration in src/modcomp/Config.java:
- paths
  - Basepath
  - Path to Feature Model (default: /model/model.xml)
  - Path to List of Faults (default: /model/faults.txt)
  - Path to result output dir (default: /output)
- NSGA-II fine-tuning
  - population size
  - number of iterations

Usage
1) Start plug-in as Eclipse Application
2) Click "Start optimization" or press "cmd+F5" or "ctrl+F5"
3) Open console and wait...
4) Explore results in output dir (default: /output)
