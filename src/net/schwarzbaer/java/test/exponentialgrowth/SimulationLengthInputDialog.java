package net.schwarzbaer.java.test.exponentialgrowth;

import java.awt.Window;

class SimulationLengthInputDialog extends UnitValueInputDialog<SimulationLengthInputDialog.TimeUnit>
{
	private static final long serialVersionUID = 2216413844171737572L;

	private SimulationLengthInputDialog(Window parent, Integer previousValue)
	{
		super(
				parent, "Enter Simulation Length",
				previousValue==null ? null : previousValue.doubleValue(),
				TimeUnit.values(),
				TimeUnit[]::new
		);
	}
	
	static Integer showDialog(Window parent, Integer previousValue)
	{
		Double result = showDialog(new SimulationLengthInputDialog( parent, previousValue ));
		if (result==null) return null;
		
		return (int) Math.round( result );
	}

	enum TimeUnit implements UnitValueInputDialog.UnitTypeIF
	{
		s  (       1, "s"),
		min(      60, "m"),
		h  (   60*60, "h"),
		day(24*60*60, "d"),
		;
		private final int length_s;
		private final String unitStr;
		private TimeUnit( int length_s, String unitStr )
		{
			this.length_s = length_s;
			this.unitStr = unitStr;
		}
		@Override public double getValue() { return length_s; }
		@Override public String getUnitStr() { return unitStr; }
	}
}
