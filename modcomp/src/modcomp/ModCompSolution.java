package modcomp;

import jmetal.core.Solution;

public class ModCompSolution implements Comparable<ModCompSolution>{

	private Solution solution;
	private Double distance;
	private Origin origin;
	public enum Origin {
		FIRST, FIRSTHEALTHY, FIRSTFAULTY, SECOND
	}
	
	public ModCompSolution(Solution solution, Double distance, Origin origin) {
		this.solution = solution;
		this.distance = distance;
		this.origin = origin;
	}
	
	public Solution getSolution() {
		return solution;
	}
	
	public Double getDistance() {
		return distance;
	}
	
	public Origin getOrigin(){
		return origin;
	}
	
	@Override
	public int compareTo(ModCompSolution o) {
		return new Double(distance).compareTo(o.getDistance());
	}
	
}
