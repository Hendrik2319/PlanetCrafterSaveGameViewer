package net.schwarzbaer.java.games.planetcrafter.savegameviewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellRenderer;

import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Achievements.AchievementList;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Achievements.PlanetAchievements;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.Layer;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.WorldObject;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypes.ObjectType;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypes.ObjectTypeValue;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypes.PhysicalValue;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypesPanel.ObjectTypesChangeEvent;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypesPanel.ObjectTypesChangeListener;
import net.schwarzbaer.java.lib.gui.ContextMenu;
import net.schwarzbaer.java.lib.gui.Tables;

class GeneralDataPanel extends JScrollPane implements ObjectTypesChangeListener {
	private static final long serialVersionUID = -9191759791973305801L;
	
	//private final Data data;
	private final EnergyPanel energyPanel;
	private final TerraformingStatesPanel terraformingStatesPanel;
	private final PlayerStatesPanel playerStatesPanel;

	GeneralDataPanel(Data data, PlanetAchievements achievements) {
		//this.data = data;
		GridBagConstraints c;
		
		
		
		JPanel upperPanel = new JPanel(new GridBagLayout());
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		
		c.weightx = 1;
		c.weighty = 1;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.gridy = 0;
		c.gridx = -1;
		
		c.gridx++; upperPanel.add(terraformingStatesPanel = new TerraformingStatesPanel(data.achievedValues, achievements), c);
		c.gridx++; upperPanel.add(playerStatesPanel = new PlayerStatesPanel(data.playerStates, data.achievedValues), c);
		c.gridx++; upperPanel.add(energyPanel = new EnergyPanel(data), c);
		
		
		JPanel gameDataPanel = new JPanel(new GridBagLayout());
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		
		c.weightx = 1;
		c.weighty = 1;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.gridx = 0;
		c.gridy = -1;
		
		
		c.gridy++; gameDataPanel.add(new NewPlayerStateValuesPanel(data.playerStates), c);
		c.gridy++; gameDataPanel.add(new GeneralData1Panel(data.generalData1), c);
		c.gridy++; gameDataPanel.add(new GeneralData2Panel(data.generalData2), c);
		
		
		JPanel lowerPanel = new JPanel(new GridBagLayout());
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		
		c.weightx = 1;
		c.weighty = 1;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.gridy = 0;
		c.gridx = -1;
		
		c.gridx++;
		lowerPanel.add(gameDataPanel, c);
		
		c.gridx++;
		lowerPanel.add(
				new SimpleTablePanel<>("Messages", data.messages,
					new SimpleTablePanel.Column("ID"      , String .class, 170, row->((Data.Message)row).stringId),
					new SimpleTablePanel.Column("Is Read?", Boolean.class,  60, row->((Data.Message)row).isRead  )
				), c);
		
		c.gridx++;
		lowerPanel.add(
				new SimpleTablePanel<>("StoryEvents", data.storyEvents,
					new SimpleTablePanel.Column("ID"      , String .class, 230, row->((Data.StoryEvent)row).stringId)
				), c);
		
		c.gridx++;
		lowerPanel.add(
			new SimpleTablePanel<>("Layers", data.layers,
				new SimpleTablePanel.Column("ID"              , String    .class, 180, row->((Data.Layer)row).layerId        ),
				new SimpleTablePanel.Column("Color Base      ", Data.Color.class,  90, row->((Data.Layer)row).colorBase      ),
				new SimpleTablePanel.Column("Color Custom    ", Data.Color.class,  90, row->((Data.Layer)row).colorCustom    ),
				new SimpleTablePanel.Column("Color BaseLerp  ", Long      .class,  90, row->((Data.Layer)row).colorBaseLerp  ),
				new SimpleTablePanel.Column("Color CustomLerp", Long      .class, 100, row->((Data.Layer)row).colorCustomLerp)
			).setDefaultRenderer(Data.Color.class, new GUI.ColorTCR((rowM, columnM) -> {
				if (rowM<0 || rowM>=data.layers.size()) return null;
				Layer layer = data.layers.get(rowM);
				if (layer!=null)
					switch (columnM) {
					case 1: return layer.colorBaseStr;
					case 2: return layer.colorCustomStr;
					}
				return null;
			})), c);
		
		
		
		JPanel mainPanel = new JPanel(new GridBagLayout());
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		
		c.weightx = 1;
		c.weighty = 1;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.gridx = 0;
		
		c.gridy = 0; mainPanel.add(upperPanel, c);
		c.gridy = 1; mainPanel.add(lowerPanel, c);
		
		
		setViewportView(mainPanel);
		//System.out.printf("%d, %d%n", horizontalScrollBar.getUnitIncrement(), verticalScrollBar.getUnitIncrement());
		horizontalScrollBar.setUnitIncrement(10);
		verticalScrollBar  .setUnitIncrement(10);
		//System.out.printf("%d, %d%n", horizontalScrollBar.getUnitIncrement(), verticalScrollBar.getUnitIncrement());
	}
	
	TerraformingStatesPanel getTerraformingStatesPanel() {
		return terraformingStatesPanel;
	}

	public void updateAfterAchievementsChange() {
		terraformingStatesPanel.updateAfterAchievementsChange();
	}

	@Override
	public void objectTypesChanged(ObjectTypesChangeEvent event) {
		if (event.eventType==ObjectTypesChangeEvent.EventType.ValueChanged) {
			energyPanel.objectTypeValueChanged(event.objectTypeID, event.changedValue);
			playerStatesPanel.objectTypeValueChanged(event.objectTypeID, event.changedValue);
		}
	}

	private static class EnergyPanel extends JPanel {
		private static final long serialVersionUID = 6260130212445154141L;
		
		private final ObjectsPanel sourcesPanel;
		private final ObjectsPanel consumersPanel;
		private final BudgetPanel  budgetPanel;

		EnergyPanel(Data data) {
			super(new BorderLayout());
			//setBorder(BorderFactory.createTitledBorder("Energy"));
			//setPreferredSize(new Dimension(250,200));
			
			sourcesPanel   = new ObjectsPanel(data.worldObjects, "Energy Sources"  , true );
			consumersPanel = new ObjectsPanel(data.worldObjects, "Energy Consumers", false);
			budgetPanel = new BudgetPanel("Energy Budget", sourcesPanel, consumersPanel);
			
			JPanel centerPanel = new JPanel(new GridLayout(1,0));
			centerPanel.add(sourcesPanel);
			centerPanel.add(consumersPanel);
			
			add(budgetPanel, BorderLayout.NORTH);
			add(centerPanel, BorderLayout.CENTER);
			
			SwingUtilities.invokeLater(this::updateValues);
		}

		void objectTypeValueChanged(String objectTypeID, ObjectTypeValue changedValue) {
			if (changedValue==ObjectTypeValue.Energy || ObjectTypeValue.isLabel( changedValue )) {
				updateValues();
			}
		}

		void updateValues() {
			sourcesPanel.updateValues();
			consumersPanel.updateValues();
			budgetPanel.updateValues();
		}
		
		private static class ObjectsPanel extends JScrollPane {
			private static final long serialVersionUID = -7778016200735929929L;
			
			private final Vector<WorldObject> worldObjects;
			private final boolean computeSources;
			private final ObjectsTableModel tableModel;
			private double totalSum;

			ObjectsPanel(Vector<Data.WorldObject> worldObjects, String title, boolean computeSources) {
				this.worldObjects = worldObjects;
				this.computeSources = computeSources;
				totalSum = 0;
				
				tableModel = new ObjectsTableModel();
				JTable table = new JTable(tableModel);
				table.setRowSorter(new Tables.SimplifiedRowSorter(tableModel));
				table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
				
				tableModel.setTable(table);
				tableModel.setColumnWidths(table);
				tableModel.setDefaultCellEditorsAndRenderers();
				
				new GUI.ObjectsTableContextMenu(table, tableModel);
				
				setViewportView(table);
				Dimension size = table.getPreferredSize();
				size.width  += 30;
				size.height = 150;
				setPreferredSize(size);
				
				setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(title), getBorder()));
				
				Data.addRemoveStateListener(tableModel::updateRemoveStates);
			}

			void updateValues() {
				HashMap<String,ObjectsTableRow> tableContent = new HashMap<>();
				totalSum = 0.0;
				for (WorldObject wo : worldObjects) {
					if (wo == null) continue;
					if (!wo.isInstalled()) continue;
					if (wo.objectType == null) continue;
					if (wo.objectType.energy == null) continue;
					
					double energy = wo.objectType.energy.doubleValue();
					if (( computeSources && energy>0) ||
						(!computeSources && energy<0) ) {
						ObjectsTableRow row = tableContent.get(wo.objectTypeID);
						if (row==null) tableContent.put(wo.objectTypeID, row = new ObjectsTableRow(wo.getName()));
						row.add(wo,energy);
						
						totalSum += energy;
					}
				}
				tableModel.setData(tableContent);
			}

			double getSum() { return totalSum; }
			
			private static class ObjectsTableRow extends GUI.ObjectsTableRow {
				
				double sum;
				
				ObjectsTableRow(String name) {
					super(name);
					sum = 0;
				}

				void add(WorldObject wo, double energy) {
					add(wo);
					sum += energy;
				}
			}
			
			private static class ObjectsTableCellRenderer implements TableCellRenderer {
				
				private final Tables.LabelRendererComponent rendererComponent;
				private final ObjectsTableModel tableModel;

				ObjectsTableCellRenderer(ObjectsTableModel tableModel) {
					this.tableModel = tableModel;
					rendererComponent = new Tables.LabelRendererComponent();
				}

				@Override
				public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int rowV, int columnV) {
					int    rowM =    rowV<0 ? -1 : table.   convertRowIndexToModel(   rowV);
					int columnM = columnV<0 ? -1 : table.convertColumnIndexToModel(columnV);
					ObjectsTableRow  row = rowM<0 ? null : tableModel.getRow(rowM);
					ObjectsTableModel.ColumnID columnID = columnM<0 ? null : tableModel.getColumnID(columnM);
					
					String valueStr = value==null ? null : value.toString();
					if (columnID==ObjectsTableModel.ColumnID.Energy && value instanceof Double ) valueStr = ObjectTypes.formatEnergyRate((Double)value);
					if (columnID==ObjectsTableModel.ColumnID.Count  && value instanceof Integer) valueStr = String.format(Locale.ENGLISH, "%d x ", value);
					
					Supplier<Color> getCustomBackground = ObjectsTableRow.createCustomBackgroundFunction(row);
					rendererComponent.configureAsTableCellRendererComponent(table, null, valueStr, isSelected, hasFocus, getCustomBackground, null);
					if (value instanceof Number)
						rendererComponent.setHorizontalAlignment(SwingConstants.RIGHT);
					else
						rendererComponent.setHorizontalAlignment(SwingConstants.LEFT);
					
					return rendererComponent;
				}
				
			}
			
			private static class ObjectsTableModel extends GUI.ObjectsTableModel<ObjectsTableRow, ObjectsTableModel.ColumnID> {
				
				enum ColumnID implements Tables.SimplifiedColumnIDInterface {
					Count ("Count" , Integer.class,  50),
					Name  ("Name"  , String .class, 130),
					Energy("Energy", Double .class,  80),
					;
					private final Tables.SimplifiedColumnConfig cfg;
					ColumnID(String name, Class<?> colClass, int width) {
						cfg = new Tables.SimplifiedColumnConfig(name, colClass, 20, -1, width, width);
					}
					@Override public Tables.SimplifiedColumnConfig getColumnConfig() {
						return cfg;
					}
				}

				ObjectsTableModel() {
					super(ColumnID.values());
				}
				
				public void setDefaultCellEditorsAndRenderers() {
					ObjectsTableCellRenderer tcr = new ObjectsTableCellRenderer(this);
					table.setDefaultRenderer(Integer.class, tcr);
					table.setDefaultRenderer(Double .class, tcr);
					table.setDefaultRenderer(String .class, tcr);
				}

				void setData(HashMap<String,ObjectsTableRow> data) {
					super.setData(data.values());
					rows.sort(Comparator.<ObjectsTableRow,Double>comparing(row->Math.abs(row.sum),Comparator.reverseOrder()).thenComparing(row->row.name));
					fireTableUpdate();
				}

				@Override
				public Object getValueAt(int rowIndex, int columnIndex, ColumnID columnID) {
					ObjectsTableRow row = getRow(rowIndex);
					if (row==null) return null;
					
					switch (columnID) {
					case Count : return row.getCount();
					case Name  : return row.name;
					case Energy: return row.sum;
					}
					return null;
				}
			}
		}
		
		private static class BudgetPanel extends JPanel {
			private static final long serialVersionUID = 8583714497440761807L;
			
			private final JTextField fieldConsumption;
			private final JTextField fieldProduction;
			private final JTextField fieldBudget;
			private final ObjectsPanel sourcesPanel;
			private final ObjectsPanel consumersPanel;

			BudgetPanel(String title, ObjectsPanel sourcesPanel, ObjectsPanel consumersPanel) {
				super(new GridBagLayout());
				this.sourcesPanel = sourcesPanel;
				this.consumersPanel = consumersPanel;
				setBorder(BorderFactory.createTitledBorder(title));
				
				GridBagConstraints c = new GridBagConstraints();
				c.fill = GridBagConstraints.BOTH;
				
				c.weighty = 0;
				c.gridwidth  = 1;
				c.gridheight = 1;
				c.gridy = 0;
				c.gridx = -1;
				
				c.weightx = 0; c.gridx++; add(new JLabel("Production: "),c);
				c.weightx = 1; c.gridx++; add(fieldProduction = GUI.createOutputTextField("---"),c);
				
				c.weightx = 0; c.gridx++; add(new JLabel("  Consumption: "),c);
				c.weightx = 1; c.gridx++; add(fieldConsumption = GUI.createOutputTextField("---"),c);
				
				c.weightx = 0; c.gridx++; add(new JLabel("  Budget: "),c);
				c.weightx = 1; c.gridx++; add(fieldBudget = GUI.createOutputTextField("---"),c);
			}

			void updateValues() {
				double sumSources   = sourcesPanel  .getSum();
				double sumConsumers = consumersPanel.getSum();
				fieldProduction .setText(ObjectTypes.formatEnergyRate( sumSources  ));
				fieldConsumption.setText(ObjectTypes.formatEnergyRate(-sumConsumers));
				fieldBudget     .setText(ObjectTypes.formatEnergyRate(sumSources+sumConsumers));
			}
		}
	}

	static class TerraformingStatesPanel extends JPanel {
		private static final long serialVersionUID = 6272012218012618784L;

		private final Row oxygenRow;
		private final Row heatRow;
		private final Row pressureRow;
		private final Row biomassRow;
		private final Row plantsRow;
		private final Row insectsRow;
		private final Row animalsRow;
		private final Row terraformRow;
		private final Row stagesRow;
		
		TerraformingStatesPanel(Data.AchievedValues data, PlanetAchievements achievements) {
			super(new GridBagLayout());
			setBorder(BorderFactory.createTitledBorder("Terraforming"));
			
			Color bgColor = getBackground();
			double terraformLevel = data.getTerraformLevel();
			double biomassLevel   = data.getBiomassLevel();
			oxygenRow    = new Row(data.oxygenLevel  , achievements, AchievementList.Oxygen        , bgColor, Data.AchievedValues::formatOxygenLevel   , PhysicalValue.Oxygen  ::formatRate);
			heatRow      = new Row(data.heatLevel    , achievements, AchievementList.Heat          , bgColor, Data.AchievedValues::formatHeatLevel     , PhysicalValue.Heat    ::formatRate);
			pressureRow  = new Row(data.pressureLevel, achievements, AchievementList.Pressure      , bgColor, Data.AchievedValues::formatPressureLevel , PhysicalValue.Pressure::formatRate);
			biomassRow   = new Row(biomassLevel      , achievements, AchievementList.Biomass       , bgColor, Data.AchievedValues::formatBiomassLevel  , val->String.format(Locale.ENGLISH, "%1.2f g/s", val));
			plantsRow    = new Row(data.plantsLevel  , achievements, AchievementList.Plants        , bgColor, Data.AchievedValues::formatBiomassLevel  , PhysicalValue.Plants  ::formatRate);
			insectsRow   = new Row(data.insectsLevel , achievements, AchievementList.Insects       , bgColor, Data.AchievedValues::formatBiomassLevel  , PhysicalValue.Insects ::formatRate);
			animalsRow   = new Row(data.animalsLevel , achievements, AchievementList.Animals       , bgColor, Data.AchievedValues::formatBiomassLevel  , PhysicalValue.Animals ::formatRate);
			terraformRow = new Row(terraformLevel    , achievements, AchievementList.Terraformation, bgColor, Data.AchievedValues::formatTerraformation, val->String.format(Locale.ENGLISH, "%1.2f Ti/s", val));
			stagesRow    = new Row(terraformLevel    , achievements, AchievementList.Stages        , bgColor, Data.AchievedValues::formatTerraformation, null, true, d -> getStageRatioStr(achievements, d));
			
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			
			c.weighty = 0;
			c.gridwidth = 1;
			c.gridheight = 1;
			c.gridy = -1;
			
			
			int y;
			y =  0; oxygenRow   .addToPanel(this, y, "Oxygen"  );
			y += 2; heatRow     .addToPanel(this, y, "Heat"    );
			y += 2; pressureRow .addToPanel(this, y, "Pressure");
			y += 2; biomassRow  .addToPanel(this, y, "Biomass" );
			y += 2; plantsRow   .addToPanel(this, y, "Plants"  );
			y += 2; insectsRow  .addToPanel(this, y, "Insects" );
			y += 2; animalsRow  .addToPanel(this, y, "Animals" );
			y += 2; terraformRow.addToPanel(this, y, "Terraformation");
			y += 2; stagesRow   .addToPanel(this, y, "Stages"  );
			
			
			c.gridy = y+1;
			c.gridx = 0;
			c.weighty = 1;
			c.weightx = 1;
			c.gridwidth = 3;
			add(new JLabel(), c);
		}

		private static String getStageRatioStr(PlanetAchievements achievements, Double terraformLevel)
		{
			if (terraformLevel==null) return "--";
			Double achievementRatio = achievements.getAchievementRatio(terraformLevel, AchievementList.Stages);
			if (achievementRatio==null) return "--";
			return String.format(Locale.ENGLISH, "%1.2f %%", achievementRatio*100);
		}

		void updateAfterAchievementsChange() {
			oxygenRow   .updateAchievementField();
			heatRow     .updateAchievementField();
			pressureRow .updateAchievementField();
			biomassRow  .updateAchievementField();
			plantsRow   .updateAchievementField();
			insectsRow  .updateAchievementField();
			animalsRow  .updateAchievementField();
			terraformRow.updateAchievementField();
			stagesRow   .updateAchievementField();
		}

		void setRateOfPhysicalValue(PhysicalValue physicalValue, double rate) {
			switch (physicalValue) {
			case Oxygen  : oxygenRow  .setRate(rate); break;
			case Heat    : heatRow    .setRate(rate); break;
			case Pressure: pressureRow.setRate(rate); break;
			case Plants  : plantsRow  .setRate(rate); break;
			case Insects : insectsRow .setRate(rate); break;
			case Animals : animalsRow .setRate(rate); break;
			}
			biomassRow.setRate(
					plantsRow .getRate()+
					insectsRow.getRate()+
					animalsRow.getRate()
			);
			terraformRow.setRate(
					oxygenRow  .getRate()+
					heatRow    .getRate()+
					pressureRow.getRate()+
					biomassRow .getRate()
			);
			stagesRow.setRate(
					terraformRow.getRate()
			);
		}
		
		static void testDurationFormater() {
			Row.testDurationFormater();
		}
		
		private static class AchievementTextField extends JTextField {
			private static final long serialVersionUID = 4367674243165558871L;
			
			private final int markerColor;
			private Double ratio;

			AchievementTextField(String text, int size, Color panelBgColor)
			{
				super(text, size);
				setOpaque(false);
				ratio = null;
				
				if (compareBrightness(panelBgColor, getForeground()) > 0)
					markerColor = 0xFFFFFF;
				else
					markerColor = 0;
			}
			
			private static int compareBrightness(Color c1,Color c2) { return getRGBSum(c1)-getRGBSum(c2); }
			private static int getRGBSum(Color c) { return c.getRed()+c.getGreen()+c.getBlue(); }

			void setText(String text, Double ratio) {
				this.ratio = ratio;
				super.setText(text);
			}

			@Override
			protected void paintComponent(Graphics g)
			{
				drawMarker(g);
				super.paintComponent(g);
			}

			private void drawMarker(Graphics g)
			{
				if (ratio==null) return;
				
				int width  = getWidth();
				int height = getHeight();
				
				int markerPos = (int)Math.round( width * Math.min(Math.max(0, ratio), 1) );
				int markerWidth = 200;
				
				float f = 0.1f;
				float f1_min = 1/(1+f*markerWidth);
				
				for (int i=0; i<=markerWidth; i++) {
					int iPos = markerPos-i;
					if (iPos <  0    ) continue;
					if (iPos >= width) continue;
					
					float f1 = 1/(1+f*i);
					f1 = (f1-f1_min)/(1-f1_min);
					int opaqueness = Math.round(0xFF * f1) << 24;
					
					g.setColor(new Color(markerColor | opaqueness, true));
					g.fillRect(iPos,2, 1,height-4);
				}
			}
		}
		
		private static class Row {
			
			private final Function<Double, String> formatLevel;
			private final Function<Double, String> formatRate;
			private final JTextField fieldLevel;
			private final JTextField fieldRate;
			private final AchievementTextField fieldAchievement;
			private final boolean oneLine;
			
			private final PlanetAchievements achievements;
			private final double level;
			private final AchievementList achievementList;
			
			private double rate;

			Row(double level, PlanetAchievements achievements, AchievementList achievementList, Color panelBgColor, Function<Double,String> formatLevel, Function<Double,String> formatRate) {
				this(level, achievements, achievementList, panelBgColor, formatLevel, formatRate, false, null);
			}
			Row(double level, PlanetAchievements achievements, AchievementList achievementList, Color panelBgColor, Function<Double,String> formatLevel, Function<Double,String> formatRate, boolean oneLine, Function<Double,String> formatLevel2) {
				this.achievements = achievements;
				this.achievementList = achievementList;
				this.level = level;
				this.rate = 0;
				this.formatLevel = formatLevel;
				this.formatRate = formatRate;
				this.oneLine = oneLine;
				if (formatLevel2==null) formatLevel2 = this.formatLevel;
				fieldLevel       = this.oneLine && formatLevel2==null ? null : GUI.createOutputTextField(formatLevel2.apply(this.level),10,JTextField.RIGHT);
				fieldRate        = this.oneLine                       ? null : GUI.createOutputTextField("--",20,JTextField.RIGHT);
				fieldAchievement = createAchievementTextField("--",20,JTextField.RIGHT, panelBgColor);
				updateAchievementField();
			}

			private AchievementTextField createAchievementTextField(String text, int size, int horizontalAlignment, Color panelBgColor)
			{
				AchievementTextField comp = new AchievementTextField(text,size,panelBgColor);
				comp.setEditable(false);
				comp.setHorizontalAlignment(horizontalAlignment);
				return comp;
			}

			void updateAchievementField() {
				Achievements.Achievement achievement = achievements.getNextAchievement(level,achievementList);
				Double achievementRatio = achievements.getAchievementRatio(level, achievementList);
				String achievementText = getAchievementText(achievement);
				fieldAchievement.setText(achievementText, achievementRatio);
			}

			private String getAchievementText(Achievements.Achievement achievement) {
				if (achievement==null)
					return "No Achievements left";
				
				String label = achievement.getLabel();
				if (label==null || label.isBlank())
					label = "<Nameless>";
					
				Double nextLevel = achievement.getLevel();
				if (nextLevel==null)
					return label;
				
				String nextLevelStr = formatLevel.apply(nextLevel);
				if (rate==0)
					return String.format("%s at %s", achievement.getLabel(), nextLevelStr);
				
				double duration_s = (nextLevel.doubleValue()-level) / rate;
				String durationsStr = getDurationsString(duration_s);
				
				return String.format("%s at %s %s", achievement.getLabel(), nextLevelStr, durationsStr);
			}
			
			private static String formatValue(String format, Object... values) {
				return String.format(Locale.ENGLISH, format, values);
			}
			
			static void testDurationFormater() {
				testDurationFormater(" 5.2 s", 5.2);
				testDurationFormater("  61 s", 61);
				testDurationFormater(" 100 s", 100);
				testDurationFormater("3599 s", 3599);
				testDurationFormater("3600 s", 3600);
				testDurationFormater("3601 s", 3601);
				testDurationFormater("3661 s", 3661);
				testDurationFormater("nearly  1 day ", 24*3600*0.99);
				testDurationFormater("        1 day ", 24*3600);
				testDurationFormater("above   1 day ", 24*3600*1.01);
				testDurationFormater("nearly 10 days", 10*24*3600*0.99);
				testDurationFormater("       10 days", 10*24*3600);
				testDurationFormater("      350 days", 350*24*3600);
				testDurationFormater("      365 days", 365*24*3600);
				testDurationFormater("            10 Y", 365.0*24*3600*10.0);
				testDurationFormater("           100 Y", 365.0*24*3600*100.0);
				testDurationFormater("          1000 Y", 365.0*24*3600*1000.0);
				testDurationFormater("         10000 Y", 365.0*24*3600*10000.0);
				testDurationFormater("        100000 Y", 365.0*24*3600*100000.0);
				testDurationFormater("       1000000 Y", 365.0*24*3600*1000000.0);
				testDurationFormater("      10000000 Y", 365.0*24*3600*10000000.0);
				testDurationFormater("     100000000 Y", 365.0*24*3600*100000000.0);
				testDurationFormater("    1000000000 Y", 365.0*24*3600*1000000000.0);
				testDurationFormater("   10000000000 Y", 365.0*24*3600*10000000000.0);
				testDurationFormater("  100000000000 Y", 365.0*24*3600*100000000000.0);
				testDurationFormater(" 1000000000000 Y", 365.0*24*3600*1000000000000.0);
				testDurationFormater("10000000000000 Y", 365.0*24*3600*10000000000000.0);
			}
			
			private static void testDurationFormater(String label, double value) {
				System.out.printf("%s -> [%s] %s%n", label, value, getDurationsString(value));
			}
			
			private static String getDurationsString(double value) {
				if (value < 60) return formatValue("in %1.1f s", value);
				value /= 60;
				int sec = (int)Math.floor((value    - Math.floor(value   ))*60);
				int min = (int)Math.floor((value/60 - Math.floor(value/60))*60);
				
				if (value < 60) return formatValue("in %d:%02d min", min, sec);
				value/=60;
				int hour = (int)Math.floor((value/24 - Math.floor(value/24))*24);
				
				if (value < 24) return formatValue("in %d:%02d:%02d h", hour, min, sec);
				value/=24;
				
				if (value <  10) return formatValue("in %1.0f d %02d:%02d h", Math.floor(value), hour, min);
				if (value < 100) return formatValue("in %1.2f d", value);
				if (value < 365) return formatValue("in %1.1f d", value);
				value/=365;
				
				if (value <   10) return formatValue("in %1.2f Y", value);
				if (value <  100) return formatValue("in %1.1f Y", value);
				if (value < 1000) return formatValue("in %1.0f Y", value);
				value/=1000;
				
				if (value <   10) return formatValue("in %1.2f kY", value);
				if (value <  100) return formatValue("in %1.1f kY", value);
				if (value < 1000) return formatValue("in %1.0f kY", value);
				value/=1000;
				
				if (value <   10) return formatValue("in %1.2f MY", value);
				if (value <  100) return formatValue("in %1.1f MY", value);
				if (value < 1000) return formatValue("in %1.0f MY", value);
				value/=1000;
				
				if (value <   10) return formatValue("in %1.2f GY", value);
				if (value <  100) return formatValue("in %1.1f GY", value);
				if (value < 1000) return formatValue("in %1.0f GY", value);
				value/=1000;
				
				return "after a long time";
			}

			void setRate(double rate) {
				this.rate = rate;
				if (fieldRate!=null && formatRate!=null) {
					double rate2Level = Math.log10(rate/level);
					if (Double.isFinite(rate2Level))
						fieldRate.setText(String.format(Locale.ENGLISH, "%s (%1.2f)", formatRate.apply(rate), rate2Level));
					else
						fieldRate.setText(formatRate.apply(rate));
				}
				updateAchievementField();
			}

			double getRate() {
				return rate;
			}
			
			void addToPanel(JPanel panel, int gridy, String label) {
				GridBagConstraints c = new GridBagConstraints();
				c.fill = GridBagConstraints.BOTH;
				
				c.weighty = 0;
				c.gridwidth = 1;
				c.gridheight = 1;
				
				c.weightx = 0; c.gridy = gridy  ; c.gridx = 0; panel.add(new JLabel(label+": "), c);
				
				if (!oneLine)
				{
					c.weightx = 0; c.gridy = gridy  ; c.gridx = 1; panel.add(fieldLevel, c);
					c.weightx = 1; c.gridy = gridy  ; c.gridx = 2; panel.add(fieldRate , c);
					c.weightx = 1; c.gridy = gridy+1; c.gridx = 1; c.gridwidth = 2; panel.add(fieldAchievement, c);
					//c.weightx = 1; c.gridy = gridy  ; c.gridx = 2; panel.add(fieldAchievement, c);
					//c.weightx = 1; c.gridy = gridy+1; c.gridx = 1; c.gridwidth = 2; panel.add(fieldRate , c);
					
					//fieldLevel.setFont(fieldLevel.getFont().deriveFont(Font.BOLD));
					fieldRate.setForeground(Color.GRAY);
					//fieldRate.setFont(fieldRate.getFont().deriveFont(Font.PLAIN));
				}
				else if (fieldLevel != null)
				{
					c.weightx = 0; c.gridy = gridy  ; c.gridx = 1; panel.add(fieldLevel      , c);
					c.weightx = 1; c.gridy = gridy  ; c.gridx = 2; panel.add(fieldAchievement, c);
				}
				else
				{
					c.gridwidth = 2;
					c.weightx = 1; c.gridy = gridy  ; c.gridx = 1; panel.add(fieldAchievement, c);
				}
			}
		}
	}

	private static class PlayerStatesPanel extends JPanel {
		private static final long serialVersionUID = 6272012218012618784L;
		
		private final JTextArea textArea;
		private final Iterable<String> unlockedObjectTypes;

		PlayerStatesPanel(Data.PlayerStates playerStates, Data.AchievedValues achievedValues) {
			super(new BorderLayout());
			
			JPanel playerStatesPanel = new ValueListPanel("Player", true, helper -> {
				helper.addLineRaw("Terra Tokens"         , Data.AchievedValues.formatTerraTokens(achievedValues.terraTokens));
				helper.addLineRaw("All Time Terra Tokens", Data.AchievedValues.formatTerraTokens(achievedValues.allTimeTerraTokens));
				helper.addLine   ("Health"  , playerStates.health, "%1.2f %%");
				helper.addLine   ("Thirst"  , playerStates.thirst, "%1.2f %%");
				helper.addLine   ("Oxygen"  , playerStates.oxygen, "%s");
				helper.addLineRaw("Position", String.format("%s", playerStates.position));
				helper.addLineRaw("Rotation", String.format("%s", playerStates.rotation));
			});
			
			ObjectType[] uotArr = null;
			if (uotArr == null) uotArr = achievedValues.unlockedObjectTypes;
			if (uotArr == null) uotArr = playerStates  .unlockedObjectTypes;
			final ObjectType[] uotArr_ = uotArr;
			unlockedObjectTypes = uotArr_==null ? new Vector<>() : ()->Arrays.stream(uotArr_).map(ObjectType::getName).sorted().iterator();
			
			//Vector<String> unlockedObjectTypes = new Vector<>(Arrays.asList(data.unlockedGroups));
			//unlockedObjectTypes.sort(Data.caseIgnoringComparator);
			
			textArea = new JTextArea(String.join(",\r\n", unlockedObjectTypes));
			textArea.setEditable(false);
			textArea.setLineWrap(false);
			//textArea.setLineWrap(true);
			//textArea.setWrapStyleWord(true);
			JScrollPane textAreaScrollPane = new JScrollPane(textArea);
			textAreaScrollPane.setPreferredSize(new Dimension(100,100));
			//textAreaScrollPane.setBorder(BorderFactory.createTitledBorder("Unlocked Groups"));
			textAreaScrollPane.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Unlocked Groups"), textAreaScrollPane.getBorder()));
			GUI.reduceTextAreaFontSize(new JLabel(), 1, textArea);
			
			
			
			add(playerStatesPanel, BorderLayout.NORTH);
			add(textAreaScrollPane, BorderLayout.CENTER);
		}

		void objectTypeValueChanged(String objectTypeID, ObjectTypeValue changedValue) {
			if (ObjectTypeValue.isLabel( changedValue ));
				textArea.setText(String.join(",\r\n", unlockedObjectTypes));
		}
	}

	private static class ValueListPanel extends JPanel {
		private static final long serialVersionUID = -9084397318415882019L;
		
		private final GridBagConstraints c;
		
		ValueListPanel(String title, Consumer<Helper> fillPanel) {
			this(title, false, fillPanel);
		}
		ValueListPanel(String title, boolean withoutEndSpacer, Consumer<Helper> fillPanel) {
			super(new GridBagLayout());
			setBorder(BorderFactory.createTitledBorder(title));
			
			c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			
			c.weighty = 0;
			c.gridwidth = 1;
			c.gridheight = 1;
			c.gridy = -1;
			
			fillPanel.accept(new Helper());
			
			c.gridy++;
			c.weighty = 1;
			c.weightx = 1;
			c.gridwidth = 2;
			c.gridx = 0;
			add(new JLabel(), c);
		}
		
		private void addLineRaw(String label, String valueStr) {
			c.gridy++;
			c.weightx = 0; c.gridx = 0; add(new JLabel(label+": "), c);
			c.weightx = 1; c.gridx = 1; add(GUI.createOutputTextField(valueStr), c);
		}
		
		private void addBlockLabel(String label) {
			c.gridy++;
			c.weightx = 0; c.gridx = 0;
			c.gridwidth = 2;
			add(new JLabel(label), c);
			c.gridwidth = 1;
		}
		
		class Helper
		{
			void addLine       (String label, String  value               ) { ValueListPanel.this.addLineRaw   (label, value==null ? "" : String.format(                "\"%s\"", value)); }
			void addLine       (String label, Long    value               ) { ValueListPanel.this.addLineRaw   (label, value==null ? "" : String.format(                  "%s"  , value)); }
			void addLine       (String label, Double  value, String format) { ValueListPanel.this.addLineRaw   (label, value==null ? "" : String.format(Locale.ENGLISH, format  , value)); }
			void addLine       (String label, Boolean value               ) { ValueListPanel.this.addLineRaw   (label, value==null ? "" : String.format(                  "%s"  , value)); }
			void addLineRaw    (String label, String valueStr             ) { ValueListPanel.this.addLineRaw   (label, valueStr); }
			void addBlockLabel (String label                              ) { ValueListPanel.this.addBlockLabel(label); }
		}
	}

	private static class GeneralData1Panel extends ValueListPanel {
		private static final long serialVersionUID = 3027572349235658926L;

		GeneralData1Panel(Data.GeneralData1 data) {
			super("General Data (1)", helper -> {
				helper.addLine("Crafted Objects"    , data.craftedObjects   );
				helper.addLine("Total SaveFile Load", data.totalSaveFileLoad);
				helper.addLine("Total SaveFile Time", data.totalSaveFileTime);
			});
		}
	}

	private static class GeneralData2Panel extends ValueListPanel {
		private static final long serialVersionUID = 6272012218012618784L;

		GeneralData2Panel(Data.GeneralData2 data) {
			super("General Data (2)", helper -> {
				helper.addLine("Save DisplayName"        , data.saveDisplayName       );
				helper.addLine("Mode"                    , data.mode                  );
				helper.addLine("Planet ID"               , data.planetIdStr           );
				helper.addLine("WorldSeed"               , data.worldSeed             );
				helper.addLine("Start Location Label"    , data.startLocationLabel    );
				helper.addLine("Game Start Location"     , data.gameStartLocation     );
				helper.addLine("Free Craft"              , data.freeCraft             );
				helper.addLine("Randomize Mineables"     , data.randomizeMineables    );
				helper.addLine("Dying Consequences Label", data.dyingConsequencesLabel);
				helper.addLine("Has Played Intro"        , data.hasPlayedIntro        );
				helper.addBlockLabel(" [ Unlocked ]");
				helper.addLine("Space Trading", data.unlockedSpaceTrading);
				helper.addLine("Ore Extrators", data.unlockedOreExtrators);
				helper.addLine("Teleporters"  , data.unlockedTeleporters );
				helper.addLine("Drones"       , data.unlockedDrones      );
				helper.addLine("Autocrafter"  , data.unlockedAutocrafter );
				helper.addLine("Everything"   , data.unlockedEverything  );
				helper.addBlockLabel(" [ Modifier ]");
				helper.addLine("Terraformation Pace"              , data.modifierTerraformationPace             , "%1.3f");
				helper.addLine("Power Consumption"                , data.modifierPowerConsumption               , "%1.3f");
				helper.addLine("Gauge Drain"                      , data.modifierGaugeDrain                     , "%1.3f");
				helper.addLine("Meteo Occurence"                  , data.modifierMeteoOccurence                 , "%1.3f");
				helper.addLine("Multiplayer Terraformation Factor", data.modifierMultiplayerTerraformationFactor, "%1.3f");
			});
		}
	}

	private static class NewPlayerStateValuesPanel extends ValueListPanel {
		private static final long serialVersionUID = 7520095610363236996L;

		NewPlayerStateValuesPanel(Data.PlayerStates data)
		{
			super("New PlayerState Values", helper -> {
				helper.addLine("ID"          , data.id         );
				helper.addLine("Name"        , data.name       );
				helper.addLine("Inventory ID", data.inventoryId);
				helper.addLine("Equipment ID", data.equipmentId);
				helper.addLine("Is Host"     , data.isHost     );
			});
		}
	}
	
	private static class SimpleTablePanel<ValueType> extends JScrollPane {
		private static final long serialVersionUID = -8500969138264337829L;
		private final JTable table;

		SimpleTablePanel(String title, Vector<ValueType> data, Column... columns) {
			SimpleTableModel<ValueType> tableModel = new SimpleTableModel<>(data,columns);
			table = new JTable(tableModel);
			table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			table.setRowSorter(new Tables.SimplifiedRowSorter(tableModel));
			table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			tableModel.setTable(table);
			tableModel.setColumnWidths(table);
			
			new TableContextMenu(table,tableModel);
			
			setViewportView(table);
			//setBorder(BorderFactory.createTitledBorder(title));
			setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(title), getBorder()));

			
			//Dimension size = table.getPreferredScrollableViewportSize();
			Dimension size = table.getPreferredSize();
			size.width  += 30;
			size.height += 50;
			setPreferredSize(size);
			
			//setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			//setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
		}
		
		SimpleTablePanel<ValueType> setDefaultRenderer(Class<?> columnClass, TableCellRenderer renderer) {
			table.setDefaultRenderer(columnClass, renderer);
			return this;
		}
		
		private class TableContextMenu extends ContextMenu {
			private static final long serialVersionUID = 1755523803906870773L;

			TableContextMenu(JTable table, SimpleTableModel<ValueType> tableModel) {
				add(GUI.createMenuItem("Show Column Widths", e->{
					System.out.printf("Column Widths: %s%n", Tables.SimplifiedTableModel.getColumnWidthsAsString(table));
				}));
				
				addTo(table);
			}
		}
		
		private static class SimpleTableModel<ValueType> extends Tables.SimplifiedTableModel<Column> {

			private final Vector<ValueType> data;

			protected SimpleTableModel(Vector<ValueType> data, Column[] columns) {
				super(columns);
				this.data = data;
			}

			@Override public int getRowCount() { return data.size(); }

			@Override public Object getValueAt(int rowIndex, int columnIndex, Column columnID) {
				if (rowIndex<0) return null;
				if (rowIndex>=data.size()) return null;
				ValueType row = data.get(rowIndex);
				return columnID.getValue.apply(row);
			}
			
		}
		
		static class Column implements Tables.SimplifiedColumnIDInterface {
			
			private final Tables.SimplifiedColumnConfig config;
			private final Function<Object, Object> getValue;

			Column(String name, Class<?> columnClass, int width, Function<Object,Object> getValue) {
				this.getValue = getValue;
				config = new Tables.SimplifiedColumnConfig(name, columnClass, 20, -1, width, width);
			}

			@Override public Tables.SimplifiedColumnConfig getColumnConfig() {
				return config;
			}
		}
	}
}