package net.schwarzbaer.java.test.exponentialgrowth;

import java.util.Vector;

import javax.swing.SwingUtilities;

import net.schwarzbaer.java.test.exponentialgrowth.ExponentialGrowth.ProgressPanel;

class Simulation
{

	private final int time_s;
	private final Vector<TableEntry> data;
	private final ProgressPanel progressPanel;
	private final ExponentialGrowthTableModel tableModel;
	private final Runnable updateAtEnd;
	private Thread thread;
	private boolean stopNow;

	Simulation(int time_s, Vector<TableEntry> data, ProgressPanel progressPanel, ExponentialGrowthTableModel tableModel, Runnable updateAtEnd)
	{
		this.time_s = time_s;
		this.data = data;
		this.progressPanel = progressPanel;
		this.tableModel = tableModel;
		this.updateAtEnd = updateAtEnd;
		thread = null;
		stopNow = false;
		
		progressPanel.configureProgressBar(0,time_s);
	}

	synchronized void start()
	{
		if (thread!=null)
			return;
		
		stopNow = false;
		thread = new Thread(this::doInThread);
		thread.start();
	}

	synchronized void stop()
	{
		if (thread==null)
			return;
		
		stopNow = true;
	}

	private void doInThread()
	{
		//System.out.printf("Thread started%n");
		
		for (int timePoint=1; timePoint<=time_s && !stopNow; timePoint++)
		{
			//System.out.printf("Thread step %d%n", timePoint);
			
			for (int i=data.size()-1; i>0 && !stopNow; i--)
			{
				//System.out.printf("data entry %d%n", i);
				
				TableEntry thisEntry = data.get(i);
				TableEntry nextEntry = data.get(i-1);
				MathUtilities.add(
						nextEntry.currentAmount,
						nextEntry.currentAmountUnit,
						thisEntry.growthRate_per_s,
						thisEntry.growthRateUnit,
						(val, valUnit) -> {
							nextEntry.currentAmount = val;
							nextEntry.currentAmountUnit = valUnit;
						}
				);
				MathUtilities.mul(
						nextEntry.currentAmount,
						nextEntry.currentAmountUnit,
						nextEntry.ratio,
						(val, valUnit) -> {
							nextEntry.growthRate_per_s = val;
							nextEntry.growthRateUnit = valUnit;
						}
				);
			}
			
			final int timePoint_ = timePoint;
			SwingUtilities.invokeLater(()->{
				progressPanel.setProgressValue(timePoint_);
				tableModel.fireTableUpdate();
			});
		}
		
		cleanUp();
		if (updateAtEnd!=null)
			SwingUtilities.invokeLater(updateAtEnd);
		
		//System.out.printf("Thread finished%n");
	}

	private synchronized void cleanUp()
	{
		thread = null;
		stopNow = false;
	}

}
