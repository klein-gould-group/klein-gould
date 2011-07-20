package kang.ising.BasicStructure;

import java.text.DecimalFormat;

import chris.util.Random;
//import chris.util.PrintUtil;

public class IsingStructure{
	
	//structures and parameters
	public int spin[];
	public int initialcopy[];   //the array of the initial copy of the system
	public int biaslabel[];
	public double dilutionmap[];
	
	
	public int L1, L2, M; //parameters for the lattice                                                                                                                                                                                                                                                                                                                                                                                                        
	public double J;     //interaction constant after normalization
	public double NJ;    //interaction constant before normalization
	public double percent;  //dilution percent
	public int deadsites;  //the number of diluted sites in the system
	
	public double totalintenergy;
	public int totalspin;
	public double magnetization;
	
	public int R;   //interaction range R=0 is NN interaction
	
	public int biasA;
	public int biasB;
	public double biaspercent;
	
	//the function for this class IsingStructure
	
	public IsingStructure(int L1, int L2, int R, double NJ, double percent, double biaspercent)     //generating function
	{
		this.L1=L1;
		this.L2=L2;
		this.M=L1*L2;
		this.R=R;
		this.percent=percent;
		this.biaspercent=biaspercent;

		this.NJ=NJ;
		if(R==0)
			this.J=NJ/4;
		if(R>0)
			this.J=NJ/((2*R+1)*(2*R+1)-1);
		this.spin=new int [M];
		this.initialcopy=new int [M];
		this.biaslabel=new int [M];
		this.dilutionmap=new double[M];
		
	}
	
	public IsingStructure clone()
	{
		IsingStructure copy= new IsingStructure(L1, L2, R, NJ, percent, biaspercent);
		for(int t=0;t<M; t++)
		{
			copy.spin[t]=spin[t];
			copy.initialcopy[t]=initialcopy[t];
			copy.biaslabel[t]=biaslabel[t];
			copy.dilutionmap[t]=dilutionmap[t];
		}
		copy.biasA=biasA;
		copy.biasB=biasB;
		copy.totalspin=totalspin;
		copy.totalintenergy=totalintenergy;
		
		
		return copy;
	}
	
	public void Dinitialization(int Dseed, int Bseed, int A, int B )//dilution initialization
	{
		Random Drand= new Random(Dseed);
		Random Brand= new Random(Bseed);
		biasA=A;
		biasB=B;
		deadsites=0;

		
		for (int t=0;t<M;t++)
			{
			spin[t]=1;
			biaslabel[t]=0;
			dilutionmap[t]=0;
			}
			
		
		int cx, cy; //x,y index for the center
		cx=(int)L1/2;
		cy=(int)L2/2;
		if(biaspercent!=percent)
		    rectangle(biaslabel, A,B, cx,cy);
		
		for(int j=0; j<M; j++)
		{
			if (biaslabel[j]==1)
				if(Brand.nextDouble()<biaspercent)
					{
					spin[j]=0;
					deadsites++;
					}
			if (biaslabel[j]==0)
				if(Drand.nextDouble()<percent)
					{
					spin[j]=0;
					deadsites++;
					}
		}
		
		/*or(int k=0; k<M; k++)
		{
			dilutionmap[k]=dilutionratio(R,k);
		} // draw the dilution map
	*/
	}
	
	public void dilutionmap(int range)
	{
		for(int k=0; k<M; k++)
		{
			dilutionmap[k]=dilutionratio(R,k);
		}
	}
	
	public void Sinitialization(int type, int Sseed)//spin config initialization (type: 0-random 1-spin up   2-spin down)
	{
		Random spinrand= new Random(Sseed);
		totalintenergy=0;
		totalspin=0;
		
		
		if(type==0)
			for (int i=0; i<M; i++)
		    {
			   if(spin[i]!=0)
			   {
				   spin[i]=-1;
			   if (spinrand.nextDouble()> 0.5)
				   {
				   spin[i]=1;

				   }
			   
			   }
				
		    }
		if(type==1)
			for (int i=0; i<M; i++)
		    {
			   if(spin[i]!=0)
				   {spin[i]=1;

				   }
		    }
		
		if(type==2)
			for (int i=0; i<M; i++)
		    {
			   if(spin[i]!=0)
				   {
				   spin[i]=-1;

				   }
		    }
		
		for (int i=0; i<M; i++)
		{

			initialcopy[i]=spin[i];  // here, make the copy of the system
			totalspin+=spin[i];      // calculate the total spin at the beginning
		}
		
		totalintenergy= TotalIntEnergy();
		magnetization=Magnetization();
		
	}
	
	//basic functions
	
	
	
	public double dilutionratio(int r, int i)
	{
		double ratio;
		double dilutedsite;
		dilutedsite=0;
		double totalinrange;
		totalinrange=0;
		int j;
		int disij;

		for(j=0; j<M;j++)
		{
            disij= distance(i,j);

			if(disij<=r*r)
			{
				totalinrange++;
				if(spin[j]==0)
					dilutedsite++;
			}
		}
	
		ratio=dilutedsite/totalinrange;
		return ratio;
	}

	public int X(int bx)
	{
		int realx=bx;
		if (bx>=L1)
			realx=bx-L1;
		if (bx<0)
			realx=bx+L1;
		return realx;
	}
	
	public int Y(int by)
	{
		int realy=by;
		if (by>=L2)
			realy=by-L2;
		if (by<0)
			realy=by+L2;
		return realy;
	}
	
	public int distance (int a, int b)     // the code to calculate the distance between two points on the lattice
	{
		int dis=0;
		int ax, ay, bx, by;
		int dx2, dy2;
		ax= a/L2;
		ay= a%L2;
		bx= b/L2;
		by= b%L2;
		
		dx2=(ax-bx)*(ax-bx);
		dy2=(ay-by)*(ay-by);
		if((ax-bx+L1)*(ax-bx+L1)<(ax-bx)*(ax-bx))
			dx2=(ax-bx+L1)*(ax-bx+L1);
		if((ax-bx-L1)*(ax-bx-L1)<(ax-bx)*(ax-bx))
			dx2=(ax-bx-L1)*(ax-bx-L1);
		if((ay-by+L2)*(ay-by+L2)<(ay-by)*(ay-by))
			dy2=(ay-by+L2)*(ay-by+L2);
		if((ay-by-L2)*(ay-by-L2)<(ay-by)*(ay-by))
			dy2=(ay-by-L2)*(ay-by-L2);

		dis=dx2+dy2;
		return dis;
	}

	public int CenterOfMass()              // return the index in the array corresponding to the center of mass
	{
		int center=0;
		
		int totalM=TotalSpin();	
		int minorityspin=0;
		// now determine which direction is the minority direction
		if(totalM>0)
			minorityspin=-1;
		if(totalM<0)
			minorityspin=1;
		// now record all the sites with minority direction
		int map[]= new int[M];
		int CGmap[]= new int[M];    // the coarse grained map[]
		int totalMinorityspins=0;
		
		for(int mi=0; mi<M; mi++)
		{
			if(spin[mi]==minorityspin)
				{
				     map[mi]=1;
				     totalMinorityspins++;     //find out how many minority spins are there in the whole lattice and then determine the range of CG
				}
			else
				map[mi]=0;
		}
		
		int CGR= (int)(Math.sqrt(totalMinorityspins)/2);   //estimate the best range for CG
		int MAX=0;
		int maxi=0;
		for(int ci=0; ci<M; ci++)
		{
			CGmap[ci]=SumInRange(map,ci,CGR);
			if(CGmap[ci]>MAX)
				{
				     maxi=ci;
				     MAX=CGmap[ci];
				}
		}
		center=maxi;
		
		return center;
		
	}
	
	public int SumInRange(int spin[], int j, int R)
	{
		int S=0;
		int nx=j/L2;
		int ny=j%L2;
		int kx, ky;

		for (int m=-R; m<=R; m++)
			for (int n=-R; n<=R; n++)
			{
				kx=X(nx+m);
				ky=Y(ny+n);
				S+=spin[kx*L2+ky];	
			}
		return S;
	}
	
	public void rectangle(int label[], int a, int b, int cx, int cy)  //draw a rectangle of 2a*2b at (cx,cy)
	{
		int bx, by;
		int x,y;
		
		for(bx=cx-a; bx<cx+a; bx++)
			for(by=cy-b; by<cy+b; by++)
			{
				x=X(bx);
				y=Y(by);
				label[x*L2+y]=1;
			}
	}
	
 	public int Nneighber(int a,int i )// function for the index of nearest neighbor
 	{
		int nx,ny; //index for neighbor
		int ni=0;
		nx=(int)i/L2;
		ny=(int)i%L2;
		
		if (a==0) {
			ni=nx*L2+ny-1;
			if  (ny==0) {
				ni=nx*L2+ny+L2-1;
		}
			
		}//(x,y-1) up
		
     	if (a==1){
			ni=(nx+1)*L2+ny;
			if  (nx==L1-1) {
				ni=(nx-L1+1)*L2+ny;
			}
			
		}//(x+1,y) right
		
		if (a==2){
			ni=nx*L2+ny+1;
			if  (ny==L2-1) {
				ni=nx*L2+ny-L2+1;
			}
			
		}//(x,y+1) down
		
		if (a==3){
			ni=(nx-1)*L2+ny;
			if  (nx==0) {
				ni=(nx+L1-1)*L2+ny;
			}
		}//(x-1,y) left
		
		return ni;
		
	}
	
 	public double interactionEchange (int j)//function for interaction energy
 	{ 
		double Energy=0;
		double Energychange=0;
		if (R==0)
		{
			int b,k;
		    for(b=0; b<4;b++)
		    {
			k=Nneighber(b,j);
			Energy=Energy+J*spin[j]*spin[k];
		    }
		    Energychange=-2*Energy;
		    
		}
		if (R!=0)
		{
			int S=SumInRange(spin, j, R);
			
			Energy=J*spin[j]*S-J;
			Energychange=-2*Energy;
			
		}
		
		return Energychange;	
    }

 	public void MetropolisSpinflip(int j, Random flip, double temperature, double field)
	{
		
 		if(spin[j]!=0)
		{
	 		double ZeemanE=2*field*spin[j]; //the change in Zeeman's energy if we flip the spin[j]
			double IntEchange=interactionEchange(j);
	 		double Echange=ZeemanE+IntEchange;
			int tempspin= spin[j];
	 		
			if(Echange<0)
			{
				spin[j]=-spin[j];
				totalspin-=tempspin*2;
				magnetization=Magnetization();
				//totalintenergy+=IntEchange;
				
			}
			
			else
			{
				if(flip.nextDouble()<=Math.exp(-Echange/temperature))
						{
					            spin[j]=-spin[j];
								totalspin-=tempspin*2;
								magnetization=Magnetization();
								//totalintenergy+=IntEchange;	
						}
			}
		}

		
	}
 	
	public void MCS(double T, double H, Random flip, double ratio)
	{
	    int j=0;
	    
	    int rationalMCS= (int) (ratio*M);
	    for (int f=0; f< rationalMCS; f++)
	    {
		   j=(int) (flip.nextDouble()*M);
		   MetropolisSpinflip(j, flip, T, H);
	    }

	}
	
 	
  	public int TotalSpin()
 	{
 		int total=0;
 		for(int k=0;k<M;k++)
 		{
 			total+=spin[k];
 		}
 		return total;
 	}
 	
 	public double SpinIntEnergy(int j)
 	{
 		double SpinE=0;
 		if(spin[j]!=0)
 		{
 			if (R==0)
 			{
 
 				int b,k;
 			    for(b=0; b<4;b++)
 			    {
 				k=Nneighber(b,j);
 				SpinE+=J*spin[j]*spin[k];
 			    } 
 			}
 			
 			if (R!=0)
 			{
 				int S=SumInRange(spin, j, R);

 				SpinE=J*spin[j]*S-J;
 				
 			}
 			
 			
 		}
 		
 		return SpinE;
 	}
 	
 	public double TotalIntEnergy()
 	{
 		double TotalE=0;
 		for(int k=0; k<M;k++)
 			TotalE+=SpinIntEnergy(k);
 		totalintenergy=TotalE;
 		return TotalE;
 	}
 	
  	public double TotalEnergy(double field)
 	{
 	    return TotalIntEnergy()+field*totalspin;	
 	}
  	
  	public double Magnetization()
  	{
  		double m;
  		m=((double)totalspin/M);
  		return m;
  	}
  	
    public double Fluctuation(double data[], int size)
    {
    	double sum=0;
    	for(int j=0; j<size; j++)
    	{
    		sum+=data[j];
    	}
    	double avg= sum/size;
    	double sumD=0;
    	for(int k=0;k<size;k++)
    	{
    		sumD+=(data[k]-avg)*(data[k]-avg);
    	}
    	return sumD/size;
    }
  	
  	
 	
}