package datalists.server;

import java.util.ArrayList;

public class MyLevel {

	private int level = -1;
	private ArrayList<String> candidateList;
	
	public MyLevel() {
		this.candidateList = new ArrayList<>();
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public ArrayList<String> getCandidateList() {
		return candidateList;
	}

	public void setCandidateList(ArrayList<String> candidateList) {
		this.candidateList = candidateList;
	}
}
