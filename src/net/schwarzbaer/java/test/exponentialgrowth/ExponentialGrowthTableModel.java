package net.schwarzbaer.java.test.exponentialgrowth;

import java.util.List;
import java.util.Locale;
import java.util.Vector;
import java.util.function.Function;

import javax.swing.SwingConstants;

import net.schwarzbaer.java.lib.gui.Tables;

class ExponentialGrowthTableModel extends Tables.SimpleGetValueTableModel<TableEntry, ExponentialGrowthTableModel.ColumnID>
{
	// Column Widths: [30, 100, 40, 100, 40, 70] in ModelOrder
	enum ColumnID implements Tables.SimpleGetValueTableModel.ColumnIDTypeInt<TableEntry>, SwingConstants
	{
		Index            ( config("#"                , Integer  .class,  30, CENTER).setValFunc( row -> row.index )),
		CurrentAmount    ( config("Current Amount"   , Double   .class, 100, null  ).setValFunc( row -> row.currentAmount ).setToString(d -> String.format(Locale.ENGLISH, "%1.3f", d))),
		CurrentAmountUnit( config("Unit"             , ExpFactor.class,  40, null  ).setValFunc( row -> row.currentAmountUnit )),
		GrowthRate_per_s ( config("Growth Rate (1/s)", Double   .class, 100, null  ).setValFunc( row -> row.growthRate_per_s ).setToString(d -> String.format(Locale.ENGLISH, "%1.3f", d))),
		GrowthRateUnit   ( config("Unit"             , ExpFactor.class,  40, null  ).setValFunc( row -> row.growthRateUnit )),
		Ratio            ( config("Ratio"            , Double   .class,  70, null  ).setValFunc( row -> row.ratio ).setToString(ColumnID::getRatioToString)),
		;
		
		final Tables.SimplifiedColumnConfig2<ExponentialGrowthTableModel, TableEntry, ?> cfg;
		ColumnID(Tables.SimplifiedColumnConfig2<ExponentialGrowthTableModel, TableEntry, ?> cfg) { this.cfg = cfg; }
		@Override public Tables.SimplifiedColumnConfig getColumnConfig() { return this.cfg; }
		@Override public Function<TableEntry, ?> getGetValue() { return cfg.getValue; }
		
		private static <T> Tables.SimplifiedColumnConfig2<ExponentialGrowthTableModel, TableEntry, T> config(String name, Class<T> columnClass, int prefWidth, Integer horizontalAlignment)
		{
			return new Tables.SimplifiedColumnConfig2<>(name, columnClass, 20, -1, prefWidth, prefWidth, horizontalAlignment);
		}
		
		private static String getRatioToString(double ratio)
		{
			double abs = Math.abs(ratio);
			if (abs <     10 ) return String.format(Locale.ENGLISH, "%1.5f", ratio);
			if (abs <    100 ) return String.format(Locale.ENGLISH, "%1.4f", ratio);
			if (abs <   1000 ) return String.format(Locale.ENGLISH, "%1.3f", ratio);
			if (abs <  10000 ) return String.format(Locale.ENGLISH, "%1.2f", ratio);
			if (abs < 100000 ) return String.format(Locale.ENGLISH, "%1.1f", ratio);
			return String.format(Locale.ENGLISH, "%1.3E", ratio);
		}
	}
	
	private Vector<TableEntry> rows;
	private boolean isEditingEnabled;
	private final Runnable notifyChangedData;

	ExponentialGrowthTableModel(Vector<TableEntry> rows, Runnable notifyChangedData)
	{
		super(ColumnID.values(), rows);
		this.rows = rows;
		this.notifyChangedData = notifyChangedData;
		isEditingEnabled = true;
	}

	@Override public void setData(TableEntry[] data) { throw new UnsupportedOperationException(); }
	@Override public void setData(List<TableEntry> data) { throw new UnsupportedOperationException(); }
	@Override public void setData(Tables.DataSource<TableEntry> data) { throw new UnsupportedOperationException(); }

	public void setData(Vector<TableEntry> rows)
	{
		super.setData(this.rows = rows);
	}
	
	void setEditingEnabled(boolean isEditingEnabled)
	{
		this.isEditingEnabled = isEditingEnabled;
	}

	boolean isEditingEnabled()
	{
		return isEditingEnabled;
	}

	@Override
	public int getRowCount()
	{
		return rows.size()+1;
	}
	
	@Override
	public void setDefaultCellEditorsAndRenderers()
	{
		ExponentialGrowthTableCellRenderer cellRenderer = new ExponentialGrowthTableCellRenderer(table, this);
		setDefaultRenderers( clazz -> cellRenderer );
		
		table.setDefaultEditor(ExpFactor.class, new Tables.ComboboxCellEditor<>(ExpFactor.values()));
		
		ValueAndUnitCellEditor valueEditor = new ValueAndUnitCellEditor(this);
		forEachColum( (columnID, tableColumn) -> {
			if (columnID==ColumnID.CurrentAmount || columnID==ColumnID.GrowthRate_per_s)
				tableColumn.setCellEditor(valueEditor);
		} );
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex, ColumnID columnID)
	{
		if (!isEditingEnabled)
			return false;
		
		if (rowIndex > rows.size())
			return false;
		
		if (columnID==null)
			return false;
		
		return switch (columnID)
		{
			case Index, Ratio -> false;
			case CurrentAmount, CurrentAmountUnit -> true;
			case GrowthRateUnit, GrowthRate_per_s -> rowIndex > 0;
		};
	}

	@Override
	protected void setValueAt(Object aValue, int rowIndex, int columnIndex, ColumnID columnID)
	{
		setValueAt(aValue, rowIndex, columnID);
	}

	private void setValueAt(Object aValue, int rowIndex, ColumnID columnID)
	{
		if (columnID == null)
			return;
		
		final TableEntry row;
		boolean rowIsNew = false;
		
		if (rowIndex == rows.size()) {
			rows.add(row = new TableEntry( rows.size() ));
			rowIsNew = true;
		} else
			row = getRow(rowIndex);
		
		if (row == null)
			return;
		
		switch (columnID)
		{
		case Index: break;
		case CurrentAmount     : row.currentAmount     = (Double   )aValue; updateRatio(rowIndex, row); notifyChangedData.run(); break;
		case CurrentAmountUnit : row.currentAmountUnit = (ExpFactor)aValue; updateRatio(rowIndex, row); notifyChangedData.run(); break;
		case GrowthRate_per_s  : row.growthRate_per_s  = (Double   )aValue; updateRatio(rowIndex, row); notifyChangedData.run(); break;
		case GrowthRateUnit    : row.growthRateUnit    = (ExpFactor)aValue; updateRatio(rowIndex, row); notifyChangedData.run(); break;
		case Ratio: break;
		}
		
		if (rowIsNew)
			fireTableUpdate();
	}

	private void updateRatio(final int rowIndex, final TableEntry row)
	{
		row.updateRatio();
		fireTableCellUpdate(rowIndex, ColumnID.Ratio);
	}

	void setUnit(int rowIndex, ExpFactor unit, ColumnID unitColumnID)
	{
		setValueAt(unit, rowIndex, unitColumnID);
		fireTableCellUpdate(rowIndex, unitColumnID);
	}

	@Override
	public void fireTableRowUpdate(int rowIndex)
	{
		super.fireTableRowUpdate(rowIndex);
	}
	
	
}
