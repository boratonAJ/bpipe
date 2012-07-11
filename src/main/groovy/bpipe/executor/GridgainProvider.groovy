package bpipe.executor

import java.util.concurrent.ExecutorService
import java.util.logging.Logger

import org.gridgain.grid.Grid
import org.gridgain.grid.GridConfigurationAdapter
import org.gridgain.grid.typedef.G

import bpipe.Config
import bpipe.EventManager
import bpipe.PipelineEvent
import bpipe.PipelineEventListener

/**
 *
 *  @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */

@Singleton
class GridgainProvider implements ExecutorServiceProvider {

    /**
     * Logger to use with this class
     */
    private static Logger log = Logger.getLogger("bpipe.executor.GridgainProvider");
    
    private boolean started

    @Lazy
    Grid grid = {

        /*
         * try to look for a bpipe-gridgain.xml file on the current path
         */

        String configFileName = Config.userConfig.get("gridgain.conf.file", "./gridgain.xml")

        File gg = new File(configFileName)
        if( gg.exists() ) {
            log.info( "Launching GridGain with user configuration: '${gg.absolutePath}'" )
            G.start(gg.absolutePath)
        }
        else {
            log.info("Launching GridGain with DEFAULT configuration")
            GridConfigurationAdapter cfg = new GridConfigurationAdapter()
            cfg.setDaemon(true)
            G.start(cfg)
        }


        /*
        * Add an event listener that will shutdown the GridGain executor when the pipeline finishes
        */
        EventManager.instance.addListener(PipelineEvent.FINISHED, new PipelineEventListener() {
            @Override
            void onEvent(PipelineEvent eventType, String desc, Map<String, Object> details) {
                if( started ) {
                    log.info("Shutting down GridGain executor")
                    G.stop(true)
                }
            }
        })


        /*
         * Start GridGain
         */
        def result = G.grid();
        started = true
        return result

    } ()

    @Override
    def getName() { "GridGain"}

    @Override
    ExecutorService getExecutor() {
        return grid.executor()
    }
}