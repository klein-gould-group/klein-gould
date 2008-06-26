package rachele.ising.dim2.apps;

import java.awt.Color;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import rachele.ising.dim2.*;
import scikit.dataset.Accumulator;
import scikit.dataset.Function;
import scikit.graphics.dim2.Grid;
import scikit.graphics.dim2.Plot;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;
import scikit.jobs.params.ChoiceValue;
import static scikit.util.Utilities.format;
import rachele.util.*;
import static java.lang.Math.*;

public class MonteCarloDataApp extends Simulation{

	//Choose function of app
	//String averages = "S_k_DO";//take s(k) averages for disorder to order case
	//String averages = "S_t_DO";//take s(t) averages for disorder to order case
	//String averages = "S_k_SC";//take s(k) averages for stripe to clump case
	String averages = "S_t_SC";//take s(k) averages for stripe to clump case
	//String averages = "S_t_SC1D";//take s(t) averages for stripe to clump case starting with 1D stripe solution
	//String averages = "None";
	
	Grid grid = new Grid("Long Range Ising Model");
	Grid sfGrid = new Grid("SF");
	Plot sfkPlot = new Plot("sf_k plot");
	Plot sftPlot = new Plot("sf_t plot");
	//if(averages == "S_t_SC1D") Plot plot1DIsing = new Plot ("1D Ising Model");
	int dx;
	IsingLR sim;
	public FourierTransformer fft;
	double [] sFactor;
	Accumulator sf_kAcc;
	Accumulator sf_tAveAcc0,sf_tAcc0;
	Accumulator sf_tAveAcc1,sf_tAcc1;
	Accumulator sf_tAveAcc2,sf_tAcc2;
	Accumulator sf_tAveAcc3,sf_tAcc3;
	Accumulator sf_tAveAcc4,sf_tAcc4;
	Accumulator sf_tAveAcc5,sf_tAcc5;
	Accumulator sf_kTheoryAcc, sf_kTheory2Acc, sf_tTheoryAcc, sf_tTheory2Acc;
    boolean clearFile;
   	double [] sfTimeArray;
	
	public static void main(String[] args) {
		new Control(new MonteCarloDataApp(), "Monte Carlo");
	}
	
	public void load(Control c) {
		
		//c.frameTogether("grids", grid, sfGrid);		
		c.frame(grid);
		if (averages == "S_k_DO" || averages == "S_k_SC"){
			c.frame(sfkPlot);
			sf_kAcc = new Accumulator();
		}else if(averages == "S_t_DO" || averages == "S_t_SC" || averages == "S_t_SC1D"){
			c.frame(sftPlot);
		}
		//if(averages == "S_t_SC1D") c.frame(plot1DIsing);
		params.addm("Dynamics", new ChoiceValue("Ising Glauber","Kawasaki Glauber", "Kawasaki Metropolis",  "Ising Metropolis"));
		params.addm("init", new ChoiceValue( "Random", "Read From File"));
		params.add("Random seed", 0);
		params.add("L", 1<<7);
		//params.add("N", 1<<8);
		params.add("R", 50);//1<<6);
		params.add("Initial magnetization", 0.0);
		params.addm("T", 0.04);
		params.addm("J", -1.0);
		params.addm("h", 0.0);
		params.addm("dt", 1/(double)(1<<6));
		params.add("time");
		params.add("magnetization");
		params.add("Lp");
		params.add("Reps");
		flags.add("Write Config");
	}	
	
	public void animate() {
		params.set("time", format(sim.time()));
		params.set("magnetization", format(sim.magnetization()));
		sim.setParameters(params);
		params.set("Lp", sim.L/dx);
		grid.registerData(sim.L/dx, sim.L/dx, sim.getField(dx));
		sfGrid.registerData(sim.L/dx, sim.L/dx, sFactor);
		if (averages == "S_k_DO"){
			sfkPlot.registerLines("sf(k)", sf_kAcc, Color.BLACK);
			sfkPlot.registerLines("theory", sf_kTheoryAcc, Color.BLUE);
			sfkPlot.registerLines("Structure theory", new Function() {
				public double eval(double kR) {
					double pot = (kR == 0) ? 1 : Math.sin(kR)/kR; 
					double D = -pot/sim.T-1;
					return  (exp(2*sim.time()*D)*(1 + 1/D)-1/D);	
				}
			}, Color.RED);
		}else if(averages == "S_t_DO"){
			sftPlot.registerLines("st(k)", sf_tAveAcc0, Color.BLACK);
			sftPlot.registerLines("theory", sf_tTheoryAcc, Color.BLUE);			
		}else if(averages == "S_t_SC"){
			sftPlot.registerLines("st(k) Ave0", sf_tAveAcc0, Color.BLACK);
			sftPlot.registerLines("st(k)0", sf_tAcc0, Color.BLACK);
			
			sftPlot.registerLines("st(k) Ave1", sf_tAveAcc1, Color.LIGHT_GRAY);
			sftPlot.registerLines("st(k)1", sf_tAcc1, Color.LIGHT_GRAY);
			
			sftPlot.registerLines("st(k) Ave2", sf_tAveAcc2, Color.BLUE);
			sftPlot.registerLines("st(k)2", sf_tAcc2, Color.BLUE);
			
			sftPlot.registerLines("st(k) Ave3", sf_tAveAcc3, Color.CYAN);
			sftPlot.registerLines("st(k)3", sf_tAcc3, Color.CYAN);
			
			sftPlot.registerLines("st(k) Ave4", sf_tAveAcc4, Color.GREEN);
			sftPlot.registerLines("st(k)4", sf_tAcc4, Color.GREEN);
			
			sftPlot.registerLines("st(k) Ave5", sf_tAveAcc5, Color.RED);
			sftPlot.registerLines("st(k)5", sf_tAcc5, Color.RED);
			
		}else if (averages == "S_t_SC1D"){
			sftPlot.registerLines("st(k) Ave", sf_tAveAcc0, Color.BLACK);
			sftPlot.registerLines("st(k)", sf_tAcc0, Color.RED);
			//plot1DIsing.registerLines(name, data, color);
		}
		if(flags.contains("Write Config")) writeConfigToFile();
		flags.clear();
	}
	
	public void clear() {
	}
	
	public void run() {
		initialize();
		fft = new FourierTransformer((int)(sim.L/dx));
		sFactor = new double [sim.L/dx*sim.L/dx];
		double step = 0.10;
		if(averages == "S_t_DO"){
			double maxTime = 0.10;
			int sfLabel = sf_t_theory(maxTime);
			sfTimeArray = new double[(int)((maxTime/step)+1)];
			int repNo = 0;
			while (true) {
				if(params.sget("init") == "Read From File") readInitialConfiguration();
				else sim.randomizeField(params.fget("Initial magnetization"));
				sim.restartClock();
				int recordInt = 0;
				int recordStep = 0;
				while (sim.time() < maxTime){
					sim.step();
					Job.animate();
					if (sim.time() > recordStep){
						sFactor = fft.calculate2DSF(sim.getField(dx), false, false);
						//sfTimeArray[recordInt] += sFactor[sfLabel];
						sf_tAveAcc0.accum(sim.time(),sFactor[sfLabel]);
						recordStep += step;
						recordInt +=1;
					}
				}	
				repNo += 1;
				params.set("Reps", repNo);
				writeArray(repNo, step);
			}
		}else if (averages == "S_k_DO"){
			double maxTime = 0.2;
			double kRmax= 25;
			sf_k_theory(sim.dTime(), kRmax);
			System.out.println("take data time = " + maxTime);
			int repNo = 0;
			sfTimeArray = new double[sim.L/dx];			
			while (true) {
				if(params.sget("init") == "Read From File") readInitialConfiguration();
				else sim.randomizeField(params.fget("Initial magnetization"));
				sim.restartClock();
				//while (sim.time() < maxTime){
					sim.step();
					Job.animate();
				//}
				sFactor = fft.calculate2DSF(sim.getField(dx), false, false);
				
				for (int i = 1; i < sim.L/(2*dx); i++ ){
					System.out.println("print time = " + sim.time());
					double kRValue = 2*Math.PI*sim.R*(double)i/sim.L;
					if (kRValue < kRmax) sf_kAcc.accum(kRValue, sFactor[i]);
				}
				repNo += 1;
				params.set("Reps", repNo);
			}
		}else if(averages == "S_t_SC"){

			double maxTime = 15.0;//max time after quench time
			double initTime = 15.0;
			double initH = 0;
			double quenchH = 0.9;
			int repNo = 0;
			sf_tAcc0 = new Accumulator(sim.dTime());
			sf_tAcc1 = new Accumulator(sim.dTime());
			sf_tAcc2 = new Accumulator(sim.dTime());
			sf_tAcc3 = new Accumulator(sim.dTime());
			sf_tAcc4 = new Accumulator(sim.dTime());
			sf_tAcc5 = new Accumulator(sim.dTime());
			sf_tAveAcc0 = new Accumulator(sim.dTime());
			sf_tAveAcc1 = new Accumulator(sim.dTime());
			sf_tAveAcc2 = new Accumulator(sim.dTime());
			sf_tAveAcc3 = new Accumulator(sim.dTime());
			sf_tAveAcc4 = new Accumulator(sim.dTime());
			sf_tAveAcc5 = new Accumulator(sim.dTime());
			
			int sfLabel0, sfLabel1, sfLabel2, sfLabel3, sfLabel4, sfLabel5;
			int sfLabelHor = findBestkR();
			int sfLabelVert = sfLabelHor*sim.L/dx;
			StringBuffer sb = new StringBuffer();
			sb.append("# sf label = ");	sb.append(sfLabelHor); sb.append(" kR values = ");
			double krvalue = 2*sim.R*Math.PI*sfLabelHor/sim.L;
			sb.append(krvalue);	sb.append(", ");
			krvalue = 2*sim.R*Math.PI*(sfLabelHor+1)/sim.L;
			sb.append(krvalue);	sb.append(", ");
			krvalue = 2*sim.R*Math.PI*(sfLabelHor+2)/sim.L;
			sb.append(krvalue);	sb.append(", ");
			krvalue = 2*sim.R*Math.PI*(sfLabelHor+3)/sim.L;
			sb.append(krvalue);	sb.append(", ");
			krvalue = 2*sim.R*Math.PI*(sfLabelHor+4)/sim.L;
			sb.append(krvalue);	sb.append(", ");
			krvalue = 2*sim.R*Math.PI*(sfLabelHor+5)/sim.L;
			sb.append(krvalue);
			

			sfTimeArray = new double[(int)((maxTime/step)+1)];

			while (true) {
				sf_tAcc0.clear();
				sf_tAcc1.clear();
				sf_tAcc2.clear();
				sf_tAcc3.clear();
				sf_tAcc4.clear();
				sf_tAcc5.clear();
				sim.randomizeField(0.0);
				params.set("h", initH);
				sim.restartClock();
				while(sim.time() < initTime){ 
					sim.step();
					Job.animate();
					//System.out.println("init time = " + sim.time() + " of " + initTime);
				}
				
				sFactor = fft.calculate2DSF(sim.getField(dx), false, false);
				if(sFactor[sfLabelHor]>sFactor[sfLabelVert]){
					sfLabel0 = sfLabelVert;
					sfLabel1 = sfLabelVert + 1;
					sfLabel2 = sfLabelVert + 2;
					sfLabel3 = sfLabelVert + 3;
					sfLabel4 = sfLabelVert + 4;
					sfLabel5 = sfLabelVert + 5;
				}else{
					sfLabel0 = sfLabelHor;
					sfLabel1 = sfLabelHor + 1*sim.L/dx;
					sfLabel2 = sfLabelHor + 2*sim.L/dx;
					sfLabel3 = sfLabelHor + 3*sim.L/dx;
					sfLabel4 = sfLabelHor + 4*sim.L/dx;
					sfLabel5 = sfLabelHor + 5*sim.L/dx;
				}

				sim.restartClock();
				params.set("h", quenchH);
				int recordInt = 0;
				int recordStep = 0;
				step = 0.25;
				while (sim.time() < maxTime){
					sim.step();
					Job.animate();
					if (sim.time() > recordStep){
						sFactor = fft.calculate2DSF(sim.getField(dx), false, false);
						sf_tAveAcc0.accum(sim.time(),sFactor[sfLabel0]);
						sf_tAveAcc1.accum(sim.time(),sFactor[sfLabel1]);
						sf_tAveAcc2.accum(sim.time(),sFactor[sfLabel2]);
						sf_tAveAcc3.accum(sim.time(),sFactor[sfLabel3]);
						sf_tAveAcc4.accum(sim.time(),sFactor[sfLabel4]);
						sf_tAveAcc5.accum(sim.time(),sFactor[sfLabel5]);
						sf_tAcc0.accum(sim.time(),sFactor[sfLabel0]);
						sf_tAcc1.accum(sim.time(),sFactor[sfLabel1]);
						sf_tAcc2.accum(sim.time(),sFactor[sfLabel2]);
						sf_tAcc3.accum(sim.time(),sFactor[sfLabel3]);
						sf_tAcc4.accum(sim.time(),sFactor[sfLabel4]);
						sf_tAcc5.accum(sim.time(),sFactor[sfLabel5]);
						recordStep += step;
						recordInt +=1;
					}
				}	
				repNo += 1;
				params.set("Reps", repNo);
				writeStSCtoFile(sfLabelHor, initTime);
			}
		}else{
			int recordStep = 0;
			while (true) {
				sim.step();
				if (sim.time() > recordStep){
					sFactor = fft.calculate2DSF(sim.getField(dx), false, false);
					recordSfDataToFile(sFactor);
					recordStep += step;
				}
				Job.animate();
			}
		}
	}

	public void initFile(String fileName, String message1, String message2){
		FileUtil.printlnToFile(fileName, message1);
		FileUtil.printlnToFile(fileName, message2);
		FileUtil.printlnToFile(fileName, "# Parameters follow:");
		FileUtil.printlnToFile(fileName, "# Dynamics = ", params.sget("Dynamics"));
		FileUtil.printlnToFile(fileName, "# init = ", params.sget("init"));
		FileUtil.printlnToFile(fileName, "# Random Seed = ", params.sget("Random seed"));
		FileUtil.printlnToFile(fileName, "# L = ", params.sget("L"));
		FileUtil.printlnToFile(fileName, "# R = ", params.sget("R"));
		FileUtil.printlnToFile(fileName, "# init mag = ", params.sget("Initial magnetization"));
		FileUtil.printlnToFile(fileName, "# temperature ", params.sget("T"));
		FileUtil.printlnToFile(fileName, "# Random Seed = ", params.sget("Random seed"));
		FileUtil.printlnToFile(fileName, "# J = ", params.sget("J"));
		FileUtil.printlnToFile(fileName, "# h = ", params.sget("h"));
		FileUtil.printlnToFile(fileName, "# dt = ", params.sget("dt"));
	}
	
	public int sf_t_theory(double maxTime){
		sf_tAveAcc0 = new Accumulator(sim.dTime());
		sf_tTheoryAcc = new Accumulator(sim.dTime());
		sf_tTheory2Acc = new Accumulator(sim.dTime());
		int kRint = findBestkR();
		double kRValue = 2*PI*kRint*sim.R/sim.L;
		while (sim.time() < maxTime){
			sim.step();
			//Job.animate();
			System.out.println("theory time = " + sim.time() + " maxtime = " + maxTime + " kr value = " + kRValue);
			double sf = theoryPoint(kRValue, sim.time());
			sf_tTheoryAcc.accum(sim.time(), sf);
		}
		return kRint;
	}
	
	public int findBestkR(){
		int kRint = (int)(IsingLR.kRpeak*sim.L/(2*Math.PI*sim.R));
		double trialkR1 = 2*PI*kRint*sim.R/sim.L;
		double trialkR2 = 2*PI*(kRint+1)*sim.R/sim.L;
		if (abs(IsingLR.kRpeak-trialkR1) > abs(IsingLR.kRpeak-trialkR2)){
			kRint += 1;
		}		
		return kRint;
	}
	
	public void sf_k_theory(double time, double kRmax){
		sf_kAcc.clear();
		System.out.println("time = " + time);
		double kRbinWidth = 0.2;
		sf_kAcc = new Accumulator(kRbinWidth);
		sf_kTheoryAcc = new Accumulator(kRbinWidth);
		sf_kTheory2Acc = new Accumulator(kRbinWidth);

		for(int i = 0; i < sim.L/(2*dx); i++){
			double kR=2*Math.PI*(double)i*sim.R/sim.L;
			if (kR < kRmax){
				double sf = theoryPoint(kR,time);
				sf_kTheoryAcc.accum(kR,sf);
			}
		}
	}
	
	private void writeStSCtoFile(int sfInt, double initializeTime){

		StringBuffer messageBuffer = new StringBuffer();
		messageBuffer.append("# sf label = ");	messageBuffer.append(sfInt); messageBuffer.append(" kR values = ");
		double krvalue = 2*sim.R*Math.PI*sfInt/sim.L;
		messageBuffer.append(krvalue);	messageBuffer.append(", ");
		krvalue = 2*sim.R*Math.PI*(sfInt+1)/sim.L;
		messageBuffer.append(krvalue);	messageBuffer.append(", ");
		krvalue = 2*sim.R*Math.PI*(sfInt+2)/sim.L;
		messageBuffer.append(krvalue);	messageBuffer.append(", ");
		krvalue = 2*sim.R*Math.PI*(sfInt+3)/sim.L;
		messageBuffer.append(krvalue);	messageBuffer.append(", ");
		krvalue = 2*sim.R*Math.PI*(sfInt+4)/sim.L;
		messageBuffer.append(krvalue);	messageBuffer.append(", ");
		krvalue = 2*sim.R*Math.PI*(sfInt+5)/sim.L;
		messageBuffer.append(krvalue);messageBuffer.append(", init time =");
		messageBuffer.append(initializeTime);
		String message1 = "#Glauber Monte Carlo run: S vs t for several values of k. Stripe to clump H quench. Init H = 0.";
		String message2 = messageBuffer.toString();
		String fileName = "../../../research/javaData/stripeToClumpInvestigation/monteCarloData/squareResults/svtRunQuenchH2/s";
		FileUtil.deleteFile(fileName);		
		initFile(fileName, message1, message2);
		FileUtil.printAccumToFile(fileName, sf_tAveAcc0);
		
		StringBuffer fileBuffer = new StringBuffer(); fileBuffer.append(fileName);fileBuffer.append(1);
		String file1 = fileBuffer.toString();
		FileUtil.deleteFile(file1);	initFile(file1, message1, message2);		
		FileUtil.printAccumToFile(file1, sf_tAveAcc1);
		
		fileBuffer.deleteCharAt(fileBuffer.length()-1);	fileBuffer.append(2);String file2 = fileBuffer.toString();
		FileUtil.deleteFile(file2);	initFile(file2, message1, message2);		
		FileUtil.printAccumToFile(file2, sf_tAveAcc2);

		fileBuffer.deleteCharAt(fileBuffer.length()-1);	fileBuffer.append(3);String file3 = fileBuffer.toString();
		FileUtil.deleteFile(file3);	initFile(file3, message1, message2);		
		FileUtil.printAccumToFile(file3, sf_tAveAcc3);
		
		fileBuffer.deleteCharAt(fileBuffer.length()-1);	fileBuffer.append(4);String file4 = fileBuffer.toString();
		FileUtil.deleteFile(file4);	initFile(file4, message1, message2);		
		FileUtil.printAccumToFile(file4, sf_tAveAcc4);
		
		fileBuffer.deleteCharAt(fileBuffer.length()-1);	fileBuffer.append(5);String file5 = fileBuffer.toString();
		FileUtil.deleteFile(file5);	initFile(file5, message1, message2);		
		FileUtil.printAccumToFile(file5, sf_tAveAcc5);
		

		
	}
	
	public double theoryPoint(double kR, double time){
		double pot = (kR == 0) ? 1 : Math.sin(kR)/kR; 
		double D = -pot/sim.T-1;
		//double V = sim.L*sim.L/(dx*dx);
		//System.out.println("D= "+D);
		double sf = (exp(2*time*D)*(1 + 1/D)-1/D);		
		return sf;
	}
	
	public void writeArray(int repNo, double stepSize){

		String fileName = "../../../research/javaData/stripeToClumpInvestigation/monteCarloData/sf";
		FileUtil.deleteFile(fileName);
		int length = sfTimeArray.length;
		for (int i = 0; i < length; i++){
			double value = sfTimeArray[i]/repNo;
			double time = stepSize*(double)i;
			FileUtil.printlnToFile(fileName, time, value);
		}
	}
	
	public void initialize(){
		sim = new IsingLR(params);
		//sim.setField(params.fget("Initial magnetization"));
		sim.randomizeField(params.fget("Initial magnetization"));		
		dx = 1;
		if(params.sget("init") == "Read From File") readInitialConfiguration();
		clearFile = true;
	}
	
	public void writeConfigToFile(){
		String configFileName = "../../../research/javaData/stripeToClumpInvestigation/monteCarloData/configs/config";
		FileUtil.deleteFile(configFileName);
		FileUtil.writeConfigToFile(configFileName, (sim.L/dx)*(sim.L/dx), sim.getField(dx));
	}
	
	public void readInitialConfiguration(){
		try{
			File myFile = new File("../../../research/javaData/stripeToClumpInvestigation/monteCarloData/configs/config");
			DataInputStream dis = new DataInputStream(new FileInputStream(myFile));
			int spaceIndex;
			double phiValue;
			int Lp = sim.L/dx;
			try{
				while(true){
					spaceIndex =dis.readInt();
					dis.readChar();       // throws out the tab
					phiValue = dis.readDouble();
					dis.readChar();
					sim.spins.set(spaceIndex%Lp,spaceIndex/Lp,(int)phiValue);
//					[spaceIndex] = phiValue;
				}
			} catch (EOFException e) {
			}

		} catch (FileNotFoundException e) {
			System.err.println("FileStreamsTest: " + e);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public void recordSfDataToFile(double [] data){
		String file0 = "../../../research/javaData/stripeToClumpInvestigation/monteCarloData/s0";
		String file1 = "../../../research/javaData/stripeToClumpInvestigation/monteCarloData/s1";
		String file2 = "../../../research/javaData/stripeToClumpInvestigation/monteCarloData/s2";
		String file3 = "../../../research/javaData/stripeToClumpInvestigation/monteCarloData/s3";
		String file4 = "../../../research/javaData/stripeToClumpInvestigation/monteCarloData/s4";
		String file5 = "../../../research/javaData/stripeToClumpInvestigation/monteCarloData/s5";
		if (clearFile){
			FileUtil.deleteFile(file0);
			FileUtil.deleteFile(file1);
			FileUtil.deleteFile(file2);
			FileUtil.deleteFile(file3);
			FileUtil.deleteFile(file4);
			FileUtil.deleteFile(file5);
			clearFile = false;
		}
		int ky = 2;
		int Lp = sim.L/dx;
		FileUtil.printlnToFile(file0, sim.time(), data[ky*Lp]*data[ky*Lp]);
		FileUtil.printlnToFile(file1, sim.time(), data[ky*Lp+1]*data[ky*Lp+1]);
		FileUtil.printlnToFile(file2, sim.time(), data[ky*Lp+2]*data[ky*Lp+2]);
		FileUtil.printlnToFile(file3, sim.time(), data[ky*Lp+3]*data[ky*Lp+3]);
		FileUtil.printlnToFile(file4, sim.time(), data[ky*Lp+4]*data[ky*Lp+4]);
		FileUtil.printlnToFile(file5, sim.time(), data[ky*Lp+5]*data[ky*Lp+5]);
		//System.out.println("data written for time = " + ising.time());
	}	
	
}
