/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.kazanik.spaceinvaders.server.task;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReadWriteLock;
import pl.kazanik.spaceinvaders.server.connection.ServerManager;

/**
 *
 * @author user
 */
public abstract class AbstractClientTask implements Runnable /*Callable<Boolean>*/ {
    
    protected final String clientToken;
    protected final ServerManager serverManager;
    protected char[] inStreamBuff;
    protected Exception error;
    protected final String TASK_TYPE;
    protected final ReadWriteLock clientTaskLock;

    public AbstractClientTask() {
        clientToken = null;
        serverManager = null;
        TASK_TYPE = null;
        clientTaskLock = null;
    }
    
    protected AbstractClientTask(String clientToken, ServerManager serverManager,
            String TASK_TYPE, ReadWriteLock clientTaskLock) {
        this.clientToken = clientToken;
        this.serverManager = serverManager;
        this.TASK_TYPE = TASK_TYPE;
        this.clientTaskLock = clientTaskLock;
    }

    protected abstract void execute() throws /*Exception*/ IOException;
    
    protected String printClientPacket(String clientAddress, String packet) {
        return TASK_TYPE + " " + clientAddress + " ::: " + packet;
    }
    
    public String getClientToken() {
        return clientToken;
    }

    public ServerManager getServerManager() {
        return serverManager;
    }

    public Exception getError() {
        return error;
    }
    
}
