package data.dto;

public enum DeploymentStatus {
	DEPLOYE("D�ploy�"),
	DEPLOIEMENT_EN_COURS("D�ploiement en cours"),
	NON_DEPLOYE("Non deploy�"),
	INCONNU("Inconnu");
	private final String label;
	private DeploymentStatus(final String label) {
		this.label = label;
	}
	public String getLabel() {
		return label;
	}
}
