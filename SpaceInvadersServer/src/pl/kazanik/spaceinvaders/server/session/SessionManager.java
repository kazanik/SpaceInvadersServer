/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.kazanik.spaceinvaders.server.session;

import java.util.concurrent.atomic.AtomicInteger;
import pl.kazanik.spaceinvaders.settings.GameConditions;

/**
 *
 * @author user
 */
public class SessionManager {
    
    private final AtomicInteger tokenCounter = new AtomicInteger(0);
    
    public String generateClientToken() {
        String token = GameConditions.CLIENT_TOKEN_PREFIX 
                + GameConditions.CLIENT_TOKEN_BODY + tokenCounter.incrementAndGet()
                + GameConditions.CLIENT_TOKEN_SUFFIX;
        System.out.println("token counter: " + tokenCounter.get());
        
        return token;
    }
    
    public int getTokenCounter() {
        return tokenCounter.get();
    }
    
    /*public void decrementTokenCounter() {
        tokenCounter.getAndDecrement();
    }*/
    
    public void resetTokenCounter() {
        tokenCounter.set(0);
    }
}
