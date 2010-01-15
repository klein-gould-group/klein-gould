package chris.queue.Apps;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;

import javax.imageio.ImageIO;

import scikit.graphics.ColorGradient;
import scikit.graphics.ColorPalette;
import scikit.graphics.dim2.Grid;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;
import scikit.jobs.params.DirectoryValue;
import scikit.jobs.params.DoubleValue;
import chris.queue.finiteRouter2D;

public class finiteRouter2Dapp extends Simulation{

	private DecimalFormat fmt = new DecimalFormat("000000");
	Grid grid = new Grid("Buffers");
	private int L, N, Mx;
	private String pth;
	private finiteRouter2D model;
	private final boolean draw = false;

	public static void main(String[] args) {
		new Control(new finiteRouter2Dapp(), "2D Router Network");
	}
	
	public void load(Control c) {
		
		params.add("Data Directory",new DirectoryValue("/Users/cserino/Desktop/"));
		params.add("Data File", "default");
		params.add("M",10);
		params.add("L",64);
		params.add("l",5);
		params.add("\u03BB",new DoubleValue(0.05,0,1));
		params.add("seed",0);
		params.add("messages");
		params.set("messages",0);
		params.add("t");
		params.set("t",0);

		c.frame(grid);
	}

	public void run() {
		
		L     = params.iget("L");
		N     = L*L;
		Mx    = N*params.iget("M");
		model = new finiteRouter2D(params); 
		pth   = params.sget("Data Directory");

		setupcolors(params.iget("M"));
		
//		int tss  = (int)(1e6);
//		int tmax = (int)(1e7);
//		int t    = 0;
//		
//		while(t++ < tss){
//			model.step(false);
//			if(t%1e4 ==0){
//				params.set("t",t-tss);
//				Job.animate();
//			}
//		}
//		t = 0;
////		while(t++ < tmax){
//			while(true){
//			model.step(true);
////			if(t%1e4 ==0){
////				params.set("t",t);
////				Job.animate();
////			}
//			Job.animate();
//		}
		while(model.step(true) < Mx)
			Job.animate();
		
//		params.set("t","Done");
//		Job.signalStop();
//		Job.animate();
		
	}
	
	private void setupcolors(int mx){
		
		ColorGradient cg = new ColorGradient();
		ColorPalette cp  = new ColorPalette();

		for (int jj = 0 ; jj <= mx ; jj++){
			cp.setColor(mx-jj,cg.getColor(jj, 0, mx));
		}
		grid.setColors(cp);
		return;
	}
	
	public void animate() {
		/*
		 * 
		 * FIX IMAGE SIZE
		 * 
		 * 
		 */
		
		
		params.set("t", model.getT());
		params.set("messages",model.getNmsg());
		grid.registerData(L,L,model.getDensity());
		if(draw){
			String SaveAs = pth + File.separator + grid.getTitle()+fmt.format(model.getT())+".png";
			try {
				ImageIO.write(grid.getImage(), "png", new File(SaveAs));
			} catch (IOException e) {
				System.err.println("Error in Writing File" + SaveAs);
			}
		}
		return;

	}

	public void clear() {
		
		grid.clear();
		return;
	}	
}