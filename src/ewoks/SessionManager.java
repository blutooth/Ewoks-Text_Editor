package ewoks;

import java.util.ArrayList;

public class SessionManager {
	// The Session Manager state
	/**List of sessions or Editors*/
    private ArrayList<Editor>  sessions = new ArrayList<Editor>();   
    /**The active session's Editor*/
    private  Editor app;
    /**The display used throughout the running of the program*/
    private Display display;
    /**Gives the position of the active Session (indexed from 0) */
    private int pos =0;  
    /** Total number of sessions that can be activated without opening any new sessions */
    private int total=1; 
    
    
    //Other variables and methods
    /** ClipBoard value updated when changing a session
     *  Need to give the newly activated app the old app's clipBoard value */
    private Text.Immutable clipBoard= null; 
    
    /** Tells us whether the SessionManager should continue with its commandLoop*/ 
    private  Boolean isAlive =true;
    
    /** Arguments for sessionCmd to either open, close or cycle through sessions*/
    public static final int OPEN =0, CLOSE=1, CYCLE=2, CLOSEALL=3; 
    
    /**If isalive:
     *Sets app.alive to true so app.commandloop won't terminate and then runs app */
    public void commandLoop() {
    	while (isAlive) {
        	app.setAlive();
    	    app.commandLoop();
    	}
    }
    
    /** Initialise or reinitialise app
     * Connects app to SesionManager and display
     * Sets app.clipBoard, app.ed.count and app.ed.session  */
    public void attach() {
    	app.setSessionManager(this);
    	app.setCountSession(total, pos+1);
    	app.activate(display);
    	app.setClipBoard(clipBoard);
    }
    
    /** Either opens a new session, cycles to next Session, closes Session or closes all Sessions */
    public void sessionCmd(int how) {
    	clipBoard= app.getClipBoard();
    	switch (how) {
        case OPEN:
        	sessions.add(pos, app);
        	app= new Editor(); 
        	total++;
        	pos =total-1;
        	break;
        	
        case CYCLE:
        	sessions.add(pos,app);
        	pos = (pos+1) % total;
        	app=sessions.remove(pos);
        	break;
        	
        case CLOSE:
        	total-=1;
        	if (total==0) {isAlive=false;}
        	else{
        	  pos= (total+pos-1) % total;
        	  app=sessions.remove(pos);
        	}
        	break;
        	
       case CLOSEALL:
            isAlive=false;
            break;
    	   
        default:
            throw new Error("Bad sessionCommand argument");
        }
    	
    	if (isAlive) {attach();}
    }
    

    
    /** Main program for the entire Ewoks application. Creates the SessionManager, Display and Terminal
     * Then calls SessionManager's command loop and performs a system exit after the loop has terminated */
    public static void main(String args[]) {
        if (args.length > 1) {
            System.err.println("Usage: ewoks [file]");
            System.exit(2);
        }
        SessionManager sm = new SessionManager();
        sm.app=new Editor(); 
        Terminal terminal = new Terminal("EWOKS");
        terminal.activate();
        sm.display = new Display(terminal);
    	sm.attach(); 	
        if (args.length > 0) sm.app.loadFile(args[0]);
        sm.commandLoop();
        System.exit(0);
    }



}
