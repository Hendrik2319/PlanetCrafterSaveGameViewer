package net.schwarzbaer.java.games.planetcrafter.savegameviewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;
import java.util.function.Predicate;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.WorldObject;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.MapShapes.MapShape;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypes.ObjectTypeValue;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypesPanel.ObjectTypesChangeEvent;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypesPanel.ObjectTypesChangeListener;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.PlanetCrafterSaveGameViewer.AppSettings;
import net.schwarzbaer.java.lib.gui.Canvas;
import net.schwarzbaer.java.lib.gui.ContextMenu;
import net.schwarzbaer.java.lib.gui.FileChooser;
import net.schwarzbaer.java.lib.gui.StandardDialog;
import net.schwarzbaer.java.lib.gui.ZoomableCanvas;
import net.schwarzbaer.java.lib.image.linegeometry.Form;
import net.schwarzbaer.java.lib.system.ClipboardTools;
import net.schwarzbaer.java.tools.lineeditor.LineEditor;

class MapPanel extends JSplitPane implements ObjectTypesChangeListener {
	private static final long serialVersionUID = 1367855618848983614L;
	
	static final Color COLOR_OVERVIEW_SCREEN     = Color.RED;
	static final Color COLOR_MAP_AXIS            = new Color(0x70000000,true);
	static final Color COLOR_MAP_BORDER          = COLOR_MAP_AXIS;
	static final Color COLOR_MAP_BACKGROUND      = Color.WHITE;
	static final Color COLOR_TOOLTIP_BORDER      = new Color(0x70000000,true);
	static final Color COLOR_TOOLTIP_BACKGORUND  = new Color(0xFFFFE9);
	static final Color COLOR_TOOLTIP_TEXT        = Color.BLACK;
	static final Color COLOR_PLAYERPOS           = Color.RED;
	static final Color COLOR_WORLDOBJECT_CONTOUR = new Color(0x70000000,true);
	static final Color COLOR_WORLDOBJECT_FILL             = Color.LIGHT_GRAY;
	static final Color COLOR_WORLDOBJECT_FILL_REMOVAL     = null;
	static final Color COLOR_WORLDOBJECT_FILL_HOVERED     = new Color(0xFFDD00);
	static final Color COLOR_WORLDOBJECT_FILL_EXTRA_SHOWN = Color.RED;
	static final Color COLOR_WORLDOBJECT_FILL_HIGHLIGHT_FOUND = Color.GREEN;
	static final Color COLOR_WORLDOBJECT_FILL_HIGHLIGHT_00    = Color.GREEN;
	static final Color COLOR_WORLDOBJECT_FILL_HIGHLIGHT_05    = Color.YELLOW;
	static final Color COLOR_WORLDOBJECT_FILL_HIGHLIGHT_10    = Color.RED;
	static final Color COLOR_WORLDOBJECT_FILL_HIGHLIGHT_MAX   = new Color(0x00BFFF);
	static final Color COLOR_MAPSHAPE_BASE = new Color(0xD0D0D0);



	private enum ColoringType {
		StoragesFillingLevel ("Filling Level of Storages"),
		ProducersFillingLevel("Filling Level of Producers"),
		GrowthState          ("Growth State"),
		FindInstalledObject  ("Find installed Object"),
		FindStoredObject     ("Find stored Object"),
		ObjectType           ("Object Type"),
		;
		private final String text;
		ColoringType(String text) {
			this.text = text;
		}
		@Override public String toString() {
			return text;
		}
		
	}
	
	private final PlanetCrafterSaveGameViewer main;
	private final MapModel mapModel;
	private final MapView mapView;
	private final MapBackgroundImage mapBackgroundImage;
	private final JComboBox<String> cmbbxObjLabels;
	private final JComboBox<ColoringType> cmbbxColoring;
	private ColoringType selectedColoringType;

	MapPanel(PlanetCrafterSaveGameViewer main, Data data) {
		super(MapPanel.HORIZONTAL_SPLIT, true);
		this.main = main;
		
		OverView overView = new OverView();
		overView.setPreferredSize(200,150);
		
		JTextArea textOut = new JTextArea();
		
		mapModel = new MapModel(data);
		mapView = new MapView(this.main.mapShapes, mapModel, overView, textOut);
		mapBackgroundImage = new MapBackgroundImage(mapView);
		new MapContextMenu(mapView, this.main, mapBackgroundImage);
		
		cmbbxColoring = new JComboBox<>(ColoringType.values());
		cmbbxColoring.setSelectedItem(selectedColoringType = ColoringType.FindInstalledObject);
		
		cmbbxObjLabels = new JComboBox<>(mapModel.installedObjectLabels);
		cmbbxObjLabels.setSelectedItem(null);
		
		cmbbxColoring.addActionListener(e->{
			selectedColoringType = cmbbxColoring.getItemAt(cmbbxColoring.getSelectedIndex());
			
			if (selectedColoringType==null)
				cmbbxObjLabels.setEnabled(false);
			else {
				switch (selectedColoringType) {
				case FindInstalledObject:
					cmbbxObjLabels.setEnabled(true);
					cmbbxObjLabels.setModel(new DefaultComboBoxModel<>(mapModel.installedObjectLabels));
					break;
					
				case FindStoredObject:
					cmbbxObjLabels.setEnabled(true);
					cmbbxObjLabels.setModel(new DefaultComboBoxModel<>(mapModel.storedObjectLabels));
					break;
					
				case ProducersFillingLevel:
				case StoragesFillingLevel:
				case GrowthState:
					cmbbxObjLabels.setEnabled(false);
					break;
					
				case ObjectType:
					cmbbxObjLabels.setEnabled(false);
					HashMap<String, Color> objectTypeColors = new GUI.ObjectTypeColorsDialog(this.main, "Object Type Colors").showDialogAndGetColors();
					mapModel.setObjectTypeColors(objectTypeColors);
					break;
				}
				mapView.setExtraShownObject(null);
			}
			cmbbxObjLabels.setSelectedItem(null);
			mapModel.setColoring(selectedColoringType, null);
			mapView.repaint();
		});
		
		cmbbxObjLabels.addActionListener(e->{
			String selectedObjLabel = cmbbxObjLabels.getItemAt(cmbbxObjLabels.getSelectedIndex());
			if (selectedColoringType==ColoringType.FindInstalledObject || selectedColoringType==ColoringType.FindStoredObject) {
				mapModel.setColoring(selectedColoringType, selectedObjLabel);
				mapView.setExtraShownObject(null);
				mapView.repaint();
			}
		});
		
		JPanel selectPanel = new JPanel(new GridLayout(0,1));
		selectPanel.add(cmbbxColoring);
		selectPanel.add(cmbbxObjLabels);
		
		JScrollPane textScrollPane = new JScrollPane(textOut);
		textScrollPane.setPreferredSize(new Dimension(300,400));
		
		JPanel lowerLeftPanel = new JPanel(new BorderLayout());
		lowerLeftPanel.add(selectPanel, BorderLayout.NORTH);
		lowerLeftPanel.add(textScrollPane, BorderLayout.CENTER);
		
		JPanel leftPanel = new JPanel(new BorderLayout());
		leftPanel.add(overView, BorderLayout.NORTH);
		leftPanel.add(lowerLeftPanel, BorderLayout.CENTER);
		
		setLeftComponent(leftPanel);
		setRightComponent(mapView);
		setDividerLocation(300);
		
		//mapView       .setBorder(BorderFactory.createTitledBorder("Map"));
		//textScrollPane.setBorder(BorderFactory.createTitledBorder("Current Object (Yellow)"));
		mapView       .setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Map"), BorderFactory.createLineBorder(Color.GRAY)));
		textScrollPane.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Object under Mouse"), textScrollPane.getBorder()));
		selectPanel   .setBorder(BorderFactory.createTitledBorder("Coloring / Highlighting"));
		overView      .setBorder(BorderFactory.createTitledBorder("OverView"));
	}
	
	void initialize() {
		mapView.reset();
		//System.out.printf("MapPanel.initialize() -> MapView.reset() -> ViewStateOk? %s%n", mapView.isViewStateOk());
		mapBackgroundImage.initialize(main.mainWindow);
	}

	void showWorldObject(WorldObject wo) {
		cmbbxColoring.setSelectedItem(null);
		cmbbxObjLabels.setSelectedItem(null);
		mapView.setExtraShownObject(wo);
		mapView.repaint();
	}

	@Override
	public void objectTypesChanged(ObjectTypesChangeEvent event) {
		if (event.eventType != ObjectTypesChangeEvent.EventType.ValueChanged)
			return;
		
		if (ObjectTypeValue.isLabel( event.changedValue )) {
			mapModel.updateInstalledObjectLabels();
			mapModel.updateStoredObjectLabels();
			
			if (selectedColoringType==ColoringType.FindInstalledObject)
				cmbbxObjLabels.setModel(new DefaultComboBoxModel<>(mapModel.installedObjectLabels));
			
			else if (selectedColoringType==ColoringType.FindStoredObject)
				cmbbxObjLabels.setModel(new DefaultComboBoxModel<>(mapModel.storedObjectLabels));
			
			cmbbxObjLabels.setSelectedItem(null);
		}
		
		mapView.repaint();
	}

	private static class OverView extends Canvas {
		private static final long serialVersionUID = 4760409371179475061L;
		private Rectangle2D.Double range = null;
		private Rectangle2D.Double screen = null;

		@Override
		protected void paintCanvas(Graphics g, int x, int y, int width, int height) {
//			g.setColor(Color.BLACK);
//			g.drawRect(x, y, width-1, height-1);
//			if (width>10 && height>10) {
//				g.setColor(Color.white);
//				g.fillRect(x+5, y+5, width-10, height-10);
//			}
			
			if (range==null && screen==null)
				return;
			
			Rectangle2D.Double max = new Rectangle2D.Double();
			if (range !=null) max.add(range );
			if (screen!=null) max.add(screen);
			
			double scale = Math.min( width/max.width, height/max.height ); // PxPerMapUnit
			
			double offsetX = width /2.0 - (max.x+max.width /2)*scale;
			double offsetY = height/2.0 - (max.y+max.height/2)*scale;
			
			if (range!=null) {
				int rx = (int)Math.round(range.x*scale + offsetX);
				int ry = (int)Math.round(range.y*scale + offsetY);
				int rw = (int)Math.round(range.width *scale);
				int rh = (int)Math.round(range.height*scale);
				rx = x+width  - rx-rw;
				ry = y+height - ry-rh;
				g.setColor(COLOR_MAP_BACKGROUND);
				g.fillRect(rx, ry, rw, rh);
				g.setColor(COLOR_MAP_BORDER);
				g.drawRect(rx-1, ry-1, rw+1, rh+1);
			}
			
			if (screen!=null) {
				g.setColor(COLOR_OVERVIEW_SCREEN);
				int rx = (int)Math.round(screen.x*scale + offsetX);
				int ry = (int)Math.round(screen.y*scale + offsetY);
				int rw = (int)Math.round(screen.width *scale);
				int rh = (int)Math.round(screen.height*scale);
				rx = x+width  - rx-rw;
				ry = y+height - ry-rh;
				g.drawRect(rx,ry,rw-1,rh-1);
			}
		}

		void setRange(Rectangle2D.Double range) {
			this.range = range;
			repaint();
		}

		void setScreen(Rectangle2D.Double screen) {
			this.screen = screen;
			repaint();
		}
	}
	
	static class MapWorldObjectData {
		private final HashSet<String> storedObjectLabels = new HashSet<>();
	}
	
	private static class MapModel {
		
		final Data.Coord3 playerPosition;
		final Data.Rotation playerOrientation;
		final Vector<WorldObject> displayableObjects;
		final MinMax minmax;
		final Rectangle2D.Double range;
		final Vector<String> installedObjectLabels;
		final Vector<String> storedObjectLabels;
		private ColoringType coloringType;
		private String objLabel;
		private HashMap<String, Color> objectTypeColors;
		
		MapModel(Data data) {
			displayableObjects = new Vector<>();
			installedObjectLabels = new Vector<>();
			storedObjectLabels = new Vector<>();
			objectTypeColors = null;
			
			// ----------------------------------------------------------------
			// playerPositions & displayableObjects & min,max
			// ----------------------------------------------------------------
			MinMax minmax = null;
			
			if (data.playerStates.isPositioned()) {
				playerPosition    = data.playerStates.position;
				playerOrientation = data.playerStates.rotation;
				minmax = new MinMax(playerPosition);
			} else {
				playerPosition = null;
				playerOrientation = null;
			}
			
			for (WorldObject wo : data.worldObjects) {
				if (!wo.isInstalled()) continue;
				displayableObjects.add(wo);
				if (minmax==null) minmax = new MinMax(wo.position);
				else              minmax.change(wo.position);
			}
			this.minmax = minmax;
			
			// ----------------------------------------------------------------
			// range
			// ----------------------------------------------------------------
			if (this.minmax!=null) {
				double width  = this.minmax.maxX - this.minmax.minX;
				double height = this.minmax.maxY - this.minmax.minY;
				double maxLength = Math.max(width, height); 
				if (maxLength>0) {
					double border = maxLength/10;
					range = new Rectangle2D.Double(this.minmax.minX-border, this.minmax.minY-border, width+2*border, height+2*border);
				} else
					range = null;
			} else
				range = null;
			
			// ----------------------------------------------------------------
			// WorldObject.mapWorldObjectData
			// ----------------------------------------------------------------
			//for (WorldObject wo : displayableObjects) {
			//	
			//}
			
			// ----------------------------------------------------------------
			// Label Lists
			// ----------------------------------------------------------------
			updateInstalledObjectLabels();
			updateStoredObjectLabels();
		}
		
		void setObjectTypeColors(HashMap<String, Color> objectTypeColors) {
			this.objectTypeColors = objectTypeColors;
		}

		void setColoring(ColoringType coloringType, String objLabel) {
			this.coloringType = coloringType;
			this.objLabel = objLabel;
		}

		boolean isHighlighted(WorldObject wo)
		{
			if (coloringType != null)
				switch (coloringType)
				{
					case FindInstalledObject  : return wo.getName().equals(objLabel);
					case FindStoredObject     : return wo.mapWorldObjectData.storedObjectLabels.contains(objLabel);
					case ProducersFillingLevel: return wo.objectType != null && wo.objectType.isProducer && wo.list != null;
					case StoragesFillingLevel : return wo.list != null;
					case ObjectType           : return wo.objectType != null && objectTypeColors != null && objectTypeColors.get(wo.objectType.id) != null;
					case GrowthState          : return wo.growth > 0;
				}
			return false;
		}
		
		Color getHighlightColor(WorldObject wo) {
			if (coloringType!=null)
				switch (coloringType) {
				
				case FindInstalledObject: case FindStoredObject:
					return COLOR_WORLDOBJECT_FILL_HIGHLIGHT_FOUND;
					
				case GrowthState:
					if (wo.growth == 100)
						return COLOR_WORLDOBJECT_FILL_HIGHLIGHT_MAX;
					return getMixedColor(wo.growth / 100.0,
							COLOR_WORLDOBJECT_FILL_HIGHLIGHT_00,
							COLOR_WORLDOBJECT_FILL_HIGHLIGHT_05,
							COLOR_WORLDOBJECT_FILL_HIGHLIGHT_10);
					
				case ProducersFillingLevel: case StoragesFillingLevel:
					if (wo.list==null) return null; // shouldn't be
					if (wo.list.worldObjIds.length == wo.list.size)
						return COLOR_WORLDOBJECT_FILL_HIGHLIGHT_MAX;
					return getMixedColor(wo.list.worldObjIds.length / (double)wo.list.size,
							COLOR_WORLDOBJECT_FILL_HIGHLIGHT_00,
							COLOR_WORLDOBJECT_FILL_HIGHLIGHT_05,
							COLOR_WORLDOBJECT_FILL_HIGHLIGHT_10);
				
				case  ObjectType:
					if (wo.objectType==null || objectTypeColors==null) return null; // shouldn't be
					return objectTypeColors.get(wo.objectType.id);
				}
			return null;
		}

		private Color getMixedColor(double value, Color color00, Color color05, Color color10) {
			value = Math.min(Math.max(0, value), 1);
			return value<0.5
					? computeColor(color00, color05, 2* value     )
					: computeColor(color05, color10, 2*(value-0.5));
		}

		private Color computeColor(Color color0, Color color1, double f) {
			int r = (int)Math.round( color0.getRed  ()*(1-f) + color1.getRed  ()*f );
			int g = (int)Math.round( color0.getGreen()*(1-f) + color1.getGreen()*f );
			int b = (int)Math.round( color0.getBlue ()*(1-f) + color1.getBlue ()*f );
			return new Color(r,g,b);
		}

		void updateInstalledObjectLabels() {
			HashSet<String> labels = new HashSet<>();
			for (WorldObject wo : displayableObjects) {
				labels.add(wo.getName());
			}
			installedObjectLabels.clear();
			installedObjectLabels.addAll(labels);
			installedObjectLabels.sort(Data.caseIgnoringComparator);
		}
		
		void updateStoredObjectLabels() {
			HashSet<String> labels = new HashSet<>();
			for (WorldObject wo : displayableObjects) {
				wo.mapWorldObjectData.storedObjectLabels.clear();
				if (wo.list!=null) {
					for (WorldObject storedObj : wo.list.worldObjs) {
						String soName = storedObj.getName();
						wo.mapWorldObjectData.storedObjectLabels.add(soName);
						labels.add(soName);
					}
				}
			}
			storedObjectLabels.clear();
			storedObjectLabels.addAll(labels);
			storedObjectLabels.sort(Data.caseIgnoringComparator);
		}

		WorldObject getNearestObject(double x, double y, Predicate<Double> checkMaxDist) {
			double minSquaredDist = Double.NaN;
			WorldObject nearestObj = null;
			for (WorldObject wo : displayableObjects) {
				double woX = wo.position.getMapX();
				double woY = wo.position.getMapY();
				double squaredDist = (woX-x)*(woX-x) + (woY-y)*(woY-y);
				if (nearestObj==null || minSquaredDist > squaredDist) {
					minSquaredDist = squaredDist;
					nearestObj = wo;
				}
			}
			if (nearestObj==null)
				return null;
			
			double dist = Math.sqrt(minSquaredDist);
			if (!checkMaxDist.test(dist))
				return null;
			
			return nearestObj;
		}

		private static class MinMax {
			
			double minX;
			double minY;
			double maxX;
			double maxY;
			
			MinMax(Data.Coord3 pos) {
				double woX = pos.getMapX();
				double woY = pos.getMapY();
				minX = woX;
				minY = woY;
				maxX = woX;
				maxY = woY;
			}
			void change(Data.Coord3 pos) {
				double woX = pos.getMapX();
				double woY = pos.getMapY();
				if (minX > woX) minX = woX;
				if (minY > woY) minY = woY;
				if (maxX < woX) maxX = woX;
				if (maxY < woY) maxY = woY;
			}
		}
	}
	
	private static class MapContextMenu extends ContextMenu {
		private static final long serialVersionUID = 8109374615040559202L;
		
		private final MapBackgroundImage.ConfigureDialog mapBackgroundImageConfigureDialog;
		private Point clickedPoint;
		private WorldObject clickedObject;
		
		MapContextMenu(MapView mapView, PlanetCrafterSaveGameViewer main, MapBackgroundImage mapBackgroundImage) {
			clickedPoint = null;
			clickedObject = null;
			
			mapBackgroundImageConfigureDialog = mapBackgroundImage.createConfigureDialog(main.mainWindow, "Configure Map Background Image");
			
			JMenuItem miCopyPosNRotToClipboard = add(GUI.createMenuItem("Copy position & rotation to clipboard", e->{
				if (clickedObject==null) return;
				String coordsStr1 = clickedObject.position==null ? "<null>" : clickedObject.position.toString();
				String coordsStr2 = clickedObject.rotation==null ? "<null>" : clickedObject.rotation.toString();
				String msg = String.format("Position/Rotation of \"%s\":  %s  /  %s", clickedObject.getName(), coordsStr1, coordsStr2);
				System.out.println(msg);
				ClipboardTools.copyToClipBoard(msg);
			}));
			JMenuItem miCopyPosToClipboard = add(GUI.createMenuItem("Copy position to clipboard", e->{
				if (clickedObject==null) return;
				String coordsStr = clickedObject.position==null ? "<null>" : clickedObject.position.toString();
				String msg = String.format("Position of \"%s\": %s", clickedObject.getName(), coordsStr);
				System.out.println(msg);
				ClipboardTools.copyToClipBoard(msg);
			}));
			JMenuItem miCopyRotToClipboard = add(GUI.createMenuItem("Copy rotation to clipboard", e->{
				if (clickedObject==null) return;
				String coordsStr = clickedObject.rotation==null ? "<null>" : clickedObject.rotation.toString();
				String msg = String.format("Rotation of \"%s\": %s", clickedObject.getName(), coordsStr);
				System.out.println(msg);
				ClipboardTools.copyToClipBoard(msg);
			}));
			
//			addSeparator();
//			
//			JMenuItem miMeasureDistanceFrom = add(GUI.createMenuItem("Measure Distance From", e->{
//				if (clickedObject==null) return;
//				String coordsStr = clickedObject.rotation==null ? "<null>" : clickedObject.rotation.toString();
//				String msg = String.format("Rotation of \"%s\": %s", clickedObject.getName(), coordsStr);
//				System.out.println(msg);
//				ClipboardTools.copyToClipBoard(msg);
//			}));
			
			addSeparator();
			
			
			add(GUI.createMenuItem("Configure Map Background Image", e->{
				mapBackgroundImageConfigureDialog.showDialog();
			}));
			
			JMenu menuBackgroundImageFixPoint = new JMenu("Set Background Image Fix Point");
			add(menuBackgroundImageFixPoint);
			menuBackgroundImageFixPoint.add(GUI.createMenuItem("Map Point 1 (Base)"     ,e->mapBackgroundImage.setPoint(MapBackgroundImage.MapBGPoint.Map1  , clickedPoint)));
			menuBackgroundImageFixPoint.add(GUI.createMenuItem("Map Point 2 (Scaling)"  ,e->mapBackgroundImage.setPoint(MapBackgroundImage.MapBGPoint.Map2  , clickedPoint)));
			menuBackgroundImageFixPoint.add(GUI.createMenuItem("Image Point 1 (Base)"   ,e->mapBackgroundImage.setPoint(MapBackgroundImage.MapBGPoint.Image1, clickedPoint)));
			menuBackgroundImageFixPoint.add(GUI.createMenuItem("Image Point 2 (Scaling)",e->mapBackgroundImage.setPoint(MapBackgroundImage.MapBGPoint.Image2, clickedPoint)));
			
			
			addSeparator();
			
			JMenuItem miEditMapShapes = add(GUI.createMenuItem("Create/Edit MapShapes", e->{
				if (clickedObject==null) return;
				main.showMapShapesEditor(clickedObject.objectType);
			}));
			JMenuItem miMarkForRemoval = add(GUI.createMenuItem("Mark hovered object for removal", e->{
				if (clickedObject==null || !clickedObject.canMarkedByUser()) return;
				clickedObject.markForRemoval( !clickedObject.isMarkedForRemoval(), true );
				Data.notifyAllRemoveStateListeners();
			}));
			
			addContextMenuInvokeListener((comp, x, y) -> {
				clickedPoint = new Point(x,y);
				clickedObject = mapView.hoveredObject;
				miMarkForRemoval    .setEnabled(clickedObject!=null && clickedObject.canMarkedByUser());
				miCopyPosNRotToClipboard.setEnabled(clickedObject!=null);
				miCopyPosToClipboard.setEnabled(clickedObject!=null);
				miCopyRotToClipboard.setEnabled(clickedObject!=null);
				miEditMapShapes     .setEnabled(clickedObject!=null);
				miMarkForRemoval.setText(
						clickedObject == null
							? "Mark hovered object for removal"
							: clickedObject.isMarkedForRemoval()
								? String.format("Remove Removal Marker from \"%s\"", clickedObject.getName())
								: String.format("Mark \"%s\" for removal", clickedObject.getName())
				);
				miCopyPosNRotToClipboard.setText(
						clickedObject == null
							? "Copy position & rotation to clipboard"
							: String.format("Copy position & rotation of \"%s\" to clipboard", clickedObject.getName())
				);
				miCopyPosToClipboard.setText(
						clickedObject == null
							? "Copy position to clipboard"
							: String.format("Copy position of \"%s\" to clipboard", clickedObject.getName())
				);
				miCopyRotToClipboard.setText(
						clickedObject == null
							? "Copy rotation to clipboard"
							: String.format("Copy rotation of \"%s\" to clipboard", clickedObject.getName())
				);
				miEditMapShapes.setText(
						clickedObject == null
							? "Create/Edit MapShapes"
							: main.mapShapes.hasShapes(clickedObject.objectType)
								? String.format(  "Edit MapShapes of \"%s\"", clickedObject.getName())
								: String.format("Create MapShapes of \"%s\"", clickedObject.getName())
				);
				
			});
			addTo(mapView);
		}
	}
	
	static class MapBackgroundImage
	{
		enum MapBGPoint {
			Map1(
					AppSettings.ValueKey.MapBackgroundImage_FixPoint_Map1X,
					AppSettings.ValueKey.MapBackgroundImage_FixPoint_Map1Y
			),
			Map2(
					AppSettings.ValueKey.MapBackgroundImage_FixPoint_Map2X,
					AppSettings.ValueKey.MapBackgroundImage_FixPoint_Map2Y
			),
			Image1(
					AppSettings.ValueKey.MapBackgroundImage_FixPoint_Image1X,
					AppSettings.ValueKey.MapBackgroundImage_FixPoint_Image1Y
			),
			Image2(
					AppSettings.ValueKey.MapBackgroundImage_FixPoint_Image2X,
					AppSettings.ValueKey.MapBackgroundImage_FixPoint_Image2Y
			);
			private final AppSettings.ValueKey keyX;
			private final AppSettings.ValueKey keyY;

			MapBGPoint(AppSettings.ValueKey keyX, AppSettings.ValueKey keyY) {
				this.keyX = keyX;
				this.keyY = keyY;
			}
		}
		
		static final String FILE_MAPBGIMAGE_EXT    = "png";
		static final String FILE_MAPBGIMAGE_FORMAT = "png";
		private static final int BRIGHTNESS_MIN = -100;
		private static final int BRIGHTNESS_MAX =  100;
		private static final int CONTRAST_MIN   = -100;
		private static final int CONTRAST_MAX   =  100;
		
		private static final AppSettings.ValueKey SETTINGSKEY_CONTRAST   = AppSettings.ValueKey.MapBackgroundImage_Contrast;
		private static final AppSettings.ValueKey SETTINGSKEY_BRIGHTNESS = AppSettings.ValueKey.MapBackgroundImage_Brightness;
		
		private final MapView mapView;
		private final MapView.ViewState mapViewState;
		private final File storedImageFile;
		private final FileChooser imageFC;
		private BufferedImage mapBgImageBase;
		private BufferedImage mapBgImage;
		private int brightness;
		private int contrast;
		private FixPoint fixPoint1;
		private FixPoint fixPoint2;
		
		MapBackgroundImage(MapView mapView) {
			this.mapView = mapView;
			storedImageFile = new File(PlanetCrafterSaveGameViewer.FILE_MAPBGIMAGE);
			imageFC = new FileChooser("PNG-File", "png");
			mapBgImageBase = null;
			mapBgImage = null;
			brightness = PlanetCrafterSaveGameViewer.settings.getInt(SETTINGSKEY_BRIGHTNESS, 0);
			contrast   = PlanetCrafterSaveGameViewer.settings.getInt(SETTINGSKEY_CONTRAST  , 0);
			mapViewState = this.mapView.setBgImage(this);
			fixPoint1 = null;
			fixPoint2 = null;
		}
		
		void setPoint(MapBGPoint pointID, Point screenPoint)
		{
			// TODO Auto-generated method stub
			
		}

		void initialize(Component errDlgParent) {
			if (storedImageFile.isFile()) {
				System.out.printf("Read Map Background Image from file \"%s\"%n", storedImageFile.getAbsolutePath());
				mapBgImageBase = runIOExceptionTask(
						()->ImageIO.read(storedImageFile),
						String.format("reading image from file \"%s\":", storedImageFile.getAbsolutePath()),
						errDlgParent, "Read Error",
						null
				);
				if (mapBgImageBase!=null) {
					System.out.printf("   %d bytes read%n", storedImageFile.length());
				}
				
				if (mapBgImageBase!=null && mapViewState.isOk()) {
					double defaultMap1X = mapViewState.convertLength_ScreenToAngle_LongX(0);
					double defaultMap1Y = mapViewState.convertLength_ScreenToAngle_LatY (0);
					double defaultMap2X = mapViewState.convertLength_ScreenToAngle_LongX(mapView.getWidth ());
					double defaultMap2Y = mapViewState.convertLength_ScreenToAngle_LatY (mapView.getHeight());
					double imageWidth  = mapBgImageBase.getWidth();
					double imageHeight = mapBgImageBase.getHeight();
					
					fixPoint1 = new FixPoint(MapBGPoint.Image1, MapBGPoint.Map1);
					fixPoint2 = new FixPoint(MapBGPoint.Image2, MapBGPoint.Map2);
					fixPoint1.getValuesFromSettings(      0   ,       0   ,defaultMap1X,defaultMap1Y);
					fixPoint2.getValuesFromSettings(imageWidth,imageHeight,defaultMap2X,defaultMap2Y);
					
				} else {
					fixPoint1 = null;
					fixPoint2 = null;
				}
				
				computeImage();
				mapView.repaint();
			}
		}
		
		private void setBaseImage(BufferedImage image)
		{
			mapBgImageBase = image;
			
			if (mapBgImageBase!=null && mapViewState.isOk()) {
				double defaultMap1X = mapViewState.convertLength_ScreenToAngle_LongX(0);
				double defaultMap1Y = mapViewState.convertLength_ScreenToAngle_LatY (0);
				double defaultMap2X = mapViewState.convertLength_ScreenToAngle_LongX(mapView.getWidth ());
				double defaultMap2Y = mapViewState.convertLength_ScreenToAngle_LatY (mapView.getHeight());
				double imageWidth  = mapBgImageBase.getWidth();
				double imageHeight = mapBgImageBase.getHeight();
				
				fixPoint1 = new FixPoint(MapBGPoint.Image1, MapBGPoint.Map1);
				fixPoint2 = new FixPoint(MapBGPoint.Image2, MapBGPoint.Map2);
				fixPoint1.setValues(      0   ,       0   ,defaultMap1X,defaultMap1Y);
				fixPoint2.setValues(imageWidth,imageHeight,defaultMap2X,defaultMap2Y);
				
			} else {
				fixPoint1 = null;
				fixPoint2 = null;
			}
			
			setImageValues(0, 0);
		}

		private void computeImage()
		{
			if (mapBgImageBase==null)
			{
				mapBgImage = null;
				return;
			}
			
			System.out.printf("MapBackgroundImage.computeImage( brightness: %d, contrast: %d )%n", brightness, contrast);
			int width  = mapBgImageBase.getWidth();
			int height = mapBgImageBase.getHeight();
			
			mapBgImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			WritableRaster sourceRaster = mapBgImageBase.getRaster();
			WritableRaster targetRaster = mapBgImage    .getRaster();
			
			int[] pixel = new int[] {0,0,0,0}; // [r,g,b,a ]
			for (int x=0; x<width; x++)
				for (int y=0; y<height; y++) {
					sourceRaster.getPixel(x, y, pixel);
					computeContrast  (pixel);
					computeBrightness(pixel);
					targetRaster.setPixel(x, y, pixel);
				}
			//sourceRaster.getPixel(width/2, height/2, pixel);
			//System.out.printf("SourceRaster.Pixel( %d, %d ) -> %s%n", width/2, height/2, Arrays.toString(pixel));
			//targetRaster.getPixel(width/2, height/2, pixel);
			//System.out.printf("TargetRaster.Pixel( %d, %d ) -> %s%n", width/2, height/2, Arrays.toString(pixel));
		}

		private void computeBrightness(int[] pixel)
		{
			if (brightness < 0) {
				float f = 1 - brightness/(float)BRIGHTNESS_MIN;
				pixel[0] = Math.round( f*pixel[0] );
				pixel[1] = Math.round( f*pixel[1] );
				pixel[2] = Math.round( f*pixel[2] );
				
			} else if (brightness > 0) {
				float f = 1 - brightness/(float)BRIGHTNESS_MAX;
				pixel[0] = Math.round( 255 - f*(255 - (pixel[0] & 0xFF)) );
				pixel[1] = Math.round( 255 - f*(255 - (pixel[1] & 0xFF)) );
				pixel[2] = Math.round( 255 - f*(255 - (pixel[2] & 0xFF)) );
			}
		}

		private void computeContrast(int[] pixel)
		{
			if (contrast < 0) { // lower contrast
				float f = 1 - contrast/(float)CONTRAST_MIN;
				pixel[0] = Math.round( f*((pixel[0] & 0xFF) - 127) + 127 );
				pixel[1] = Math.round( f*((pixel[1] & 0xFF) - 127) + 127 );
				pixel[2] = Math.round( f*((pixel[2] & 0xFF) - 127) + 127 );
				
			} else if (contrast > 0) { // raise contrast
				float f = 1 - contrast/(float)CONTRAST_MAX;
				pixel[0] = raiseContrast( f, pixel[0] );
				pixel[1] = raiseContrast( f, pixel[1] );
				pixel[2] = raiseContrast( f, pixel[2] );
			}
		}

		private int raiseContrast(float f, int n)
		{
			float n_ = ((n & 0xFF) - 127)/(float)128; // 0 .. 255  ->  -1.0 .. 1.0
			n_ = Math.min(Math.max(-1, n_), 1);
			n_ = Math.signum(n_) * (float) Math.pow( Math.abs(n_), f);
			return Math.round( 255 * ((n_+1)/2) );
		}

		ConfigureDialog createConfigureDialog(Window parent, String title) {
			return new ConfigureDialog(parent, title);
		}
		
		private void setBrightness(int brightness) { setImageValues(brightness, null); }
		private void setContrast  (int contrast  ) { setImageValues(null, contrast); }
		
		private void setImageValues(Integer brightness, Integer contrast)
		{
			if (brightness!=null) {
				this.brightness = brightness;
				System.out.printf("MapBackgroundImage.Brightness = %s%n", this.brightness);
				PlanetCrafterSaveGameViewer.settings.putInt(SETTINGSKEY_BRIGHTNESS, this.brightness);
			}
			if (contrast!=null) {
				this.contrast = contrast;
				System.out.printf("MapBackgroundImage.Contrast = %s%n", this.contrast);
				PlanetCrafterSaveGameViewer.settings.putInt(SETTINGSKEY_CONTRAST, this.contrast);
			}
			if (brightness!=null || contrast!=null) {
				computeImage();
				mapView.repaint();
			}
		}

		void drawImage(Graphics2D g2, int x, int y, int width, int height)
		{
			g2.drawImage(mapBgImage, x, y, width, height, null);
			// TODO Auto-generated method stub
		}

		private interface IOExceptionTask<ReturnValue> {
			ReturnValue run() throws IOException;
		}

		private static <ReturnValue> ReturnValue runIOExceptionTask( IOExceptionTask<ReturnValue> task, String taskDesc, Component errDlgParent, String errDlgtitle, ReturnValue errorValue ) {
			try {
				return task.run();
			}
			catch (IOException ex) {
				JOptionPane.showMessageDialog(
						errDlgParent,
						new String[] {
								String.format("IOException while %s:", taskDesc),
								ex.getMessage(),
						},
						errDlgtitle, JOptionPane.ERROR_MESSAGE
				);
				System.err.printf("IOException while %s:%n   %s%n", taskDesc, ex.getMessage());
				// ex.printStackTrace();
				return errorValue;
			}
		}

		@SuppressWarnings("unused")
		private static class FixPoint
		{
			private double imageX;
			private double imageY;
			private double mapX;
			private double mapY;
			private final MapBGPoint imagePointId;
			private final MapBGPoint mapPointId;
		
			FixPoint(MapBGPoint imagePointId, MapBGPoint mapPointId){
				this.imageX = 0;
				this.imageY = 0;
				this.mapX = 0;
				this.mapY = 0;
				this.imagePointId = imagePointId;
				this.mapPointId = mapPointId;
			}

			void getValuesFromSettings(double defaultImageX, double defaultImageY, double defaultMapX, double defaultMapY)
			{
				imageX = PlanetCrafterSaveGameViewer.settings.getDouble(imagePointId.keyX, defaultImageX);            
				imageY = PlanetCrafterSaveGameViewer.settings.getDouble(imagePointId.keyY, defaultImageY);            
				mapX   = PlanetCrafterSaveGameViewer.settings.getDouble(mapPointId  .keyX, defaultMapX  );
				mapY   = PlanetCrafterSaveGameViewer.settings.getDouble(mapPointId  .keyY, defaultMapY  );
			}

			void setValues(double imageX, double imageY, double mapX, double mapY)
			{
				PlanetCrafterSaveGameViewer.settings.putDouble(imagePointId.keyX, this.imageX = imageX);            
				PlanetCrafterSaveGameViewer.settings.putDouble(imagePointId.keyY, this.imageY = imageY);            
				PlanetCrafterSaveGameViewer.settings.putDouble(mapPointId  .keyX, this.mapX   = mapX  );
				PlanetCrafterSaveGameViewer.settings.putDouble(mapPointId  .keyY, this.mapY   = mapY  );
			}
		}

		class ConfigureDialog extends StandardDialog
		{
			private static final long serialVersionUID = -8078213429132869645L;
			private JSlider sliderBrightness;
			private JSlider sliderContrast;
			private JTextField outputBrightness;
			private JTextField outputContrast;
			
			public ConfigureDialog(Window parent, String title)
			{
				super(parent, title, ModalityType.APPLICATION_MODAL, true);
				
				String btnLoadImageTitle = storedImageFile.isFile() ? "Replace Image" : "Load Image";
				JButton btnLoadImage = GUI.createButton(btnLoadImageTitle, true, e -> loadImage  ());
				JButton btnClose     = GUI.createButton("Close"          , true, e -> closeDialog());
				
				sliderBrightness = GUI.createSlider(JSlider.HORIZONTAL, BRIGHTNESS_MIN, BRIGHTNESS_MAX, brightness, null);
				sliderContrast   = GUI.createSlider(JSlider.HORIZONTAL, CONTRAST_MIN  , CONTRAST_MAX  , contrast  , null);
				
				outputBrightness = GUI.createOutputTextField(Integer.toString(sliderBrightness.getValue()), 5);
				outputContrast   = GUI.createOutputTextField(Integer.toString(sliderContrast  .getValue()), 5);
				
				sliderBrightness.addChangeListener(chev -> {
					outputBrightness.setText(Integer.toString(sliderBrightness.getValue()));
					if (!sliderBrightness.getValueIsAdjusting())
						setBrightness(sliderBrightness.getValue());
				});
				sliderContrast.addChangeListener(chev -> {
					outputContrast.setText(Integer.toString(sliderContrast.getValue()));
					if (!sliderContrast.getValueIsAdjusting())
						setContrast(sliderContrast.getValue());
				});
				
				JPanel contentPane = new JPanel(new GridBagLayout());
				GridBagConstraints c = new GridBagConstraints();
				c.fill = GridBagConstraints.BOTH;
				
				c.weightx = 1;
				c.weighty = 0;
				c.gridwidth = 3;
				c.gridheight = 1;
				c.gridx = 0; c.gridy = 0; contentPane.add(btnLoadImage, c);
		
				c.gridwidth = 1;
				c.gridheight = 1;
				
				c.weightx = 0;
				c.weighty = 0;
				c.gridx = 0; c.gridy = 1; contentPane.add(new JLabel("Brightness: "), c);
				c.gridx = 0; c.gridy = 2; contentPane.add(new JLabel("Contrast: "  ), c);
				c.weightx = 1;
				c.gridx = 1; c.gridy = 1; contentPane.add(sliderBrightness, c);
				c.gridx = 1; c.gridy = 2; contentPane.add(sliderContrast  , c);
				c.weightx = 0;
				c.gridx = 2; c.gridy = 1; contentPane.add(outputBrightness, c);
				c.gridx = 2; c.gridy = 2; contentPane.add(outputContrast, c);
				
				createGUI(contentPane, btnClose);
			}
		
			private void loadImage()
			{
				if (storedImageFile.isFile()) {
					int result = JOptionPane.showConfirmDialog(
							this,
							"Do you want to replace existing background image?",
							"Replace Existing Background Image",
							JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE
					);
					if (result != JOptionPane.YES_OPTION)
						return;
				}
				
				if (imageFC.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
					return;
				
				File imageFile = imageFC.getSelectedFile();
				
				
				System.out.printf("Read image from file \"%s\"%n", imageFile.getAbsolutePath());
				BufferedImage image = runIOExceptionTask(
						()->ImageIO.read(imageFile),
						String.format("reading image from file \"%s\":", imageFile.getAbsolutePath()),
						this, "Read Error",
						null
				);
				if (image==null) return;
				System.out.printf("   %d bytes read%n", imageFile.length());
				
				
				System.out.printf("Write image to file \"%s\"%n", storedImageFile.getAbsolutePath());
				boolean success = runIOExceptionTask(
						()->{ ImageIO.write(image, FILE_MAPBGIMAGE_FORMAT, storedImageFile); return true; },
						String.format("writing image to file \"%s\":", storedImageFile.getAbsolutePath()),
						this, "Write Error",
						false
				);
				if (!success) return;
				System.out.printf("   %d bytes written%n", storedImageFile.length());
				
				setBaseImage(image);
				sliderBrightness.setValue(brightness);
				sliderContrast  .setValue(contrast  );
				outputBrightness.setText(Integer.toString(sliderBrightness.getValue()));
				outputContrast  .setText(Integer.toString(sliderContrast  .getValue()));
			}
		}
	}

	private static class MapView extends ZoomableCanvas<MapView.ViewState> {
		private static final long serialVersionUID = -5838969838377820166L;
		private static final int NEAREST_OBJECT_MAX_DIST = 15;
		
		private final OverView overView;
		private final JTextArea textOut;
		private WorldObject extraShownObject;
		private WorldObject hoveredObject;
		private ToolTipBox toolTipBox;

		private final MapModel mapModel;
		private final MapShapes mapShapes;
		private MapBackgroundImage mapBackgroundImage;

		MapView(MapShapes mapShapes, MapModel mapModel, OverView overView, JTextArea textOut) {
			this.mapShapes = mapShapes;
			this.mapModel = mapModel;
			this.overView = overView;
			this.textOut = textOut;
			hoveredObject = null;
			extraShownObject = null;
			toolTipBox = null;
			mapBackgroundImage = null;
			overView.setRange(this.mapModel.range);
			
			activateMapScale(COLOR_MAP_AXIS, "px", true);
			activateAxes(COLOR_MAP_AXIS, true,true,true,true);
			
			addPanListener(new PanListener() {
				@Override public void panStarted() {}
				@Override public void panStopped() { updateOverviewImage(); }
			});
			addZoomListener(new ZoomListener() {
				@Override public void zoomChanged() { updateOverviewImage(); }
			});
		}

		ViewState setBgImage(MapBackgroundImage mapBackgroundImage)
		{
			this.mapBackgroundImage = mapBackgroundImage;
			return viewState;
		}

		void setExtraShownObject(WorldObject extraShownObject) {
			this.extraShownObject = extraShownObject;
		}

		@Override
		protected void sizeChanged(int width, int height) {
			super.sizeChanged(width, height);
			updateOverviewImage();
		}
		
		private static class ToolTipBox {

			final Point pos;
			final Object source;
			final String text;

			public ToolTipBox(Point pos, Object source, String text) {
				this.pos = new Point(pos);
				this.source = source;
				this.text = text;
			}

			void draw(Graphics2D g2, int x, int y, int width, int height) {
				
				Font font = g2.getFont();
				FontRenderContext frc = g2.getFontRenderContext();
				Rectangle2D textBounds = font.getStringBounds(text, frc);
				
				int textOffsetX =  1;
				int textOffsetY = -1;
				int paddingH = 3;
				int paddingV = 1;
				textBounds.setRect(
					textBounds.getX()-paddingH-textOffsetX,
					textBounds.getY()-paddingV-textOffsetY,
					textBounds.getWidth ()+2*paddingH,
					textBounds.getHeight()+2*paddingV
				);
				
				int spacing = 15;
				boolean rightOfPos = (pos.x+spacing+textBounds.getWidth () < x+width ) || (pos.x-spacing-textBounds.getWidth () <= x);
				boolean belowPos   = (pos.y+spacing+textBounds.getHeight() < y+height) || (pos.y-spacing-textBounds.getHeight() <= y);
				
				int strX = (int)Math.round(pos.x - textBounds.getX() + spacing - (rightOfPos ? 0 : 2*spacing + textBounds.getWidth ()));
				int strY = (int)Math.round(pos.y - textBounds.getY() + spacing - (belowPos   ? 0 : 2*spacing + textBounds.getHeight()));
				
				int boxX = (int)Math.round(strX + textBounds.getX());
				int boxY = (int)Math.round(strY + textBounds.getY());
				int boxW = (int)Math.round(textBounds.getWidth ());
				int boxH = (int)Math.round(textBounds.getHeight());
				
				g2.setColor(COLOR_TOOLTIP_BACKGORUND);
				g2.fillRect(boxX, boxY, boxW, boxH);
				g2.setColor(COLOR_TOOLTIP_BORDER);
				g2.drawRect(boxX-1, boxY-1, boxW+1, boxH+1);
				g2.setColor(COLOR_TOOLTIP_TEXT);
				g2.drawString(text, strX, strY);
			}

			void setPos(Point pos) {
				this.pos.setLocation(pos);
			}
			
			boolean isPos(Point pos) {
				return this.pos.equals(pos);
			}
		}

		@Override public void mouseEntered(MouseEvent e) { updateHoveredObject(e.getPoint()); }
		@Override public void mouseMoved  (MouseEvent e) { updateHoveredObject(e.getPoint()); }
		@Override public void mouseExited (MouseEvent e) { updateHoveredObject(null); }

		private void updateHoveredObject(Point mouse) {
			WorldObject nearestObject = getNearestObject(mouse);
			if (hoveredObject != nearestObject) {
				hoveredObject = nearestObject;
				textOut.setText(hoveredObject==null ? "" : hoveredObject.generateOutput());
			}
			if (hoveredObject!=null && mouse!=null) {
				if (toolTipBox==null || toolTipBox.source!=hoveredObject)
					toolTipBox = new ToolTipBox(mouse, hoveredObject, hoveredObject.getName() + (hoveredObject.text.isEmpty() ? "" : String.format(" (\"%s\")", hoveredObject.text)));
				else if (!toolTipBox.isPos(mouse))
					toolTipBox.setPos(mouse);
				
			} else if (toolTipBox!=null) {
				toolTipBox = null;
			}
			repaint();
		}

		private WorldObject getNearestObject(Point mouse) {
			if (!viewState.isOk()) return null;
			if (mouse==null) return null;
			
			double x = viewState.convertPos_ScreenToAngle_LongX(mouse.x);
			double y = viewState.convertPos_ScreenToAngle_LatY (mouse.y);
			
			return mapModel.getNearestObject(x, y, dist->{
				Double dist_screen = viewState.convertLength_LengthToScreenF(dist);
				if (dist_screen==null) return false;
				return dist_screen <= NEAREST_OBJECT_MAX_DIST;
			});
		}

		protected void updateOverviewImage() {
			if (viewState.isOk() && mapModel.range!=null) {
				double x0 = viewState.convertPos_ScreenToAngle_LongX(0);
				double x1 = viewState.convertPos_ScreenToAngle_LongX(width);
				double y0 = viewState.convertPos_ScreenToAngle_LatY(0);
				double y1 = viewState.convertPos_ScreenToAngle_LatY(height);
				if (x0>x1) { double temp=x0; x0=x1; x1=temp; }
				if (y0>y1) { double temp=y0; y0=y1; y1=temp; }
				Rectangle2D.Double screen = new Rectangle2D.Double(x0,y0,x1-x0,y1-y0);
				overView.setScreen(screen);
			}
		}

		@Override
		protected void paintCanvas(Graphics g, int x, int y, int width, int height) {
			//System.out.printf("MapView.paintCanvas: viewState.isOk: %s%n", viewState.isOk());
			
			if (g instanceof Graphics2D && viewState.isOk()) {
				Graphics2D g2 = (Graphics2D) g;
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
				
				Shape prevClip = g2.getClip();
				Rectangle clip = new Rectangle(x, y, width, height);
				g2.setClip(clip);
				
				if (mapModel.range!=null) {
					int screenX0 = /*x+*/viewState.convertPos_AngleToScreen_LongX(mapModel.range.x);
					int screenY0 = /*y+*/viewState.convertPos_AngleToScreen_LatY (mapModel.range.y);
					int screenX1 = /*x+*/viewState.convertPos_AngleToScreen_LongX(mapModel.range.x+mapModel.range.width);
					int screenY1 = /*y+*/viewState.convertPos_AngleToScreen_LatY (mapModel.range.y+mapModel.range.height);
					if (screenX0>screenX1) { int temp=screenX0; screenX0=screenX1; screenX1=temp; }
					if (screenY0>screenY1) { int temp=screenY0; screenY0=screenY1; screenY1=temp; }
					g2.setColor(COLOR_MAP_BACKGROUND);
					g2.fillRect(screenX0, screenY0, screenX1-screenX0, screenY1-screenY0);
					if (mapBackgroundImage!=null)
						mapBackgroundImage.drawImage(g2, x, y, width, height);
					g2.setColor(COLOR_MAP_BORDER);
					g2.drawRect(screenX0-1, screenY0-1, screenX1-screenX0+1, screenY1-screenY0+1);
				}
				else
					if (mapBackgroundImage!=null)
						mapBackgroundImage.drawImage(g2, x, y, width, height);
				
				drawShapes(g2);
				
				HashMap<String,Boolean> showMarkerCache = new HashMap<>();
				for (WorldObject wo : mapModel.displayableObjects)
					if (wo!=hoveredObject && wo!=extraShownObject && !mapModel.isHighlighted(wo) && shouldShowMarker(showMarkerCache,wo)) {
						Color fill = wo.isMarkedForRemoval() ? COLOR_WORLDOBJECT_FILL_REMOVAL : COLOR_WORLDOBJECT_FILL;
						drawWorldObject(g2, clip, wo, COLOR_WORLDOBJECT_CONTOUR, fill);
					}
				
				for (WorldObject wo : mapModel.displayableObjects)
					if (wo!=hoveredObject && wo!=extraShownObject && mapModel.isHighlighted(wo))
						drawWorldObject(g2, clip, wo, COLOR_WORLDOBJECT_CONTOUR, mapModel.getHighlightColor(wo));
				
				if (extraShownObject!=null && extraShownObject!=hoveredObject)
					drawWorldObject(g2, clip, extraShownObject, COLOR_WORLDOBJECT_CONTOUR, COLOR_WORLDOBJECT_FILL_EXTRA_SHOWN);
				
				if (hoveredObject!=null)
					drawWorldObject(g2, clip, hoveredObject, COLOR_WORLDOBJECT_CONTOUR, COLOR_WORLDOBJECT_FILL_HOVERED);
				
				if (mapModel.playerPosition!=null)
					drawPlayerPosition(g2, clip, mapModel.playerPosition, mapModel.playerOrientation, COLOR_WORLDOBJECT_CONTOUR, COLOR_PLAYERPOS);
				
				if (toolTipBox!=null)
					toolTipBox.draw(g2, x, y, width, height);
				
				drawMapDecoration(g2, x, y, width, height);
				
				g2.setClip(prevClip);
			}
		}

		private void drawShapes(Graphics2D g2)
		{
			double originX_scr = viewState.convertPos_AngleToScreen_LongXf(0);
			double originY_scr = viewState.convertPos_AngleToScreen_LatYf (0);
			
			AffineTransform origTransform = g2.getTransform();
			HashMap<String,MapShape> shapeCache = new HashMap<>(); 
			for (WorldObject wo : mapModel.displayableObjects)
			{
				MapShape cachedShape = shapeCache.get(wo.objectType.id);
				if (cachedShape==null)
				{
					cachedShape = mapShapes.getSelectedShape(wo.objectType);
					if (cachedShape == null)
						cachedShape = new MapShape("dummy");
					shapeCache.put(wo.objectType.id, cachedShape);
				}
				Vector<Form> forms = cachedShape.getForms();
				if (!forms.isEmpty())
				{
					double woX_scr = viewState.convertPos_AngleToScreen_LongXf(wo.position.getMapX());
					double woY_scr = viewState.convertPos_AngleToScreen_LatYf (wo.position.getMapY());
					/*
					// Without Rotation
					AffineTransform transform = new AffineTransform(origTransform);
					transform.translate(
							woX_scr-originX_scr,
							woY_scr-originY_scr
					);
					*/
					// With Rotation
					AffineTransform transform = new AffineTransform(origTransform);
					transform.translate( woX_scr, woY_scr );
					transform.concatenate(wo.rotation.computeMapTransform());
					transform.translate( -originX_scr, -originY_scr );
					
					g2.setTransform(transform);
					g2.setColor(COLOR_MAPSHAPE_BASE);
					LineEditor.drawForms(g2, forms, viewState);
				}
			}
			g2.setTransform(origTransform);
		}

		private boolean shouldShowMarker(HashMap<String, Boolean> cache, WorldObject wo)
		{
			if (wo==null) return false;
			Boolean result = cache.get(wo.objectType.id);
			if (result==null) cache.put(wo.objectType.id, result = mapShapes.shouldShowMarker(wo.objectType));
			return result;
		}

		private void drawPlayerPosition(Graphics2D g2, Rectangle clip, Data.Coord3 position, Data.Rotation orientation, Color contourColor, Color fillColor) {
			double posX_scr = viewState.convertPos_AngleToScreen_LongXf(position.getMapX());
			double posY_scr = viewState.convertPos_AngleToScreen_LatYf (position.getMapY());
			if (!clip.contains(posX_scr, posY_scr)) return;
			
			AffineTransform origTransform = g2.getTransform();
			AffineTransform transform = new AffineTransform(origTransform);
			transform.translate( posX_scr, posY_scr );
			transform.concatenate(orientation.computeMapTransform());
			
			g2.setTransform(transform);
			
			g2.setColor(contourColor);
			g2.drawLine( 10,-8, 0,0 );
			g2.drawLine(-15, 0, 0,0 );
			g2.drawLine( 10,-8, 0,0 );
			g2.drawLine( 10, 8, 0,0 );
			
			g2.setColor(fillColor);
			g2.drawLine( 10,-8,   5, 0 );
			g2.drawLine( 10, 8,   5, 0 );
			g2.drawLine( 10,-8, -15, 0 );
			g2.drawLine( 10, 8, -15, 0 );
			
			g2.setTransform(origTransform);
		}

		private void drawWorldObject(Graphics2D g2, Rectangle clip, WorldObject wo, Color contourColor, Color fillColor) {
			int r = 3;
			int screenX = /*x+*/viewState.convertPos_AngleToScreen_LongX(wo.position.getMapX());
			int screenY = /*y+*/viewState.convertPos_AngleToScreen_LatY (wo.position.getMapY());
			if (clip.contains(screenX, screenY)) {
				if (fillColor!=null) {
					g2.setColor(fillColor);
					g2.fillOval(screenX-r, screenY-r, 2*r+1, 2*r+1);
				}
				if (contourColor!=null) {
					g2.setColor(contourColor);
					g2.drawOval(screenX-r, screenY-r, 2*r, 2*r);
				}
			}
		}

		@Override
		protected ViewState createViewState() {
			return new ViewState();
		}

		class ViewState extends ZoomableCanvas.ViewState {
		
			protected ViewState() {
				super(MapView.this, 0.1f);
				setPlainMapSurface();
				setVertAxisDownPositive(false);
				setHorizAxisRightPositive(false);
			}
		
			@Override
			protected void determineMinMax(MapLatLong min, MapLatLong max) {
				if (mapModel.minmax!=null) {
					min.longitude_x = mapModel.minmax.minX;
					min.latitude_y  = mapModel.minmax.minY;
					max.longitude_x = mapModel.minmax.maxX;
					max.latitude_y  = mapModel.minmax.maxY;
				} else {
					min.longitude_x = 0.0;
					min.latitude_y  = 0.0;
					max.longitude_x = 100.0;
					max.latitude_y  = 100.0;
				}
				
				if (min.longitude_x==max.longitude_x && min.latitude_y==max.latitude_y) {
					min.longitude_x = max.longitude_x-50;
					min.latitude_y  = max.latitude_y -50;
					max.longitude_x = max.longitude_x+50;
					max.latitude_y  = max.latitude_y +50;
				}
				
				//System.out.printf(Locale.ENGLISH, "MapView.ViewState.MinMax: (%s,%s) -> (%s,%s)%n",
				//		min.longitude_x,
				//		min.latitude_y ,
				//		max.longitude_x,
				//		max.latitude_y 
				//);
			}
			
		}
	}
}
