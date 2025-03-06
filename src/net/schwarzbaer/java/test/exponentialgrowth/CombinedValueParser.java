package net.schwarzbaer.java.test.exponentialgrowth;

import java.util.Arrays;
import java.util.Vector;
import java.util.function.Function;
import java.util.function.IntFunction;

class CombinedValueParser<UnitType>
{
	private final UnitType[] units;
	private final Function<UnitType, String> toString;

	CombinedValueParser(UnitType[] units, Function<UnitType, String> toString, IntFunction<UnitType[]> createArrayFcn )
	{
		this.units = fixOrder( units, toString, createArrayFcn );
		this.toString = toString;
	}
	
	private static <UnitType> UnitType[] fixOrder(UnitType[] units, Function<UnitType, String> toString, IntFunction<UnitType[]> createArrayFcn)
	{
		Vector<UnitType> unitsVec = new Vector<>( Arrays.asList(units) );
		for (int i=0; i<unitsVec.size(); i++)
		{
			UnitType unitI = unitsVec.get(i);
			if (unitI==null) continue;
			
			String unitIStr = toString.apply(unitI);
			if (unitIStr==null || unitIStr.isBlank())
			{
				unitsVec.set(i, null);
				continue;
			}
			
			for (int j=0; j<i; j++)
			{
				UnitType unitJ = unitsVec.get(j);
				if (unitJ==null) continue;
				
				String unitJStr = toString.apply(unitJ);
				if (unitIStr==null || unitIStr.isBlank())
					throw new IllegalStateException();
				
				if (unitIStr.equals(unitJStr))
					throw new IllegalArgumentException("Found 2 units (%s, %s) with same label \"%s\".".formatted(unitI, unitJ, unitIStr));
				
				if (unitIStr.endsWith(unitJStr))
				{
					if (unitJStr.endsWith(unitIStr)) // unexpected state that leads to an endless loop
						throw new IllegalStateException("Found 2 units (%s[%s], %s[%s]), that first \"ends with\" second and vice versa.".formatted(unitI, unitIStr, unitJ, unitJStr));
					
					unitsVec.set(j, null);
					unitsVec.add(unitJ);
				}
			}
		}
		return unitsVec
				.stream()
				.filter( unit -> unit!=null )
				.toArray(createArrayFcn);
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
