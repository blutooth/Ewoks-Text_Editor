
/*
Ewoks1
======
Building from the implementation offered by Mike Spivey, I have added state to track a history of editing sessions, an undo
operation, state for different editing sessions (like tabs) to be displayed and also an ability to move between tabs. Below, one
asterisk denotes files that were modified by me. Two asterisks denotes files I created. 

It contains the following classes:

    *Display         Keeps terminal display up to date, and
                    determines policy for scrolling

    *EdBuffer        Holds state of one editing session: text plus
                    current position and other variables

    *Editor          The main application class, with methods
                    that correspond to undoable editing commands;
                    these methods are bound to keys using reflection.
                    
    *Keymap          Represents a mapping from keystrokes to
                    commands.

    *MiniBuffer      A small editing area that can be displayed on
                    the bottom line of the screen, e.g. for
                    reading file names.  Implements file name completion.

    *PlaneText       A subclass of Text that keeps track of the
                    division of the text into lines.  Little changed.

    Terminal        A simulation of an old-fashioned VDU, implemented
                    as a Swing GUI.  Unchenged.

    Testbed	    An alternative main program that can replay a
                    canned sequence of keystrokes. Unchanged.

    *Text            Represents a text, with methods for inserting
                    and deleting characters.  Extended subtly.
    **TextStack      TextStack is a class that allows for efficient storage of a sequence of text with runs of identical elements
                   Has methods for popping, pushing and reseting. Used by Undoable

    *Undoable       Abstract superclass for classes that provide undoable 
                    actions on some state.  Records a history of actions
                    and provides methods for moving backwards and forwards
                    through the history. Major modifications.
                    
    **SessionManager   contains Main program for the entire Ewoks application. Creates the SessionManager, Display and Terminal
                     Then calls SessionManager's command loop and performs a system exit after the loop has terminated. A 
                     Session Manager stores the Editors/Sessions and allows one to cycle between them.
    
    
*/

/*
 * Copyright (c) 2006, 2013 J. M. Spivey
 * All rights reserved
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
