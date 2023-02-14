package org.matsim.evacuationgui.model;

import org.matsim.evacuationgui.model.Constants.ModuleType;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * class to describe the flow of modules.
 * nextModules defines modules to activate,
 * whilte pastModules defines the ones to
 * deactivate.
 * 
 * @author wdoering
 *
 */
public abstract class ModuleChain
{
	
	protected HashMap<ModuleType,ArrayList<ModuleType>> nextModules;
	protected HashMap<ModuleType,ArrayList<ModuleType>> pastModules;

	public ModuleChain()
	{
		this.nextModules = new HashMap<ModuleType, ArrayList<ModuleType>>();
		this.pastModules = new HashMap<ModuleType, ArrayList<ModuleType>>();
	}
	
	public ArrayList<ModuleType> getNextModules(ModuleType module)
	{
		return this.nextModules.get(module);
	}
	
	public ArrayList<ModuleType> getPastModules(ModuleType module)
	{
		return this.pastModules.get(module);
	}

}
