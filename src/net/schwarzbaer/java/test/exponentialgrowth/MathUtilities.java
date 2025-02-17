package net.schwarzbaer.java.test.exponentialgrowth;

import java.util.Locale;

class MathUtilities
{
	interface SetResultFunction
	{
		void setResult(double val, ExpFactor valUnit);
	}
	
	static void add(double a, ExpFactor aUnit, double b, ExpFactor bUnit, SetResultFunction setResult)
	{
		double sum = a*aUnit.value + b*bUnit.value;
		ReducedValue reducedValue = ReducedValue.reduce(sum);
		setResult.setResult(reducedValue.val, reducedValue.valUnit);
	}

	static void mul(double a, ExpFactor aUnit, double b, SetResultFunction setResult)
	{
		double prod = a*aUnit.value * b;
		ReducedValue reducedValue = ReducedValue.reduce(prod);
		setResult.setResult(reducedValue.val, reducedValue.valUnit);
	}
	
	record ReducedValue(double val, ExpFactor valUnit)
	{
		static ReducedValue reduce(double value)
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
					return new ReducedValue(sign * value / f.value, f);
			}
			
			throw new IllegalStateException();
		}
		
		String toString(String numberFormat)
		{
			return String.format(Locale.ENGLISH, numberFormat+" %s", val, valUnit);
		}
		
		static String toString(double value, String numberFormat)
		{
			return reduce( value ).toString( numberFormat );
		}
	}
}
