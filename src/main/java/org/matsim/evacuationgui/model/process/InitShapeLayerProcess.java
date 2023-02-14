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

package org.matsim.evacuationgui.model.process;

import org.matsim.evacuationgui.control.Controller;
import org.matsim.evacuationgui.model.process.BasicProcess;
import org.matsim.evacuationgui.view.renderer.ShapeRenderer;

public class InitShapeLayerProcess extends BasicProcess {

	public InitShapeLayerProcess(Controller controller)
	{
		super(controller);
	}
	
	@Override
	public void start()
	{
		// check if there is already a primary shape layer
		if (!controller.hasShapeRenderer())
			this.controller.addRenderLayer(new ShapeRenderer(controller, controller.getImageContainer()));
		
	}
	
	

}
