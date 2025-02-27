package net.schwarzbaer.java.test.exponentialgrowth;

import java.util.Arrays;
import java.util.function.Function;
import java.util.function.IntFunction;

class CombinedValueParser<UnitType>
{
	private final UnitType[] units;
	private final Function<UnitType, String> toString;

	CombinedValueParser(UnitType[] units, Function<UnitType, String> toString )
	{
		this.units = units;
		this.toString = toString;
	}
	
	static <UnitType> UnitType[] reversed(UnitType[] units, IntFunction<UnitType[]> toArrayFcn)
	{
		return Arrays.asList(units).reversed().toArray(toArrayFcn);
	}

	ParsedInput<UnitType> parseInput(String text)
	{
		text = text.trim();
		
		UnitType unit = null;
		
		for (UnitType unit_ : units)
		{
			String unitStr = toString.apply(unit_);
			if (text.endsWith(unitStr))
			{
				unit = unit_;
				text = text.substring( 0, text.length()-unitStr.length() ).trim();
				break;
			}
		}
		
		text = text.replace(',', '.');
		Double d = null;
		try { d = Double.parseDouble(text); }
		catch (NumberFormatException e) {}
		
		
		if (d!=null && Double.isFinite(d))
			return new ParsedInput<>(d, text, unit, true);
		
		return new ParsedInput<>(0, null, unit, false);
	}

	record ParsedInput<UnitType>(
		double value,
		String valueWithoutUnit,
		UnitType unit,
		boolean isOk
	) {}
	
}
