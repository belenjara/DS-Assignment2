package datalists.server;

import java.util.ArrayList;

import activitystreamer.util.Settings;

public class MyLevel {

	private int level = -1;
	private ArrayList<String> candidateList;
	boolean imPotentialRoot = false;
	
	public boolean isImPotentialRoot() {
		return imPotentialRoot;
	}

	public void setImPotentialRoot(boolean imPotentialRoot) {
		this.imPotentialRoot = imPotentialRoot;
	}

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
		if (this.level == 0 || (this.candidateList == null || this.candidateList.size() == 0)) {
			this.candidateList = new ArrayList<>();
			this.candidateList.add(Settings.getIdServer());
		}
		return candidateList;
	}

	public void setCandidateList(ArrayList<String> candidateList) {
		this.candidateList.clear();
		this.candidateList.addAll(candidateList);
	}
}
