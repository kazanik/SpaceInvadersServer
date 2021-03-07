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
import pl.kazanik.spaceinvaders.settings.GameConditions;

/**
 *
 * @author user
 */
public class ClientUpdateTask extends AbstractClientTask {

    private final String location = "update task execute";
    
    public ClientUpdateTask(String clientToken, ServerManager serverManager, 
            ReadWriteLock clientTaskLock) {
        super(clientToken, serverManager, GameConditions.UPDATE_TASK, clientTaskLock);
    }
    
    @Override
    public Boolean call() throws /*Exception*/ IOException {
        try {
            execute();
            return true;
        } catch(IOException e) {
            String exLocation = "update task execute ioex";
            ClientDisconnectedException cde = new ClientDisconnectedException(
                    clientToken, exLocation, e.getMessage(), e);
            error = cde;
            throw cde;
            //throw new RuntimeException("ex updating clients", e);
            //throw e;
        }
    }
    
    @Override
    protected void execute() throws IOException {
        if(!serverManager.areAtleastTwoConnected())
            return;
        Client client = serverManager.getClient(clientToken, location);
        String clientAddress = client.getSocket().getRemoteSocketAddress().toString();
        String inMessage = client.peekInMessage();
        if(inMessage != null && !inMessage.isEmpty() && 
                inMessage.startsWith(GameConditions.SERVER_MODE_SEND)) {
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
        }
        String outMessage = GameConditions.SERVER_MODE_SEND;
        client.pushOutMessage(outMessage);
    }
}
