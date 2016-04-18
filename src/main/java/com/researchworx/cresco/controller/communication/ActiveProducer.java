package com.researchworx.cresco.controller.communication;

import java.util.Map;
import java.util.Timer;
import java.util.Map.Entry;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import com.researchworx.cresco.controller.core.Launcher;
import com.researchworx.cresco.library.messaging.MsgEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActiveProducer {
	public Map<String,ActiveProducerWorker> producerWorkers;

	private Launcher plugin;
	private String URI;
	private Timer timer;

	private static final Logger logger = LoggerFactory.getLogger(ActiveProducer.class);

	class ClearProducerTask extends TimerTask {
		private Logger logger = LoggerFactory.getLogger(ClearProducerTask.class);
	    public void run()  {
	    	logger.trace("Tick");
	    	for (Entry<String, ActiveProducerWorker> entry : producerWorkers.entrySet()) {
	    		ActiveProducerWorker apw = entry.getValue();
	    		if(apw.isActive) {
	    			apw.isActive = false;
		    	} else {
		    		if(apw.shutdown()) {
		    			producerWorkers.remove(entry.getKey());
		    		}
				}
			}
		}
	}
	
	public ActiveProducer(Launcher plugin, String URI)  {
		this.plugin = plugin;
		try {
			producerWorkers = new ConcurrentHashMap<>();
			this.URI = URI;
			timer = new Timer();
			timer.scheduleAtFixedRate(new ClearProducerTask(), 5000, 5000);
			logger.debug("Producer initialized");
		} catch(Exception ex) {
			logger.error("Constructor " + ex.toString());
		}
	}

	public void shutdown() {
		for (Entry<String, ActiveProducerWorker> entry : producerWorkers.entrySet()) {
			entry.getValue().shutdown();
		}
		timer.cancel();
		logger.debug("Producer has shutdown");
	}

	public boolean sendMessage(MsgEvent sm) {
		boolean isSent = false;
		try {
			ActiveProducerWorker apw = null;
			String dstPath;
			if((sm.getParam("dst_region") != null) && (sm.getParam("dst_agent") != null)) {
				dstPath = sm.getParam("dst_region") + "_" + sm.getParam("dst_agent");
			}
			else if(sm.getParam("dst_region") != null) {
				dstPath = sm.getParam("dst_region"); //send to broker for routing
			}
			else {
				return false;
			}

			/*
			String dstPath;
			if(PluginEngine.isRegionalController) {
				dstPath = sm.getParam("dst_region") + "_" + sm.getParam("dst_agent");
			} else {
				dstPath = sm.getParam("dst_region"); //send to broker for routing
			}
			*/
			if(producerWorkers.containsKey(dstPath)) {
				if(this.plugin.isReachableAgent(dstPath)) {
					apw = producerWorkers.get(dstPath);
				} else {
					System.out.println(dstPath + " is unreachable...");
				}
			} else {
				if (this.plugin.isReachableAgent(dstPath)) {
					System.out.println("Creating new ActiveProducerWorker [" + dstPath + "]");
					apw = new ActiveProducerWorker(dstPath, URI);
					producerWorkers.put(dstPath, apw);
				} else {
					System.out.println(dstPath + " is unreachable...");
				}
			}
			if(apw != null) {
				apw.isActive = true;
				apw.sendMessage(sm);
				isSent = true;
			} else {
				System.out.println("apw is null");
			}
		} catch(Exception ex) {
			System.out.println("ActiveProducer : sendMessage Error " + ex.toString());
		}
		return isSent;
	}
}