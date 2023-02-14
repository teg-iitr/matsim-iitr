package org.matsim.evacuationgui.scenariomanager;

import org.matsim.evacuationgui.control.Controller;
import org.matsim.evacuationgui.model.AbstractModule;
import org.matsim.evacuationgui.model.Constants;
import org.matsim.evacuationgui.model.imagecontainer.BufferedImageContainer;
import org.matsim.evacuationgui.model.process.BasicProcess;
import org.matsim.evacuationgui.view.DefaultWindow;

public class ScenarioXMLEditor extends AbstractModule {

	public static void main(String[] args) {
		// set up controller and image interface
		final Controller controller = new Controller(args);
		controller.setImageContainer(BufferedImageContainer.getImageContainer(
				width, height, border));

		// inform controller that this module is running stand alone
		controller.setStandAlone(true);

		// instantiate evacuation area selector
		AbstractModule scenarioXMLEditor = new ScenarioXMLEditor(controller);

		// create default window for running this module standalone
		DefaultWindow frame = new DefaultWindow(controller);
		frame.setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

		// set parent component to forward the (re)paint event
		controller.setParentComponent(frame);
		controller.setMainPanel(frame.getMainPanel(), true);

		// start the process chain
		scenarioXMLEditor.start();

		frame.requestFocus();
	}

	public ScenarioXMLEditor(Controller controller) {
		super(controller.getLocale().moduleScenarioXml(),
				Constants.ModuleType.SCENARIOXML, controller);

		this.processList.add(new BasicProcess(this, this.controller) {

			@Override
			public void start() {
				// in case this is only part of something bigger
				controller.disableAllRenderLayers();

				// create scenario xml mask, disable toolbox
				ScenarioXMLMask mask = new ScenarioXMLMask(
						ScenarioXMLEditor.this, controller);
				this.controller.setMainPanel(mask, false);
				this.controller.setToolBoxVisible(false);


				mask.readConfig();

				// finally: enable all layers
				controller.enableAllRenderLayers();
			}
		});
	}

}
