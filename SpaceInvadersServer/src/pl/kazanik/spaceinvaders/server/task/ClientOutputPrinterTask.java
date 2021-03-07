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
import pl.kazanik.spaceinvaders.server.connection.ServerManager;
import pl.kazanik.spaceinvaders.settings.GameConditions;

/**
 *
 * @author user
 */
public class ClientOutputPrinterTask extends AbstractClientTask {
    
    private final String location = "input listener task execute";
    
    public ClientOutputPrinterTask(String clientToken, ServerManager serverManager, 
            ReadWriteLock clientTaskLock) {
        super(clientToken, serverManager, GameConditions.UPDATE_TASK, clientTaskLock);
    }
    
    @Override
    protected void execute() throws IOException {
        Client client = serverManager.getClient(clientToken, location);
        String outputLine = client.pollOutMessage();
        if(outputLine != null) {
            client.printLine(outputLine);
            //System.out.println("out message: "+outputLine);
        }
    }

    @Override
    public Boolean call() throws Exception {
        try {
            execute();
            return true;
        } catch(IOException e) {
            String exLocation = "input listener task execute ioex";
            ClientDisconnectedException cde = new ClientDisconnectedException(
                    clientToken, exLocation, e.getMessage(), e);
            error = cde;
            throw cde;
            //throw new RuntimeException("ex listening client heartbeat", e);
            //throw e;
        }
    }
}
