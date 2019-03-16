package com.sse3.gamesense;

import com.sse3.gamesense.config.Config;
import com.sse3.gamesense.config.GameSenseModConfig;
import com.sse3.gamesense.internal.EventHandler;
import com.sse3.gamesense.internal.EventReceiver;
import com.sse3.gamesense.lib.VersionChecker;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentString;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

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

@Mod(GameSenseMod.MODID)
public class GameSenseMod
{
    public static final String MODID = "gamesense";
    public static final String VERSION = "1.12.10";
    public static final String MINECRAFTVERSIONS = "1.13.2";
    public static final boolean beta = false;
    public static GameSenseMod instance;

    private static CloseableHttpClient sseClient = null;
    private static HttpPost ssePost = null;
    private Boolean isConnected = false;
    private long lastTick = 0;

    public static Logger logger = LogManager.getLogger("GameSense Mod");

    public GameSenseMod()
    {
        instance = this;
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_CONFIG);

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::initClient);

        MinecraftForge.EVENT_BUS.register(this);

        Config.loadConfig(Config.CLIENT_CONFIG, FMLPaths.CONFIGDIR.get().resolve("gamesense-client.toml"));

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
                ssePost.reset();
            }

        } catch (ConnectTimeoutException e) {
            // Couldn't actually connect.
            isConnected = false;
            if(player != null) {
                player.sendMessage(new TextComponentString("There was an error connecting to SteelSeries Engine 3"));
            }
        } catch (Exception e) {
            //e.printStackTrace();
        }

    }

    private static boolean isSSE3installed(String jsonAddress)
    {
        return !jsonAddress.isEmpty();
    }

    private static void ConnectToSSE3()
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

    @OnlyIn(Dist.CLIENT)
    private void initClient(final FMLCommonSetupEvent event)
    {
        if(GameSenseModConfig.modEnabled.get()){
            GameSenseMod.ConnectToSSE3();
        }
        if (GameSenseModConfig.CheckForUpdates.get()) {
            VersionChecker.updateCheck(GameSenseMod.MODID);
        }
        MinecraftForge.EVENT_BUS.register(new EventHandler());
        MinecraftForge.EVENT_BUS.register(new EventReceiver(Minecraft.getInstance()));
    }

}