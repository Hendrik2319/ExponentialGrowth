package net.schwarzbaer.java.test.exponentialgrowth;

import java.util.function.Consumer;

enum ExpFactor
{
	_1 ( "-", 1e0  ),
	K  ( "K", 1e3  ),
	M  ( "M", 1e6  ),
	B  ( "B", 1e9  ),
	T  ( "T", 1e12 ),
	AA ("AA", 1e15 ),
	BB ("BB", 1e18 ),
	CC ("CC", 1e21 ),
	DD ("DD", 1e24 ),
	EE ("EE", 1e27 ),
	FF ("FF", 1e30 ),
	GG ("GG", 1e33 ),
	HH ("HH", 1e36 ),
	II ("II", 1e39 ),
	JJ ("JJ", 1e42 ),
	KK ("KK", 1e45 ),
	LL ("LL", 1e48 ),
	MM ("MM", 1e51 ),
	NN ("NN", 1e54 ),
	OO ("OO", 1e57 ),
	PP ("PP", 1e60 ),
	QQ ("QQ", 1e63 ),
	RR ("RR", 1e66 ),
	SS ("SS", 1e69 ),
	TT ("TT", 1e72 ),
	UU ("UU", 1e75 ),
	VV ("VV", 1e78 ),
	WW ("WW", 1e81 ),
	XX ("XX", 1e84 ),
	YY ("YY", 1e87 ),
	ZZ ("ZZ", 1e90 ),
	;
	final String label;
	final double value;

	ExpFactor(String label, double value)
	{
		this.label = label;
		this.value = value;
		
	}

	@Override public String toString() { return label; }

	static void parse(String str, Consumer<ExpFactor> setValue)
	{
		try { setValue.accept( valueOf(str) ); }
		catch (Exception e) {}
	}
}