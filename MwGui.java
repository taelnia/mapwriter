package mapwriter;

import java.awt.Point;
import java.io.File;

import mapwriter.forge.MwKeyHandler;
import mapwriter.map.MapView;
import mapwriter.map.Marker;
import mapwriter.map.MarkerManager;
import mapwriter.map.StandardMapRenderer;
import mapwriter.map.mapmode.FullScreenMapMode;
import mapwriter.map.mapmode.MapMode;
import mapwriter.region.MergeTask;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class MwGui extends GuiScreen {
	private Mw mw;
    private MapMode mapMode;
    private MapView mapView;
    private StandardMapRenderer map;
    
    private TextDialog currentTextDialog = null;
    
    private static final int menuY = 5;
    private static final int menuX = 5;
    
    private int mouseLeftHeld = 0;
    private int mouseRightHeld = 0;
    private int mouseMiddleHeld = 0;
    private int mouseLeftDragStartX = 0;
    private int mouseLeftDragStartY = 0;
    private double viewXStart;
    private double viewZStart;
    private Marker movingMarker = null;
    private int movingMarkerXStart = 0;
    private int movingMarkerZStart = 0;
    private int mouseBlockX = 0;
    private int mouseBlockY = 0;
    private int mouseBlockZ = 0;

    private int exit = 0;
    
    private Label helpLabel;
    private Label optionsLabel;
    private Label dimensionLabel;
    private Label groupLabel;
    
    class Label {
    	int x = 0, y = 0, w = 1, h = 12;
    	public Label() {
    	}
    	
    	public void draw(int x, int y, String s) {
    		this.x = x;
    		this.y = y;
    		this.w = MwGui.this.fontRenderer.getStringWidth(s) + 4;
    		MwGui.this.drawRect(this.x, this.y, this.x + this.w, this.y + this.h, 0x80000000);
    		MwGui.this.drawString(MwGui.this.fontRenderer, s, this.x + 2, this.y + 2, 0xffffff);
    	}
    	
    	public void drawToRightOf(Label label, String s) {
    		this.draw(label.x + label.w + 5, label.y, s);
    	}
    	
    	public boolean posWithin(int x, int y) {
    		return (x >= this.x) && (y >= this.y) && (x <= (this.x + this.w)) && (y <= (this.y + this.h));
    	}
    }
    
    class MarkerTextDialog extends TextDialog {
    	private final MarkerManager markerManager;
    	private Marker editingMarker;
    	private String markerName = "name";
        private String markerGroup = "group";
        private int markerX = 0;
        private int markerY = 80;
        private int markerZ = 0;
        private int state = 0;
        
    	public MarkerTextDialog(MarkerManager markerManager, String markerName, String markerGroup, int x, int y, int z) {
    		super(MwGui.this.width, MwGui.this.height, MwGui.this.fontRenderer,
    				"Marker Name:", markerName, "marker must have a name");
    		this.markerManager = markerManager;
    		this.markerName = markerName;
    		this.markerGroup = markerGroup;
    		this.markerX = x;
    		this.markerY = y;
    		this.markerZ = z;
    		this.editingMarker = null;
    	}
    	
    	public MarkerTextDialog(MarkerManager markerManager, Marker editingMarker) {
    		super(MwGui.this.width, MwGui.this.height, MwGui.this.fontRenderer,
    				"Edit Marker Name:", editingMarker.name, "marker must have a name");
    		this.markerManager = markerManager;
    		this.editingMarker = editingMarker;
    		this.markerName = editingMarker.name;
    		this.markerGroup = editingMarker.groupName;
    		this.markerX = editingMarker.x;
    		this.markerY = editingMarker.y;
    		this.markerZ = editingMarker.z;
    	}
    	
    	@Override
    	public void submit() {
    		switch(this.state) {
    		case 0:
    			this.markerName = this.getInputAsString();
    			if (this.inputValid) {
    				this.title = "Marker Group:";
    				this.setText(this.markerGroup);
    				this.error = "marker must have a group name";
    				this.state++;
    			}
    			break;
    		case 1:
    			this.markerGroup = this.getInputAsString();
    			if (this.inputValid) {
    				this.title = "Marker X:";
    				this.setText("" + this.markerX);
    				this.error = "invalid value";
    				this.state++;
    			}
    			break;
    		case 2:
    			this.markerX = this.getInputAsInt();
    			if (this.inputValid) {
    				this.title = "Marker Y:";
    				this.setText("" + this.markerY);
    				this.error = "invalid value";
    				this.state++;
    			}
    			break;
    		case 3:
    			this.markerY = this.getInputAsInt();
    			if (this.inputValid) {
    				this.title = "Marker Z:";
    				this.setText("" + this.markerZ);
    				this.error = "invalid value";
    				this.state++;
    			}
    			break;
    		case 4:
    			this.markerZ = this.getInputAsInt();
    			if (this.inputValid) {
    				this.closed = true;
    				int colour = Marker.getCurrentColour();
    	    		if (this.editingMarker != null) {
    	    			colour = this.editingMarker.colour;
    	    			this.markerManager.delMarker(this.editingMarker);
    	    			this.editingMarker = null;
    	    		}
    	    		this.markerManager.addMarker(this.markerName, this.markerGroup,
    						this.markerX, this.markerY, this.markerZ, colour);
    				this.markerManager.setVisibleGroupName(this.markerGroup);
    				this.markerManager.update();
    			}
    			break;
    		}
    	}
    }
    
    class TeleportTextDialog extends TextDialog {
    	int teleportX, teleportZ;
    	
    	public TeleportTextDialog(int x, int y, int z) {
    		super(MwGui.this.width, MwGui.this.height, MwGui.this.fontRenderer,
    				"Teleport Height:", "" + y, "invalid height");
    		this.teleportX = x;
    		this.teleportZ = z;
    	}
    	
    	public void submit() {
    		int height = this.getInputAsInt();
    		if (this.inputValid) {
	    		height = Math.min(Math.max(0, height), 255);
	    		MwGui.this.mw.defaultTeleportHeight = height;
	    		this.closed = true;
	    		MwGui.this.mw.teleportToMapPos(MwGui.this.mapView, this.teleportX, height, this.teleportZ);
	    		MwGui.this.exitGui();
    		}
    	}
    }
    
    class DimensionTextDialog extends TextDialog {
    	public DimensionTextDialog(int dimension) {
    		super(MwGui.this.width, MwGui.this.height, MwGui.this.fontRenderer,
    				"Set dimension to:", "" + dimension, "invalid dimension");
    	}
    	
    	public void submit() {
    		int dimension = this.getInputAsInt();
    		if (this.inputValid) {
	    		this.closed = true;
	    		MwGui.this.mapView.setDimension(dimension);
	    		MwGui.this.mw.overlayManager.overlayView.setDimension(dimension);
	    		MwGui.this.mw.addDimension(dimension);
    		}
    	}
    }
    
    
    
    
    public MwGui(Mw mw) {
    	this.mw = mw;
    	this.mapMode = new FullScreenMapMode(mw.config);
    	this.mapView = new MapView();
    	this.map = new StandardMapRenderer(this.mw, this.mw.mapTexture, this.mw.markerManager, this.mapMode, this.mapView);
    	
    	this.mapView.setDimension(this.mw.overlayManager.overlayView.getDimension());
    	this.mapView.setViewCentreScaled(this.mw.playerX, this.mw.playerZ, this.mw.playerDimension);
    	this.mapView.setZoomLevel(0);
    	
    	this.helpLabel = new Label();
    	this.optionsLabel = new Label();
    	this.dimensionLabel = new Label();
    	this.groupLabel = new Label();
    }
    
    // called when gui is displayed and every time the screen
    // is resized
    public void initGui() {
		if (this.currentTextDialog != null) {
			this.currentTextDialog.update(this.width, this.height, this.fontRenderer);
		}
    }

    // called when a button is pressed
    protected void actionPerformed(GuiButton button) {
    	
    }
    
    public void exitGui() {
    	//MwUtil.log("closing GUI");
    	this.map.close();
    	this.mapMode.close();
    	Keyboard.enableRepeatEvents(false);
    	this.mc.displayGuiScreen((GuiScreen) null);
        this.mc.setIngameFocus();
        this.mc.sndManager.resumeAllSounds();
    }
    
    
    
    
    
    // get a marker near the specified block pos if it exists.
    // the maxDistance is based on the view width so that you need to click closer
    // to a marker when zoomed in to select it.
    public Marker getMarkerNearScreenPos(int x, int y) {
    	Marker nearMarker = null;
        for (Marker marker : this.mw.markerManager.visibleMarkerList) {
        	if (marker.screenPos != null) {
	            if (marker.screenPos.distanceSq(x, y) < 6.0) {
	            	nearMarker = marker;
	            }
        	}
        }
        return nearMarker;
    }
    
    public int getHeightAtBlockPos(int bX, int bZ) {
    	int bY = 0;
    	if (this.mw.mc.theWorld.provider.dimensionId != -1) {
    		bY = this.mw.mc.theWorld.getChunkFromBlockCoords(bX, bZ).getHeightValue(bX & 0xf, bZ & 0xf);
    	}
    	return bY;
    }
    
    public boolean isPlayerNearScreenPos(int x, int y) {
    	Point.Double p = this.map.playerArrowScreenPos;
        return p.distanceSq(x, y) < 9.0;
    }
    
    public void deleteSelectedMarker() {
    	if (this.mw.markerManager.selectedMarker != null) {
    		//MwUtil.log("deleting marker %s", this.mw.markerManager.selectedMarker.name);
    		this.mw.markerManager.delMarker(this.mw.markerManager.selectedMarker);
    		this.mw.markerManager.update();
    		this.mw.markerManager.selectedMarker = null;
    	}
    }
    
    public void mergeMapViewToImage() {
    	// get free output file name in the minecraft launcher dir
		File outputFile = MwUtil.getFreeFilename(null, this.mw.worldDir.getName(), "png");
		if (outputFile != null) {
			this.mw.regionManager.saveChunks();
			this.mw.executor.addTask(new MergeTask(this.mw, outputFile, this.mapView.getDimension(),
					(int) this.mapView.getX(),
					(int) this.mapView.getZ(),
					(int) this.mapView.getWidth(),
					(int) this.mapView.getHeight()));
			
			MwUtil.printBoth("merging to '" + outputFile.getAbsolutePath() + "'");
		} else {
			MwUtil.printBoth("error: could not get free output filename");
		}
    }
    
    public void regenerateView() {
    	MwUtil.printBoth(String.format("regenerating %dx%d blocks starting from (%d, %d)",
				(int) this.mapView.getWidth(),
				(int) this.mapView.getHeight(),
				(int) this.mapView.getMinX(),
				(int) this.mapView.getMinZ()));
		this.mw.reloadBlockColours();
		this.mw.regionManager.reloadRegions(
				(int) this.mapView.getMinX(),
				(int) this.mapView.getMinZ(),
				(int) this.mapView.getWidth(),
				(int) this.mapView.getHeight(),
				this.mapView.getDimension());
    }
    
    // c is the ascii equivalent of the key typed.
    // key is the lwjgl key code.
    protected void keyTyped(char c, int key) {
    	//MwUtil.log("MwGui.keyTyped(%c, %d)", c, key);
    	if (this.currentTextDialog == null) {
    		// normal state
    		switch(key) {
    		case Keyboard.KEY_ESCAPE:
    			this.exitGui();
    			break;
    			
    		case Keyboard.KEY_DELETE:
	        	this.deleteSelectedMarker();	        	
	        	break;
	        	
    		case Keyboard.KEY_SPACE:
	        	// next marker group
	        	this.mw.markerManager.nextGroup();
	        	this.mw.markerManager.update();
	        	break;
	        	
    		case Keyboard.KEY_C:
	        	// cycle selected marker colour
	        	if (this.mw.markerManager.selectedMarker != null) {
	        		this.mw.markerManager.selectedMarker.colourNext();
	        	}
	        	break;
	        
    		case Keyboard.KEY_N:
	        	// select next visible marker
	        	this.mw.markerManager.selectNextMarker();
	        	break;
	        	
    		case Keyboard.KEY_HOME:
	        	// centre map on player
	        	this.mapView.setViewCentreScaled(this.mw.playerX, this.mw.playerZ, this.mw.playerDimension);
	        	break;
	        
    		case Keyboard.KEY_END:
	        	// centre map on selected marker
	        	if (this.mw.markerManager.selectedMarker != null) {
	        		this.mapView.setViewCentreScaled(
	        				this.mw.markerManager.selectedMarker.x,
	        				this.mw.markerManager.selectedMarker.z,
	        				0);
	        	}
	        	break;
	        	
    		case Keyboard.KEY_P:
	        	this.mergeMapViewToImage();
				this.exitGui();
				break;
				
    		case Keyboard.KEY_T:
	        	if (this.mw.markerManager.selectedMarker != null) {
	        		this.mw.teleportToMarker(this.mw.markerManager.selectedMarker);
	        		this.exitGui();
	        	} else {
	        		this.currentTextDialog = new TeleportTextDialog(this.mouseBlockX, this.mw.defaultTeleportHeight, this.mouseBlockZ);
	        	}
	        	break;
    		
    		case Keyboard.KEY_LEFT:
    			this.mapView.panView(-Mw.PAN_FACTOR, 0);
    			break;
    		case Keyboard.KEY_RIGHT:
    			this.mapView.panView(Mw.PAN_FACTOR, 0);
    			break;
    		case Keyboard.KEY_UP:
    			this.mapView.panView(0, -Mw.PAN_FACTOR);
    			break;
    		case Keyboard.KEY_DOWN:
    			this.mapView.panView(0, Mw.PAN_FACTOR);
    			break;
    		
    		case Keyboard.KEY_R:
    			this.regenerateView();
    			this.exitGui();
    			break;
    			
    		//case Keyboard.KEY_9:
    		//	MwUtil.log("refreshing maptexture");
    		//	this.mw.mapTexture.updateTexture();
    		//	break;
    		
    		default:
    			if (key == MwKeyHandler.keyMapGui.keyCode) {
    				// exit on the next tick
        			this.exit = 1;
        		} else if (key == MwKeyHandler.keyZoomIn.keyCode) {
        			this.mapView.adjustZoomLevel(-1);
        		} else if (key == MwKeyHandler.keyZoomOut.keyCode) {
        			this.mapView.adjustZoomLevel(1);
        		} else if (key == MwKeyHandler.keyNextGroup.keyCode) {
        			this.mw.markerManager.nextGroup();
    	        	this.mw.markerManager.update();
        		}
    			break;
	        }
    	} else {
    		// currently in text dialog
    		this.currentTextDialog.keyTyped(c, key);
    	}
    }
    
    // override GuiScreen's handleMouseInput to process
    // the scroll wheel.
    @Override
    public void handleMouseInput() {
    	int x = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int y = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
    	int direction = Mouse.getEventDWheel();
    	if (direction != 0) {
    		this.mouseDWheelScrolled(x, y, direction);
    	}
    	super.handleMouseInput();
    }
    
    // mouse button clicked. 0 = LMB, 1 = RMB, 2 = MMB
    protected void mouseClicked(int x, int y, int button) {
    	//MwUtil.log("MwGui.mouseClicked(%d, %d, %d)", x, y, button);
    	
    	//int bX = this.mouseToBlockX(x);
		//int bZ = this.mouseToBlockZ(y);
		//int bY = this.getHeightAtBlockPos(bX, bZ);
    	
    	Marker marker = this.getMarkerNearScreenPos(x, y);
    	Marker prevMarker = this.mw.markerManager.selectedMarker;
    	
    	if (button == 0) {
    		if (this.dimensionLabel.posWithin(x, y)) {
    			if (this.currentTextDialog == null) {
    				this.currentTextDialog = new DimensionTextDialog(this.mapView.getDimension());
    			}
    		} else if (this.optionsLabel.posWithin(x, y)) {
    			this.mc.displayGuiScreen(new MwGuiOptions(this.mw, this));
    		} else {
	    		this.mouseLeftHeld = 1;
	    		this.mouseLeftDragStartX = x;
	    		this.mouseLeftDragStartY = y;
	    		this.mw.markerManager.selectedMarker = marker;
	    		
	    		if ((marker != null) && (prevMarker == marker)) {
	    			// clicked previously selected marker.
	    			// start moving the marker.
	    			this.movingMarker = marker;
	    			this.movingMarkerXStart = marker.x;
	    			this.movingMarkerZStart = marker.z;
	    		}
    		}
    		
    	} else if (button == 1) {
    		this.mouseRightHeld = 1;
    		if (this.currentTextDialog == null) {
    			if ((marker != null) && (prevMarker == marker)) {
        			// right clicked previously selected marker.
        			// start moving the marker.
        			this.currentTextDialog = new MarkerTextDialog(this.mw.markerManager, marker);
        			
        		} else if (marker == null) {
        			// open new marker dialog
        			String group = this.mw.markerManager.getVisibleGroupName();
            		if (group.equals("none")) {
            			group = "group";
            		}
            		int scale = 1;
            		if (this.mapView.getDimension() == -1) {
            			scale = 8;
            		}
        			this.currentTextDialog = new MarkerTextDialog(this.mw.markerManager, "", group,
        					this.mouseBlockX * scale,
        					(this.mouseBlockY > 0) ? this.mouseBlockY : this.mw.defaultTeleportHeight,
        					this.mouseBlockZ * scale);
        		}
    		}
    	}
    	this.viewXStart = this.mapView.getX();
		this.viewZStart = this.mapView.getZ();
		//this.viewSizeStart = this.mapManager.getViewSize();
    }

    // mouse button released. 0 = LMB, 1 = RMB, 2 = MMB
    // not called on mouse movement.
    protected void mouseMovedOrUp(int x, int y, int button) {
    	//MwUtil.log("MwGui.mouseMovedOrUp(%d, %d, %d)", x, y, button);
    	if (button == 0) {
    		this.mouseLeftHeld = 0;
    		this.movingMarker = null;
    	} else if (button == 1) {
    		this.mouseRightHeld = 0;
    	}
    }
    
    // zoom on mouse direction wheel scroll
    public void mouseDWheelScrolled(int x, int y, int direction) {
    	Marker marker = this.getMarkerNearScreenPos(x, y);
    	if ((marker != null) && (marker == this.mw.markerManager.selectedMarker)) {
    		if (direction > 0) {
    			marker.colourNext();
    		} else {
    			marker.colourPrev();
    		}
    		
    	} else if (this.dimensionLabel.posWithin(x, y)) {
    		int n = (direction > 0) ? 1 : -1;
	    	this.mapView.nextDimension(this.mw.dimensionList, n);
	    	this.mw.overlayManager.overlayView.setDimension(this.mapView.getDimension());
	    	
    	} else if (this.groupLabel.posWithin(x, y)) {
    		int n = (direction > 0) ? 1 : -1;
    		this.mw.markerManager.nextGroup(n);
    		this.mw.markerManager.update();
    		
    	} else {
    		int zF = (direction > 0) ? -1 : 1;
    		this.mapView.zoomToPoint(this.mapView.getZoomLevel() + zF, this.mouseBlockX, this.mouseBlockZ);
    	}
    }

    // called every frame
    public void updateScreen() {
    	//MwUtil.log("MwGui.updateScreen() " + Thread.currentThread().getName());
    	// need to wait one tick before exiting so that the game doesn't
    	// handle the 'm' key and re-open the gui.
    	// there should be a better way.
    	if (this.exit > 0) {
    		this.exit++;
    	}
    	if (this.exit > 2) {
    		this.exitGui();
    	}
        super.updateScreen();
    }
    
    public void drawStatus(int bX, int bY, int bZ) {
    	 String s;
    	 if (bY != 0) {
          	s = String.format("cursor: (%d, %d, %d)", bX, bY, bZ);
          } else {
          	s = String.format("cursor: (%d, ?, %d)", bX, bZ);
          }
    	 if (this.mc.theWorld != null) {
    		 if (!this.mc.theWorld.getChunkFromBlockCoords(bX, bZ).isEmpty()) {
    			 s += String.format(", biome: %s", this.mc.theWorld.getBiomeGenForCoords(bX, bZ).biomeName);
    		 }
    	 }
         
         /*if (this.mw.markerManager.selectedMarker != null) {
         	s += ", current marker: " + this.mw.markerManager.selectedMarker.name;
         }*/
         this.drawRect(10, this.height - 21, this.width - 20, this.height - 6, 0x80000000);
         this.drawCenteredString(this.fontRenderer,
         		s, this.width / 2, this.height - 18, 0xffffff);
    }
    
    public void drawHelp() {
    	this.drawRect(10, 20, 288, 215, 0x80000000);
    	this.fontRenderer.drawSplitString(
    			"Keys:\n\n" + 
    			"  Space\n" +
    			"  Delete\n" +
    			"  C\n" +
    			"  Home\n" +
    			"  End\n" +
    			"  N\n" +
    			"  T\n" +
    			"  P\n" +
    			"  R\n\n" +
    			"Left click drag or arrow keys pan the map.\n" +
    			"Mouse wheel or Page Up/Down zooms map.\n" +
    			"Right click map to create a new marker.\n" +
    			"Left click drag a selected marker to move it.\n" +
    			"Mouse wheel over selected marker to cycle colour.\n" + 
    			"Mouse wheel over dimension or group box to cycle.\n",
    			14, 24, 280, 0xffffff);
    	this.fontRenderer.drawSplitString(
    			"| Next marker group\n" +
    			"| Delete selected marker\n" +
    			"| Cycle selected marker colour\n" +
    			"| Centre map on player\n" +
    			"| Centre map on selected marker\n" +
    			"| Select next marker\n" +
    			"| Teleport to cursor or selected marker\n" +
    			"| Save PNG of visible map area\n" +
    			"| Regenerate visible map area\n",
    			70, 42, 210, 0xffffff);
    }
    
    public void drawMouseOverHint(int x, int y, String title, int mX, int mY, int mZ) {
    	String desc = String.format("(%d, %d, %d)", mX, mY, mZ);
    	int stringW = Math.max(
    			this.fontRenderer.getStringWidth(title),
    			this.fontRenderer.getStringWidth(desc));
    	
    	x = Math.min(x, this.width - (stringW + 16));
    	y = Math.min(Math.max(10, y), this.height - 14);
    	
    	this.drawRect(x + 8, y - 10, x + stringW + 16, y + 14, 0x80000000);
    	this.drawString(this.fontRenderer,
    			title,
    			x + 10, y - 8, 0xffffff);
    	this.drawString(this.fontRenderer,
    			desc,
    			x + 10, y + 4, 0xcccccc);
    }
    
    // also called every frame
    public void drawScreen(int mouseX, int mouseY, float f) {
    	
        this.drawDefaultBackground();
        double xOffset = 0.0;
        double yOffset = 0.0;
        //double zoomFactor = 1.0;

    	if (this.mouseLeftHeld > 2) {
    		xOffset = (this.mouseLeftDragStartX - mouseX) * this.mapView.getWidth() / this.mapMode.w;
    		yOffset = (this.mouseLeftDragStartY - mouseY) * this.mapView.getHeight() / this.mapMode.h;
    		
    		if (this.movingMarker != null) {
    			if (this.mapView.getDimension() == -1) {
    				this.movingMarker.x = this.movingMarkerXStart - (int) (xOffset * 8);
        			this.movingMarker.z = this.movingMarkerZStart - (int) (yOffset * 8);
        		} else {
        			this.movingMarker.x = this.movingMarkerXStart - (int) xOffset;
        			this.movingMarker.z = this.movingMarkerZStart - (int) yOffset;
        		}
    		} else {
	    		this.mapView.setViewCentre(this.viewXStart + xOffset, this.viewZStart + yOffset);
    		}
    	}
    	
        if (this.mouseLeftHeld > 0) {
        	this.mouseLeftHeld++;
        }
        
        // update and draw the map
        this.map.update();
        this.map.draw();
        
        // let the renderEngine know we have changed the texture.
    	this.mc.renderEngine.resetBoundTexture();
        
        // get the block the mouse is currently hovering over
    	Point p = this.mapMode.screenXYtoBlockXZ(this.mapView, mouseX, mouseY);
        this.mouseBlockX = p.x;
        this.mouseBlockZ = p.y;
        this.mouseBlockY = this.getHeightAtBlockPos(this.mouseBlockX, this.mouseBlockZ);
        
        // draw name of marker under mouse cursor
        Marker marker = this.getMarkerNearScreenPos(mouseX, mouseY);
        if (marker != null) {
        	this.drawMouseOverHint(mouseX, mouseY, marker.name, marker.x, marker.y, marker.z);
        }
        
        // draw name of player under mouse cursor
        if (this.isPlayerNearScreenPos(mouseX, mouseY)) {
        	this.drawMouseOverHint(mouseX, mouseY, this.mc.thePlayer.getEntityName(),
        			this.mw.playerXInt,
					this.mw.playerYInt,
					this.mw.playerZInt);
        }
        
        // draw status message
       this.drawStatus(this.mouseBlockX, this.mouseBlockY, this.mouseBlockZ);
        
        // draw labels
       this.helpLabel.draw(menuX, menuY, "[help]");
       this.optionsLabel.drawToRightOf(this.helpLabel, "[options]");
       String dimString = String.format("[dimension: %d]", this.mapView.getDimension());
       this.dimensionLabel.drawToRightOf(this.optionsLabel, dimString);
       String groupString = String.format("[group: %s]", this.mw.markerManager.getVisibleGroupName());
       this.groupLabel.drawToRightOf(this.dimensionLabel, groupString);
        
        // help message on mouse over
		if (this.helpLabel.posWithin(mouseX, mouseY)) {
		    this.drawHelp();
		}
        
        // draw text dialog
		if (this.currentTextDialog != null) {
			if (!this.currentTextDialog.closed) {
				this.currentTextDialog.draw();
			} else {
				this.currentTextDialog = null;
			}
		}
        
        super.drawScreen(mouseX, mouseY, f);
    }
}
