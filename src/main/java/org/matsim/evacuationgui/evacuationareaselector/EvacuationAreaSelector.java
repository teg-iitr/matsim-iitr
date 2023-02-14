/* *********************************************************************** *
 * project: org.matsim.*
 * MyMapViewer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.evacuationgui.evacuationareaselector;

import org.matsim.evacuationgui.control.Controller;
import org.matsim.evacuationgui.control.ShapeFactory;

import org.matsim.evacuationgui.model.AbstractModule;
import org.matsim.evacuationgui.model.AbstractToolBox;
import org.matsim.evacuationgui.model.Constants;
import org.matsim.evacuationgui.model.imagecontainer.BufferedImageContainer;
import org.matsim.evacuationgui.model.process.*;
import org.matsim.evacuationgui.model.shape.CircleShape;
import org.matsim.evacuationgui.view.DefaultWindow;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * 
 * <code>InitProcess</code> <code>InnerEvacAreaListener</code>
 * 
 * @author wdoering
 * 
 */
public class EvacuationAreaSelector extends AbstractModule {

	public static void main(String[] args) {
		// set up controller and image interface
		final Controller controller = new Controller(args);
		controller.setImageContainer(BufferedImageContainer.getImageContainer(width, height, border));

		// inform controller that this module is running stand alone
		controller.setStandAlone(true);

		// instantiate evacuation area selector
		AbstractModule evacAreaSelector = new EvacuationAreaSelector(controller);

		// create default window for running this module standalone
		DefaultWindow frame = new DefaultWindow(controller);

		// set parent component to forward the (re)paint event
		controller.setParentComponent(frame);
		controller.setMainPanel(frame.getMainPanel(), true);

		// start the process chain
		evacAreaSelector.start();
		frame.requestFocus();

	}

	public EvacuationAreaSelector(Controller controller) {
		super(controller.getLocale().moduleEvacAreaSelector(), Constants.ModuleType.EVACUATION, controller);

		// disable all layers
		this.processList.add(new DisableLayersProcess(controller));

		this.processList.add(new InitEvacuationConfigProcess(controller));

		// check if the default render panel is set
		this.processList.add(new InitMainPanelProcess(controller));

		// check if there is already a map viewer running, or just (re)set
		// center position
		this.processList.add(new InitMapLayerProcess(controller));

		// set module listeners
		this.processList.add(new SetModuleListenerProcess(controller, this, new EvacEventListener(controller)));

		// check if there is already a primary shape layer
		this.processList.add(new InitShapeLayerProcess(controller));

		// add bounding box
		this.processList.add(new BasicProcess(controller) {
			@Override
			public void start() {
				int shapeRendererId = controller.getVisualizer().getPrimaryShapeRenderLayer().getId();
				Rectangle2D bbRect = controller.getBoundingBox();
				controller.addShape(ShapeFactory.getNetBoxShape(shapeRendererId, bbRect, false));
			}

		});
		
		// load evacuation area shape (if available)
		this.processList.add(new InitEvacShapeProcess(controller));

		// add toolbox
		this.processList.add(new SetToolBoxProcess(controller, getToolBox()));

		// enable all layers
		this.processList.add(new EnableLayersProcess(controller));

	}

	@Override
	public AbstractToolBox getToolBox() {
		if (this.toolBox == null)
			this.toolBox = new EvacToolBox(this, this.controller);

		return this.toolBox;
	}

	public Point getGeoPoint(Point mousePoint) {
		Rectangle viewPortBounds = this.controller.getViewportBounds();
		return new Point(mousePoint.x + viewPortBounds.x - offsetX, mousePoint.y + viewPortBounds.y - offsetY);
	}

	public void setEvacCircle(Point2D c0, Point2D c1) {
		CircleShape evacCircle = ShapeFactory.getEvacCircle(controller.getVisualizer().getPrimaryShapeRenderLayer().getId(), c0, c1);
		controller.addShape(evacCircle);
		this.controller.getVisualizer().getPrimaryShapeRenderLayer().updatePixelCoordinates(evacCircle);

	}

	@Override
	public boolean saveChanges() {
		return this.toolBox.save();
	}

}
