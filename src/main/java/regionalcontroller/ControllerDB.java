package regionalcontroller;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import plugincore.PluginEngine;
import shared.MsgEvent;
import shared.MsgEventType;


public class ControllerDB {

	Map<String,AgentNode> agentMap;
    private static final Logger logger = LoggerFactory.getLogger(ControllerDB.class);

    public ControllerDB()
	{
			agentMap = new ConcurrentHashMap<String,AgentNode>();		
	}
	
	public Boolean isNode(String region, String agent, String plugin)
	{
        boolean isNode = false;
		try{
		if((region != null) && (agent != null) && (plugin == null)) //agent node
		{
			if(agentMap.containsKey(agent))
			{
                isNode = true;
			}

		}
		else if((region != null) && (agent != null) && (plugin != null)) //plugin node
		{
			if(isNode(region, agent, null))
			{
				if(agentMap.get(agent).isPlugin(plugin))
				{
                    isNode = true;
				}
			}
		}
		}
		catch(Exception ex)
		{
			logger.error("Controller : ControllerDB : isNode ERROR : " + ex.toString());
		}
        return isNode;
	}
	
	private MsgEvent controllerMsgEvent(String region, String agent, String plugin, String controllercmd)
	{
		MsgEvent ce = new MsgEvent(MsgEventType.CONFIG,null,null,null,"Generated by ControllerDB");
		ce.setParam("controllercmd", controllercmd);
		
		
		if((region != null) && (agent != null) && (plugin != null))
		{
			ce.setParam("src_region", region);
			ce.setParam("src_agent", agent);
			ce.setParam("src_plugin", plugin);
			
		}
		else if((region != null) && (agent != null) && (plugin == null))
		{
			ce.setParam("src_region", region);
			ce.setParam("src_agent", agent);
		}
		return ce;
	}

    public void addNode(String region, String agent, String plugin)
    {
        logger.info("Adding Node: " + region + " " + agent + " " + plugin);
        //CODY
        try{
            if((region != null) && (agent != null) && (plugin == null)) //agent node
            {
                AgentNode aNode = new AgentNode(agent);
                agentMap.put(agent, aNode);
                //add to controller
                if(PluginEngine.hasGlobalController)
                {
                    if(!PluginEngine.globalControllerChannel.addNode(controllerMsgEvent(region,agent,plugin,"addnode")))
                    {
                        logger.info("Controller : ControllerDB : Failed to addNode to Controller");
                    }
                }

            }
            else if((region != null) && (agent != null) && (plugin != null)) //plugin node
            {
                if(!isNode(region, agent, null))
                {
                    //AgentNode aNode = new AgentNode(agent);
                    //agentMap.put(agent, aNode);
                    //CODY
                    addNode(region, agent, null);
                }
                agentMap.get(agent).addPlugin(plugin);
                //add to controller
                if(PluginEngine.hasGlobalController)
                {
                    if(!PluginEngine.globalControllerChannel.addNode(controllerMsgEvent(region,agent,plugin,"addnode")))
                    {
                        logger.info("Controller : ControllerDB : Failed to addNode to Controller");
                    }
                }
            }
        }
        catch(Exception ex)
        {
            logger.error("Controller : ControllerDB : addNode ERROR : " + ex.toString());
        }
    }


    public void setNodeParams(String region, String agent, String plugin, Map<String,String> paramMap)
	{
		//extract config from param Map
		Map<String,String> configMap = PluginEngine.config.buildPluginMap(paramMap.get("msg"));

		try
		{
			
			if((region != null) && (agent != null) && (plugin == null)) //agent node
			{
				agentMap.get(agent).setAgentParams(configMap);
				if(PluginEngine.hasGlobalController)
				{
					MsgEvent ce = controllerMsgEvent(region,agent,plugin,"setparams");
					ce.setParam("configparams", paramMap.get("msg"));
					if(!PluginEngine.globalControllerChannel.setNodeParams(ce))
					{
						System.out.println("Controller : ControllerDB : Failed to setParams for Node on Controller");
					}
				}
			}
			else if((region != null) && (agent != null) && (plugin != null)) //plugin node
			{
				agentMap.get(agent).setPluginParams(plugin, configMap);
				if(PluginEngine.hasGlobalController)
				{
					MsgEvent ce = controllerMsgEvent(region,agent,plugin,"setparams");
					ce.setParam("configparams", paramMap.get("msg"));
					if(!PluginEngine.globalControllerChannel.setNodeParams(ce))
					{
						System.out.println("Controller : ControllerDB : Failed to setParams for Node on Controller");
					}
				}
			}
		}
		catch(Exception ex)
		{
			System.out.println("Controller : ControllerDB : setNodeParams ERROR : " + ex.toString());
		}
	}
	
	public Map<String,String> getNodeParams(String region, String agent, String plugin)
	{
		try{
		if((region != null) && (agent != null) && (plugin == null)) //agent node
		{
			return agentMap.get(agent).getAgentParams();
		}
		else if((region != null) && (agent != null) && (plugin != null)) //plugin node
		{
			return agentMap.get(agent).getPluginParams(plugin);
		}	
		return null;
		}
		catch(Exception ex)
		{
			System.out.println("Controller : ControllerDB : getNodeParams ERROR : " + ex.toString());
			return null;
		}
	}
	
	
	public void setNodeParam(String region, String agent, String plugin, String key, String value)
	{
		try{
		if((region != null) && (agent != null) && (plugin == null)) //agent node
		{
			agentMap.get(agent).setAgentParam(key, value);
		}
		else if((region != null) && (agent != null) && (plugin != null)) //plugin node
		{
			agentMap.get(agent).setPluginParam(plugin, key, value);
		}
		}
		catch(Exception ex)
		{
			System.out.println("Controller : ControllerDB : setNodeParam ERROR : " + ex.toString());
		}
	}
	
	public void removeNode(String region, String agent, String plugin)
	{
		try{
		if((region != null) && (agent != null) && (plugin == null)) //agent node
		{
			agentMap.remove(agent);
			//controller
			if(PluginEngine.hasGlobalController)
			{
				if(!PluginEngine.globalControllerChannel.removeNode(controllerMsgEvent(region,agent,plugin,"removenode")))
				{
					System.out.println("Controller : ControllerDB : Failed to addNode to Controller");
				}
			}
		}
		else if((region != null) && (agent != null) && (plugin != null)) //plugin node
		{
			agentMap.get(agent).removePlugin(plugin);
			//controller
			if(PluginEngine.hasGlobalController)
			{
				if(!PluginEngine.globalControllerChannel.removeNode(controllerMsgEvent(region,agent,plugin,"removenode")))
				{
					System.out.println("Controller : ControllerDB : Failed to addNode to Controller");
				}
			}
		}
		}
		catch(Exception ex)
		{
			System.out.println("Controller : ControllerDB : removeNode ERROR : " + ex.toString());
		}
	}
}
