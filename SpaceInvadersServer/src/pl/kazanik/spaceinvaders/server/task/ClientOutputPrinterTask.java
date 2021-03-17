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
public class ClientOutputPrinterTask extends AbstractClientTask {
    
    private final String location = "server output listener task execute";
    
    public ClientOutputPrinterTask(String clientToken, ServerManager serverManager, 
            ReadWriteLock clientTaskLock) {
        super(clientToken, serverManager, GameConditions.UPDATE_TASK, clientTaskLock);
    }
    
    @Override
    protected void execute() throws IOException {
        Client client = serverManager.getClient(clientToken, location);
        while(serverManager.checkClientAlive(clientToken)) {
            try {
                Thread.sleep(GameConditions.SERVER_SYNCH_DELAY2);
                String outputLine = client.pollOutMessage();
                if(outputLine != null) {
                    client.printLine(outputLine);
                    //System.out.println("out message: "+outputLine);
                    client.setLastHeartBeat(System.currentTimeMillis());
                    System.out.println("output");
                }
            } catch(InterruptedException ex) {
                if(ExceptionUtils.isCausedByIOEx(ex)) {
                    System.out.println("@@@@@server output execute: "
                        + "output task exception catched, "
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
            serverManager.setOutputRunning(false);
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
