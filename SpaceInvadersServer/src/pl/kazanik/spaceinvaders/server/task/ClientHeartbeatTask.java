/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.kazanik.spaceinvaders.server.task;

import java.io.IOException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.regex.Matcher;
import pl.kazanik.spaceinvaders.client.exception.ClientDisconnectedException;
import pl.kazanik.spaceinvaders.server.connection.ServerManager;
import pl.kazanik.spaceinvaders.client.Client;
import pl.kazanik.spaceinvaders.client.exception.ExceptionUtils;
import pl.kazanik.spaceinvaders.settings.GameConditions;

/**
 *
 * @author user
 */
public class ClientHeartbeatTask extends AbstractClientTask {

    private final String location = "server heartbeat task execute";
    
    public ClientHeartbeatTask(String clientToken, ServerManager serverManager,
            ReadWriteLock clientTaskLock) {
        super(clientToken, serverManager, GameConditions.HEARTBEAT_TASK, clientTaskLock);
    }
    
    @Override
    public void run() {
//    public Boolean call() throws /*Exception*/ IOException {
        try {
            execute();
//            return true;
        } catch(IOException e) {
            serverManager.setHeartbeatRunning(false);
            try {
                serverManager.disconnectClient(clientToken);
            } finally {
                ClientDisconnectedException cde = new ClientDisconnectedException(
                        clientToken, location, e.getMessage(), e);
                error = cde;
    //            throw cde;
                throw new RuntimeException("ex listening client heartbeat", cde);
                //throw e;
            }
        }
    }
    
    @Override
    protected void execute() throws IOException {
        Client client = serverManager.getClient(clientToken, location);
        while(serverManager.checkClientAlive(clientToken)) {
            try {
                Thread.sleep(GameConditions.SERVER_SYNCH_DELAY2);
                String inMessage = client.peekInMessage(); //2
                if(inMessage != null && !inMessage.isEmpty() && 
                        inMessage.startsWith(GameConditions.SERVER_MODE_HEARTBEAT)) {
                    client.pollInMessage();
                    String[] inMessageSplitArray = inMessage.split(
                            GameConditions.MESSAGE_FRAGMENT_SEPARATOR);
                    String inToken = inMessageSplitArray[1];
                    Matcher tokenMatcher = GameConditions.TOKEN_PATTERN.matcher(inToken);
                    //if(tokenMatcher.matches()) {}
                    client.setLastHeartBeat(System.currentTimeMillis());
                    System.out.println("heartbeat");
                }
                String outMessage = GameConditions.SERVER_MODE_HEARTBEAT + 
                    GameConditions.MESSAGE_FRAGMENT_SEPARATOR + clientToken;
                //client.printLine(outMessage);
                client.pushOutMessage(outMessage);
            } catch(InterruptedException ex) {
                if(ExceptionUtils.isCausedByIOEx(ex)) {
                    System.out.println("@@@@@server heartbeat execute: "
                        + "heartbeat task exception catched, "
                        + "now try stop thread and close resources");
                } else {
                    System.out.println("so timeout");
                }
            }
        }
    }
    
}
