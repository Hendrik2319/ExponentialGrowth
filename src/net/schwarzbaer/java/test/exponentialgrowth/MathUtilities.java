package net.schwarzbaer.java.test.exponentialgrowth;

class MathUtilities
{
	interface SetResultFunction
	{
		void setResult(double val, ExpFactor valUnit);
	}
	
	static void add(double a, ExpFactor aUnit, double b, ExpFactor bUnit, SetResultFunction setResult)
	{
		double sum = a*aUnit.value + b*bUnit.value;
		setResult(setResult, sum);
	}

	static void mul(double a, ExpFactor aUnit, double b, SetResultFunction setResult)
	{
		double prod = a*aUnit.value * b;
		setResult(setResult, prod);
	}

	private static void setResult(SetResultFunction setResult, double value)
	{
		int sign = 1;
		if (value<0)
		{
			sign = -1;
			value = -value;
		}
		
		ExpFactor[] expFactors = ExpFactor.values();
		for (int i=0; i<expFactors.length; i++)
		{
			ExpFactor f = expFactors[i];
			ExpFactor fNext = i+1<expFactors.length ? expFactors[i+1] : null;
			if (fNext==null || value / fNext.value < 1)
			{
				setResult.setResult(sign * value / f.value, f);
				break;
			}
		}
	}
}
