package playground.nidhi.practice.eventHandlingPract.analysis;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.router.MainModeIdentifier;

import java.util.*;

public class ModeStats implements StartupListener, IterationEndsListener {

public static final String fileName = "modestats";
final private Population population;
final private String modeFileName;
final private boolean createPNG;
private final ControlerConfigGroup controlerConfigGroup;
private final Controler controler;

Map<String, Map<Integer, Double>> modeHistories = new HashMap<>();
private int minIteration =0;
private MainModeIdentifier mainModeIdentifier;
private  Map<String, Double> modeCnt = new TreeMap<>();
private int firstIteration = -1;

private final Set<String> modes = new TreeSet<>();


    public ModeStats(Population population, String modeFileName, boolean createPNG, ControlerConfigGroup controlerConfigGroup, Controler controler) {
        this.population = population;
        this.modeFileName = controler.getControlerIO().getOutputFilename(fileName);
        this.createPNG = controlerConfigGroup.isCreateGraphs();
        this.controlerConfigGroup = controlerConfigGroup;
        this.controler = controler;
        this.mainModeIdentifier=mainModeIdentifier;
        
    }


    @Override
    public void notifyStartup(StartupEvent event) {
        this.minIteration=controlerConfigGroup.getFirstIteration();
    }
    
    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        collectModeShareInfo(event);
    }

    private void collectModeShareInfo(IterationEndsEvent event) {
        if(firstIteration<0){
            firstIteration=event.getIteration();
        }

        
        
    }


}
