package net.schwarzbaer.java.test.exponentialgrowth;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Vector;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import net.schwarzbaer.java.lib.gui.ContextMenu;
import net.schwarzbaer.java.lib.gui.FileChooser;
import net.schwarzbaer.java.lib.gui.StandardMainWindow;
import net.schwarzbaer.java.lib.gui.Tables;
import net.schwarzbaer.java.lib.system.Settings;
import net.schwarzbaer.java.test.exponentialgrowth.GrowthDiagram.DataPointGroup;

public class ExponentialGrowth
{

	public static void main(String[] args)
	{
		try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {}
		
		new ExponentialGrowth().initialize();
	}

	private final StandardMainWindow mainWindow;
	private final ExponentialGrowthTableModel tableModel;
	private final JTable table;
	private final FileChooser fileChooser;
	private final Vector<TableEntry> data;
	private Simulation simulation;
	private final ProgressPanel progressPanel;
	private final JToolBar toolBar;
	private final JPanel contentPane;
	private final JButton btnShowDiagram;
	private DataPointGroup[] computedValues;
	private File currentfile;
	private Integer selectedLength_s;
	
	private ExponentialGrowth()
	{
		simulation = null;
		computedValues = null;
		currentfile = null;
		selectedLength_s = null;
		
		mainWindow = new StandardMainWindow("", this::windowIsClosing, StandardMainWindow.DefaultCloseOperation.DO_NOTHING_ON_CLOSE);
		fileChooser = new FileChooser("Settings-File", "data");
		
		data = new Vector<>();
		tableModel = new ExponentialGrowthTableModel(data, this::notifyChangedData);
		table = new JTable(tableModel);
		tableModel.setTable(table);
		table.setRowSorter(new Tables.SimplifiedRowSorter(tableModel));
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		tableModel.setColumnWidths(table);
		tableModel.setDefaultCellEditorsAndRenderers();
		
		JScrollPane tableScrollPane = new JScrollPane(table);
		tableScrollPane.setPreferredSize(new Dimension(400,250));
		
		TableContextMenu contextMenu = new TableContextMenu();
		contextMenu.addTo(table, () -> ContextMenu.computeSurrogateMousePos(table, tableScrollPane, tableModel.getColumn(ExponentialGrowthTableModel.ColumnID.CurrentAmount)));
		contextMenu.addTo(tableScrollPane);
		
		toolBar = new JToolBar();
		toolBar.setFloatable(false);
		
		toolBar.add(createButton("New", e -> {
			currentfile = null;
			data.clear();
			tableModel.setData(data);
			tableModel.setEditingEnabled(true);
			setComputedValues(null);
			updateWindowTitle();
		}));
		toolBar.add(createButton("Open", e -> {
			if (fileChooser.showOpenDialog(mainWindow)==JFileChooser.APPROVE_OPTION)
			{
				loadDataFromFile( currentfile = fileChooser.getSelectedFile() );
				tableModel.setData(data);
				tableModel.setEditingEnabled(true);
				setComputedValues(null);
				updateWindowTitle();
			}
		}));
		toolBar.add(createButton("Save", e -> {
			saveDataToFile();
		}));
		toolBar.addSeparator();
		toolBar.add(createButton("Initial Values", e->{
			tableModel.setData(data);
			tableModel.setEditingEnabled(true);
			setComputedValues(null);
		}));
		toolBar.addSeparator();
		toolBar.add(new JLabel("Simulate "));
		SimulationLength[] simulationLengths = {
			new SimulationLength("10 s"  ,            10),
			new SimulationLength("1 min" ,         1* 60),
			new SimulationLength("10 min",        10* 60),
			new SimulationLength("1 h"   ,     1* 60* 60),
			new SimulationLength("2 h"   ,     2* 60* 60),
			new SimulationLength("5 h"   ,     5* 60* 60),
			new SimulationLength("12 h"  ,    12* 60* 60),
			new SimulationLength("1 day" , 1* 24* 60* 60),
			new SimulationLength("2 day" , 2* 24* 60* 60),
			new SimulationLength("4 day" , 4* 24* 60* 60),
			new SimulationLength("8 day" , 8* 24* 60* 60),
			new SimulationLength("Custom ...", null),
		};
		toolBar.add(createComboBox(simulationLengths, sl -> {
			if (sl!=null)
			{
				Integer length_s = sl.length_s;
				if (length_s == null) length_s = SimulationLengthInputDialog.showDialog(mainWindow, selectedLength_s);
				if (length_s != null)
				{
					selectedLength_s = length_s;
					simulate(length_s);
				}
			}
		}));
		toolBar.add(btnShowDiagram = createButton("Show Diagram", e -> {
			if (computedValues!=null)
				GrowthDiagramDialog.showDialog(mainWindow, getDiagramTitle(), computedValues);
		}));
		btnShowDiagram.setEnabled(false);
		
		progressPanel = new ProgressPanel(this::stopSimulation);
		
		contentPane = new JPanel(new BorderLayout());
		contentPane.add(toolBar, BorderLayout.PAGE_START);
		contentPane.add(tableScrollPane, BorderLayout.CENTER);
		
		mainWindow.startGUI(contentPane);
		updateWindowTitle();
		
		AppSettings.getInstance().registerAppWindow(mainWindow);
	}

	private boolean saveDataToFile()
	{
		if (fileChooser.showSaveDialog(mainWindow)==JFileChooser.APPROVE_OPTION)
		{
			saveDataToFile( fileChooser.getSelectedFile() );
			return true;
		}
		return false;
	}
	
	private void windowIsClosing(WindowEvent e)
	{
		if (currentfile==null && !data.isEmpty())
		{
			boolean windowIsAllowedToClose = false;
			while (!windowIsAllowedToClose)
			{
				String[] msg = {
						"There are unsaved data.",
						"Do you want to write it to file?"
				};
				String title = "Unsaved Data";
				int result = JOptionPane.showConfirmDialog(mainWindow, msg, title, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
				
				switch (result)
				{
				case JOptionPane.YES_OPTION:
					windowIsAllowedToClose = saveDataToFile();
					break;
					
				case JOptionPane.NO_OPTION:
					windowIsAllowedToClose = true;
					break;
					
				default: // Cancel or anything else (-> Cancel)
					return;
				}
			}
		}
		
		mainWindow.setVisible(false);
		mainWindow.dispose();
		System.exit(0);
	}

	private String getDiagramTitle()
	{
		return "Exponential Growth Diagram" + (currentfile == null ? "" : " - "+currentfile.getName());
	}

	private void updateWindowTitle()
	{
		mainWindow.setTitle( "Exponential Growth" + (currentfile == null ? "" : " - "+currentfile.getName()) );
	}

	private void setComputedValues(DataPointGroup[] computedValues)
	{
		this.computedValues = computedValues;
		btnShowDiagram.setEnabled(this.computedValues != null);
	}

	private static class SimulationLength
	{
		private final String label;
		private final Integer length_s;

		SimulationLength(String label, Integer length_s)
		{
			this.label = label;
			this.length_s = length_s;
			
		}

		@Override public String toString() { return label; }
	}
	
	static class ProgressPanel extends JPanel
	{
		private static final long serialVersionUID = -1674771085401755479L;
		
		private final JProgressBar progressBar;

		ProgressPanel(Runnable stopSimulationTask)
		{
			super(new BorderLayout());
			progressBar = new JProgressBar();
			add(progressBar, BorderLayout.CENTER);
			add(createButton("Stop", e->stopSimulationTask.run()), BorderLayout.EAST);
		}

		void configureProgressBar(int min, int max)
		{
			progressBar.setMinimum(min);
			progressBar.setMaximum(max);
		}

		void setProgressValue(int value)
		{
			progressBar.setValue(value);
		}
	}

	private void initialize()
	{
	}

	private void simulate(int time_s)
	{
		if (simulation!=null)
			return;
		
		toolBar.setEnabled(false);
		contentPane.add(progressPanel, BorderLayout.SOUTH);
		mainWindow.pack();
		
		Vector<TableEntry> data2 = new Vector<>( data.stream().map(TableEntry::new).toList() );
		tableModel.setData(data2);
		tableModel.setEditingEnabled(false);
		
		simulation = new Simulation(time_s, data2, progressPanel, tableModel, computedValues -> {
			contentPane.remove(progressPanel);
			mainWindow.pack();
			toolBar.setEnabled(true);
			simulation = null;
			setComputedValues(computedValues);
		});
		simulation.start();
	}

	private void stopSimulation()
	{
		if (simulation!=null)
			simulation.stop();
	}
	
	private void notifyChangedData()
	{
		currentfile = null;
		updateWindowTitle();
	}

	private void loadDataFromFile(File file)
	{
		System.out.printf("Read data from file \"%s\" ...%n", file.getAbsolutePath());
		data.clear();
		
		try (BufferedReader in = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8)))
		{
			String line, valueStr;
			TableEntry entry = null;
			
			while ( (line=in.readLine())!=null )
			{
				if (line.equals("[Entry]"))
				{
					if (entry!=null)
						entry.updateRatio();
					data.add(entry = new TableEntry(data.size()));
				}
				
				if ( (valueStr=getValue(line, "currentAmount = "))!=null && entry!=null)
				{
					TableEntry row_ = entry;
					parseDouble(valueStr, d -> row_.currentAmount = d);
				}
				
				if ( (valueStr=getValue(line, "currentAmountUnit = "))!=null && entry!=null)
				{
					TableEntry row_ = entry;
					ExpFactor.parse(valueStr, f -> row_.currentAmountUnit = f);
				}
				
				if ( (valueStr=getValue(line, "growthRate = "))!=null && entry!=null)
				{
					TableEntry row_ = entry;
					parseDouble(valueStr, d -> row_.growthRate_per_s = d);
				}
				
				if ( (valueStr=getValue(line, "growthRateUnit = "))!=null && entry!=null)
				{
					TableEntry row_ = entry;
					ExpFactor.parse(valueStr, f -> row_.growthRateUnit = f);
				}
			}
			
			if (entry!=null)
				entry.updateRatio();
		}
		catch (FileNotFoundException ex) {}
		catch (IOException ex)
		{
			System.err.printf("IOException while reading from file: %s%n", ex.getMessage());
			// ex.printStackTrace();
		}
		
		System.out.printf("... done%n");
	}
	
	private static void parseDouble(String valueStr, Consumer<Double> setValue)
	{
		try {
			double value = Double.parseDouble(valueStr);
			if (Double.isFinite(value))
				setValue.accept(value);
		} catch (NumberFormatException e) {}
	}

	private static String getValue(String line, String prefix)
	{
		if (line.startsWith(prefix))
			return line.substring(prefix.length());
		return null;
	}

	private void saveDataToFile(File file)
	{
		System.out.printf("Write data to file \"%s\" ...%n", file.getAbsolutePath());
		
		try (PrintWriter out = new PrintWriter(file, StandardCharsets.UTF_8))
		{
			for(TableEntry row : data)
			{
				out.println("[Entry]");
				if (Double.isFinite(row.currentAmount   )) out.printf(Locale.ENGLISH, "currentAmount"    +" = %1.6f%n", row.currentAmount);
				if (row.currentAmountUnit !=null         ) out.printf(                "currentAmountUnit"+" = %s%n"   , row.currentAmountUnit.name());
				if (Double.isFinite(row.growthRate_per_s)) out.printf(Locale.ENGLISH, "growthRate"       +" = %1.6f%n", row.growthRate_per_s);
				if (row.growthRateUnit !=null            ) out.printf(                "growthRateUnit"   +" = %s%n"   , row.growthRateUnit.name());
				out.println();
			}
		}
		catch (IOException ex)
		{
			System.err.printf("IOException while writing to file: %s%n", ex.getMessage());
			// ex.printStackTrace();
		}
		
		System.out.printf("... done%n");
		currentfile = file;
		updateWindowTitle();
	}

	static JButton createButton(String text, ActionListener al)
	{
		JButton comp = new JButton(text);
		if (al!=null) comp.addActionListener(al);
		return comp;
	}

	static JMenuItem createMenuItem(String text, ActionListener al)
	{
		JMenuItem comp = new JMenuItem(text);
		if (al!=null) comp.addActionListener(al);
		return comp;
	}

	static <E> JComboBox<E> createComboBox(E[] values, Consumer<E> valueSelected)
	{
		JComboBox<E> comp = new JComboBox<>(values);
		if (valueSelected!=null)
			comp.addActionListener( e -> valueSelected.accept( comp.getItemAt( comp.getSelectedIndex() ) ) );
		return comp;
	}

	private class TableContextMenu extends ContextMenu
	{
		private static final long serialVersionUID = 8756273253569442771L;
		
		private TableEntry clickedRow;
		private int clickedRowIndexM;

		TableContextMenu()
		{
			clickedRow = null;
			clickedRowIndexM = -1;
			
			JMenuItem miAddAmount = add(createMenuItem("##", e->{
				Double value = UnitValueInputDialog.showDialog(
						mainWindow, "Enter amount delta",
						null,
						ExpFactor.values(),
						ExpFactor[]::new
				);
				if (value==null) return;
				clickedRow.addToAmount( value );
				notifyChangedData();
				tableModel.fireTableRowUpdate( clickedRowIndexM );
			}));
			
			addSeparator();
			
			add(createMenuItem("Show Column Widths", e->{
				System.out.printf("Column Widths: %s%n", ExponentialGrowthTableModel.getColumnWidthsAsString( table ));
			}));
			
			addContextMenuInvokeListener((comp, x,y) -> {
				int rowV = table.rowAtPoint(new Point(x, y));
				clickedRowIndexM = rowV<0 ? -1 : table.convertRowIndexToModel( rowV );
				clickedRow = tableModel.getRow( clickedRowIndexM );
				if (clickedRow!=null)
					table.setRowSelectionInterval( clickedRowIndexM, clickedRowIndexM );
				
				miAddAmount.setEnabled( clickedRow!=null && tableModel.isEditingEnabled() );
				miAddAmount.setText(
						clickedRow==null
							? "Add amount ..."
							: "Add amount to row %d ...".formatted( clickedRowIndexM )
				);
			});
		}
	}
	
	static class AppSettings extends Settings.DefaultAppSettings<AppSettings.ValueGroup,AppSettings.ValueKey> {
		public enum ValueKey {
		}

		private enum ValueGroup implements Settings.GroupKeys<ValueKey> {
			;
			ValueKey[] keys;
			ValueGroup(ValueKey...keys) { this.keys = keys;}
			@Override public ValueKey[] getKeys() { return keys; }
		}
		
		private static AppSettings instance = null;
		
		static AppSettings getInstance()
		{
			if (instance == null)
				instance = new AppSettings();
			return instance; 
		}

		AppSettings() {
			super(ExponentialGrowth.class, ValueKey.values());
		}
	}
}
