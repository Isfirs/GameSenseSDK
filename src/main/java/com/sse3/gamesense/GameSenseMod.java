package com.sse3.gamesense;

import com.sse3.gamesense.config.ModConfig;
import com.sse3.gamesense.internal.EventHandler;
import com.sse3.gamesense.internal.EventReceiver;
import com.sse3.gamesense.lib.VersionChecker;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentString;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.FMLInjectionData;

import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.Scanner;

import net.darkhax.tips.TipsAPI;

@Mod(modid = GameSenseMod.MODID,
        name = GameSenseMod.MODNAME,
        version = GameSenseMod.VERSION,
        acceptedMinecraftVersions = GameSenseMod.MINECRAFTVERSIONS,
        updateJSON = GameSenseMod.JSON,
        guiFactory = GameSenseMod.GUIFACTORY
)

public class GameSenseMod
{
    public static final String MODID = "%MODID%";
    public static final String MODNAME = "%MODNAME%";
    public static final String VERSION = "%VERSION%";
    public static final String MINECRAFTVERSIONS = "%MINECRAFTVERSIONS%";
    public static final boolean beta = false;
    public static final String JSON = "https://lateur.pro/mods/gamesense/latest.json";
    static final Boolean isOtherModLoaded = Loader.isModLoaded("tips");
    public static final String GUIFACTORY = "com.sse3.gamesense.lib.ModGuiFactory";

    @Instance(MODID)
    public static GameSenseMod instance;
    public static File minecraftDir;
    public static String currentMcVersion;

    //private HttpURLConnection sse3Connection = null;
    private CloseableHttpClient sseClient = null;
    private HttpPost ssePost = null;
    private Boolean isConnected = false;
    private long lastTick = 0;

    public static Logger logger = LogManager.getLogger("GameSense Mod");

    public void GameSenseMod(){
        if (minecraftDir != null) {
            return;//get called twice, once for IFMLCallHook
        }
        minecraftDir = (File) FMLInjectionData.data()[6];
        currentMcVersion = (String) FMLInjectionData.data()[4];
    }

    public void SendGameEvent(String eventName, int data, EntityPlayer player)
    {
        JSONObject eventData = new JSONObject();
        eventData.put("value", data);
        SendGameEvent(eventName, eventData, player);
    }

    public void SendGameEvent(String eventName, Boolean data, EntityPlayer player)
    {
        JSONObject eventData = new JSONObject();
        eventData.put("value", data);
        SendGameEvent(eventName, eventData, player);
    }

    public void SendGameEvent(String eventName, String data, EntityPlayer player)
    {
        JSONObject eventData = new JSONObject();
        eventData.put("value", data);
        SendGameEvent(eventName, eventData, player);
    }

    private void SendGameEvent(String eventName, JSONObject dataObject, EntityPlayer player)
    {
        JSONObject event = new JSONObject();
        event.put("game", "SSMCMOD");
        event.put("event", eventName);
        event.put("data", dataObject.toString());
        //System.out.println("Sending " + event.toString());
        executePost(event.toString(), player);
    }

    private void executePost(String urlParameters, EntityPlayer player)
    {

        try {

            // If we're not connected, retry after a certain amount of time has elapsed.
            if(!isConnected) {
                // Don't try to reconnect for another 5 seconds
                if(System.currentTimeMillis() - this.lastTick < 5000) {
                    return;
                } else {
                    // reset lastTick and continue
                    this.lastTick = System.currentTimeMillis();
                }
            }

            // Assume we're connected.
            isConnected = true;
            HttpResponse response;
            StringEntity se = new StringEntity(urlParameters);
            se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
            ssePost.setEntity(se);
            response = sseClient.execute(ssePost);

            if(response != null) {
                // Don't care about response?
                //InputStream is = response.getEntity().getContent();

                // reset the post so we can reuse it.
                ssePost.reset();
            }

        } catch (ConnectTimeoutException e) {
            // Couldn't actually connect.
            isConnected = false;
            if(player != null) {
                player.sendMessage(new TextComponentString("There was an error connecting to SteelSeries Engine 3"));
            }
        } catch (Exception e) {
            // Likely a socket timeout w/ "Read timed out" which is fine, we just want to set & forget.
            //e.printStackTrace();
        }

    }

    private boolean isSSE3installed(String jsonAddress){
        return !jsonAddress.isEmpty();
    }

    private void ConnectToSSE3()
    {
        String jsonAddress;
        jsonAddress = "";
        boolean SSE3installed;
        // First open the config file to see what port to connect to.

        // Try to open Windows path one first
        try {
            String corePropsFileName = System.getenv("PROGRAMDATA") + "\\SteelSeries\\SteelSeries Engine 3\\coreProps.json";
            BufferedReader coreProps = new BufferedReader(new FileReader(corePropsFileName));
            jsonAddress = coreProps.readLine();
            SSE3installed = isSSE3installed(jsonAddress);
            logger.debug("Opened coreprops.json and read: " + jsonAddress);
            coreProps.close();
        } catch (FileNotFoundException e) {
            //e.printStackTrace();
            SSE3installed = false;
            logger.error("coreprops.json not found (Mac check)");
        } catch (IOException e) {
            e.printStackTrace();
            SSE3installed = false;
            logger.error("Something terrible happened looking for coreProps.json");
        }

        // If not on Windows, jsonAddress is probably still "", so try to open w/ Mac path
        if(jsonAddress.isEmpty()) {
            try {
                String corePropsFileName = "/Library/Application Support/SteelSeries Engine 3/coreProps.json";
                BufferedReader coreProps = new BufferedReader(new FileReader(corePropsFileName));
                jsonAddress = coreProps.readLine();
                SSE3installed = isSSE3installed(jsonAddress);
                logger.debug("Opened coreprops.json and read: " + jsonAddress);
                coreProps.close();
            } catch (FileNotFoundException e) {
                //e.printStackTrace();
                SSE3installed = false;
                logger.error("coreprops.json not found (Windows check)");
            } catch (IOException e) {
                e.printStackTrace();
                SSE3installed = false;
                logger.error("Something terrible happened looking for coreProps.json");
            }
        }

        if (SSE3installed){
            try {
                // If we got a json string of address of localhost:<port> open a connection to it
                String sse3Address;
                if(!jsonAddress.isEmpty()) {
                    JSONObject obj = new JSONObject(jsonAddress);
                    sse3Address = "http://" + obj.getString("address") + "/game_event";
                } else {
                    // Debug default:
                    sse3Address = "http://localhost:3000/game_event";
                }

                sseClient = HttpClients.createDefault();
                RequestConfig sseReqCfg = RequestConfig.custom()
                        .setSocketTimeout(10)
                        .setConnectTimeout(10)
                        .setConnectionRequestTimeout(50)
                        .build();
                ssePost = new HttpPost(sse3Address);
                ssePost.setConfig(sseReqCfg);

            } catch (JSONException e) {
                e.printStackTrace();
                logger.error("Something terrible happened creating JSONObject from coreProps.json.");
            }
        }
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ModConfig.initializeConfiguration();
        if (isOtherModLoaded){
            TipsAPI.addTips(tips -> {
                tips.add("Do you have Steelseries Engine 3 already installed?");
                tips.add("You can choose the colors of every game event is Steelseries Engine 3?");
                tips.add("With the 'GameSense Mod' you can bind game events to your devices.");
                tips.add("GameSense Mod is regularly updated and upgraded with new features.");
                tips.add("Bugs of Gamesense Mod can be reported on the Curseforge-page.");
            });
        }
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event)
    {
        if (ModConfig.modEnabled){
            ConnectToSSE3();
        }
        if (event.getSide().isClient()) {
            if (ModConfig.CheckForUpdates) {
                VersionChecker.updateCheck("GameSenseMod");
            }
        }
        MinecraftForge.EVENT_BUS.register(new EventHandler());
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event)
    {
        MinecraftForge.EVENT_BUS.register(new EventReceiver(Minecraft.getMinecraft()));
    }

}