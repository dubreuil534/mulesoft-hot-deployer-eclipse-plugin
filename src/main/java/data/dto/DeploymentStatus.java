package data.dto;

public enum DeploymentStatus {
	DEPLOYE("D�ploy�"),
	DEPLOIEMENT_EN_COURS("A d�ployer"),
	NON_DEPLOYE("Non deploy�");
	private final String label;
	private DeploymentStatus(final String label) {
		this.label = label;
	}
	public String getLabel() {
		return label;
	}
}
