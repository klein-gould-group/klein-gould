package rachele.damage2D.apps;

import java.awt.Color;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import rachele.damage2D.OFC_DamageLattice;
import rachele.util.FileUtil;
import scikit.dataset.Accumulator;
import scikit.dataset.DatasetBuffer;
import scikit.dataset.Histogram;
import scikit.graphics.dim2.Geom2D;
import scikit.graphics.dim2.Grid;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;
import scikit.jobs.params.ChoiceValue;
import scikit.jobs.params.DirectoryValue;
import scikit.util.Utilities;

/**
* Equilibrate in OFC, run in damage to max percent damage, equilibrate again in OFC, take data in OFC
*/
public class OFC_DamageDelayWriteApp extends Simulation{

	int dt;
	Grid grid = new Grid("Lattice");
	Grid cgGrid = new Grid("CG grid");
	Grid cgGridTimeAverage = new Grid("Time ave CG grid");
	Grid plateUpdateGrid = new Grid("Plate Update grid");
	Grid deadGrid = new Grid("Alive/Dead Lattice");

	Histogram sizeHist = new Histogram(1);
	String iMetFile;
	String sizeHistFile;
	String sizeFile;
	int maxSize;	
	int cg_dt;
	double percentDead;
	double maxPercentDamage;
	
	OFC_DamageLattice ofc;
	
	Accumulator invStressMetTempAcc = new Accumulator();
	Accumulator cgInvStressMetTempAcc = new Accumulator();
	Accumulator cgInvActivityMetTempAcc = new Accumulator();
	Accumulator maxSizeTempAcc = new Accumulator();
	Accumulator invSizeActMetTempAcc = new Accumulator();
	Accumulator	cgMaxSizeActBoxTempAcc = new Accumulator();
	Accumulator timeAveForMaxCGSizeActTempAcc = new Accumulator();
	
	public static void main(String[] args) {
		new Control(new OFC_DamageDelayWriteApp(), "OFC Damage Model");
	}

	public void load(Control c) {
		c.frameTogether("Grids", grid, cgGrid, plateUpdateGrid, cgGridTimeAverage);
		c.frame(deadGrid);

		params.add("Data Dir",new DirectoryValue("/Users/erdomi/data/damage/testRuns"));
		params.add("Interaction", new ChoiceValue( "Circle", "Fully Connected", "Square", "Small World") );
		params.addm("Random Seed", 1);
		params.addm("CG size", 30);
		params.addm("dx", 9);
		params.addm("Coarse Grained dt (PU)", 100);
		params.addm("Equilibration Updates", 500);
		params.addm("Max PU", 1000000);
		params.addm("Data points per write", 10);
		params.addm("R", 16);// 0 -> fully connected
		params.addm("Residual Stress", 0.625);
		params.addm("Dissipation Param", 0.3);
		params.addm("Res. Max Noise", 0.125);
		params.addm("Lower Cutoff", 1);
		params.addm("Mean Max Failures", 1);
		params.addm("Failures Max Noise", 0);
		params.addm("Mean Heal Time", 0);
		params.addm("Heal Time Noise", 0);
		params.addm("Max Percent Damage", 0.05);
		params.add("L");
		params.add("Time");
		params.add("Av Size");
		params.add("Plate Updates");
		params.add("Percent dead sites");
	}

	public void animate() {
		
		grid.registerData(ofc.L, ofc.L, ofc.stress);
		cgGrid.registerData(ofc.Lp, ofc.Lp, ofc.epicenterCount);
		cgGridTimeAverage.registerData(ofc.Lp, ofc.Lp, ofc.CG_ActivityTimeAve);
		plateUpdateGrid.registerData(ofc.L, ofc.L, ofc.plateUpdateFailLocations);
		deadGrid.registerData(ofc.L, ofc.L, ofc.noFails);
		
		grid.clearDrawables();
		double radius = 1.0/(2.0*ofc.L);
		double failSite_y = ((double)(ofc.epicenterSite/ofc.L))/ofc.L + radius;
		double failSite_x = ((double)(ofc.epicenterSite%ofc.L))/ofc.L + radius;
		grid.addDrawable(
				Geom2D.circle(failSite_x, failSite_y, radius, Color.GREEN));
		
		params.set("Time", Utilities.format(ofc.cg_time));
		params.set("Plate Updates", ofc.plateUpdates);
		percentDead = ofc.noDeadSites/(double)(ofc.L*ofc.L);
		params.set("Percent dead sites", percentDead);
		params.set("Av Size", ofc.avSize);

	}

	public void clear() {
	}

	
	public void run() {
		ofc = new OFC_DamageLattice(params);
		initFiles();
		
		dt = params.iget("Coarse Grained dt (PU)");
		maxPercentDamage = params.fget("Max Percent Damage");
		double nextAccumTime = 0;
		int maxTime = params.iget("Max PU");
		int dataPointsPerWrite = params.iget("Data points per write");
		
		maxSize = 0;
		int dataPointsCount = 0;
		percentDead = 0;
		
		// Equilibrate
		System.out.println("Equlibrating");
		ofc.initEquilibrate(params.iget("Equilibration Updates"));
		while (ofc.plateUpdates < 0){
			ofc.equilibrate();
			Job.animate();
		}
		System.out.println("Finished Equilibration 1");
		
		// Kill
		while (percentDead <= maxPercentDamage){
			ofc.damagePreStep();
			Job.animate();
			while (ofc.nextSiteToFail >= 0){
				ofc.damageStepIter();
				Job.animate();
			}	
		}
		System.out.println(percentDead + " dead after " + ofc.plateUpdates + " PU");
		FileUtil.printlnToFile(iMetFile, "# ", percentDead, " percent dead after " , ofc.plateUpdates, " PU.");

		// Equilibrate again
		ofc.initEquilibrate(params.iget("Equilibration Updates"));
 		while (ofc.plateUpdates < 0){
			ofc.equilibrate();
			Job.animate();
		}
		System.out.println("Finished Equilibration 2");
		ofc.plateUpdates = 0;
		
		while(true){
			ofc.healPreStep();
			Job.animate();
			while (ofc.nextSiteToFail >= 0){
				ofc.healStepIter();
				Job.animate();
			}	
			
			int size = ofc.avSize;
			sizeHist.accum(size);
			if (size > maxSize) {
				maxSize = size;
			}
			double stressMetric = ofc.calcStressMetric();
//			System.out.println(nextAccumTime);
			if(ofc.plateUpdates > nextAccumTime){ //Accumulate data to be written
				Job.animate();
				
				//maxSize
				maxSizeTempAcc.accum(ofc.cg_time, maxSize);
				
				int maxEpicenter=0;
				int loc = 0;
				for(int i = 0; i < ofc.Np; i++){
					if (ofc.epicenterSize[i] > maxEpicenter){
						maxEpicenter = ofc.epicenterSize[i];
						loc = i;
					}
				}
				cgMaxSizeActBoxTempAcc.accum(ofc.cg_time, maxEpicenter);
				timeAveForMaxCGSizeActTempAcc.accum(ofc.cg_time, ofc.CG_SizeActTimeAve[loc]);
				if(dataPointsCount ==0) System.out.println("max loc = " + maxEpicenter + " " + ofc.CG_SizeActTimeAve[loc]);
				
				//stress metric
				double inverseStressMetric = 1.0/stressMetric;
				invStressMetTempAcc.accum(ofc.plateUpdates, inverseStressMetric);
				//CG stress metric
				double cgInverseStressMetric = 1.0/ofc.calcCG_stressMetric();				
				cgInvStressMetTempAcc.accum(ofc.cg_time, cgInverseStressMetric);
				//CG activity metric
				double cgInverseActivityMetric = 1.0/ofc.calcCG_activityMetric();
				cgInvActivityMetTempAcc.accum(ofc.cg_time, cgInverseActivityMetric);
				// size activity metric
				double cgInverseSizeActMet = 1.0/ofc.calcCG_sizeActMetric();
				invSizeActMetTempAcc.accum(ofc.cg_time, cgInverseSizeActMet);
				
				nextAccumTime += dt;
				dataPointsCount += 1;
				maxSize = 0;

			}
			
//			System.out.println("no data pts = " + dataPointsCount + " cg time = " + ofc.cg_time + " next accum time = " + nextAccumTime);
			
			if(dataPointsCount > dataPointsPerWrite -1){ //write accumulated data
				FileUtil.initFile(sizeHistFile, params, "avalanch size histogram");
				FileUtil.printHistToFile(sizeHistFile, sizeHist);
				printAccumsToFile(iMetFile, invStressMetTempAcc, cgInvStressMetTempAcc, cgInvActivityMetTempAcc, invSizeActMetTempAcc);
				
				printAccumsToFile2(sizeFile, maxSizeTempAcc,cgMaxSizeActBoxTempAcc, timeAveForMaxCGSizeActTempAcc);
			
				//restart counts
				maxSize = 0;
				dataPointsCount = 0;
				
				//clear temp accumulators
				invStressMetTempAcc.clear();
				cgInvStressMetTempAcc.clear();
				cgInvActivityMetTempAcc.clear();
				maxSizeTempAcc.clear();
				invSizeActMetTempAcc.clear();
				cgMaxSizeActBoxTempAcc.clear();
				timeAveForMaxCGSizeActTempAcc.clear();

			}

		
			if(ofc.plateUpdates > maxTime) Job.signalStop();

		}
	}
	
	static void printAccumsToFile(String fileName, Accumulator acc1, Accumulator acc2, Accumulator acc3, Accumulator acc4){
		DatasetBuffer data1 = acc1.copyData();
		DatasetBuffer data2 = acc2.copyData();
		DatasetBuffer data3 = acc3.copyData();
		DatasetBuffer data4 = acc4.copyData();
		int size = data1.size();
		try{
			File file = new File(fileName);
			PrintWriter pw = new PrintWriter(new FileWriter(file, true), true);
			for (int i = 0; i < size; i++){
				pw.println(data1.x(i) + " " + data1.y(i) + " " + data2.x(i) + " " + data2.y(i) + " " + data3.y(i) + " " + data4.y(i)) ;
			}
		} catch (IOException ex){
			ex.printStackTrace();
		}
	}
	
	static void printAccumsToFile2(String fileName, Accumulator acc1, Accumulator acc2, Accumulator acc3){
		DatasetBuffer data1 = acc1.copyData();
		DatasetBuffer data2 = acc2.copyData();
		DatasetBuffer data3 = acc3.copyData();
		int size = data1.size();
		try{
			File file = new File(fileName);
			PrintWriter pw = new PrintWriter(new FileWriter(file, true), true);
			for (int i = 0; i < size; i++){
				pw.println(data1.x(i) + " " + data1.y(i) + " " + data2.y(i) + " " + data3.y(i) ) ;
			}
		} catch (IOException ex){
			ex.printStackTrace();
		}
	}
	
	void initFiles(){
		iMetFile = params.sget("Data Dir") + File.separator + "im.txt";  // to record iverse metric data
		FileUtil.initFile(iMetFile, params, " time (plate updates), stress inverse metric, time/coarse grained time, coarse grained activity inverse metric, coarse grained stress inverse metric, cg size act inverse metric");
		sizeFile = params.sget("Data Dir") + File.separator + "s.txt";
		FileUtil.initFile(sizeFile, params, "time (plate updates),  max avalanche size, time/coarse grained time,");
		sizeHistFile = params.sget("Data Dir") + File.separator + "sh.txt";	//to record size histogram data

	}
}
