package modcomp;

public class QualityAttributeWithValue{
	
	private String attributeName;
	private Double value;
	
	public QualityAttributeWithValue(String attributeName, Double value) {
		this.attributeName = attributeName;
		this.value = value;
	}
	
	public String getAttributeName() {
		return attributeName;
	}
	
	public Double getValue() {
		return value;
	}	
}
