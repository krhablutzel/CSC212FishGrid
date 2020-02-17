package edu.smith.cs.csc212.fishgrid;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * This class manages our model of gameplay: missing and found fish, etc.
 * @author jfoley
 *
 */
public class FishGame {
	/**
	 * This is the world in which the fish are missing. (It's mostly a List!).
	 */
	World world;
	
	/**
	 * The player (a Fish.COLORS[0]-colored fish) goes seeking their friends.
	 */
	Fish player;
	
	/**
	 * The home location.
	 */
	FishHome home;
	
	/**
	 * Number of rocks
	 */
	public static final int NUM_ROCKS = 10;
	
	/**
	 * These are the missing fish!
	 */
	List<Fish> missing;
	
	/**
	 * These are fish we've found!
	 */
	List<Fish> found;
	
	/**
	 * These are fish that have made it home/safe!
	 */
	List<Fish> safe;
	
	/**
	 * Number of steps!
	 */
	int stepsTaken;
	
	/**
	 * Score!
	 */
	int score;
	
	/**
	 * Create a FishGame of a particular size.
	 * @param w how wide is the grid?
	 * @param h how tall is the grid?
	 */
	public FishGame(int w, int h) {
		world = new World(w, h);
		
		missing = new ArrayList<Fish>();
		found = new ArrayList<Fish>();
		safe = new ArrayList<Fish>();
		
		// Add a home!
		home = world.insertFishHome();
		
		// Make the rocks! 50% chance of falling
		// Steal the random number generator from existing world object
		// TODO: Own Rand - Don't piggyback off the home rand  
		for (int i=0; i<NUM_ROCKS; i++) {
			if (home.rand.nextDouble() < 0.5) {
				world.insertFallingRockRandomly();
			} else {
				world.insertRockRandomly();	
			}
		}
		
		// Make the snail!
		world.insertSnailRandomly();
		
		// Make the player out of the 0th fish color.
		player = new Fish(0, world);
		// Start the player at "home".
		player.setPosition(home.getX(), home.getY());
		player.markAsPlayer();
		world.register(player);
		
		// Generate fish of all the colors but the first into the "missing" List.
		for (int ft = 1; ft < Fish.COLORS.length; ft++) {
			Fish friend = world.insertFishRandomly(ft);
			missing.add(friend);
		}		
	}
	
	
	/**
	 * How we tell if the game is over: if missingFishLeft() == 0.
	 * @return the size of the missing list.
	 */
	public int missingFishLeft() {
		return missing.size();
	}
	
	/**
	 * This method is how the Main app tells whether we're done.
	 * @return true if the player has won (or maybe lost?).
	 */
	public boolean gameOver() {
		// TODO(FishGrid) We want to bring the fish home before we win!
		return missing.isEmpty() && found.isEmpty();
	}

	/**
	 * Update positions of everything (the user has just pressed a button).
	 */
	public void step() {
		// Keep track of how long the game has run.
		this.stepsTaken += 1;
				
		// All the player's various interactions with the world
		playerInteracts();
		
		// Make sure missing fish *do* something.
		wanderMissingFish();
		
		// When fish get added to "found" they will follow the player around.
		World.objectsFollow(player, found);	
		
		// Found fish have a chance of wandering
		wanderFollowFish();
		
		// Random chance of a heart appearing on the board
		hearts();
		
		// Step any world-objects that run themselves.
		world.stepAll();
	}
	
	/**
	 * Player interacts with rest of the world
	 */
	private void playerInteracts() {
		// These are all the objects in the world in the same cell as the player.
		List<WorldObject> overlap = this.player.findSameCell();
		// The player is there, too, let's skip them.
		overlap.remove(this.player);
		
		// If we find a fish, remove it from missing.
		// If we return home, found fish are safe.
		// And if we find a heart, collect it.
		for (WorldObject wo : overlap) {
			// It is missing if it's in our missing list.
			if (missing.contains(wo)) {
				if (!(wo instanceof Fish)) {
					throw new AssertionError("wo must be a Fish since it was in missing!");
				}
				// Convince Java it's a Fish (we know it is!)
				Fish justFound = (Fish) wo;
				
				// Add to found; take from missing
				found.add(justFound);
				missing.remove(justFound);
				
				// Increase score when you find a fish!
				score += justFound.points;
				
			} else if (wo instanceof FishHome) {
				// Found fish are safe/home
				safe.addAll(found); // https://www.geeksforgeeks.org/java-util-arraylist-addall-method-java/
				
				// Remove fish from world and from found list
				for (Fish friend : found) {
					friend.remove();
				}
				found.removeAll(found); // https://www.geeksforgeeks.org/arraylist-removeall-method-in-java-with-examples/
				
			} else if (wo instanceof Heart) {
				// Remove heart from world
				wo.remove();
				// Increase score for collecting heart!
				score += Heart.points;
			}
		}
	}
	
	/**
	 * Call moveRandomly() on all of the missing fish to make them seem alive.
	 */
	private void wanderMissingFish() {
		Random rand = ThreadLocalRandom.current();
		List<Fish> saved = new ArrayList<>();
		for (Fish lost : missing) {
			// Move
			double moveProb;
			if (lost.fastScared) {
				// fastScared fish move randomly 80% of the time
				moveProb = 0.8;
			} else {
				// not fastScared fish move randomly 30% of the time
				moveProb = 0.3;
			}
			if (rand.nextDouble() < moveProb) {
				lost.moveRandomly(); 
			}
			
			// Check for home or heart at new location
			List<WorldObject> overlap = lost.findSameCell();
			overlap.remove(lost);
			for (WorldObject wo: overlap) {
				if (wo instanceof FishHome) {
					// note that fish is home
					// after loop through missing, THEN transfer from missing to safe
					saved.add(lost);
		
					// remove from world
					lost.remove();
				} else if (wo instanceof Heart) {
					// Remove heart from world
					wo.remove();
				}
			}
		}		
		// move saved fish from missing to safe
		safe.addAll(saved);
		missing.removeAll(saved);
	}
	
	/**
	 * Found fish have a chance of wandering off
	 */
	private void wanderFollowFish() {
		// do a thing
		// is there a difference between commit and push vs commit? Definitely didn't do the wrong one
	}

	/**
	 * This gets a click on the grid. We want it to destroy rocks that ruin the game.
	 * @param x - the x-tile.
	 * @param y - the y-tile.
	 */
	public void click(int x, int y) {
		// TODO(FishGrid) use this print to debug your World.canSwim changes!
		System.out.println("Clicked on: "+x+","+y+ " world.canSwim(player,...)="+world.canSwim(player, x, y));
		List<WorldObject> atPoint = world.find(x, y);
		
		// remove clicked rocks/fallingRocks
		for (WorldObject objAtPoint : atPoint) {
			// Found instanceof at https://www.geeksforgeeks.org/java-instanceof-and-its-applications/
			if (objAtPoint instanceof Rock) {
				objAtPoint.remove();
			}
		}
	}
	
	/**
	 * This has a random chance every step of adding a heart to the game.
	 */
	public void hearts() {
		double heartChance = 0.03;
		// TODO: Fix Rand here too - Don't piggyback off the player's rand 
		if (this.player.rand.nextDouble() < heartChance) {
			world.insertHeartRandomly();
		}
	}
	
}
