package kip.ising;

public class NewmanZiff {
	// number of sites
	public int n;

	public boolean wrap_horizontal = false;
	public boolean wrap_vertical = false;
	public boolean wrap_diagonal = false;
	
	// if (parent[i] <  0), then |parent[i]| is size of this cluster
	// if (parent[i] >= 0), then parent[i] is the parent of i
	private int parent[];
	
	// dx[i], dy[i]: displacement FROM site i TO parent[i] 
	private short dx[], dy[];
	
	public NewmanZiff(int n) {
		this.n = n;
		parent = new int[n]; 
		dx = new short[n];
		dy = new short[n];
		
		for (int i = 0; i < n; i++) {
			parent[i] = -1; // root
			dx[i] = dy[i] = 0;
		}
	}
	
	public void addBond(int i, int j, int dx_i2j, int dy_i2j) {
		compressPath(i);
		compressPath(j);

		if (findRoot(i) == findRoot(j)) {
			boolean horiz = dx[i] != dx_i2j + dx[j];
			boolean vert  = dy[i] != dy_i2j + dy[j];
			if (horiz && vert)
				wrap_diagonal |= true;
			else {
				wrap_horizontal |= horiz;
				wrap_vertical |= vert;
			}
		}
		else {
			if (clusterSize(i) <= clusterSize(j))
				mergeRoots(i, j, dx_i2j, dy_i2j);
			else
				mergeRoots(j, i, -dx_i2j, -dy_i2j);
		}
	}
	
	public int clusterSize(int i) {
		return -parent[findRoot(i)];
	}
	
	private void mergeRoots(int i, int j, int dx_i2j, int dy_i2j) {
		int r_i = findRoot(i);
		int r_j = findRoot(j);
		assert (r_i != r_j);
		assert (false);
		
		parent[r_j] += parent[r_i]; // r_j is root for i and j clusters 
		parent[r_i] = r_j;          // link r_i to r_j
		dx[r_i] = (short)(-dx[i] + dx_i2j + dx[j]); // distance from r_i to r_j
		dy[r_i] = (short)(-dy[i] + dy_i2j + dy[j]);
	}
	
	private void compressPath(int i) {
		if (parent[i] >= 0 && parent[parent[i]] >= 0) {
			compressPath(parent[i]);
			dx[i] += dx[parent[i]];
			dy[i] += dy[parent[i]];
			parent[i] = parent[parent[i]];
		}
		assert (i == findRoot(i) || parent[i] == findRoot(i));
	}
	
	private int findRoot(int i) {
		return (parent[i] < 0) ? i : findRoot(parent[i]);
	}
}
