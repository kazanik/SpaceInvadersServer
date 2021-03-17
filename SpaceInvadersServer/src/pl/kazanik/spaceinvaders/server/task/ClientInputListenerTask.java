/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.kazanik.spaceinvaders.server.task;

import java.io.IOException;
import java.util.concurrent.locks.ReadWriteLock;
import pl.kazanik.spaceinvaders.client.Client;
import pl.kazanik.spaceinvaders.client.exception.ClientDisconnectedException;
import pl.kazanik.spaceinvaders.client.exception.ExceptionUtils;
import pl.kazanik.spaceinvaders.server.connection.ServerManager;
import pl.kazanik.spaceinvaders.settings.GameConditions;

/**
 *
 * @author user
 */
public class ClientInputListenerTask extends AbstractClientTask {
    
    private final String location = "server io listener task execute";
    
    public ClientInputListenerTask(String clientToken, ServerManager serverManager, 
            ReadWriteLock clientTaskLock) {
        super(clientToken, serverManager, GameConditions.UPDATE_TASK, clientTaskLock);
    }
    
    @Override
    protected void execute() throws IOException {
        while(serverManager.checkClientAlive(clientToken)) {
            try {
                Thread.sleep(GameConditions.SERVER_SYNCH_DELAY2);
                Client client = serverManager.getClient(clientToken, location);
                // input
                String inputLine = client.readLine();
                if(inputLine != null && !inputLine.isEmpty()) {
                    client.pushInMessage(inputLine);
        //            System.out.println(client.getToken()+", in message: "+inputLine
        //                +", last heartbeat: "+client.getLastHeartBeat());
                    serverManager.updateClient(client.getToken(), location);
//                    client.setLastHeartBeat(System.currentTimeMillis());
                    System.out.println("input");
                }
                // output
                String outputLine = client.pollOutMessage();
                if(outputLine != null) {
                    client.printLine(outputLine);
                    //System.out.println("out message: "+outputLine);
                    System.out.println("output");
                }
            } catch(InterruptedException ex) {
                if(ExceptionUtils.isCausedByIOEx(ex)) {
                    System.out.println("@@@@@server io execute: "
                        + "io task exception catched, "
                        + "now try stop thread and close resources");
                } else {
                    System.out.println("so timeout");
                }
            }
        }
    }

    @Override
    public void run() {
//    public Boolean call() throws Exception {
        try {
            execute();
//            return true;
        } catch(IOException e) {
            serverManager.setInputRunning(false);
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
}
