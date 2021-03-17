/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.kazanik.spaceinvaders.server.task;

import java.io.IOException;
import java.util.concurrent.locks.ReadWriteLock;
import pl.kazanik.spaceinvaders.client.exception.ClientDisconnectedException;
import pl.kazanik.spaceinvaders.server.connection.ServerManager;
import pl.kazanik.spaceinvaders.client.Client;
import pl.kazanik.spaceinvaders.client.exception.ExceptionUtils;
import pl.kazanik.spaceinvaders.entity.EntityManager;
import pl.kazanik.spaceinvaders.settings.GameConditions;

/**
 *
 * @author user
 */
public class ClientUpdateTask extends AbstractClientTask {

    private final String location = "server update task execute";
    
    public ClientUpdateTask(String clientToken, ServerManager serverManager, 
            ReadWriteLock clientTaskLock) {
        super(clientToken, serverManager, GameConditions.UPDATE_TASK, clientTaskLock);
    }
    
    @Override
    public void run() {
//    public Boolean call() throws /*Exception*/ IOException {
        try {
            execute();
//            return true;
        } catch(IOException e) {
            serverManager.setUpdateRunning(false);
            try {
                serverManager.disconnectClient(clientToken);
            } finally {
                ClientDisconnectedException cde = new ClientDisconnectedException(
                        clientToken, location, e.getMessage(), e);
                error = cde;
    //            throw cde;
                throw new RuntimeException("ex updating clients", cde);
                //throw e;
            }
        }
    }
    
    @Override
    protected void execute() throws IOException {
//        if(!serverManager.areAtleastTwoConnected())
//            return;
        while(serverManager.checkClientAlive(clientToken)) {
            try {
                Thread.sleep(GameConditions.SERVER_SYNCH_DELAY2);
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
            } catch(InterruptedException ex) {
                if(ExceptionUtils.isCausedByIOEx(ex)) {
                    System.out.println("@@@@@server update execute: "
                        + "update task exception catched, "
                        + "now try stop thread and close resources");
                } else {
                    System.out.println("so timeout");
                }
            }
        }
    }
}
