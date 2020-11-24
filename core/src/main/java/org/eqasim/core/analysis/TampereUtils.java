package org.eqasim.core.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public class TampereUtils {
	
	Set<Id<Link>> entry_links = new HashSet<Id<Link>>();
	Set<Id<Link>> exit_links = new HashSet<Id<Link>>();
	
	Map<Id<Link>, Map<Id<Link>, Double>> input_demand_entry = new HashMap<Id<Link>, Map<Id<Link>, Double>>();
	Map<Id<Link>, Map<Id<Link>, Double>> input_demand_exit = new HashMap<Id<Link>, Map<Id<Link>, Double>>();
	
	Map<Id<Link>, Double> entry_demand = new HashMap<Id<Link>, Double>();
	Map<Id<Link>, Double> exit_demand = new HashMap<Id<Link>, Double>();
	
	Map<Id<Link>, Double> entry_capacities = new HashMap<Id<Link>, Double>();
	Map<Id<Link>, Double> exit_capacities = new HashMap<Id<Link>, Double>();
	
	Map<Id<Link>, Map<Id<Link>, Double>> C_entry = new HashMap<Id<Link>, Map<Id<Link>, Double>>();
	Map<Id<Link>, Map<Id<Link>, Double>> C_exit = new HashMap<Id<Link>, Map<Id<Link>, Double>>();
	
	Map<Id<Link>, Map<Id<Link>, Double>> Q_entry = new HashMap<Id<Link>, Map<Id<Link>, Double>>();
	Map<Id<Link>, Map<Id<Link>, Double>> Q_exit = new HashMap<Id<Link>, Map<Id<Link>, Double>>();
	
	Map<Id<Link>, ArrayList<Id<Link>>> U = new HashMap<Id<Link>, ArrayList<Id<Link>>>();
	ArrayList<Id<Link>> J = new ArrayList<Id<Link>>();
	
	Map<Id<Link>, Double> alpha = new HashMap<Id<Link>, Double>();
	Map<Id<Link>, Double> R = new HashMap<Id<Link>, Double>();
	
	public TampereUtils(Map<Id<Link>, Map<Id<Link>, Double>> input_demand, Map<Id<Link>, Double> entry_capacities,
			Map<Id<Link>, Double> exit_capacities) {
		this.input_demand_entry = new HashMap<Id<Link>, Map<Id<Link>, Double>>(input_demand);
		this.entry_capacities = entry_capacities;
		this.exit_capacities = exit_capacities;
		
		this.entry_links = this.entry_capacities.keySet();
		this.exit_links = this.exit_capacities.keySet();
		
		// Transpose input demand
		for (Id<Link> out : this.exit_links) {
			Map<Id<Link>, Double> transposed = new HashMap<Id<Link>, Double>();
			for (Id<Link> in : this.entry_links) {
				transposed.put(in, this.input_demand_entry.get(in).get(out));
			}
			this.input_demand_exit.put(out, transposed);
		}
		
		// Sum up to compute entry and exit demand
		for (Id<Link> in : this.entry_links) {
			Map<Id<Link>, Double> demand = this.input_demand_entry.get(in);
			Collection<Double> values = demand.values();
			double sum = values.stream().mapToDouble(Double::doubleValue).sum();
			this.entry_demand.put(in, sum);
		}
		for (Id<Link> out : this.exit_links) {
			Map<Id<Link>, Double> demand = this.input_demand_exit.get(out);
			Collection<Double> values = demand.values();
			double sum = values.stream().mapToDouble(Double::doubleValue).sum();
			this.exit_demand.put(out, sum);
		}
		
		// Compute vector R
		this.R = new HashMap<Id<Link>, Double>(this.exit_capacities);
		
		// Compute matrices C, first by incoming links
		for (Id<Link> idl : this.entry_links) {
			Map<Id<Link>, Double> smallC = new HashMap<Id<Link>, Double>();
			double total_demand = this.entry_demand.get(idl);
			double capacity = this.entry_capacities.get(idl);
			for (Id<Link> out : this.exit_links) {
				double demand = this.input_demand_entry.get(idl).get(out);
				smallC.put(out, demand / total_demand * capacity);
			}
			this.C_entry.put(idl, smallC);
		}
		
		for (Id<Link> idl : this.exit_links) {
			Map<Id<Link>, Double> transposed = new HashMap<Id<Link>, Double>();
			for (Id<Link> in : this.entry_links) {
				transposed.put(in, this.C_entry.get(in).get(idl));
			}
			this.C_exit.put(idl, transposed);
		}
		
		// Create U
		for (Id<Link> out : this.exit_links) {
			ArrayList<Id<Link>> current_u = new ArrayList<Id<Link>>();
			for (Id<Link> in : this.entry_links) {
				if (this.input_demand_entry.get(in).get(out) > 0) {
					current_u.add(in);
				}
			}
			this.U.put(out, current_u);
		}
		
		// Create J
		for (Id<Link> out: this.exit_links) {
			if (this.exit_demand.get(out) > 0) {
				this.J.add(out);
			}
		}
		
		// Compute R
		for (Id<Link> out : this.exit_links ) {
			double localalpha = this.R.get(out) / this.C_exit.get(out).values().stream().mapToDouble(Double::doubleValue).sum();
			this.alpha.put(out, localalpha);
		}
		
		// Initialize the Q
		for (Id<Link> in : this.entry_links) {
			Map<Id<Link>, Double> zeros = new HashMap<Id<Link>, Double>();
			for (Id<Link> out : this.exit_links) {
				zeros.put(out, 0.0);
			}
			this.Q_entry.put(in, zeros);
		}
		
		for (Id<Link> out : this.exit_links) {
			Map<Id<Link>, Double> zeros = new HashMap<Id<Link>, Double>();
			for (Id<Link> in : this.entry_links) {
				zeros.put(in, 0.0);
			}
			this.Q_entry.put(out, zeros);
		}		
	}
	
	public void solve_intersection() {
		int cpt = 0;
		while(this.J.size() > 0) {
			this.solve_step();
			cpt += 1;
			this.printResult();
			if (cpt >= 3) {
				break;
			}
		}
		System.out.println("Number of iterations: " + cpt);
	}
	
	public void printResult() {
		// Just print Q and C? 
		System.out.println("Here you can see the results");
		System.out.println("First check Q");
		for (Id<Link> inlink : this.entry_links) {
			System.out.print(inlink + "      ");
			Map<Id<Link>, Double > qline = this.Q_entry.get(inlink);
			for (Id<Link> outlink : this.exit_links) {
				System.out.print(outlink + ": " + qline.get(outlink) + ", ");
			}
			System.out.println(" ");
		}
		System.out.println("Then check C");
		for (Id<Link> inlink : this.entry_links) {
			System.out.print(inlink + "      ");
			Map<Id<Link>, Double > qline = this.C_entry.get(inlink);
			for (Id<Link> outlink : this.exit_links) {
				System.out.print(outlink + ": " + qline.get(outlink) + ", ");
			}
			System.out.println(" ");
		}
	}
	
	public void solve_step() {
		// 1. Find alpha min and j hat
		double alphamin = Double.POSITIVE_INFINITY;
		Id<Link> jhat = Id.createLinkId("FakeLink");
		for (Id<Link> j : this.alpha.keySet()) {
			double current_alpha = this.alpha.get(j);
			if (current_alpha < alphamin) {
				alphamin = current_alpha;
				jhat = j;
			}
		}
		
		// 2. Update entry capacity
		Map<Id<Link>, Double> sumCentry = new HashMap<Id<Link>, Double>();
		for (Id<Link> inlink : this.entry_links) {
			sumCentry.put(inlink, this.C_entry.get(inlink).values().stream().mapToDouble(Double::doubleValue).sum());
		}
		
		// 3. Define list of links that are impacted by alphamin
		ArrayList<Id<Link>> ujhat = this.U.get(jhat);
		Map<Id<Link>, Double> newcap = new HashMap<Id<Link>, Double>();
		if (ujhat != null) {
			for (Id<Link> link : ujhat) {
				newcap.put(link, alphamin * sumCentry.get(link));
			} 
		}
		// 4. Find links that are demand constrained
		ArrayList<Id<Link>> isDemandConstrained = new ArrayList<Id<Link>>();
		if (ujhat != null) {
			for (Id<Link> link : ujhat) {
				if (newcap.get(link) > this.entry_demand.get(link)) {
					isDemandConstrained.add(link);
				}
			} 
		}
		System.out.println("DMD constrained: " + isDemandConstrained);
		System.out.println("ujhat: " + ujhat);
		
		// 5. First case: some links are demand constrained
		if (isDemandConstrained.size() > 0) {			
			for (Id<Link> idlink : isDemandConstrained) {
				Map<Id<Link>, Double> dmd =  this.input_demand_entry.get(idlink);
				this.Q_entry.put(idlink, dmd);
				Map<Id<Link>, Double> cap = this.C_entry.get(idlink);
				for (Id<Link> out : this.exit_links) {
					this.R.put(out, this.R.get(out) - dmd.get(out));
					cap.put(out, 0.0);
				}
				this.C_entry.put(idlink, cap);
				
				// Remove the link from all U vectors
				for (Id<Link> out : this.exit_links) {
					ArrayList<Id<Link>> us = this.U.get(out);
					if (us.contains(idlink)) {
						us.remove(idlink);
					}
					this.U.put(out, us);
					System.out.println("U[" + out + "]= " + this.U.get(out));
				}
				
				// Update J
				ArrayList<Id<Link>> newJ = new ArrayList<Id<Link>>(this.J);
				for (Id<Link> out : this.J) {
					if (this.U.get(out).size() == 0) {
						newJ.remove(out);
					}
				}
				this.J = new ArrayList<Id<Link>>(newJ);
			} 
		}
		
		
		else  {
			
			if (ujhat != null) {
				ArrayList<Id<Link>> otherujhat = new ArrayList<Id<Link>>(ujhat);
				for (Id<Link> inlink : otherujhat) {
					//Map<Id<Link>, Double> demand = this.input_demand_entry.get(inlink);
					Map<Id<Link>, Double> smallQ = new HashMap<Id<Link>, Double>();
					Map<Id<Link>, Double> cap = this.C_entry.get(inlink);

					for (Id<Link> out : this.exit_links) {
						smallQ.put(out, cap.get(out) * alphamin);
						this.R.put(out, this.R.get(out) - alphamin * cap.get(out));
						cap.put(out, 0.0);
					}

					this.C_entry.put(inlink, cap);
					this.Q_entry.put(inlink, smallQ);

					// Remove the link from all U vectors
					for (Id<Link> out : this.exit_links) {
						ArrayList<Id<Link>> us = this.U.get(out);
						if (us.contains(inlink)) {
							us.remove(inlink);
						}
						this.U.put(out, us);
						System.out.println("U[" + out + "]= " + this.U.get(out));
					}

					// Update J
					ArrayList<Id<Link>> newJ = new ArrayList<Id<Link>>(this.J);
					for (Id<Link> out : this.J) {
						if (this.U.get(out).size() == 0) {
							newJ.remove(out);
						}
					}
					this.J = new ArrayList<Id<Link>>(newJ);
				} 
			}
		}
		
		// Transpose C
		for (Id<Link> idl : this.exit_links) {
			Map<Id<Link>, Double> transposed = new HashMap<Id<Link>, Double>();
			for (Id<Link> in : this.entry_links) {
				transposed.put(in, this.C_entry.get(in).get(idl));
			}
			this.C_exit.put(idl, transposed);
		}
		
		for (Id<Link> out : this.exit_links) {
			if (this.J.contains(out)) {
				this.alpha.put(out, this.R.get(out) / 
					this.C_exit.get(out).values().stream().mapToDouble(Double::doubleValue).sum());
				System.out.println("Alpha[" + out + "]= " + this.alpha.get(out));
			}
			else {
				alpha.remove(out);
			}
		}
		
	}
	
	public static void main(String[] args) {
		Map<Id<Link>, Map<Id<Link>, Double>> inputdmd = new HashMap<Id<Link>, Map<Id<Link>, Double>>();
		
		Id<Link> l1 = Id.createLinkId("1");
		Id<Link> l2 = Id.createLinkId("2");
		Id<Link> l3 = Id.createLinkId("3");
		Id<Link> l4 = Id.createLinkId("4");
		Id<Link> l5 = Id.createLinkId("5");
		Id<Link> l6 = Id.createLinkId("6");
		Id<Link> l7 = Id.createLinkId("7");
		Id<Link> l8 = Id.createLinkId("8");
		
		Map<Id<Link>, Double> input1 = new HashMap<Id<Link>, Double>();
		input1.put(l5, 0.0);
		input1.put(l6, 50.0);
		input1.put(l7, 150.0);
		input1.put(l8, 300.0);
		Map<Id<Link>, Double> input2 = new HashMap<Id<Link>, Double>();
		input2.put(l5, 100.0);
		input2.put(l6, 0.0);
		input2.put(l7, 300.0);
		input2.put(l8, 1600.0);
		Map<Id<Link>, Double> input3 = new HashMap<Id<Link>, Double>();
		input3.put(l5, 100.0);
		input3.put(l6, 100.0);
		input3.put(l7, 0.0);
		input3.put(l8, 600.0);
		Map<Id<Link>, Double> input4 = new HashMap<Id<Link>, Double>();
		input4.put(l5, 100.0);
		input4.put(l6, 800.0);
		input4.put(l7, 800.0);
		input4.put(l8, 0.0);
		
		inputdmd.put(l1, input1);
		inputdmd.put(l2, input2);
		inputdmd.put(l3, input3);
		inputdmd.put(l4, input4);
		
		Map<Id<Link>, Double> incap = new HashMap<Id<Link>, Double>();
		incap.put(l1, 1000.0);
		incap.put(l2, 2000.0);
		incap.put(l3, 1000.0);
		incap.put(l4, 2000.0);
		Map<Id<Link>, Double> outcap = new HashMap<Id<Link>, Double>();
		outcap.put(l5, 1000.0);
		outcap.put(l6, 2000.0);
		outcap.put(l7, 1000.0);
		outcap.put(l8, 2000.0);
		
		TampereUtils tu = new TampereUtils(inputdmd, incap, outcap);
		tu.solve_intersection();
		
	}

}
