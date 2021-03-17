/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.kazanik.spaceinvaders.server.task;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReadWriteLock;
import pl.kazanik.spaceinvaders.client.Client;
import pl.kazanik.spaceinvaders.server.connection.ServerManager;
import pl.kazanik.spaceinvaders.client.exception.ExceptionUtils;
import pl.kazanik.spaceinvaders.entity.EntityManager;
import pl.kazanik.spaceinvaders.settings.GameConditions;

/**
 *
 * @author user
 */
public class ClientListenerTask extends AbstractClientTask {

    private boolean runn;
    private int frames;
    
    public ClientListenerTask(String clientToken, ServerManager serverManager,
            ReadWriteLock clientTaskLock) {
        super(clientToken, serverManager, GameConditions.LISTENER_TASK, clientTaskLock);
        runn = true;
    }
    
    @Override
    protected void execute() throws IOException {
        int max_frames = 2_000_000_000;
        String location = "server client listener task execute";
        while(serverManager.checkClientAlive(clientToken)) {
            try {
//                System.out.println("client listener 1");
                frames++;
                Thread.sleep(GameConditions.SERVER_SYNCH_DELAY2);
//                Thread.sleep(1000);
                //if(frames%2 == 0) {
                // update
                Client client = serverManager.getClient(clientToken, location);
                String inMessage = client.peekInMessage();
                EntityManager em = EntityManager.getInstance();
                if(inMessage != null && !inMessage.isEmpty() && 
                        inMessage.startsWith(GameConditions.SERVER_MODE_SEND)) {
//                    client.setLastHeartBeat(System.currentTimeMillis());
                    serverManager.updateClient(client.getToken(), location);
                    client.pollInMessage();
                    String[] inMessageSplitArray = inMessage.split(
                            GameConditions.MESSAGE_FRAGMENT_SEPARATOR);
                    String inSerEnts = inMessageSplitArray[1];
                    for(Client clientOther : serverManager.getClients()) {
                        String clientOtherToken = clientOther.getToken();
                        if(!clientOtherToken.equals(clientToken)) {
                            //clientOther.printLine(GameConditions.SERVER_MODE_RECEIVE
                                //+ GameConditions.MESSAGE_FRAGMENT_SEPARATOR + inSerEnts);
                            String outMessage = GameConditions.SERVER_MODE_RECEIVE
                                + GameConditions.MESSAGE_FRAGMENT_SEPARATOR + inSerEnts;
                            clientOther.pushOutMessage(outMessage);
                        }
                    }
                    System.out.println("update");
                }
                String serEnts = em.serializeServerEntities();
                String outMessage = GameConditions.SERVER_MODE_SEND + 
                        GameConditions.MESSAGE_FRAGMENT_SEPARATOR + serEnts;
                client.pushOutMessage(outMessage);
//                System.out.println("client listener 2");
                // io
                // input
                String inputLine = "";
                try {
                    inputLine = client.readLine();
                } catch(SocketTimeoutException te) {}
//                System.out.println("client listener 21");
                if(inputLine != null && !inputLine.isEmpty()) {
                    client.pushInMessage(inputLine);
//                    System.out.println("client listener 22");
        //            System.out.println(client.getToken()+", in message: "+inputLine
        //                +", last heartbeat: "+client.getLastHeartBeat());
                    serverManager.updateClient(client.getToken(), location);
//                    client.setLastHeartBeat(System.currentTimeMillis());
                    System.out.println("input");
                }
//                System.out.println("client listener 3");
                // output
                String outputLine = client.pollOutMessage();
                if(outputLine != null) {
                    client.printLine(outputLine);
                    //System.out.println("out message: "+outputLine);
                    System.out.println("output");
                }
//                System.out.println("client listener 4");
                serverManager.checkClientAlive(clientToken);
                if(frames >= max_frames)
                    frames = 0;
//                System.out.println("client listener 5");
            } catch(InterruptedException ex) {
//            catch (ExecutionException ex) {
                if(ExceptionUtils.isCausedByIOEx(ex)) {
                    System.out.println("@@@@@client listener execute: "
                        + "listen/update task exception catched, "
                        + "now try stop thread and close resources");
                    runn = false;
                } else {
                    System.out.println("so timeout");
                }
            }
        }
        serverManager.disconnectClient(clientToken);
    }

    @Override
    public void run() {
//    public Boolean call() throws Exception {
        try {
            execute();
//            return true;
        } catch(IOException e) {
            System.out.println("listener run catch io");
            try {
                serverManager.disconnectClient(clientToken);
            } finally {
                error = e;
                throw new RuntimeException("ex client listener", e);
    //            throw e;
            }
        }
    }
    
}
