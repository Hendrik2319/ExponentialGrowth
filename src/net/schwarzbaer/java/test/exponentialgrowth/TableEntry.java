package net.schwarzbaer.java.test.exponentialgrowth;

class TableEntry
{
	final int index;
	double currentAmount;
	ExpFactor currentAmountUnit;
	double growthRate_per_s;
	ExpFactor growthRateUnit;
	double ratio;
	
	TableEntry (int index) {
		this.index = index;
		currentAmount = 0;
		currentAmountUnit = ExpFactor._1;
		growthRate_per_s = 0;
		growthRateUnit = ExpFactor._1;
		updateRatio();
	}

	void updateRatio()
	{
		if (currentAmountUnit==null || growthRateUnit==null || currentAmount==0)
			ratio = Double.NaN;
		else
			ratio = (growthRate_per_s * growthRateUnit.value) / (currentAmount * currentAmountUnit.value);
	}
}