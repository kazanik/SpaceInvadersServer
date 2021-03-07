/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.kazanik.spaceinvaders.server.main;

import java.io.IOException;
import pl.kazanik.spaceinvaders.client.exception.ClientDisconnectedException;
import pl.kazanik.spaceinvaders.client.exception.ExceptionUtils;
import pl.kazanik.spaceinvaders.difficulty.Difficulties;



/**
 *
 * @author user
 */
public class SpaceInvadersServer {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        //ServerInstanceManager server = new ServerInstanceManager();
        //server.runServer();
        int maxPlayerSlots = 2;
        Difficulties gameDifficulty = Difficulties.EASY;
        ServerMain server = new ServerMain();
        server.runServer(maxPlayerSlots, gameDifficulty);
        /*
        // test #1
        String token = new SessionManager().generateClientToken();
        Matcher tokenMatcher = GameConditions.TOKEN_PATTERN.matcher(token);
        System.out.println(tokenMatcher.groupCount());
        if(tokenMatcher.matches()) {
            System.out.println(tokenMatcher.start());
            System.out.println(tokenMatcher.end());
            System.out.println(tokenMatcher.group());
        }
        
        // test #2
        IOException ioe = new ClientDisconnectedException(
            "tokenA", "exception polymorphism test", "", null);
        ClientDisconnectedException cde = (ClientDisconnectedException) ioe;
        System.out.println(ioe.toString());
        System.out.println(cde.toString());
        
        // test #3
        Exception exCDE = new Exception(new IOException(new ClientDisconnectedException()));
        Exception exNotCDE = new Exception(new IOException(new RuntimeException()));
        
        System.out.println(ExceptionUtils.isRootCauseCDEx(exCDE));  // true
        System.out.println(ExceptionUtils.isRootCauseCDEx(exNotCDE));  // false
        System.out.println(ExceptionUtils.isRootCauseIOEx(exCDE));  // true
        System.out.println(ExceptionUtils.isRootCauseIOEx(exNotCDE));  // true
        */
        System.out.println("cpu cores count: " + Runtime.getRuntime().availableProcessors());
    }
    
}
