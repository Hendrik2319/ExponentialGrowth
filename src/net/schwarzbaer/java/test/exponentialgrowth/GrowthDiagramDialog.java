package net.schwarzbaer.java.test.exponentialgrowth;

import java.awt.BorderLayout;
import java.awt.Window;

import javax.swing.JPanel;

import net.schwarzbaer.java.lib.gui.StandardDialog;
import net.schwarzbaer.java.test.exponentialgrowth.GrowthDiagram.DataPointGroup;

class GrowthDiagramDialog extends StandardDialog
{
	private static final long serialVersionUID = -2888008418787036911L;

	private GrowthDiagramDialog(Window parent, DataPointGroup[] data)
	{
		super(parent, "", ModalityType.APPLICATION_MODAL, false);
		GrowthDiagram growthDiagram = new GrowthDiagram();
		growthDiagram.setData(data);
		
		JPanel contentPane = new JPanel(new BorderLayout());
		contentPane.add(growthDiagram, BorderLayout.CENTER);
		
		createGUI(contentPane, ExponentialGrowth.createButton("Close", e -> closeDialog() ));
	}
	
	static void showDialog(Window parent, DataPointGroup[] data)
	{
		new GrowthDiagramDialog(parent, data).showDialog();
	}
}
