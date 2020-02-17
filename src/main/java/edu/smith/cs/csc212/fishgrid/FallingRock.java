package edu.smith.cs.csc212.fishgrid;

public class FallingRock extends Rock{
	
	/**
	 * A FallingRock knows what World it belongs to, because Rocks are WorldObjects
	 * and all WorldObjects know this.
	 * @param world The world itself.
	 */
	public FallingRock(World world) {
		// calls rock constructor
		super(world);
	}
	
	/**
	 * Provide step behavior for falling rocks!
	 */
	@Override
	public void step() {
		// Rocks move down if they can	
		this.moveDown();
	}
}
