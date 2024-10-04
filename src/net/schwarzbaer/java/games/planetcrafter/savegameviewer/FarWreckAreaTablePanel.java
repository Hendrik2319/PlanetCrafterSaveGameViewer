package net.schwarzbaer.java.games.planetcrafter.savegameviewer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.util.Locale;
import java.util.Vector;
import java.util.function.Function;

import javax.swing.BorderFactory;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;

import net.schwarzbaer.java.games.planetcrafter.savegameviewer.FarWreckAreas.WreckArea;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.TwoSidedTablePanel.TablePanelWithTextArea;
import net.schwarzbaer.java.lib.gui.GeneralIcons.GrayCommandIcons;
import net.schwarzbaer.java.lib.gui.Tables;
import net.schwarzbaer.java.lib.gui.Tables.SimplifiedColumnConfig;
import net.schwarzbaer.java.lib.gui.ZoomableCanvas;

class FarWreckAreaTablePanel extends JSplitPane
{
	private static final long serialVersionUID = -5756462259451478608L;
	
	FarWreckAreaTablePanel() {
		super(JSplitPane.VERTICAL_SPLIT, true);
		
		PointTablePanel pointTablePanel = new PointTablePanel();
		WreckAreaTablePanel wreckAreaTablePanel = new WreckAreaTablePanel(pointTablePanel);
		
		setTopComponent(wreckAreaTablePanel);
		setBottomComponent(pointTablePanel);
	}

	private static class WreckAreaTablePanel extends TwoSidedTablePanel<WreckArea, WreckAreaTableModel.ColumnID, WreckAreaTableModel, WreckAreaMapView>
	{
		private static final long serialVersionUID = -5756462259451478608L;
		
		private final PointTablePanel pointTablePanel;
		
		WreckAreaTablePanel(PointTablePanel pointTablePanel) {
			super(WreckAreaMapView::new, new WreckAreaTableModel(pointTablePanel.tableModel), true, false, ContextMenu::new, LayoutPos.Right, new Dimension(200,200));
			this.pointTablePanel = pointTablePanel;
			this.pointTablePanel.setMapView(sideComp);
			table.setPreferredScrollableViewportSize(table.getMinimumSize());
		}
		
		@Override
		protected void tableSelectionChanged(WreckArea row, int rowM)
		{
			pointTablePanel.setData(row);
			sideComp.setWreck(row);
			super.tableSelectionChanged(row, rowM);
		}
	
		private static class ContextMenu extends TableContextMenu
		{
			private static final long serialVersionUID = -798929965802654061L;
	
			public ContextMenu(JTable table, WreckAreaTableModel tableModel)
			{
				super(table);
				
				addSeparator();
				
				add(GUI.createMenuItem("Add empty area", e->{
					FarWreckAreas wreckAreas = FarWreckAreas.getInstance();
					wreckAreas.addEmptyArea();
					tableModel.updateData();
					wreckAreas.writeToFile();
				}));
			}
		}
	}
	
	private static class WreckAreaMapView extends ZoomableCanvas<ZoomableCanvas.ViewState>
	{
		private static final long serialVersionUID = -5273193934458901334L;
		private WreckArea wreckArea;

		WreckAreaMapView()
		{
			this.wreckArea = null;
			activateMapScale(MapPanel.COLOR_MAP_AXIS, "m", true);
			activateAxes(MapPanel.COLOR_MAP_AXIS, true,true,true,true);
			setBorder(BorderFactory.createLineBorder(Color.GRAY));
		}

		void setWreck(WreckArea wreckArea)
		{
			this.wreckArea = wreckArea;
			reset();
		}

		@Override
		protected void paintCanvas(Graphics g, int x, int y, int width, int height)
		{
			if (g instanceof Graphics2D g2 && viewState.isOk())
			{
				Shape prevClip = g2.getClip();
				Rectangle clip = new Rectangle(x, y, width, height);
				g2.setClip(clip);
				
				g2.setColor(MapPanel.COLOR_MAP_BACKGROUND);
				g2.fillRect(x, y, width, height);
				
				drawMapDecoration(g2, x, y, width, height);
				
				if (wreckArea!=null)
					MapPanel.MapView.drawWreckArea(g2, viewState, clip, wreckArea, false);
				
				g2.setClip(prevClip);
			}
		}

		@Override
		protected ViewState createViewState() {
			ViewState viewState = new ViewState(WreckAreaMapView.this, 0.1f) {
				@Override protected void determineMinMax(MapLatLong min, MapLatLong max) {
					if (wreckArea!=null && wreckArea.hasPoints())
					{
						min.longitude_x = Double.NaN;
						min.latitude_y  = Double.NaN;
						max.longitude_x = Double.NaN;
						max.latitude_y  = Double.NaN;
						wreckArea.foreachPoint(p -> {
							if (!Double.isFinite(min.longitude_x)) min.longitude_x = p.x; else min.longitude_x = Math.min(p.x, min.longitude_x);
							if (!Double.isFinite(min.latitude_y )) min.latitude_y  = p.y; else min.latitude_y  = Math.min(p.y, min.latitude_y );
							if (!Double.isFinite(max.longitude_x)) max.longitude_x = p.x; else max.longitude_x = Math.max(p.x, max.longitude_x);
							if (!Double.isFinite(max.latitude_y )) max.latitude_y  = p.y; else max.latitude_y  = Math.max(p.y, max.latitude_y );
						});
						if (!Double.isFinite(min.longitude_x)) min.longitude_x = 0.0;
						if (!Double.isFinite(min.latitude_y )) min.latitude_y  = 0.0;
						if (!Double.isFinite(max.longitude_x)) max.longitude_x = 100.0;
						if (!Double.isFinite(max.latitude_y )) max.latitude_y  = 100.0;
						
						double width  = max.longitude_x-min.longitude_x;
						double height = max.latitude_y -min.latitude_y ;
						min.longitude_x -= width *0.5;
						min.latitude_y  -= height*0.5;
						max.longitude_x += width *0.5;
						max.latitude_y  += height*0.5;
					}
					else
					{
						min.longitude_x = 0.0;
						min.latitude_y  = 0.0;
						max.longitude_x = 100.0;
						max.latitude_y  = 100.0;
					}
					
					if (min.longitude_x==max.longitude_x && min.latitude_y==max.latitude_y) {
						min.longitude_x -= 50;
						min.latitude_y  -= 50;
						max.longitude_x += 50;
						max.latitude_y  += 50;
					}
				}
			};
			viewState.setPlainMapSurface();
			viewState.setVertAxisDownPositive(false);
			viewState.setHorizAxisRightPositive(false);
			return viewState;
		}
	}

	private static class WreckAreaTableModel
		extends Tables.SimpleGetValueTableModel<WreckArea, WreckAreaTableModel.ColumnID>
		implements TablePanelWithTextArea.TableModelExtension<WreckArea>
	{
		// Column Widths: [22, 59, 450] in ModelOrder
		enum ColumnID implements Tables.SimpleGetValueTableModel.ColumnIDTypeInt<WreckArea>
		{
			Index          ("#"                    , Integer.class,  25, null),
			Editable       ("Editable"             , Boolean.class,  60, null),
			Visible        ("Visible"              , Boolean.class,  60, wa->wa.isVisible),
			AutoPointOrder ("Automatic Point Order", Boolean.class, 130, wa->wa.hasAutomaticPointOrder()),
			Points         ("Points"               , String .class, 450, ColumnID::getPointsAsString),
			;
			private SimplifiedColumnConfig cfg;
			private Function<WreckArea, ?> getValue;
			
			<ColumnClass> ColumnID(String name, Class<ColumnClass> colClass, int width, Function<WreckArea, ColumnClass> getValue) {
				this.getValue = getValue;
				cfg = new Tables.SimplifiedColumnConfig(name, colClass, 20, -1, width, width);
			}
			
			private static String getPointsAsString(WreckArea wa)
			{
				return wa.getPointsAsString(
						", ",
						p->String.format(Locale.ENGLISH, "(%1.2f, %1.2f)", p.x, p.y)
				);
			}
	
			@Override public SimplifiedColumnConfig getColumnConfig() { return cfg; }
			@Override public Function<WreckArea, ?> getGetValue() { return getValue; }
		}
		
		private final PointTableModel pointTableModel;
		
		WreckAreaTableModel(PointTableModel pointTableModel) {
			super(ColumnID.values(), FarWreckAreas.getInstance().getAreas());
			this.pointTableModel = pointTableModel;
		}
		
		void updateData()
		{
			setData(FarWreckAreas.getInstance().getAreas());
		}
	
		@Override
		public String getRowText(WreckArea row, int rowIndex)
		{
			if (row==null)
				return "";
			
			if (!row.hasPoints())
				return "no points";
			
			StringBuilder sb = new StringBuilder();
			
			row.foreachPoint(p -> {
				sb.append(String.format(Locale.ENGLISH, "(%1.4f, %1.4f)%n", p.x, p.y));
			});
			
			return sb.toString();
		}
	
		@Override
		protected Object getValueAt(int rowIndex, int columnIndex, ColumnID columnID, WreckArea row)
		{
			if (columnID == ColumnID.Index)
				return rowIndex+1;
			
			if (columnID == ColumnID.Editable)
				return FarWreckAreas.getInstance().getEditableArea() == row;
			
			return super.getValueAt(rowIndex, columnIndex, columnID, row);
		}
	
		@Override
		protected boolean isCellEditable(int rowIndex, int columnIndex, ColumnID columnID)
		{
			return
					columnID==ColumnID.Editable ||
					columnID==ColumnID.Visible ||
					columnID==ColumnID.AutoPointOrder ||
					super.isCellEditable(rowIndex, columnIndex, columnID);
		}
	
		@Override
		protected void setValueAt(Object aValue, int rowIndex, int columnIndex, ColumnID columnID)
		{
			WreckArea row = getRow(rowIndex);
			
			if (row!=null && columnID!=null)
				switch (columnID)
				{
				case Editable:
					FarWreckAreas.getInstance().setEditableArea(row);
					fireTableColumnUpdate(ColumnID.Editable);
					break;
					
				case AutoPointOrder:
					if (aValue instanceof Boolean boolVal)
					{
						row.setAutomaticPointOrder(boolVal.booleanValue());
						pointTableModel.fireTableUpdate();
						FarWreckAreas.getInstance().writeToFile();
					}
					break;
					
				case Visible:
					if (aValue instanceof Boolean boolVal)
					{
						row.isVisible = boolVal.booleanValue();
						FarWreckAreas.getInstance().writeToFile();
					}
					break;
					
				default:
					break;
				}
			
			super.setValueAt(aValue, rowIndex, columnIndex, columnID);
		}
	}

	private static class PointTablePanel extends TablePanelWithTextArea<Point2D.Double, PointTableModel.ColumnID, PointTableModel>
	{
		private static final long serialVersionUID = -6063503904777655451L;

		PointTablePanel() {
			super(new PointTableModel(), true, ContextMenu::new, LayoutPos.Right, new Dimension(200,200));
			table.setPreferredScrollableViewportSize(table.getMinimumSize());
		}
		
		void setMapView(WreckAreaMapView wreckAreaMapView)
		{
			if (tableContextMenu instanceof ContextMenu contextMenu)
				contextMenu.setMapView(wreckAreaMapView);
		}

		void setData(WreckArea wreckArea)
		{
			tableModel.setData(wreckArea);
			setText("");
		}

		private static class ContextMenu extends TableContextMenu
		{
			private static final long serialVersionUID = -2005702283744870615L;
			
			private int clickedRowIndex = -1;
			private Point2D.Double clickedRow = null;
			private WreckAreaMapView wreckAreaMapView = null;

			public ContextMenu(JTable table, PointTableModel tableModel)
			{
				super(table);
				
				addSeparator();
				
				JMenuItem miMovePointUp = add(GUI.createMenuItem("Move Point Up", GrayCommandIcons.IconGroup.Up, true, e->{
					tableModel.swapRows(clickedRowIndex, clickedRowIndex-1);
					if (wreckAreaMapView!=null) wreckAreaMapView.repaint();
					FarWreckAreas.getInstance().writeToFile();
				}));
				
				JMenuItem miMovePointDown = add(GUI.createMenuItem("Move Point Down", GrayCommandIcons.IconGroup.Down, true, e->{
					tableModel.swapRows(clickedRowIndex, clickedRowIndex+1);
					if (wreckAreaMapView!=null) wreckAreaMapView.repaint();
					FarWreckAreas.getInstance().writeToFile();
				}));
				
				JMenuItem miMoveListToEnd = add(GUI.createMenuItem("Move List to end at Point", GrayCommandIcons.IconGroup.Down, true, e->{
					tableModel.shiftListToEnd(clickedRowIndex);
					if (wreckAreaMapView!=null) wreckAreaMapView.repaint();
					FarWreckAreas.getInstance().writeToFile();
				}));
				
				JMenuItem miDeletePoint = add(GUI.createMenuItem("Delete Point", GrayCommandIcons.IconGroup.Delete, true, e->{
					String msg = String.format("Do you really want to delete point %d (%1.3f, %1.3f) ?", clickedRowIndex+1, clickedRow.x, clickedRow.y);
					String title = "Delete Point ?";
					if (JOptionPane.showConfirmDialog(table, msg, title, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION)
						return;
					
					tableModel.deleteRow(clickedRowIndex);
					if (wreckAreaMapView!=null) wreckAreaMapView.repaint();
					FarWreckAreas.getInstance().writeToFile();
				}));
				
				
				addContextMenuInvokeListener((comp,x,y) -> {
					int rowV = table.rowAtPoint(new Point(x,y));
					clickedRowIndex = rowV<0 ? -1 : table.convertRowIndexToModel(rowV);
					clickedRow = clickedRowIndex<0 ? null : tableModel.getRow(clickedRowIndex);
					
					miMovePointUp  .setEnabled(tableModel.wreckArea!=null && !tableModel.wreckArea.hasAutomaticPointOrder() && clickedRowIndex>0);
					miMovePointDown.setEnabled(tableModel.wreckArea!=null && !tableModel.wreckArea.hasAutomaticPointOrder() && clickedRowIndex>=0 && clickedRowIndex+1<tableModel.getRowCount());
					miMoveListToEnd.setEnabled(tableModel.wreckArea!=null && !tableModel.wreckArea.hasAutomaticPointOrder() && clickedRowIndex>=0 && clickedRowIndex  <tableModel.getRowCount());
					miDeletePoint  .setEnabled(clickedRow!=null);
					miMovePointUp  .setText( clickedRowIndex<0 ? "Move Point Up"   : "Move Point %d Up"  .formatted(clickedRowIndex+1) );
					miMovePointDown.setText( clickedRowIndex<0 ? "Move Point Down" : "Move Point %d Down".formatted(clickedRowIndex+1) );
					miDeletePoint  .setText( clickedRowIndex<0 ? "Delete Point"    : "Delete Point %d"   .formatted(clickedRowIndex+1) );
					miMoveListToEnd.setText( clickedRowIndex<0 ? "Move List to end at Point" : "Move List to end at Point %d".formatted(clickedRowIndex+1) );
				});
			}
			
			void setMapView(WreckAreaMapView wreckAreaMapView)
			{
				this.wreckAreaMapView = wreckAreaMapView;
			}
		}
	}
	
	private static class PointTableCellRenderer implements TableCellRenderer
	{
		private final Tables.LabelRendererComponent rendComp;
		private final PointTableModel tableModel;
		
		PointTableCellRenderer(PointTableModel tableModel)
		{
			this.tableModel = tableModel;
			rendComp = new Tables.LabelRendererComponent();
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int rowV, int columnV)
		{
			int    rowM =    rowV<0 ? -1 : table.convertRowIndexToModel(rowV);
			int columnM = columnV<0 ? -1 : table.convertColumnIndexToModel(columnV);
			PointTableModel.ColumnID columnID = tableModel.getColumnID(columnM);
			Point2D.Double row = tableModel.getRow(rowM);
			
			String valueStr;
			if (row!=null && columnID!=null && columnID.getDisplayStr!=null)
				valueStr = columnID.getDisplayStr.apply(row);
			else
				valueStr = value==null ? null : value.toString();
			
			rendComp.configureAsTableCellRendererComponent(table, null, valueStr, isSelected, hasFocus);
			if (columnID!=null && columnID.cfg.columnClass!=null && Number.class.isAssignableFrom(columnID.cfg.columnClass))
				rendComp.setHorizontalAlignment(SwingConstants.RIGHT);
			else
				rendComp.setHorizontalAlignment(SwingConstants.LEFT);
			
			return rendComp;
		}
		
	}
	
	private static class PointTableModel
		extends Tables.SimpleGetValueTableModel<Point2D.Double, PointTableModel.ColumnID>
		implements TablePanelWithTextArea.TableModelExtension<Point2D.Double>
	{
		enum ColumnID implements Tables.SimpleGetValueTableModel.ColumnIDTypeInt<Point2D.Double>
		{
			Index ("#", Integer.class,  25, null, null),
			X     ("X", Double .class,  80, p->p.x, p->String.format(Locale.ENGLISH, "%1.3f", p.x)),
			Y     ("Y", Double .class,  80, p->p.y, p->String.format(Locale.ENGLISH, "%1.3f", p.y)),
			;			
			private SimplifiedColumnConfig cfg;
			private Function<Point2D.Double, ?> getValue;
			private Function<Point2D.Double, String> getDisplayStr;
			
			<ColumnClass> ColumnID(String name, Class<ColumnClass> colClass, int width, Function<Point2D.Double, ColumnClass> getValue, Function<Point2D.Double, String> getDisplayStr) {
				this.getValue = getValue;
				this.getDisplayStr = getDisplayStr;
				cfg = new Tables.SimplifiedColumnConfig(name, colClass, 20, -1, width, width);
			}
	
			@Override public SimplifiedColumnConfig getColumnConfig() { return cfg; }
			@Override public Function<Point2D.Double, ?> getGetValue() { return getValue; }
		}
		
		private WreckArea wreckArea;
		
		PointTableModel() {
			super(ColumnID.values(), new Vector<>());
			wreckArea = null;
		}

		void setData(WreckArea wreckArea)
		{
			this.wreckArea = wreckArea;
			setData(this.wreckArea==null ? null : this.wreckArea.points);
		}

		void shiftListToEnd(int rowIndex)
		{
			if (wreckArea==null) return;
			wreckArea.shiftListToEnd(rowIndex);
			fireTableUpdate();
		}

		void swapRows(int rowIndex1, int rowIndex2)
		{
			if (wreckArea==null) return;
			wreckArea.swapPoints(rowIndex1, rowIndex2);
			fireTableRowUpdate(rowIndex1);
			fireTableRowUpdate(rowIndex2);
		}

		void deleteRow(int rowIndex)
		{
			if (wreckArea==null) return;
			wreckArea.deletePoint(rowIndex);
			fireTableRowRemoved(rowIndex);
		}

		@Override
		public void setDefaultCellEditorsAndRenderers()
		{
			PointTableCellRenderer ptcr = new PointTableCellRenderer(this);
			setDefaultRenderers(clazz -> ptcr);
			super.setDefaultCellEditorsAndRenderers();
		}

		@Override
		public String getRowText(Point2D.Double row, int rowIndex)
		{
			return row==null ? "" : String.format(Locale.ENGLISH, "(%1.6f, %1.6f)", row.x, row.y);
		}

		@Override
		protected Object getValueAt(int rowIndex, int columnIndex, ColumnID columnID, Point2D.Double row)
		{
			if (columnID == ColumnID.Index)
				return rowIndex+1;
			
			return super.getValueAt(rowIndex, columnIndex, columnID, row);
		}
	}
	
}
