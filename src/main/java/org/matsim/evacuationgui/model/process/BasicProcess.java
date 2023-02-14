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
import org.matsim.evacuationgui.control.eventlistener.AbstractListener;
import org.matsim.evacuationgui.model.AbstractModule;
import org.matsim.evacuationgui.model.AbstractToolBox;
import org.matsim.evacuationgui.model.locale.Locale;
import org.matsim.evacuationgui.view.renderer.GridRenderer;

/**
 * class for encapsulated description of
 * function calls for the repeated use in
 * different modules. 
 * 
 * @author wdoering
 *
 */
public abstract class BasicProcess implements ProcessInterface
{
	protected Controller controller;
	protected Locale locale;
	protected AbstractModule module;
	protected static BasicProcess process;
	
	public BasicProcess(AbstractModule module, Controller controller)
	{
		this.module = module;
		this.controller = controller;
		this.locale = controller.getLocale();
		
	}
	
	public BasicProcess(Controller controller)
	{
		this.controller = controller;
		this.locale = controller.getLocale();
	}
	
	public void setListeners(AbstractListener listeners)
	{
		//set and attach listeners
		controller.setListener(listeners);
		controller.setMainPanelListeners(true);
	}
	
	
	public void addNetworkRenderer(GridRenderer renderer)
	{
		controller.addRenderLayer(renderer);
	}
	
	public void addToolBox(AbstractToolBox toolBox)
	{
		this.controller.setActiveToolBox(toolBox);
	}

	@Override
	public void start() {}

}
