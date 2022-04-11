
import robocode.*;
import robocode.util.Utils;
import java.awt.Color;
import java.awt.geom.*;
import java.util.*;


 
public class Team1 extends AdvancedRobot
{
	
	private final static int RADARTIMEOUT = 20;	
	private Point2D	myPosition;					
	private	Point2D	myGoalPosition;				
	private static Point2D	centerPoint;		
	private long	currTime;					

	
	private long	lastscan;				

	
	private double	oldHeading;				
	private EnemyInfo currentTarget;		
	private HashMap	enemies;				

	
	private static double	fieldWidth;		
	private static double	fieldHeight;	
    private RoundRectangle2D.Double playField; 
	private Rectangle2D.Double fireField;	



	
	public void run() {
		
		setColors( Color.red, Color.orange, Color.yellow);

		fieldWidth = getBattleFieldWidth();
		fieldHeight = getBattleFieldHeight();
        playField = new RoundRectangle2D.Double( 35, 35,
	    fieldWidth - 70, fieldHeight - 70, 50, 50);
        fireField = new Rectangle2D.Double( 17, 17, fieldWidth - 34, fieldHeight - 34);
		centerPoint = new Point2D.Double( fieldWidth / 2, fieldHeight / 2 );


	
		setAdjustGunForRobotTurn( true);
		setAdjustRadarForGunTurn( true);
		setTurnRadarRight( Double.POSITIVE_INFINITY);

	
		enemies = new HashMap();
		myPosition = new Point2D.Double();
		myGoalPosition = new Point2D.Double( getX(), getY());

		
		while( true ) {
			currTime = getTime();
			
			doMeleeMovement();
			if (getRadarTurnRemaining() == 0.0) {
				setTurnRadarRight( Double.POSITIVE_INFINITY);
			}
			execute();
		}
	}

	
	public void onScannedRobot(ScannedRobotEvent e) {

		double absBearing;
		double dist;
		double power;
		boolean fired = false;
		EnemyInfo enemy = (EnemyInfo)enemies.get( e.getName());
		if (enemy == null)
			enemies.put( e.getName(), enemy = new EnemyInfo());

		myPosition.setLocation( getX(), getY());
		
		absBearing = getHeadingRadians() + e.getBearingRadians();
		dist = e.getDistance();
		enemy.setLocation( myPosition.getX() + Math.sin( absBearing) * dist, myPosition.getY() + Math.cos( absBearing) * dist);

		
		enemy.attrac = (e.getEnergy() < 20 ? dist * 0.75 : dist);
		if (enemy != currentTarget) {
			if ((currentTarget == null) || ((enemy.attrac < (currentTarget.attrac * 0.8)) && (currentTarget.attrac > 80) && (dist < 1200))) {
				currentTarget = enemy;	
			}
		}
		if (enemy != currentTarget) return;		

		
		if (currTime < 10) {
			setTurnGunRightRadians( Utils.normalRelativeAngle( absBearing - getRadarHeadingRadians()));
			return;
		}

		
		power = ( dist > 850 ? 0.1 : (dist > 700 ? 0.49 : (dist > 250 ? 1.9 : 3.0)));
		power = Math.min( getEnergy()/5, Math.min( (e.getEnergy()/4) + 0.2, power));

		
		long deltahittime;
		Point2D.Double point = new Point2D.Double();
		double head, chead, bspeed;
		double tmpx, tmpy;

		
		tmpx = enemy.getX();
		tmpy = enemy.getY();
		head = e.getHeadingRadians();
		chead = head - oldHeading;
		oldHeading = head;
		point.setLocation( tmpx, tmpy);
		deltahittime = 0;
		do {
			tmpx += Math.sin( head) * e.getVelocity();
			tmpy +=	Math.cos( head) * e.getVelocity();
			head += chead;
			deltahittime++;
			// if position not in field, adapt
			if (!fireField.contains( tmpx, tmpy)) {
				bspeed = point.distance( myPosition) / deltahittime;
				power = Math.max( Math.min( (20 - bspeed) / 3.0, 3.0), 0.1);
				break;
			}
			point.setLocation( tmpx, tmpy);
		} while ( (int)Math.round( (point.distance( myPosition) - 18) / Rules.getBulletSpeed( power)) > deltahittime);
		point.setLocation( Math.min( fieldWidth  - 34, Math.max( 34, tmpx)), Math.min( fieldHeight - 34, Math.max( 34, tmpy)));
				
		
		if ((getGunHeat() == 0.0) && (getGunTurnRemaining() == 0.0) && (power > 0.0) && (getEnergy() > 0.1)) {
			
			setFire( power);
			fired = true;
		}
		if ((fired == true) || (currTime >= (lastscan + RADARTIMEOUT))) {
			setTurnRadarRightRadians( Double.NEGATIVE_INFINITY * Utils.normalRelativeAngle( absBearing - getRadarHeadingRadians()));
			lastscan = currTime;
		}
		else {
			
			setTurnRadarRightRadians( 2.2 * Utils.normalRelativeAngle( absBearing - getRadarHeadingRadians()));
		}
		
		setTurnGunRightRadians( Utils.normalRelativeAngle(((Math.PI / 2) - Math.atan2( point.y - myPosition.getY(), point.x - myPosition.getX())) - getGunHeadingRadians()));
	}

	public void onRobotDeath(RobotDeathEvent e)
	{
		
		if (enemies.remove(e.getName()) == currentTarget) {
			currentTarget = null;
			setTurnRadarRight( Double.POSITIVE_INFINITY);
		}
	}

	public void doMeleeMovement()
	{
		double travel;
		double goalrisk;
		double testrisk;
		double temprisk;
		double angle;
		Point2D testPoint = new Point2D.Double();
		Point2D tempPoint = new Point2D.Double();

		myPosition.setLocation( getX(), getY());
		if (currentTarget != null)
		{
			
			travel = Math.max( 60, myPosition.distance(currentTarget) * 0.35);
			temprisk = goalrisk = calcRisk( myGoalPosition, getHeadingRadians());			

			for (angle = 0; angle < Math.PI*2; angle += Math.PI/36) {
				testPoint.setLocation( myPosition.getX() + Math.sin( angle) * travel, myPosition.getY() + Math.cos( angle) * travel);
				if ((playField.contains( testPoint)) && ((testrisk = calcRisk( testPoint, angle)) < temprisk )) {
					tempPoint.setLocation( testPoint);
					temprisk = testrisk;
				}
			}
			
			if (temprisk < (goalrisk * 0.9)) {
				myGoalPosition.setLocation( tempPoint);
				goalrisk = temprisk;
			}

			
			angle = Utils.normalRelativeAngle( Math.atan2( myPosition.getX() - myGoalPosition.getX(), myPosition.getY() - myGoalPosition.getY()) - getHeadingRadians());
			setTurnRightRadians( Math.atan( Math.tan( angle)));
			setMaxVelocity( 10 - (4 * Math.abs(getTurnRemainingRadians())));
			setAhead( (angle == Math.atan( Math.tan( angle)) ? -1.0 : 1.0) * travel);			
		}
	}

	private double calcRisk(Point2D point, double movang)
	{
		

		double totrisk;
		double botrisk;
		double botangle;
		Collection enemySet;
		Iterator it = (enemySet = enemies.values()).iterator();

		
		totrisk = (8 * (getOthers() - 1)) / point.distanceSq( centerPoint);
		totrisk += 3 / point.distanceSq( 0, 0);
		totrisk += 3 / point.distanceSq( fieldWidth, 0);
		totrisk += 3 / point.distanceSq( 0, fieldHeight);
		totrisk += 3 / point.distanceSq( fieldWidth, fieldHeight);

		do
		{
			EnemyInfo e = (EnemyInfo)it.next();
			
		    botangle = Utils.normalRelativeAngle( Math.atan2( e.getX() - point.getX(), e.getY() - point.getY()) - movang);
			
			botrisk = 100;
			
			if (e == currentTarget) botrisk += 40;
			
			botrisk *= (1.0 + ((1 - (Math.abs(Math.sin(botangle)))) + Math.abs(Math.cos(botangle))) / 2);

			
			totrisk += (botrisk / point.distanceSq( e));
		}
		while (it.hasNext());
		
		totrisk += (Math.random() * 0.5) / point.distanceSq( myPosition);
		return totrisk;
	}

}


class EnemyInfo extends Point2D.Double
{
	double attrac;
}
