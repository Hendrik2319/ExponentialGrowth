package net.schwarzbaer.java.test.exponentialgrowth;

class TableEntry
{
	final int index;
	double    currentAmount;
	ExpFactor currentAmountUnit;
	double    growthRate_per_s;
	ExpFactor growthRateUnit;
	double    ratio;
	
	TableEntry (int index) {
		this.index = index;
		currentAmount = 0;
		currentAmountUnit = ExpFactor._1;
		growthRate_per_s = 0;
		growthRateUnit = ExpFactor._1;
		updateRatio();
	}

	TableEntry(TableEntry other)
	{
		this.index             = other.index            ;
		this.currentAmount     = other.currentAmount    ;
		this.currentAmountUnit = other.currentAmountUnit;
		this.growthRate_per_s  = other.growthRate_per_s ;
		this.growthRateUnit    = other.growthRateUnit   ;
		this.ratio             = other.ratio            ;
	}

	void updateRatio()
	{
		if (currentAmountUnit==null || growthRateUnit==null || currentAmount==0)
			ratio = Double.NaN;
		else
			ratio = (growthRate_per_s * growthRateUnit.value) / (currentAmount * currentAmountUnit.value);
	}
}