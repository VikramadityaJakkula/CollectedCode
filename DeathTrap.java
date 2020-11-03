//==============================================================================
//  Filename:       DeathTrap.java
//  Authors:        Jeff Jewell     -> ClosingSequence.java
//                  Chris Latham    -> Sound Design and Effects
//                  Fred Palmer     -> Lead Programmer and Design
//  Due Date:       24 April 2001
//  Instructor:     Dr. Jungsoon Yoo, Middle Tennesse State University
//  Purpose:        Game using the 2-D API to mimic 3D perpective.
//                  Features include:
//                  a. Depth shading
//                  b. Sound effects and score for game events.
//                  c. Fading effects using alpha values for rendering graphics
//                  d. Game timer using a single thread
//
//  Object:         You must escape the Death Trap by navigating through a series 
//                  of ten mazes. In order to clear a maze, you must locate and 
//                  step on the green panels. Doing so will transport you to the 
//                  next level. Once you clear all ten mazes, the game is complete. 
//                  You have a total of thirty seconds to clear each maze. You 
//                  have a total of three lives. If you should fail to clear a 
//                  level, you will lose one life. If you should fail to clear 
//                  all ten mazes before losing three lives, you will rot in the 
//                  Death Trap for eternity!                                                        
//
//  Dependencies:   Map.java             -> Class that encapsulates the actual
//                                          maps and its operations.
//                  OpeningSequence.java -> File used for the opening sequence
//                                          or the start screen.
//                  ClosingSequence.java -> File used for the closing sequence 
//                                          or the credits screen.
//                  Compass.java         -> File used for the creation of the 
//                                          minimap and its operations and 
//                                          rendering.
//                  DeathTrap.java       -> The main class file that puts it all
//                                          together.
//                  SoundEffects.java    -> File that handles the sound effects.
//                  Player.java          -> File that handles the player operations
//                                          and data such as the how much time
//                                          the player has left, lives, health,
//                                          soundevents mapped to player state.
//                  Viewport.java        -> File that handles the actual finding
//                                          of the x-axis, y-axis intersection
//                                          of the map.  This is where the 
//                                          map gets rendered on the screen.
//==============================================================================

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.applet.AudioClip;
import java.awt.geom.AffineTransform;

public class DeathTrap extends JApplet implements Runnable
{
    //--------------------------------------------------------------------------
    //  States of the game.
    //--------------------------------------------------------------------------
    static final byte       
    BEGINNING = 0, RUNNING = 1, END = 2, GAME_OVER = 3;
    private int             alphaDirection;         //direction of the fade.
    private int             gameState;              //game state 

    //--------------------------------------------------------------------------
    //  Used for the "game over" animation.
    //--------------------------------------------------------------------------
    private int             gameOverAnimationCounter;
    private int             x, y;
    private int             leftDoorxPoints[], 
    leftDooryPoints[], rightDoorxPoints[], rightDooryPoints[];

    private float           alpha;              //used for fading effects
    private boolean         running;            //used in the thread.
    private boolean         levelFade;          //used for level transition
    private long            timeToSleep;        //time for the thread to sleep
    private Thread          ticker;             //the thread used in game.
    private Dimension       appletDimensions;   //the dimensions of the applet.
    private Graphics2D      g2Context;          //graphics context to draw on
    private Font            myFont;             //Font used in the game.

    //--------------------------------------------------------------------------
    //  Loaded images from the images directory.
    //--------------------------------------------------------------------------
    private Image           title, pressAnyKey, healthImage, livesImage,
    gameOverImage, background;

    //--------------------------------------------------------------------------
    //  Created offscreen image.
    //--------------------------------------------------------------------------
    private Image           imageToCreate;

    //--------------------------------------------------------------------------
    //  User defined classes.
    //--------------------------------------------------------------------------
    private OpeningSequence begScene;
    private Map             map;
    private Compass         miniRadar;
    private ViewPort        view;
    private ClosingSequence endScene;
    private SoundEffects    sounds;
    private Player          player;

    //--------------------------------------------------------------------------
    //  Sound effects used in the game.
    //--------------------------------------------------------------------------
    private AudioClip       introMusic, introLoop, mazeSolved, mazeLoop, 
    playerHit, playerDeath, monsterHit, monsterDeath, gunshot, credits;

    //--------------------------------------------------------------------------
    //  Inner class that handles keys pressed in the applet.  Since the
    //  KeyListener (and all listener classes) are abstract, we extend an
    //  Adapter class that takes care of all the empty methods.  
    //--------------------------------------------------------------------------
    class MazeKeyAdapter extends KeyAdapter
    {
        public void keyPressed(KeyEvent e)
        {
            //------------------------------------------------------------------
            //  Get the key code of the key that was just hit.
            //------------------------------------------------------------------
            int keyCode = e.getKeyCode();

            switch(gameState)
            {
                case BEGINNING:
                    setState(RUNNING); 
                    break;
                case RUNNING:
                    //----------------------------------------------------------
                    //  If space bar, play the gun.
                    //----------------------------------------------------------
                    if(keyCode == KeyEvent.VK_SPACE)
                    {
                        sounds.playGun();
                    }
                    //----------------------------------------------------------
                    //  If the escape key is hit, set state to game over.
                    //----------------------------------------------------------
                    else if(keyCode == KeyEvent.VK_ESCAPE)
                    {
                        setState(GAME_OVER);
                    }
                    //----------------------------------------------------------
                    //  Else process the move.
                    //----------------------------------------------------------
                    else
                    {
                        map.doMove(keyCode);
                    }
                    //----------------------------------------------------------
                    //  repaint the applet.
                    //----------------------------------------------------------
                    repaint();
                    break;
                case GAME_OVER:
                    //----------------------------------------------------------
                    //  Basically supress any user input.
                    //----------------------------------------------------------
                    break;
                case END:
                    //----------------------------------------------------------
                    //  set state back to the beginning if space bar, escape or
                    //  enter key is hit.
                    //----------------------------------------------------------
                    switch(keyCode)
                    {
                        case KeyEvent.VK_SPACE:
                        case KeyEvent.VK_ESCAPE:
                        case KeyEvent.VK_ENTER:
                            setState(BEGINNING);
                    }
                    break;
            }
        }
    }            

    //--------------------------------------------------------------------------
    //  init()
    //
    //  Called when the applet is started.
    //
    //--------------------------------------------------------------------------
    public void init()
    {
        //----------------------------------------------------------------------
        //  Get dimensions of applet.
        //----------------------------------------------------------------------
        appletDimensions = new Dimension(getSize());

        //----------------------------------------------------------------------
        //  Allocate memory for the doors.
        //----------------------------------------------------------------------
        leftDoorxPoints = new int[3];
        leftDooryPoints = new int[3];
        rightDoorxPoints = new int[3];
        rightDooryPoints = new int[3];

        //----------------------------------------------------------------------
        //  Assign the initial points for the doors.
        //----------------------------------------------------------------------
        leftDoorxPoints[0] = 0;
        leftDoorxPoints[1] = 0;
        leftDoorxPoints[2] = appletDimensions.width; 
        leftDooryPoints[0] = appletDimensions.height;
        leftDooryPoints[1] = 0;
        leftDooryPoints[2] = 0;
        rightDoorxPoints[0] = 0;
        rightDoorxPoints[1] = appletDimensions.width;
        rightDoorxPoints[2] = appletDimensions.width;
        rightDooryPoints[0] = appletDimensions.height;
        rightDooryPoints[1] = appletDimensions.height;
        rightDooryPoints[2] = 0;

        //----------------------------------------------------------------------
        //  Game over animation stuff.
        //----------------------------------------------------------------------
        gameOverAnimationCounter = 40;
        x = 250; 
        y = 0;

        //----------------------------------------------------------------------
        //  Load the media for the game.
        //----------------------------------------------------------------------
        loadImages();
        loadSoundEffects();

        //----------------------------------------------------------------------
        //  Instantiate the user defined classes.
        //----------------------------------------------------------------------
        map = new Map();
        miniRadar = new Compass(appletDimensions, map);
        view = new ViewPort(appletDimensions, map);
        addKeyListener(new MazeKeyAdapter());

        begScene = 
        new OpeningSequence(appletDimensions, title, pressAnyKey, background);

        endScene = new ClosingSequence(appletDimensions);

        sounds = new SoundEffects(introMusic, 
                                  introLoop, 
                                  mazeSolved,
                                  mazeLoop,
                                  playerHit,
                                  playerDeath,
                                  monsterHit,
                                  monsterDeath,
                                  gunshot,
                                  credits);

        player = new Player(map, 
                            appletDimensions, 
                            this, 
                            healthImage, 
                            livesImage,
                            sounds);

        //----------------------------------------------------------------------
        //  Set up fading values.
        //----------------------------------------------------------------------
        alphaDirection = OpeningSequence.UP;
        alpha = 0.2f;
        levelFade = false;

        //----------------------------------------------------------------------
        //  Make the font used for the program.
        //----------------------------------------------------------------------
        myFont = new Font("serif", Font.PLAIN,  40);
        //----------------------------------------------------------------------
        //  Set the state of the game.
        //----------------------------------------------------------------------
        setState(BEGINNING);
    }                  

    public void paint(Graphics g)                               
    {
        //----------------------------------------------------------------------
        //  Type cast this to a graphics2D object.
        //----------------------------------------------------------------------
        Graphics2D g2 = (Graphics2D) g;

        if(g2Context == null)
        {
            //------------------------------------------------------------------
            //  Create image offscreen for double buffering.
            //------------------------------------------------------------------
            imageToCreate = createImage(appletDimensions.width, 
                                        appletDimensions.height);
            //------------------------------------------------------------------
            //  Get a graphics context for the image so it can be drawn to.
            //------------------------------------------------------------------
            g2Context = (Graphics2D)imageToCreate.getGraphics();
        }

        //----------------------------------------------------------------------
        //  Based on the state of the game perform the appropriate action.
        //----------------------------------------------------------------------
        switch(gameState)
        {
            case BEGINNING:
                //--------------------------------------------------------------
                //  If it's the beginning draw the opening scene.
                //--------------------------------------------------------------
                begScene.draw(g2Context); 
                break;
            case RUNNING:
                //--------------------------------------------------------------
                //  If it's in the running state do the following.
                //--------------------------------------------------------------
                view.drawCurrentScene(g2Context, map);
                player.processPlayer(g2Context, map);
                miniRadar.drawCompass(g2Context, map);
                player.drawCurrentStats(g2Context, map);
                //--------------------------------------------------------------
                //  Check status of player each time through.
                //--------------------------------------------------------------
                if(player.getStatus() == Player.DEAD)
                {
                    setState(GAME_OVER);
                    break;
                }

                //--------------------------------------------------------------
                //  TODO:  This next line is only for debugging purposes.
                //--------------------------------------------------------------
                showStatus("Player position: " + 
                           map.getPlayerPosition().x + ", " + 
                           map.getPlayerPosition().y + 
                           "; Maze end: " + 
                           map.getMazeEnd().x + ", " + 
                           map.getMazeEnd().y +
                           "; Maze solved: " + map.mazeSolved() + 
                           "; Current level: " + map.getLevel() +
                           "; Game state: " + gameState +
                           "; Player state: " + player.getStatus());

                //--------------------------------------------------------------
                //  If the map is solved.
                //--------------------------------------------------------------
                if(map.mazeSolved())
                {
                    //----------------------------------------------------------
                    //  Reset the player after a level solved.
                    //----------------------------------------------------------
                    player.resetAfterLevel();
                    //----------------------------------------------------------
                    //  Draw the doors again.
                    //----------------------------------------------------------
                    resetDoorPoints();
                    System.out.println("Level " + map.getLevel() + " Solved.");
                    //----------------------------------------------------------
                    //  Switch to the next map.
                    //----------------------------------------------------------
                    if(!map.nextMap())
                    {
                        //------------------------------------------------------
                        //  If there are no more maps the game is over.
                        //------------------------------------------------------
                        setState(GAME_OVER);
                        levelFade = true;
                    }

                    //----------------------------------------------------------
                    //  If the game state is not equal to game over after trying
                    //  to hit the next map, then play the new level sound.
                    //----------------------------------------------------------
                    if(gameState != GAME_OVER)
                    {
                        sounds.playMazeSolved();
                    }

                    levelFade = false;
                    miniRadar.drawMapImage(map.getCurrentMap());
                }
                if(levelFade == false)
                {
                    //----------------------------------------------------------
                    //  This is only called on the initial ticks of a level transtion
                    //----------------------------------------------------------
                    drawLevelOpening(g2Context, map.getLevel());
                }
                break;
            case GAME_OVER:
                //--------------------------------------------------------------
                //  Draws the game over animation.
                //--------------------------------------------------------------
                if(gameOverAnimationCounter > 0)
                {
                    view.drawCurrentScene(g2Context, map);
                    drawGameOver(g2Context);
                }
                else
                {
                    //----------------------------------------------------------
                    //  After the counter for the game over counter is over, then
                    //  switch states.
                    //----------------------------------------------------------
                    setState(END);
                }
                --gameOverAnimationCounter;
                break;
            case END:
                //--------------------------------------------------------------
                //  Draw the closing scene.
                //--------------------------------------------------------------
                endScene.draw(g2Context);
                break;
        }

        //----------------------------------------------------------------------
        //  Draw the image that we create each time, for each case to the screen.
        //----------------------------------------------------------------------
        g2.drawImage(imageToCreate, 0, 0, null);
    }

    //--------------------------------------------------------------------------
    //  run()
    //
    //  Used for the thread in the game, making it sleep for the specified time,
    //  while it is in the running mode.
    //
    //--------------------------------------------------------------------------
    public void run()
    {
        while(running)
        {
            repaint();
            try
            {
                ticker.sleep(timeToSleep);
            }
            catch(InterruptedException e)
            {
            }
        }
    }

    //--------------------------------------------------------------------------
    //  start()
    //
    //  Used when the thread is initially started.
    //
    //--------------------------------------------------------------------------
    public synchronized void start()
    {
        if(ticker == null || !ticker.isAlive())
        {
            timeToSleep = 1000 / 40;
            running = true;
            ticker = new Thread(this);
            ticker.setPriority(Thread.MIN_PRIORITY + 1);
            ticker.start();
        }

        switch(gameState)
        {
            case(BEGINNING):
                sounds.playIntroLoop();
                break;
            case(RUNNING):
                sounds.playLevelLoop();
                break;
            case(END):
                sounds.playCreditsLoop();
                break;
        }
    }

    //--------------------------------------------------------------------------
    //  stop()
    //
    //  When the applet is stopped we need to also stop the thread.
    //
    //--------------------------------------------------------------------------
    public synchronized void stop()
    {
        running = false;
        sounds.stopAll();
    }

    //--------------------------------------------------------------------------
    //  setState()
    //
    //  Sets the state of the game.  Takes a new state to change it to.
    //
    //--------------------------------------------------------------------------
    public void setState(int newState)
    {
        //----------------------------------------------------------------------
        //  Based on the new state, perform the appropriate actions.
        //----------------------------------------------------------------------
        switch(newState)
        {
            case RUNNING:
                sounds.playMazeSolved();
                sounds.stopIntroLoop();
                sounds.playLevelLoop();
                timeToSleep = 1000 / 10;
                break;
            case END:
                endScene.resetClosingSequence();
                resetGame();
                sounds.stopLevelLoop();
                sounds.playCreditsLoop();
                timeToSleep = 1000 / 40;
                break;
            case GAME_OVER:
                timeToSleep = 1000 / 200;
                break;
            case BEGINNING:
                resetDoorPoints();
                resetGame();
                sounds.stopCreditsLoop();
                sounds.playIntroLoop();
                timeToSleep = 1000 / 40;
                break;
        }

        //----------------------------------------------------------------------
        //  Set the state.
        //----------------------------------------------------------------------
        gameState = newState;
    }

    //--------------------------------------------------------------------------
    //  drawGameOver()
    //
    //  Draws the game over animation to the screen.
    //
    //--------------------------------------------------------------------------
    public void drawGameOver(Graphics2D g)
    {
        //----------------------------------------------------------------------
        //  Set the y value for drawing to.  Basically if it has come down to 250
        //  then stop adding to it.
        //----------------------------------------------------------------------
        y += (y > 250) ? 0 : 10;

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.40f));
        g.setColor(Color.black);
        g.fillRect(0, 0, appletDimensions.width, appletDimensions.height);
        //----------------------------------------------------------------------
        //  Turn on antialiasing.
        //----------------------------------------------------------------------
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                           RenderingHints.VALUE_ANTIALIAS_ON);

        //----------------------------------------------------------------------
        //  Set the alpha value.
        //----------------------------------------------------------------------
        step();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

        g.drawImage(gameOverImage,
                    new AffineTransform(1f, 0f, 0f, 1f, x, y),
                    null);

        //----------------------------------------------------------------------
        //  Turn off antialiasing.
        //----------------------------------------------------------------------
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                           RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

    }

    //--------------------------------------------------------------------------
    //  resetDoorPoints()
    //
    //  Resets the points for the doors, so they will seem to open at the beginning
    //  of each level.
    //
    //--------------------------------------------------------------------------
    public void resetDoorPoints()
    {
        gameOverAnimationCounter = 40;
        leftDoorxPoints[0] = 0;
        leftDoorxPoints[1] = 0;
        leftDoorxPoints[2] = appletDimensions.width; 
        leftDooryPoints[0] = appletDimensions.height;
        leftDooryPoints[1] = 0;
        leftDooryPoints[2] = 0;
        rightDoorxPoints[0] = 0;
        rightDoorxPoints[1] = appletDimensions.width;
        rightDoorxPoints[2] = appletDimensions.width;
        rightDooryPoints[0] = appletDimensions.height;
        rightDooryPoints[1] = appletDimensions.height;
        rightDooryPoints[2] = 0;
    }

    //--------------------------------------------------------------------------
    //  resetGame()
    //
    //  Helper function to reset the game.
    //
    //--------------------------------------------------------------------------
    public void resetGame()
    {
        x = 250; 
        y = 0;
        player.resetAll();
        map.setBeginningState(0);
        levelFade = false;
    }

    //--------------------------------------------------------------------------
    //  step()
    //
    //  Sets up the fade alpha value.
    //
    //--------------------------------------------------------------------------
    public void step()
    {
        if(alphaDirection == OpeningSequence.UP)
        {
            if((alpha += 0.07) > .99)
            {
                alphaDirection = OpeningSequence.DOWN;
                alpha = 1.0f;
            }
        }
        else if(alphaDirection == OpeningSequence.DOWN)
        {
            if((alpha -= .07) < 0.01)
            {
                alphaDirection = OpeningSequence.UP;
                alpha = 0.0f;
                levelFade = true;
            }
        }
    }

    //--------------------------------------------------------------------------
    //  drawLevelOpening()
    //
    //  Draws the doors and the current level fading effect at the beginning of
    //  each level.
    //
    //--------------------------------------------------------------------------
    public void drawLevelOpening(Graphics2D g, int level)
    {
        drawDoors(g);
        //----------------------------------------------------------------------
        //  Turn on antialiasing.
        //----------------------------------------------------------------------
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                           RenderingHints.VALUE_ANTIALIAS_ON);

        //----------------------------------------------------------------------
        //  Set the alpha value.
        //----------------------------------------------------------------------
        step();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

        //----------------------------------------------------------------------
        //  Draw the oval in the background.
        //----------------------------------------------------------------------
        g.setColor(Color.black);
        g.fillOval(appletDimensions.width / 2 + 17, 
                   (int)appletDimensions.height / 2 - 74, 
                   120, 
                   120);

        g.setColor(Color.red);
        g.setFont(myFont);
        g.drawString("L E V I L " + level, 
                     (int)appletDimensions.width / 2 - 20, 
                     (int)appletDimensions.height / 2);

        //----------------------------------------------------------------------
        //  Turn off antialiasing.
        //----------------------------------------------------------------------
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                           RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }

    //--------------------------------------------------------------------------
    //  drawDoors()
    //
    //  Using leftDoorxPoints, leftDooryPoints, rightDoorxPoints, rightDooryPoints
    //  this method will draw two triangles at the opening of a level that shrink
    //  in size, simulating a door opening, while it is syncronized with a sound
    //  effect.
    //
    //--------------------------------------------------------------------------
    public void drawDoors(Graphics2D g)
    {
        int changeFactor = 60;

        //----------------------------------------------------------------------
        //  Left door settings.
        //----------------------------------------------------------------------
        leftDoorxPoints[0] = 0;
        leftDooryPoints[0] -= changeFactor;

        leftDoorxPoints[1] = 0;
        leftDooryPoints[1] = 0;

        leftDoorxPoints[2] -= changeFactor; 
        leftDooryPoints[2] = 0;

        //----------------------------------------------------------------------
        //  Right door settings.
        //----------------------------------------------------------------------
        rightDoorxPoints[0] += changeFactor;
        rightDooryPoints[0] = appletDimensions.height;

        rightDoorxPoints[1] = appletDimensions.width;
        rightDooryPoints[1] = appletDimensions.height;

        rightDoorxPoints[2] = appletDimensions.width;
        rightDooryPoints[2] += changeFactor;

        g.setColor(Color.darkGray);
        //----------------------------------------------------------------------
        //  Draw the doors.
        //----------------------------------------------------------------------
        g.fillPolygon(leftDoorxPoints, leftDooryPoints, 3);
        g.fillPolygon(rightDoorxPoints, rightDooryPoints, 3);
    }

    //--------------------------------------------------------------------------
    //  loadSoundEffects()
    //
    //  Loads all the sound effects used in the game and puts them into AudioClip
    //  objects.  They are currently located in the sounds/ directory.
    //
    //--------------------------------------------------------------------------
    public void loadSoundEffects()
    {
        introMusic = getAudioClip(getDocumentBase(), "sounds/introMusic.au");
        introLoop = getAudioClip(getDocumentBase(), "sounds/introLoop.au");
        mazeSolved = getAudioClip(getDocumentBase(), "sounds/mazeSolved.au");
        monsterHit = getAudioClip(getDocumentBase(), "sounds/monsterHit.au");
        monsterDeath = getAudioClip(getDocumentBase(), "sounds/monsterDeath.au");
        playerHit = getAudioClip(getDocumentBase(), "sounds/playerHit.au");
        playerDeath = getAudioClip(getDocumentBase(), "sounds/playerDeath.au");
        mazeLoop = getAudioClip(getDocumentBase(), "sounds/mazeLoop.au");
        gunshot = getAudioClip(getDocumentBase(), "sounds/gunshot.au");
        credits = getAudioClip(getDocumentBase(), "sounds/credits.au");
    }

    //--------------------------------------------------------------------------
    //  loadImages()
    //
    //  Loads all images used in the game from the images/ directory and puts
    //  them into Image objects.
    //
    //--------------------------------------------------------------------------
    public void loadImages()
    {
        title = getImage(getDocumentBase(), "images/title.gif");
        pressAnyKey = getImage(getDocumentBase(), "images/press_any_key.gif");
        healthImage = getImage(getDocumentBase(), "images/health.gif");
        livesImage = getImage(getDocumentBase(), "images/lives.gif");
        gameOverImage = getImage(getDocumentBase(), "images/game_over.gif");
        background = getImage(getDocumentBase(), "images/background.jpg");
    }
}
