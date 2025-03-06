package net.schwarzbaer.java.test.exponentialgrowth;

import java.util.function.Consumer;

enum ExpFactor implements UnitValueInputDialog.UnitTypeIF
{
	_1 ( "-", "", 1),
	K  ( 1e3  ),
	M  ( 1e6  ),
	B  ( 1e9  ),
	T  ( 1e12 ),
	AA ( 1e15 ),
	BB ( 1e18 ),
	CC ( 1e21 ),
	DD ( 1e24 ),
	EE ( 1e27 ),
	FF ( 1e30 ),
	GG ( 1e33 ),
	HH ( 1e36 ),
	II ( 1e39 ),
	JJ ( 1e42 ),
	KK ( 1e45 ),
	LL ( 1e48 ),
	MM ( 1e51 ),
	NN ( 1e54 ),
	OO ( 1e57 ),
	PP ( 1e60 ),
	QQ ( 1e63 ),
	RR ( 1e66 ),
	SS ( 1e69 ),
	TT ( 1e72 ),
	UU ( 1e75 ),
	VV ( 1e78 ),
	WW ( 1e81 ),
	XX ( 1e84 ),
	YY ( 1e87 ),
	ZZ ( 1e90 ),
	;
	final String label;
	final String unitStr;
	final double value;

	ExpFactor(double value)
	{
		this.label   = name();
		this.unitStr = name();
		this.value   = value;
	}
	ExpFactor(String label, String unitStr, double value)
	{
		this.label = label;
		this.unitStr = unitStr;
		this.value = value;
	}

	@Override public String toString() { return label; }

	static void parse(String str, Consumer<ExpFactor> setValue)
	{
		try { setValue.accept( valueOf(str) ); }
		catch (Exception e) {}
	}
	
	@Override public double getValue  () { return value; }
	@Override public String getUnitStr() { return unitStr; }
}