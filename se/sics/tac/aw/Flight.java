package se.sics.tac.aw;
import se.sics.tac.util.ArgEnumerator;
import java.util.logging.*;

public class Flight
{
	private int day;
	private int direction;
	private float price;

	public Flight(int d, int dir, float p)
	{
		this.day = d;
		this.direction = dir;
		this.price = p;
	}

	public int day()
	{
		return this.day;
	}

	public int direction()
	{
		return this.direction;
	}

	public float price()
	{
		return this.price;
	}
}