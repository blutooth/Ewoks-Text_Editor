// Editor.java
// Copyright (c) 2013 J. M. Spivey

package ewoks;



// The editor state extended with methods for editor commands.
class Editor extends Undoable<Editor> {
    private EdBuffer ed = new EdBuffer();
    private SessionManager sm;
    private Display display;
    
    /**ClipBoard storing copied or cut text*/
    private Text.Immutable clipBoard=null;
    

    //Accessors invoked by the Session Manager when Editor is initialised
    /**Sets the SessionManager */
    public void setSessionManager(SessionManager sm){this.sm=sm;}  
    
    /**Sets app Alive so that we can continue with commandLoop */
    public void setAlive() {alive=true;}
    
    /**Allows the SessionManager to tell the Editor to give the EdBuffer a count and session*/
    public void setCountSession(int count, int session) {
    	ed.setCount(count); ed.setSession(session);
    }
    /**On initialisation we need to know the ClipBoard text from the previous Editor.
     *  SessionManager tells us this.*/
    public void setClipBoard(Text.Immutable txt) {clipBoard=txt;}
    
    /**On closing a session, SessionManager needs to know the Clipboard text to pass it on to
     * the next active Editor. */
    public Text.Immutable getClipBoard() {return clipBoard;}
    
    
    //All other variables methods and loops
    /** Whether the command loop should continue */ 
    private boolean alive = true;
    
    /** Direction for use as argument to moveCommand or deleteCommand. */
    public static final int LEFT = 1, RIGHT = 2, UP = 3, DOWN = 4,
        HOME = 5, END = 6, PAGEUP = 7, PAGEDOWN = 8;
    
    /** Amount to scroll the screen for PAGEUP and PAGEDOWN */
    private static final int SCROLL = Display.HEIGHT - 3;
    
    /** Show the buffer on a specified display */
    public void activate(Display display) {
        this.display = display;
        display.show(ed);
        ed.register(display);
        ed.initDisplay();
    }
    
    /** Test if the buffer is modified */
    public boolean isModified() { return ed.isModified(); }

    /** Ask for confirmation if the buffer is not clean */
    public boolean checkClean(String action) {
        if (! isModified()) return true;
        String question = 
            String.format("Buffer modified -- really %s?", action);
        return MiniBuffer.ask(display, question);
    }
    
    /**Ask for confirmation if there are multiple tabs */
    public boolean multTab(String action) {
        if (ed.getCount()<=1) return true;
        	String question = 
                    String.format("Multiple Tabs -- really %s?", action);
            return MiniBuffer.ask(display, question);
        	
    }

    /** Load a file into the buffer */
    public void loadFile(String fname) {
        ed.loadFile(fname);
    }

    /** Command: Move the cursor in the specified direction */
    public void moveCommand(int dir) {
        int p = ed.getPoint();
        int row = ed.getRow(p);

        switch (dir) {
        case LEFT:
            if (p > 0) p--;
            break;

        case RIGHT:
            if (p < ed.length()) p++;
            break;

        case UP:
            p = ed.getPos(row-1, goalColumn());
            break;

        case DOWN:
            p = ed.getPos(row+1, goalColumn());
            break;

        case HOME:
            p = ed.getPos(row, 0);
            break;

        case END:
            p = ed.getPos(row, ed.getLineLength(row)-1);
            break;
            
        case PAGEDOWN:
            p = ed.getPos(row + SCROLL, 0);
            display.scroll(+SCROLL);
            break;

        case PAGEUP:
            p = ed.getPos(row - SCROLL, 0);
            display.scroll(-SCROLL);
            break;

        default:
            throw new Error("Bad direction");
        }

        ed.setPoint(p);
    }

    /** Scrap that records an insertion */
    public class Insertion extends Undoable.Scrap {
        /** Location of insertion */
        int pos;

        /** The text inserted. */
        Text.Immutable text;

        public Insertion(int pos, Text.Immutable text) { 
            this.pos = pos;
            this.text = text;
        } 

        public void undo() {
            ed.deleteRange(pos, text.length());
        }

        public void redo() {
            ed.insert(pos, text);
        }
    }
    
    
    /** Insertion that can be amalgamated with adjacent, similar scraps */
    public class AmalgInsertion extends Undoable.Scrap {
        /** Location of insertion */
        int pos;

        /** The text inserted by all commands amalgamated with this one */
        Text text;
        
        public AmalgInsertion(int pos, char ch) {
            this.pos = pos;
            this.text = new Text(ch);
        }

        public void undo() {
            ed.deleteRange(pos, text.length());
        }

        public void redo() {
            ed.insert(pos, text);
        }

        @Override
        public boolean amalgamate(Undoable.Scrap scrap) {
            try {
                AmalgInsertion other = (AmalgInsertion) scrap;

                if (text.charAt(text.length()-1) == '\n'
                    || other.pos != this.pos + this.text.length()) 
                    return false;

                text.append(other.text);
                return true;
            }
            catch (ClassCastException _) {
                return false;
            }
        }
    }
    
    
    /**Sets the mark to that of the current Editing Position*/
    public void markCommand() {
        ed.setMark(ed.getPoint());
    }
    
    /**Swaps the mark and the current Editing position
     * error if mark not set or mark==point */ 
    public void swapCommand() {
        int mark = ed.getMark();
        if (mark < 0) {
            beep();
            System.out.println("Error: Mark not set.");
            return;
        }
        else {
          int point = ed.getPoint();
          ed.setPoint(mark);
          ed.setMark(point);
        }
    }

    /** Command: Insert a character */
    public Undoable.Scrap insertCommand(char ch) {
        int p = ed.getPoint();
        ed.insert(p, ch);
        ed.setPoint(p+1);
        ed.setModified();
        return new AmalgInsertion(p, ch);
    }

    /** Scrap that records a deletion */
    public class Deletion extends Undoable.Scrap {
        /** Position of the deletion */
        int pos;
        
        /** Character that was deleted */
        Text.Immutable deleted;

        public Deletion(int pos, char deleted) {
            this.pos = pos;
            this.deleted = new Text.Immutable(""+deleted);
        }
        
        public Deletion(int pos, Text.Immutable deleted) {
            this.pos = pos;
            this.deleted = deleted;
        }

        public void undo() {
            ed.insert(pos, deleted);
        }

        public void redo() {
            ed.deleteRange(pos, deleted.length());
        }
    }

    /** Command: Delete in a specified direction */
    public Undoable.Scrap deleteCommand(int dir) {
        int p = ed.getPoint();
        char deleted;
        
        switch (dir) {
        case LEFT:
            if (p == 0) { beep(); return null; }
            p--;
            deleted = ed.charAt(p);
            ed.deleteChar(p); 
            ed.setPoint(p);
            break;

        case RIGHT:
            if (p == ed.length()) { beep(); return null; }
            deleted = ed.charAt(p);
            ed.deleteChar(p);
            break;

        default:
            throw new Error("Bad direction");
        }

        ed.setModified();
        return new Deletion(p, deleted);
    }
    
    /** Command: Save the file */
    public void saveFileCommand() {
        String name = 
            MiniBuffer.readFilename(display, "Write file", ed.getFilename());
        if (name == null || name.length() == 0) return;
        ed.saveFile(name);
    }

    /** Prompt for a file to read into the buffer.  */
    public void replaceFileCommand() {
        if (! checkClean("overwrite")) return;
        String name = 
            MiniBuffer.readFilename(display, "Read file", ed.getFilename()); 
        if (name == null || name.length() == 0) return;

        ed.setPoint(0);
        ed.loadFile(name);
        ed.initDisplay();
        reset();
    }

    public void chooseOrigin() {
        display.chooseOrigin();
    }
    
    /** Quit, after asking about modified buffer and multiple tabs being open */
    public void quit() {
        if (checkClean("quit")){
        	if (multTab("quit")){
               alive = false;
               sm.sessionCmd(SessionManager.CLOSEALL); 
        	}
        }
    }

     
    /** Open a new Editing Session, terminating this editors command loop */
    public void openSession() { 
    	alive=false;
    	sm.sessionCmd(SessionManager.OPEN);
    	
    }
    /** cycle to a new Editing Session, terminating this editors command loop */
    public void cycleSession(){
    	alive=false;
    	sm.sessionCmd(SessionManager.CYCLE);
    }
    
    /** Close current Editing Session, after asking about modified buffer */
    public void closeSession(){
    	if (! checkClean("close session")) return;
    	alive=false;	
    	sm.sessionCmd(SessionManager.CLOSE);
    }

    
    // Command execution protocol
    
    /** Goal column for vertical motion. */
    private int goal = -1, prevgoal;
    
    /** Execute a command, wrapping it in actions common to all commands */
    public Undoable.Scrap obey(Undoable.Action<Editor> cmd) {
        prevgoal = goal; goal = -1;
        display.setMessage(null);
        EdBuffer.Memento before = ed.getState();
        Undoable.Scrap scrap = cmd.execute(this);
        EdBuffer.Memento after = ed.getState();
        ed.update();
        return wrapChange(before, scrap, after);
    }
    
    /** The desired column for the cursor after an UP or DOWN motion */
    private int goalColumn() {  
        /* Successive UP and DOWN commands share the same goal column,
         * but other commands cause it to be reset to the current column */
        if (goal < 0) {
            int p = ed.getPoint();
            goal = (prevgoal >= 0 ? prevgoal : ed.getColumn(p));
        }
        
        return goal;
    }

    /** Beep */
    public void beep() { display.beep(); }

    /** Read keystrokes and execute commands */
    public void commandLoop() {
        activate(display);

        while (alive) {
            int key = display.getKey();
            Keymap.Command<Editor> cmd = keymap.find(key);
            if (cmd != null) 
                cmd.command(this);
            else
                beep();
        }
    }


    private Undoable.Scrap wrapChange(EdBuffer.Memento before,
                Undoable.Scrap change, EdBuffer.Memento after) {
        if (change == null)
            return null;
        else
            return new EditorScrap(before, change, after);
    }
    
    private class EditorScrap extends Undoable.Scrap {
        private EdBuffer.Memento before;
        private Undoable.Scrap change;
        private EdBuffer.Memento after;
        
        public EditorScrap(EdBuffer.Memento before, 
                Undoable.Scrap change, EdBuffer.Memento after) {
            this.before = before;
            this.change = change;
            this.after = after;
        }
        
        public void undo() {
            change.undo(); before.restore();
        }
            
        public void redo() {
            change.redo(); after.restore();
        }
            
        
        public boolean amalgamate(EditorScrap other) {
            if (! change.amalgamate(other.change)) return false;
            after = other.after;
            return true;
        }

        public boolean amalgamate(Undoable.Scrap other) {
            return amalgamate((EditorScrap) other);
        }
    }

    /**Scrap that makes use of Undoable.memoryText
     * to store the underlying Text from PasteCommands that have been undone and could still be redone. */
    public class PasteScrap extends  Undoable<Editor>.MemoryScrap {
    	/** Location of first insertion */
    	int pos;
    	/** The length of the text inserted */
    	int len;

        
        public PasteScrap(int pos, int len) {
            this.pos = pos;
            this.len = len;
        }
        
        public void undo() {
        	push(ed.getRange(pos, len));
            ed.deleteRange(pos, len); 
        }

        public void redo() {
            ed.insert(pos, pop()); 
        }
	
    }

    /**Command returns a PasteScrap
     * Pastes text from clipBoard into the EdBuffer at the editing position & moves cursor to end of the paste.
     * Checks clipBoard =/= null */    
    public Undoable.Scrap pasteCommand(){
        if (clipBoard==null) {
            beep(); 
            System.out.println("No Text Copied");
            return null;
        }
        else   {  
            int point= ed.getPoint();
            ed.insert(point, clipBoard);
            ed.setPoint(point+clipBoard.length());
            ed.setMark(-1);
            ed.setModified();
            return new PasteScrap(point, clipBoard.length());
        }    
    }
    
    /** Command sets clipBoard to the text between the point and the mark.
     *  Returns an error when no text is selected, i.e when mark is not set or mark==point  */
    public void copyCommand() {
        int mark =ed.getMark();
        int point= ed.getPoint();
        if (mark<0 || mark==point) {
            beep(); 
            System.out.println("No Text Selected");
        }
        else   {  
            int first= Math.min(mark,  point);
            int last = Math.max(mark, point);
            int length = last-first;
            Text.Immutable txt =ed.getRange(first,length);     
            clipBoard=txt;
            ed.setMark(-1);
        }
    }
    
    /** Returns a deletion scrap. 
     *  Sets clipBoard to the text between the point and the mark, then deletes this range.
     *  Prints an error message when no text is selected, i.e when mark is not set or mark==point.  */
    public Undoable.Scrap cutCommand(){
        int mark =ed.getMark();
        int point= ed.getPoint();
        if (mark<0 || mark==point) {
            beep(); 
            System.out.println("No Text Selected");
            return null;
        }
        else   {  
        	int first = Math.min(mark,  point);
        	int last = Math.max(mark,  point);
            int length = last-first;
            Text.Immutable txt =ed.getRange(first,length);
            ed.deleteRange(first, length);                //delete 
            ed.setMark(-1); ed.setPoint(first);
            clipBoard=txt;
            ed.setModified();
            return new Deletion(first, txt);
        }    
    }


    /** Keymap for editor commands */
    private static Keymap<Editor> keymap = new Keymap<>();

    /** Editor action that inserts a certain character. */
    private static Keymap.Command<Editor> insertAction(char ch) {
        return editorAction("insertCommand", ch);
    }

    /** Editor action that moves in a certain direction. */
    private static Keymap.Command<Editor> moveAction(int dir) {
        return editorAction("moveCommand", dir);
    }

    /** Editor action that deletes in a certain direction. */
    private static Keymap.Command<Editor> deleteAction(int dir) {
        return editorAction("deleteCommand", dir);
    }
    
    /** Editor action that calls a specified method */
    private static Keymap.Command<Editor>
                editorAction(String name, Object... args) {
        return Undoable.reflectAction(Editor.class, name, args);
    }
    
    static {
        for (char ch = 32; ch < 128; ch++)
            keymap.register((int) ch, insertAction(ch));

        keymap.register(Display.RETURN, insertAction('\n'));
        keymap.register(Display.RIGHT, moveAction(Editor.RIGHT));
        keymap.register(Display.LEFT, moveAction(Editor.LEFT));
        keymap.register(Display.UP, moveAction(Editor.UP));
        keymap.register(Display.DOWN, moveAction(Editor.DOWN));
        keymap.register(Display.HOME, moveAction(Editor.HOME));
        keymap.register(Display.END, moveAction(Editor.END));
        keymap.register(Display.PAGEUP, moveAction(Editor.PAGEUP));
        keymap.register(Display.PAGEDOWN, moveAction(Editor.PAGEDOWN));
        keymap.register(Keymap.ctrl('?'), deleteAction(Editor.LEFT));
        keymap.register(Display.DEL, deleteAction(Editor.RIGHT));
        keymap.register(Keymap.ctrl('A'), moveAction(Editor.HOME));
        keymap.register(Keymap.ctrl('B'), moveAction(Editor.LEFT));
        keymap.register(Keymap.ctrl('D'), deleteAction(Editor.RIGHT));
        keymap.register(Keymap.ctrl('E'), moveAction(Editor.END));
        keymap.register(Keymap.ctrl('F'), moveAction(Editor.RIGHT));
        keymap.register(Keymap.ctrl('G'), editorAction("beep"));
        keymap.register(Keymap.ctrl('L'), editorAction("chooseOrigin"));
        keymap.register(Keymap.ctrl('N'), moveAction(Editor.DOWN));
        keymap.register(Keymap.ctrl('P'), moveAction(Editor.UP));
        keymap.register(Keymap.ctrl('Q'), editorAction("quit"));
        keymap.register(Keymap.ctrl('R'), editorAction("replaceFileCommand"));
        keymap.register(Keymap.ctrl('W'), editorAction("saveFileCommand"));
        keymap.register(Keymap.ctrl('Y'), editorAction("redo"));
        keymap.register(Keymap.ctrl('Z'), editorAction("undo"));
        keymap.register(Keymap.ctrl('M'), editorAction("markCommand")); // q7
        keymap.register(Keymap.ctrl('O'), editorAction("swapCommand")); // q7
        keymap.register(Keymap.ctrl('X'), editorAction("cutCommand"));  //q8
        keymap.register(Keymap.ctrl('C'), editorAction("copyCommand")); //q8
        keymap.register(Keymap.ctrl('V'), editorAction("pasteCommand")); //q8
        keymap.register(Display.F2, editorAction("openSession"));  //(a)
        keymap.register(Display.F3, editorAction("cycleSession")); //(a)
        keymap.register(Display.F4, editorAction("closeSession")); //(a)
        


    }
}
