package cannon;

/*########################################################################
 *
 * Auth: Sean E. Russell
 *       University of Oregon, CIS
 * Date: Thu Aug 10 10:34:06 PDT 1995
 *
 * Synopsis:
 *
 * - Mon Aug 14 15:23:46 PDT 1995
 *   It's not my fault.  I started this when Java was in it's Alpha-3
 * release, when the documentation was non-existant, examples were
 * scarce, and the AWT package was primative.  I refuse to take any
 * blame for injuries incurred while reading this code.
 *   That said, cannon is a Java class for HotJava which is supposed
 * to provide an example of a simple gravity/velocity environment.  The
 * user has a cannon which he may shoot.  He is able to vary the angle
 * of the cannon and the velocity of the projectile, and view the
 * results of his input.  That's all it does so far, and it doesn't
 * do it well.  AWT is a nightmare.
 *
 * - Thu Aug 17 17:41:59 PDT 1995
 *   Well, considering what I had to go through to figure out how the
 * layout engine behaves, the code doesn't look *too* bad.  Sound and
 * rudimentry animation has been added.  As it is, I would consider
 * this a usable app.
 *   Needing to be done: explosion for target and sounds for a miss.
 * There are numerous cosmetic features that can be worked on, but
 * I'll leave those until someone requests them.
 *
 * - Sun Aug 20 00:34:18 PDT 1995
 *   Ha!  Perfection!  The animation, the smoke trail; the sound...
 * it even refreshes correctly.  I don't like the way the code looks;
 * it's very messy, and a lot of things are hardcode that shouldn't be.
 * Due to the need for my services on a number of other projects, I'm
 * not going to fix things like the dependancy on a small font size in
 * the UI.  Maybe later.
 *   I was harsh on AWT.  Its not poorly designed; its just poorly
 * documented.  All in all, not bad for an Alpha release, even if the
 * layout engine is unpredictable at times.
 *
 ######################################################################*/

import java.awt.*;
import java.net.*;
import java.lang.Math;
import java.lang.Integer;

public class cannon extends java.applet.Applet
{
  double dx, dy, theta, velocity, gravity, windage;
  int xmax, ymax, numshots;
  Color black, erase;
  double x, y;
  double xo[] = new double[4];
  double yo[] = new double[4];
  double rads;
  boolean dosound;
  CannonFrame frame;

  Graphics drawarea;

  Image can, shot, targ, targ2,
	bang1, puff1, puff2, puff3, puff4;

  public void more()
     {
     numshots = 4;
     repaint();
     }

  public void shootShot()
    {
      if (numshots > 0)
	{
	  numshots--;
	  play(getCodeBase(), "sounds/cannon.au");
	  dosound = true;
	  repaint();
	}
    }

  public void loadImages()
    {
      can = getImage(getCodeBase(), "images/cannon.gif");
      shot = getImage(getCodeBase(), "images/ammo.gif");
      targ = getImage(getCodeBase(), "images/target.gif");
      targ2 = getImage(getCodeBase(), "images/target2.gif");
      bang1 = getImage(getCodeBase(), "images/bang1.gif");
      puff1 = getImage(getCodeBase(), "images/puff1.gif");
      puff2 = getImage(getCodeBase(), "images/puff2.gif");
      puff3 = getImage(getCodeBase(), "images/puff3.gif");
      puff4 = getImage(getCodeBase(), "images/puff4.gif");
    }

  public void init()
    {
      loadImages();
      numshots = 4;
      rads = 57.29577866f;
      xmax = 600;
      ymax = 300;
      resize(xmax,ymax+20);
      theta = 60;		// degrees
      theta /= rads;		// -> radians
      velocity = 3;		// m/s x 10^-1
      gravity = .098f;		// m/s^2 x 10^-2
      windage = 0;
      black = Color.black;
      erase = Color.blue;
      frame = new CannonFrame(this);
    }
  
  public void changeAngle(int val)
    {
      Integer temp = new Integer(val);
      theta = (double)val;		// degrees
      theta /= rads;		// -> radians
      frame.ang.setText(temp.toString());
    }

  public void changeVelocity(int val)
    {
      Integer temp = new Integer(val);
      velocity = (double)val / 5;
      frame.vel.setText(temp.toString());
    }

  public void changeGravity(int val)
    {
      Float temp = new Float((float)val / 10);
      gravity = (double)val / 1000;
      frame.grav.setText(temp.toString());
    }

  public void changeWindage(int val)
    {
      Integer temp = new Integer(val);
      windage = (double)val / 200;
      frame.wind.setText(temp.toString());
    }

  public void paint(Graphics g)
    {
      if (can == null) {
	  return;
      }

      drawarea = g;
      //##### Cannon part

      //##### Draw images
      g.drawImage(can, 20, ymax - can.getHeight(this), this);
      g.drawImage(targ, xmax - (targ.getWidth(this) + 10), ymax - targ.getHeight(this), this);
      if (numshots > 3) g.drawImage(shot, 0, ymax - ((2*shot.getHeight(this))+2), this);
      if (numshots > 2) g.drawImage(shot, 2 + shot.getWidth(this), 
				    ymax - ((2*shot.getHeight(this))+2), this);
      if (numshots > 1) g.drawImage(shot, 0, ymax - shot.getHeight(this), this);
      if (numshots  >0) g.drawImage(shot, 2 + shot.getWidth(this), ymax - shot.getHeight(this), this);

      if (numshots < 4)
	{
	  //##### Shoot da shot
	  x = 65;
	  y = ymax-55;
	  dx = velocity * Math.cos(theta);
	  dy = velocity * Math.sin(theta);
	  g.setColor(Color.gray);
	  
	  while ((x > 0) && (x < xmax) && (y < ymax))
	    {
	      if (xo[3] != 0)
		g.clearRect((int)xo[3] - 4, (int)yo[3] - 4, 8, 7);
	      if (xo[0] != 0)
		  g.drawRect((int)xo[0]-1, (int)yo[0]-1, 2, 2);
	      g.drawImage(shot, (int)x - 4, (int)y - 4, this);
	      if (x < 75) g.drawImage(can, 20, ymax - can.getHeight(this), this);
	      if ((x>xmax-(targ.getWidth(this)+10)) && (y>ymax-targ.getHeight(this)))
		g.drawImage(targ, xmax - (targ.getWidth(this)+10), 
			    ymax - targ.getHeight(this), this);
	      if (dosound)try {Thread.sleep(5);} catch (InterruptedException e){}
	      xo[0] = xo[1]; xo[1] = xo[2]; xo[2] = xo[3];
	      yo[0] = yo[1]; yo[1] = yo[2]; yo[2] = yo[3];
	      xo[3] = x; yo[3] = y;
	      x += dx;
	      dx -= windage/2;
	      y -= dy;
	      dy -= gravity/2;
	      
	      //##### Check for hit
	      if ((x>(xmax-43)) && (x<(xmax-27)) && 
		  (y>(ymax-40)) && (y<(ymax-25)))
		{
		  if (dosound)
		    {
		      play(getCodeBase(), "sounds/explosion.au");
		      //##### Draw FX
		      // Bang over full target
		      g.drawImage(bang1, xmax - (targ.getWidth(this)+10), 
				  ymax - targ.getHeight(this), this);
		      
		      // puff1 over targ2
		try {Thread.sleep(75);} catch (InterruptedException e){}
		      g.clearRect(xmax-(targ.getWidth(this)+10), ymax-targ2.getHeight(this),
				  (targ.getWidth(this)+10), targ2.getHeight(this));
		      g.drawImage(targ2, xmax - (targ.getWidth(this)+10), 
				  ymax - targ2.getHeight(this), this);
		      play(getCodeBase(), "sounds/applause.au");
		      g.drawImage(puff1, xmax - (targ.getWidth(this)+15), 
				  ymax - (targ.getHeight(this)+40), this);
		      
		      // Puff2 over targ2
		try {Thread.sleep(75);} catch (InterruptedException e){}
		      g.clearRect(xmax-(targ.getWidth(this)+15), 
				  ymax-(targ.getHeight(this)+40),
				  (targ.getWidth(this)+15), (targ.getHeight(this)+40));
		      g.drawImage(targ2, xmax - (targ.getWidth(this)+10), 
				  ymax - targ2.getHeight(this), this);
		      g.drawImage(puff2, xmax - (targ.getWidth(this)+15), 
				  ymax - (targ.getHeight(this)+40), this);
		      
		      // puff3 over targ2
		try {Thread.sleep(75);} catch (InterruptedException e){}
		      g.clearRect(xmax-(targ.getWidth(this)+15), 
				  ymax-(targ.getHeight(this)+40),
				  (targ.getWidth(this)+15), (targ.getHeight(this)+40));
		      g.drawImage(targ2, xmax - (targ.getWidth(this)+10), 
				  ymax - targ2.getHeight(this), this);	
		      g.drawImage(puff3, xmax - (targ.getWidth(this)+10), 
				  ymax - (targ.getHeight(this)+40), this);
		      
		      // puff4 over targ2
		try {Thread.sleep(75);} catch (InterruptedException e){}
		      g.clearRect(xmax-(targ.getWidth(this)+15), 
				  ymax-(targ.getHeight(this)+40),
				  (targ.getWidth(this)+15), (targ.getHeight(this)+40));
		      g.drawImage(targ2, xmax - (targ.getWidth(this)+10), 
				  ymax - targ2.getHeight(this), this);
		      g.drawImage(puff4, xmax - (targ.getWidth(this)+10), 
				  ymax - (targ.getHeight(this)+40), this);
		      dosound = false;
		      
		try {Thread.sleep(75);} catch (InterruptedException e){}
		    }
		  // targ2
		  g.clearRect(xmax-(targ.getWidth(this)+15), ymax-(targ.getHeight(this)+40),
			      (targ.getWidth(this)+15), (targ.getHeight(this)+40));
		  g.drawImage(targ2, xmax - (targ.getWidth(this)+10), 
			      ymax - targ2.getHeight(this), this);
		  x=0;
		}
	    }
	  for (int i=0; i<4; i++) xo[i] = yo[i] = 0;
	}
    }

  public void start()
    {
    if (frame != null)
	{
	frame.show();
	}
    }      

  public void stop()
     {
     if (frame != null)
	{
	frame.hide();
	}
     }

  public void destroy()
     {
     if (frame != null)
	{
	frame.dispose();
	frame = null;
	}
     }
}

class CannonFrame extends Frame {
    cannon can;

    TextField ang;
    TextField vel;
    TextField grav;
    TextField wind;

    Scrollbar s_ang;
    Scrollbar s_vel;
    Scrollbar s_grav;
    Scrollbar s_wind;

    CannonFrame(cannon can) {
	this.can = can;

	setTitle("Cannon");

	Panel p;
	add("North", p = new Panel());

	p.add(new Label("Ang:"));
	p.add(ang = new TextField("60", 4));
	ang.setEditable(false);

	p.add(new Label("Vel:"));
	p.add(vel = new TextField("15", 4));
	vel.setEditable(false);

	p.add(new Label("Grav:"));
	p.add(grav = new TextField("9.8", 4));
	grav.setEditable(false);

	p.add(new Label("Wind:"));
	p.add(wind = new TextField("0", 4));
	wind.setEditable(false);

	add("Center", p = new Panel());
	p.setLayout(new GridLayout(4, 0));

	p.add(s_ang = new Scrollbar(Scrollbar.HORIZONTAL));
	s_ang.setValues(60, 10, 1, 90);

	p.add(s_vel = new Scrollbar(Scrollbar.HORIZONTAL));
	s_vel.setValues(10, 2, 1, 30);

	p.add(s_grav = new Scrollbar(Scrollbar.HORIZONTAL));
	s_grav.setValues(98, 10, 40, 200);

	p.add(s_wind = new Scrollbar(Scrollbar.HORIZONTAL));
	s_wind.setValues(0, 1, -10, 10);

	add("South", p = new Panel());
	p.add(new Button("Shoot"));
	p.add(new Button("More Ammo"));
	pack();
	//list();
    }

    public boolean handleEvent(Event evt) {
	//System.out.println("evt = " + evt);
	if (evt.target == s_ang) {
	    can.changeAngle(((Integer)evt.arg).intValue());
	    return true;
	}
	if (evt.target == s_vel) {
	    can.changeVelocity(((Integer)evt.arg).intValue());
	    return true;
	}
	if (evt.target == s_grav) {
	    can.changeGravity(((Integer)evt.arg).intValue());
	    return true;
	}
	if (evt.target == s_wind) {
	    can.changeWindage(((Integer)evt.arg).intValue());
	    return true;
	}
	if ("Shoot".equals(evt.arg)) {
	    can.shootShot();
	    return true;
	}
	if ("More Ammo".equals(evt.arg)) {
	    can.more();
	    return true;
	}
	return super.handleEvent(evt);
    }
}
